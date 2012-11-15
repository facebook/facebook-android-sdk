/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import com.facebook.android.Util;
import com.facebook.model.GraphObject;
import com.facebook.internal.Validate;
import org.json.JSONException;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows some customization of sdk behavior.
 */
public final class Settings {
    private static final HashSet<LoggingBehaviors> loggingBehaviors = new HashSet<LoggingBehaviors>();
    private static volatile Executor executor;
    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 128;
    private static final int DEFAULT_KEEP_ALIVE = 1;
    private static final Object LOCK = new Object();

    private static final Uri ATTRIBUTION_ID_CONTENT_URI =
            Uri.parse("content://com.facebook.katana.provider.AttributionIdProvider");
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";

    private static final String ATTRIBUTION_PREFERENCES = "com.facebook.sdk.attributionTracking";
    private static final String PUBLISH_ACTIVITY_PATH = "%s/activities";
    private static final String MOBILE_INSTALL_EVENT = "MOBILE_APP_INSTALL";
    private static final String SUPPORTS_ATTRIBUTION = "supports_attribution";
    private static final String APPLICATION_FIELDS = "fields";
    private static final String ANALYTICS_EVENT = "event";
    private static final String ATTRIBUTION_KEY = "attribution";

    private static final BlockingQueue<Runnable> DEFAULT_WORK_QUEUE = new LinkedBlockingQueue<Runnable>(10);

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "FacebookSdk #" + counter.incrementAndGet());
        }
    };


    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Returns the types of extended logging that are currently enabled.
     *
     * @return a set containing enabled logging behaviors
     */
    public static final Set<LoggingBehaviors> getLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            return Collections.unmodifiableSet(new HashSet<LoggingBehaviors>(loggingBehaviors));
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Enables a particular extended logging in the sdk.
     *
     * @param behavior
     *          The LoggingBehavior to enable
     */
    public static final void addLoggingBehavior(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.add(behavior);
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Disables a particular extended logging behavior in the sdk.
     *
     * @param behavior
     *          The LoggingBehavior to disable
     */
    public static final void removeLoggingBehavior(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            loggingBehaviors.remove(behavior);
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Disables all extended logging behaviors.
     */
    public static final void clearLoggingBehaviors() {
        synchronized (loggingBehaviors) {
            loggingBehaviors.clear();
        }
    }

    /**
     * Certain logging behaviors are available for debugging beyond those that should be
     * enabled in production.
     *
     * Checks if a particular extended logging behavior is enabled.
     *
     * @param behavior
     *          The LoggingBehavior to check
     * @return whether behavior is enabled
     */
    public static final boolean isLoggingBehaviorEnabled(LoggingBehaviors behavior) {
        synchronized (loggingBehaviors) {
            return loggingBehaviors.contains(behavior);
        }
    }

    /**
     * Returns the Executor used by the SDK for non-AsyncTask background work.
     *
     * By default this uses AsyncTask Executor via reflection if the API level is high enough.
     * Otherwise this creates a new Executor with defaults similar to those used in AsyncTask.
     *
     * @return an Executor used by the SDK.  This will never be null.
     */
    public static Executor getExecutor() {
        synchronized (LOCK) {
            if (Settings.executor == null) {
                Executor executor = getAsyncTaskExecutor();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE,
                            DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS, DEFAULT_WORK_QUEUE, DEFAULT_THREAD_FACTORY);
                }
                Settings.executor = executor;
            }
        }
        return Settings.executor;
    }

    /**
     * Sets the Executor used by the SDK for non-AsyncTask background work.
     *
     * @param executor
     *          the Executor to use; must not be null.
     */
    public static void setExecutor(Executor executor) {
        Validate.notNull(executor, "executor");
        synchronized (LOCK) {
            Settings.executor = executor;
        }
    }

    private static Executor getAsyncTaskExecutor() {
        Field executorField = null;
        try {
            executorField = AsyncTask.class.getField("THREAD_POOL_EXECUTOR");
        } catch (NoSuchFieldException e) {
            return null;
        }

        if (executorField == null) {
            return null;
        }

        Object executorObject = null;
        try {
            executorObject = executorField.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }

        if (executorObject == null) {
            return null;
        }

        if (!(executorObject instanceof Executor)) {
            return null;
        }

        return (Executor) executorObject;
    }

    public static void publishInstallAsync(final Context context, final String applicationId) {
        // grab the application context ahead of time, since we will return to the caller immediately.
        final Context applicationContext = context.getApplicationContext();
        Settings.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Settings.publishInstallAndWait(applicationContext, applicationId);
            }
        });
    }

    /**
     * Manually publish install attribution to the Facebook graph.  Internally handles tracking repeat calls to prevent
     * multiple installs being published to the graph.
     * @param context
     * @return returns false on error.  Applications should retry until true is returned.  Safe to call again after
     * true is returned.
     */
    public static boolean publishInstallAndWait(final Context context, final String applicationId) {
        try {
            if (applicationId == null) {
                return false;
            }
            String attributionId = Settings.getAttributionId(context.getContentResolver());
            SharedPreferences preferences = context.getSharedPreferences(ATTRIBUTION_PREFERENCES, Context.MODE_PRIVATE);
            String pingKey = applicationId+"ping";
            long lastPing = preferences.getLong(pingKey, 0);
            if (lastPing == 0 && attributionId != null) {
                Bundle supportsAttributionParams = new Bundle();
                supportsAttributionParams.putString(APPLICATION_FIELDS, SUPPORTS_ATTRIBUTION);
                Request pingRequest = Request.newGraphPathRequest(null, applicationId, null);
                pingRequest.setParameters(supportsAttributionParams);

                GraphObject supportResponse = pingRequest.executeAndWait().getGraphObject();
                Object doesSupportAttribution = supportResponse.getProperty(SUPPORTS_ATTRIBUTION);

                if (!(doesSupportAttribution instanceof Boolean)) {
                    throw new JSONException(String.format(
                            "%s contains %s instead of a Boolean", SUPPORTS_ATTRIBUTION, doesSupportAttribution));
                }

                if ((Boolean)doesSupportAttribution) {
                    GraphObject publishParams = GraphObject.Factory.create();
                    publishParams.setProperty(ANALYTICS_EVENT, MOBILE_INSTALL_EVENT);
                    publishParams.setProperty(ATTRIBUTION_KEY, attributionId);

                    String publishUrl = String.format(PUBLISH_ACTIVITY_PATH, applicationId);

                    Request publishRequest = Request.newPostRequest(null, publishUrl, publishParams, null);
                    publishRequest.executeAndWait();

                    // denote success since no error threw from the post.
                    SharedPreferences.Editor editor = preferences.edit();
                    lastPing = System.currentTimeMillis();
                    editor.putLong(pingKey, lastPing);
                    editor.commit();
                }
            }
            return lastPing != 0;
        } catch (Exception e) {
            // if there was an error, fall through to the failure case.
            Util.logd("Facebook-publish", e.getMessage());
        }
        return false;
    }

    public static String getAttributionId(ContentResolver contentResolver) {
        String [] projection = {ATTRIBUTION_ID_COLUMN_NAME};
        Cursor c = contentResolver.query(ATTRIBUTION_ID_CONTENT_URI, projection, null, null, null);
        if (c == null || !c.moveToFirst()) {
            return null;
        }
        String attributionId = c.getString(c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME));
        c.close();
        return attributionId;
    }
}
