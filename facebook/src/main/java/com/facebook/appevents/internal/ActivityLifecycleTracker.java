/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.internal;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.Utility;

import junit.framework.Assert;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ActivityLifecycleTracker {
    private static final String TAG = ActivityLifecycleTracker.class.getCanonicalName();
    private static final String INCORRECT_IMPL_WARNING = "Unexpected activity pause without a " +
            "matching activity resume. Logging data may be incorrect. Make sure you call " +
            "activateApp from your Application's onCreate method";
    private static final long INTERRUPTION_THRESHOLD_MILLISECONDS = 1000;
    private static final ScheduledExecutorService singleThreadExecutor =
            Executors.newSingleThreadScheduledExecutor();
    private static volatile ScheduledFuture currentFuture;
    private static AtomicInteger foregroundActivityCount = new AtomicInteger(0);
    // This member should only be changed or updated when executing on the singleThreadExecutor.
    private static volatile SessionInfo currentSession;
    private static AtomicBoolean tracking = new AtomicBoolean(false);
    private static String appId;
    private static long currentActivityAppearTime;

    public static void startTracking(Application application, final String appId) {
        if (!tracking.compareAndSet(false, true)) {
            return;
        }

        ActivityLifecycleTracker.appId = appId;

        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(
                            final Activity activity,
                            Bundle savedInstanceState) {
                        AppEventUtility.assertIsMainThread();
                        ActivityLifecycleTracker.onActivityCreated(activity);
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {}

                    @Override
                    public void onActivityResumed(final Activity activity) {
                        AppEventUtility.assertIsMainThread();
                        ActivityLifecycleTracker.onActivityResumed(activity);
                    }

                    @Override
                    public void onActivityPaused(final Activity activity) {
                        AppEventUtility.assertIsMainThread();
                        ActivityLifecycleTracker.onActivityPaused(activity);
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                        AppEventsLogger.onContextStop();
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                });
    }

    public static boolean isTracking() {
        return tracking.get();
    }

    public static UUID getCurrentSessionGuid() {
        return currentSession != null ? currentSession.getSessionId() : null;
    }

    // Public in order to allow unity sdk to correctly log app events
    public static void onActivityCreated(final Activity activity) {
        final long currentTime = System.currentTimeMillis();
        Runnable handleActivityCreate = new Runnable() {
            @Override
            public void run() {
                if (currentSession == null) {
                    Context applicationContext = activity.getApplicationContext();
                    String activityName = Utility.getActivityName(activity);

                    SessionInfo lastSession =
                            SessionInfo.getStoredSessionInfo();
                    if (lastSession != null) {
                        SessionLogger.logDeactivateApp(
                                applicationContext,
                                activityName,
                                lastSession,
                                appId);
                    }

                    currentSession = new SessionInfo(currentTime, null);
                    SourceApplicationInfo sourceApplicationInfo =
                            SourceApplicationInfo.Factory.create(activity);
                    currentSession.setSourceApplicationInfo(sourceApplicationInfo);
                    SessionLogger.logActivateApp(
                            applicationContext,
                            activityName,
                            sourceApplicationInfo,
                            appId);
                }
            }
        };
        singleThreadExecutor.execute(handleActivityCreate);
    }

    // Public in order to allow unity sdk to correctly log app events
    public static void onActivityResumed(final Activity activity) {
        foregroundActivityCount.incrementAndGet();
        cancelCurrentTask();
        final long currentTime = System.currentTimeMillis();
        ActivityLifecycleTracker.currentActivityAppearTime = currentTime;
        Runnable handleActivityResume = new Runnable() {
            @Override
            public void run() {
                Context applicationContext = activity.getApplicationContext();
                String activityName = Utility.getActivityName(activity);

                if (currentSession == null) {
                    currentSession = new SessionInfo(currentTime, null);
                    SessionLogger.logActivateApp(
                            applicationContext,
                            activityName,
                            null,
                            appId);
                } else if (currentSession.getSessionLastEventTime() != null) {
                    long suspendTime =
                            currentTime - currentSession.getSessionLastEventTime();
                    if (suspendTime > getSessionTimeoutInSeconds() * 1000) {
                        // We were suspended for a significant amount of time.
                        // Count this as a new session and log the old session
                        SessionLogger.logDeactivateApp(
                                applicationContext,
                                activityName,
                                currentSession,
                                appId);
                        SessionLogger.logActivateApp(
                                applicationContext,
                                activityName,
                                null,
                                appId);
                        currentSession = new SessionInfo(currentTime, null);
                    } else if (suspendTime > INTERRUPTION_THRESHOLD_MILLISECONDS) {
                        currentSession.incrementInterruptionCount();
                    }
                }

                currentSession.setSessionLastEventTime(currentTime);
                currentSession.writeSessionToDisk();
            }
        };

        singleThreadExecutor.execute(handleActivityResume);
    }

    private static void onActivityPaused(Activity activity) {
        int count = foregroundActivityCount.decrementAndGet();
        if (count < 0) {
            // Our ref count can be off if a developer doesn't call activate
            // app from the Application's onCreate method.
            foregroundActivityCount.set(0);

            Log.w(TAG, INCORRECT_IMPL_WARNING);
        }

        cancelCurrentTask();
        final long currentTime = System.currentTimeMillis();

        // Pull out this information now to avoid holding a reference to the activity
        final Context applicationContext = activity.getApplicationContext();
        final String activityName = Utility.getActivityName(activity);

        Runnable handleActivityPaused = new Runnable() {
            @Override
            public void run() {
                if (currentSession == null) {
                    // This can happen if a developer doesn't call activate
                    // app from the Application's onCreate method
                    currentSession = new SessionInfo(currentTime, null);
                }

                currentSession.setSessionLastEventTime(currentTime);
                if (foregroundActivityCount.get() <= 0) {
                    // Schedule check to see if we still have 0 foreground
                    // activities in our set time. This indicates that the app has
                    // been backgrounded
                    Runnable task = new Runnable() {
                        @Override
                        public void run() {
                            if (foregroundActivityCount.get() <= 0) {
                                SessionLogger.logDeactivateApp(
                                        applicationContext,
                                        activityName,
                                        currentSession,
                                        appId);
                                SessionInfo.clearSavedSessionFromDisk();
                                currentSession = null;
                            }

                            currentFuture = null;
                        }
                    };
                    currentFuture = singleThreadExecutor.schedule(
                            task,
                            getSessionTimeoutInSeconds(),
                            TimeUnit.SECONDS);

                }

                long appearTime = ActivityLifecycleTracker.currentActivityAppearTime;
                long timeSpentOnActivityInSeconds =  appearTime > 0
                        ? (currentTime - appearTime) / 1000
                        : 0;
                AutomaticAnalyticsLogger.logActivityTimeSpentEvent(
                        applicationContext,
                        appId,
                        activityName,
                        timeSpentOnActivityInSeconds
                );

                currentSession.writeSessionToDisk();
            }
        };
        singleThreadExecutor.execute(handleActivityPaused);
    }

    private static int getSessionTimeoutInSeconds() {
        Utility.FetchedAppSettings settings =
                Utility.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId());
        if (settings == null) {
            return Constants.getDefaultAppEventsSessionTimeoutInSeconds();
        }

        return settings.getSessionTimeoutInSeconds();
    }

    private static void cancelCurrentTask() {
        if (currentFuture != null) {
            currentFuture.cancel(false);
        }

        currentFuture = null;
    }
}
