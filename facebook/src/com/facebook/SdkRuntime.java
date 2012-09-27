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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.AsyncTask;

/**
 * Allows some customization of sdk behavior.
 */
public final class SdkRuntime {
    static final String LOG_TAG_BASE = "FacebookSDK.";
    private static final HashSet<LoggingBehaviors> loggingBehaviors = new HashSet<LoggingBehaviors>();
    private static volatile Executor executor;
    private static final int DEFAULT_CORE_POOL_SIZE = 5;
    private static final int DEFAULT_MAXIMUM_POOL_SIZE = 128;
    private static final int DEFAULT_KEEP_ALIVE = 1;
    private static final Object LOCK = new Object();

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
            if (SdkRuntime.executor == null) {
                Executor executor = getAsyncTaskExecutor();
                if (executor == null) {
                    executor = new ThreadPoolExecutor(DEFAULT_CORE_POOL_SIZE, DEFAULT_MAXIMUM_POOL_SIZE,
                            DEFAULT_KEEP_ALIVE, TimeUnit.SECONDS, DEFAULT_WORK_QUEUE, DEFAULT_THREAD_FACTORY);
                }
                SdkRuntime.executor = executor;
            }
        }
        return SdkRuntime.executor;
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
            SdkRuntime.executor = executor;
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
}
