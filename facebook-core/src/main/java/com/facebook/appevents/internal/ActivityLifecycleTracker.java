/*
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
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.appevents.aam.MetadataIndexer;
import com.facebook.appevents.codeless.CodelessManager;
import com.facebook.appevents.suggestedevents.SuggestedEventsManager;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ActivityLifecycleTracker {
  private static final String TAG = ActivityLifecycleTracker.class.getCanonicalName();
  private static final String INCORRECT_IMPL_WARNING =
      "Unexpected activity pause without a "
          + "matching activity resume. Logging data may be incorrect. Make sure you call "
          + "activateApp from your Application's onCreate method";
  private static final long INTERRUPTION_THRESHOLD_MILLISECONDS = 1000;
  private static final ScheduledExecutorService singleThreadExecutor =
      Executors.newSingleThreadScheduledExecutor();
  private static volatile ScheduledFuture currentFuture;
  private static final Object currentFutureLock = new Object();
  private static AtomicInteger foregroundActivityCount = new AtomicInteger(0);
  // This member should only be changed or updated when executing on the singleThreadExecutor.
  private static volatile SessionInfo currentSession;
  private static AtomicBoolean tracking = new AtomicBoolean(false);
  private static String appId;
  private static long currentActivityAppearTime;

  private static int activityReferences = 0;

  private static WeakReference<Activity> currActivity;

  public static void startTracking(Application application, final String appId) {
    if (!tracking.compareAndSet(false, true)) {
      return;
    }

    FeatureManager.checkFeature(
        FeatureManager.Feature.CodelessEvents,
        new FeatureManager.Callback() {
          @Override
          public void onCompleted(boolean enabled) {
            if (enabled) {
              CodelessManager.enable();
            } else {
              CodelessManager.disable();
            }
          }
        });

    ActivityLifecycleTracker.appId = appId;

    application.registerActivityLifecycleCallbacks(
        new Application.ActivityLifecycleCallbacks() {
          @Override
          public void onActivityCreated(final Activity activity, Bundle savedInstanceState) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityCreated");
            AppEventUtility.assertIsMainThread();
            ActivityLifecycleTracker.onActivityCreated(activity);
          }

          @Override
          public void onActivityStarted(Activity activity) {
            ActivityLifecycleTracker.activityReferences++;
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityStarted");
          }

          @Override
          public void onActivityResumed(final Activity activity) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityResumed");
            AppEventUtility.assertIsMainThread();
            ActivityLifecycleTracker.onActivityResumed(activity);
          }

          @Override
          public void onActivityPaused(final Activity activity) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityPaused");
            AppEventUtility.assertIsMainThread();
            ActivityLifecycleTracker.onActivityPaused(activity);
          }

          @Override
          public void onActivityStopped(Activity activity) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityStopped");
            AppEventsLogger.onContextStop();
            ActivityLifecycleTracker.activityReferences--;
          }

          @Override
          public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivitySaveInstanceState");
          }

          @Override
          public void onActivityDestroyed(Activity activity) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "onActivityDestroyed");
            ActivityLifecycleTracker.onActivityDestroyed(activity);
          }
        });
  }

  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public static boolean isInBackground() {
    return 0 == activityReferences;
  }

  public static boolean isTracking() {
    return tracking.get();
  }

  public static UUID getCurrentSessionGuid() {
    return currentSession != null ? currentSession.getSessionId() : null;
  }

  // Public in order to allow unity sdk to correctly log app events
  public static void onActivityCreated(Activity activity) {
    singleThreadExecutor.execute(
        new Runnable() {
          @Override
          public void run() {
            if (currentSession == null) {
              currentSession = SessionInfo.getStoredSessionInfo();
            }
          }
        });
  }

  // Public in order to allow unity sdk to correctly log app events
  public static void onActivityResumed(Activity activity) {
    currActivity = new WeakReference<>(activity);
    foregroundActivityCount.incrementAndGet();
    cancelCurrentTask();
    final long currentTime = System.currentTimeMillis();
    ActivityLifecycleTracker.currentActivityAppearTime = currentTime;
    final String activityName = Utility.getActivityName(activity);

    CodelessManager.onActivityResumed(activity);
    MetadataIndexer.onActivityResumed(activity);
    SuggestedEventsManager.trackActivity(activity);

    final Context appContext = activity.getApplicationContext();
    Runnable handleActivityResume =
        new Runnable() {
          @Override
          public void run() {
            if (currentSession == null) {
              currentSession = new SessionInfo(currentTime, null);
              SessionLogger.logActivateApp(activityName, null, appId, appContext);
            } else if (currentSession.getSessionLastEventTime() != null) {
              long suspendTime = currentTime - currentSession.getSessionLastEventTime();
              if (suspendTime > getSessionTimeoutInSeconds() * 1000) {
                // We were suspended for a significant amount of time.
                // Count this as a new session and log the old session
                SessionLogger.logDeactivateApp(activityName, currentSession, appId);
                SessionLogger.logActivateApp(activityName, null, appId, appContext);
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

    final String activityName = Utility.getActivityName(activity);
    CodelessManager.onActivityPaused(activity);
    Runnable handleActivityPaused =
        new Runnable() {
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
              Runnable task =
                  new Runnable() {
                    @Override
                    public void run() {
                      if (currentSession == null) {
                        currentSession = new SessionInfo(currentTime, null);
                      }
                      if (foregroundActivityCount.get() <= 0) {
                        SessionLogger.logDeactivateApp(activityName, currentSession, appId);
                        SessionInfo.clearSavedSessionFromDisk();
                        currentSession = null;
                      }

                      synchronized (currentFutureLock) {
                        currentFuture = null;
                      }
                    }
                  };

              synchronized (currentFutureLock) {
                currentFuture =
                    singleThreadExecutor.schedule(
                        task, getSessionTimeoutInSeconds(), TimeUnit.SECONDS);
              }
            }

            long appearTime = ActivityLifecycleTracker.currentActivityAppearTime;
            long timeSpentOnActivityInSeconds =
                appearTime > 0 ? (currentTime - appearTime) / 1000 : 0;
            AutomaticAnalyticsLogger.logActivityTimeSpentEvent(
                activityName, timeSpentOnActivityInSeconds);

            currentSession.writeSessionToDisk();
          }
        };
    singleThreadExecutor.execute(handleActivityPaused);
  }

  private static void onActivityDestroyed(Activity activity) {
    CodelessManager.onActivityDestroyed(activity);
  }

  private static int getSessionTimeoutInSeconds() {
    FetchedAppSettings settings =
        FetchedAppSettingsManager.getAppSettingsWithoutQuery(FacebookSdk.getApplicationId());
    if (settings == null) {
      return Constants.getDefaultAppEventsSessionTimeoutInSeconds();
    }

    return settings.getSessionTimeoutInSeconds();
  }

  private static void cancelCurrentTask() {
    synchronized (currentFutureLock) {
      if (currentFuture != null) {
        currentFuture.cancel(false);
      }

      currentFuture = null;
    }
  }

  @Nullable
  public static Activity getCurrentActivity() {
    return currActivity != null ? currActivity.get() : null;
  }
}
