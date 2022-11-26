/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.annotation.RestrictTo
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getExecutor
import com.facebook.GraphRequest
import com.facebook.GraphRequest.Companion.newPostRequest
import com.facebook.LoggingBehavior
import com.facebook.appevents.codeless.CodelessManager.getCurrentDeviceSessionID
import com.facebook.appevents.codeless.CodelessManager.getIsAppIndexingEnabled
import com.facebook.appevents.codeless.CodelessManager.updateAppIndexing
import com.facebook.appevents.codeless.internal.Constants
import com.facebook.appevents.codeless.internal.UnityReflection
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.AppEventUtility.getAppVersion
import com.facebook.appevents.internal.AppEventUtility.getRootView
import com.facebook.internal.InternalSettings.isUnityApp
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Utility.md5hash
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
class ViewIndexer(activity: Activity) {
  private val uiThreadHandler: Handler
  private val activityReference: WeakReference<Activity> = WeakReference(activity)
  private var indexingTimer: Timer? = null
  private var previousDigest: String? = null

  fun schedule() {
    val indexingTask: TimerTask =
        object : TimerTask() {
          override fun run() {
            try {
              val activity = activityReference.get()
              val rootView = getRootView(activity)
              if (null == activity || null == rootView) {
                return
              }
              val activityName = activity.javaClass.simpleName
              if (!getIsAppIndexingEnabled()) {
                return
              }
              if (isUnityApp) {
                UnityReflection.captureViewHierarchy()
                return
              }
              val screenshotFuture = FutureTask(ScreenshotTaker(rootView))
              uiThreadHandler.post(screenshotFuture)
              var screenshot: String? = ""
              try {
                screenshot = screenshotFuture[1, TimeUnit.SECONDS]
              } catch (e: Exception) {
                Log.e(TAG, "Failed to take screenshot.", e)
              }
              val viewTree = JSONObject()
              try {
                viewTree.put("screenname", activityName)
                viewTree.put("screenshot", screenshot)
                val viewArray = JSONArray()
                val rootViewInfo = ViewHierarchy.getDictionaryOfView(rootView)
                viewArray.put(rootViewInfo)
                viewTree.put("view", viewArray)
              } catch (e: JSONException) {
                Log.e(TAG, "Failed to create JSONObject")
              }
              val tree = viewTree.toString()
              sendToServer(tree)
            } catch (e: Exception) {
              Log.e(TAG, "UI Component tree indexing failure!", e)
            }
          }
        }
    try {
      getExecutor().execute {
        try {
          indexingTimer?.cancel()
          previousDigest = null
          val timer = Timer()
          timer.scheduleAtFixedRate(
              indexingTask, 0, Constants.APP_INDEXING_SCHEDULE_INTERVAL_MS.toLong())
          indexingTimer = timer
        } catch (e: Exception) {
          Log.e(TAG, "Error scheduling indexing job", e)
        }
      }
    } catch (e: RejectedExecutionException) {
      Log.e(TAG, "Error scheduling indexing job", e)
    }
  }

  fun unschedule() {
    activityReference.get() ?: return
    try {
      indexingTimer?.cancel()

      indexingTimer = null
    } catch (e: Exception) {
      Log.e(TAG, "Error unscheduling indexing job", e)
    }
  }

  private fun sendToServer(tree: String) {
    getExecutor()
        .execute(
            Runnable {
              val currentDigest = md5hash(tree)
              val accessToken = getCurrentAccessToken()
              if (currentDigest != null && currentDigest == previousDigest) {
                return@Runnable
              }
              val request =
                  buildAppIndexingRequest(
                      tree, accessToken, getApplicationId(), Constants.APP_INDEXING)
              processRequest(request, currentDigest)
            })
  }

  /**
   * Process graph request
   *
   * @param request graphRequest to process
   * @param currentDigest
   */
  fun processRequest(request: GraphRequest?, currentDigest: String?) {
    if (request == null) {
      return
    }
    val res = request.executeAndWait()
    try {
      val jsonRes = res.getJSONObject()
      if (jsonRes != null) {
        if ("true" == jsonRes.optString(SUCCESS)) {
          log(LoggingBehavior.APP_EVENTS, TAG, "Successfully send UI component tree to server")
          previousDigest = currentDigest
        }
        if (jsonRes.has(Constants.APP_INDEXING_ENABLED)) {
          val appIndexingEnabled = jsonRes.getBoolean(Constants.APP_INDEXING_ENABLED)
          updateAppIndexing(appIndexingEnabled)
        }
      } else {
        Log.e(TAG, "Error sending UI component tree to Facebook: " + res.error)
      }
    } catch (e: JSONException) {
      Log.e(TAG, "Error decoding server response.", e)
    }
  }

  private class ScreenshotTaker internal constructor(rootView: View) : Callable<String> {
    private val rootView: WeakReference<View>
    override fun call(): String {
      val view = rootView.get()
      if (view == null || view.width == 0 || view.height == 0) {
        return ""
      }
      val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.RGB_565)
      val canvas = Canvas(bitmap)
      view.draw(canvas)
      val outputStream = ByteArrayOutputStream()
      // TODO: T25009391, Support better screenshot image quality by using file attachment.
      bitmap.compress(Bitmap.CompressFormat.JPEG, 10, outputStream)
      return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    init {
      this.rootView = WeakReference(rootView)
    }
  }

  companion object {
    private val TAG = ViewIndexer::class.java.canonicalName ?: ""
    private const val SUCCESS = "success"
    private const val TREE_PARAM = "tree"
    private const val APP_VERSION_PARAM = "app_version"
    private const val PLATFORM_PARAM = "platform"
    private const val REQUEST_TYPE = "request_type"
    private var instance: ViewIndexer? = null

    /**
     * Build current app index request and process it
     *
     * @param tree current view tree
     */
    @JvmStatic
    fun sendToServerUnityInstance(tree: String) {
      instance?.sendToServer(tree)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @JvmStatic
    fun buildAppIndexingRequest(
        appIndex: String?,
        accessToken: AccessToken?,
        appId: String?,
        requestType: String
    ): GraphRequest? {
      if (appIndex == null) {
        return null
      }
      val postRequest =
          newPostRequest(
              accessToken, String.format(Locale.US, "%s/app_indexing", appId), null, null)
      var requestParameters = postRequest.parameters
      if (requestParameters == null) {
        requestParameters = Bundle()
      }
      requestParameters.putString(TREE_PARAM, appIndex)
      requestParameters.putString(APP_VERSION_PARAM, getAppVersion())
      requestParameters.putString(PLATFORM_PARAM, Constants.PLATFORM)
      requestParameters.putString(REQUEST_TYPE, requestType)
      if (requestType == Constants.APP_INDEXING) {
        requestParameters.putString(Constants.DEVICE_SESSION_ID, getCurrentDeviceSessionID())
      }
      postRequest.parameters = requestParameters
      postRequest.callback =
          GraphRequest.Callback { log(LoggingBehavior.APP_EVENTS, TAG, "App index sent to FB!") }
      return postRequest
    }
  }

  init {
    previousDigest = null
    uiThreadHandler = Handler(Looper.getMainLooper())
    instance = this
  }
}
