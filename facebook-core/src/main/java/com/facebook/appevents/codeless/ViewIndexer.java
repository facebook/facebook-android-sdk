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

package com.facebook.appevents.codeless;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.codeless.internal.Constants;
import com.facebook.appevents.codeless.internal.UnityReflection;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.internal.InternalSettings;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@AutoHandleExceptions
public class ViewIndexer {
  private static final String TAG = ViewIndexer.class.getCanonicalName();
  private static final String SUCCESS = "success";
  private static final String TREE_PARAM = "tree";
  private static final String APP_VERSION_PARAM = "app_version";
  private static final String PLATFORM_PARAM = "platform";
  private static final String REQUEST_TYPE = "request_type";

  private final Handler uiThreadHandler;
  private WeakReference<Activity> activityReference;
  private Timer indexingTimer;
  private String previousDigest;
  private static ViewIndexer instance;

  public ViewIndexer(Activity activity) {
    activityReference = new WeakReference<>(activity);
    previousDigest = null;
    uiThreadHandler = new Handler(Looper.getMainLooper());
    instance = this;
  }

  public void schedule() {
    final TimerTask indexingTask =
        new TimerTask() {
          @Override
          public void run() {
            try {
              final Activity activity = activityReference.get();
              final View rootView = AppEventUtility.getRootView(activity);
              if (null == activity || null == rootView) {
                return;
              }
              final String activityName = activity.getClass().getSimpleName();

              if (!CodelessManager.getIsAppIndexingEnabled()) {
                return;
              }

              if (InternalSettings.isUnityApp()) {
                UnityReflection.captureViewHierarchy();
                return;
              }

              final FutureTask<String> screenshotFuture =
                  new FutureTask<>(new ScreenshotTaker(rootView));
              uiThreadHandler.post(screenshotFuture);

              String screenshot = "";
              try {
                screenshot = screenshotFuture.get(1, TimeUnit.SECONDS);
              } catch (Exception e) {
                Log.e(TAG, "Failed to take screenshot.", e);
              }

              JSONObject viewTree = new JSONObject();

              try {
                viewTree.put("screenname", activityName);
                viewTree.put("screenshot", screenshot);

                JSONArray viewArray = new JSONArray();
                JSONObject rootViewInfo = ViewHierarchy.getDictionaryOfView(rootView);
                viewArray.put(rootViewInfo);
                viewTree.put("view", viewArray);
              } catch (JSONException e) {
                Log.e(TAG, "Failed to create JSONObject");
              }

              String tree = viewTree.toString();
              sendToServer(tree);
            } catch (Exception e) {
              Log.e(TAG, "UI Component tree indexing failure!", e);
            }
          }
        };

    try {
      FacebookSdk.getExecutor()
          .execute(
              new Runnable() {
                @Override
                public void run() {
                  try {
                    if (indexingTimer != null) {
                      indexingTimer.cancel();
                    }
                    previousDigest = null;
                    indexingTimer = new Timer();
                    indexingTimer.scheduleAtFixedRate(
                        indexingTask, 0, Constants.APP_INDEXING_SCHEDULE_INTERVAL_MS);
                  } catch (Exception e) {
                    Log.e(TAG, "Error scheduling indexing job", e);
                  }
                }
              });
    } catch (RejectedExecutionException e) {
      Log.e(TAG, "Error scheduling indexing job", e);
    }
  }

  public void unschedule() {
    final Activity activity = activityReference.get();
    if (null == activity) {
      return;
    }

    if (indexingTimer != null) {
      try {
        indexingTimer.cancel();
        indexingTimer = null;
      } catch (Exception e) {
        Log.e(TAG, "Error unscheduling indexing job", e);
      }
    }
  }

  public static void sendToServerUnityInstance(final String tree) {
    if (null == instance) {
      return;
    }
    instance.sendToServerUnity(tree);
  }

  @Deprecated
  public void sendToServerUnity(final String tree) {
    instance.sendToServer(tree);
  }

  @VisibleForTesting
  String getPreviousDigest() {
    return previousDigest;
  }

  private void sendToServer(final String tree) {
    FacebookSdk.getExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                final String currentDigest = Utility.md5hash(tree);
                final AccessToken accessToken = AccessToken.getCurrentAccessToken();
                if (currentDigest != null && currentDigest.equals(previousDigest)) {
                  return;
                }
                GraphRequest request =
                    buildAppIndexingRequest(
                        tree, accessToken, FacebookSdk.getApplicationId(), Constants.APP_INDEXING);
                processRequest(request, currentDigest);
              }
            });
  }

  void processRequest(@Nullable GraphRequest request, String currentDigest) {
    if (request == null) {
      return;
    }

    GraphResponse res = request.executeAndWait();
    try {
      JSONObject jsonRes = res.getJSONObject();
      if (jsonRes != null) {
        if ("true".equals(jsonRes.optString(SUCCESS))) {
          Logger.log(
              LoggingBehavior.APP_EVENTS, TAG, "Successfully send UI component tree to server");
          previousDigest = currentDigest;
        }

        if (jsonRes.has(Constants.APP_INDEXING_ENABLED)) {
          Boolean appIndexingEnabled = jsonRes.getBoolean(Constants.APP_INDEXING_ENABLED);
          CodelessManager.updateAppIndexing(appIndexingEnabled);
        }
      } else {
        Log.e(TAG, "Error sending UI component tree to Facebook: " + res.getError());
      }
    } catch (JSONException e) {
      Log.e(TAG, "Error decoding server response.", e);
    }
  }

  @Nullable
  @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
  public static GraphRequest buildAppIndexingRequest(
      final String appIndex,
      final AccessToken accessToken,
      final String appId,
      final String requestType) {
    if (appIndex == null) {
      return null;
    }

    final GraphRequest postRequest =
        GraphRequest.newPostRequest(
            accessToken, String.format(Locale.US, "%s/app_indexing", appId), null, null);

    Bundle requestParameters = postRequest.getParameters();
    if (requestParameters == null) {
      requestParameters = new Bundle();
    }

    requestParameters.putString(TREE_PARAM, appIndex);
    requestParameters.putString(APP_VERSION_PARAM, AppEventUtility.getAppVersion());
    requestParameters.putString(PLATFORM_PARAM, Constants.PLATFORM);
    requestParameters.putString(REQUEST_TYPE, requestType);
    if (requestType.equals(Constants.APP_INDEXING)) {
      requestParameters.putString(
          Constants.DEVICE_SESSION_ID, CodelessManager.getCurrentDeviceSessionID());
    }

    postRequest.setParameters(requestParameters);

    postRequest.setCallback(
        new GraphRequest.Callback() {
          @Override
          public void onCompleted(GraphResponse response) {
            Logger.log(LoggingBehavior.APP_EVENTS, TAG, "App index sent to FB!");
          }
        });

    return postRequest;
  }

  private static class ScreenshotTaker implements Callable<String> {
    private WeakReference<View> rootView;

    ScreenshotTaker(View rootView) {
      this.rootView = new WeakReference<>(rootView);
    }

    @Override
    public String call() {
      View view = this.rootView.get();
      if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
        return "";
      }
      Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.RGB_565);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      // TODO: T25009391, Support better screenshot image quality by using file attachment.
      bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream);
      return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP);
    }
  }
}
