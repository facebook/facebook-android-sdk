/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.os.Bundle
import android.view.View
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.GraphRequest.Companion.newPostRequest
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.ViewHierarchyConstants.SCREEN_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.VIEW_KEY
import com.facebook.appevents.ml.ModelManager
import com.facebook.appevents.ml.ModelManager.predict
import com.facebook.appevents.suggestedevents.FeatureExtractor.getDenseFeatures
import com.facebook.appevents.suggestedevents.FeatureExtractor.getTextFeature
import com.facebook.appevents.suggestedevents.SuggestedEventsManager.isEligibleEvents
import com.facebook.appevents.suggestedevents.SuggestedEventsManager.isProductionEvents
import com.facebook.internal.Utility.getAppName
import com.facebook.internal.Utility.runOnNonUiThread
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.ref.WeakReference
import java.util.Locale
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ViewOnClickListener
private constructor(hostView: View, rootView: View, activityName: String) : View.OnClickListener {
  private val baseListener: View.OnClickListener? =
      ViewHierarchy.getExistingOnClickListener(hostView)
  private val rootViewWeakReference: WeakReference<View> = WeakReference(rootView)
  private val hostViewWeakReference: WeakReference<View> = WeakReference(hostView)
  private val activityName: String = activityName.toLowerCase().replace("activity", "")
  override fun onClick(view: View) {
    baseListener?.onClick(view)
    process()
  }

  private fun process() {
    val rootView = rootViewWeakReference.get()
    val hostView = hostViewWeakReference.get()
    if (rootView == null || hostView == null) {
      return
    }
    try {
      val buttonText = SuggestedEventViewHierarchy.getTextOfViewRecursively(hostView)
      // query history
      val pathID = PredictionHistoryManager.getPathID(hostView, buttonText) ?: return
      if (queryHistoryAndProcess(pathID, buttonText)) {
        return
      }

      // run prediction
      val data = JSONObject()
      data.put(VIEW_KEY, SuggestedEventViewHierarchy.getDictionaryOfView(rootView, hostView))
      data.put(SCREEN_NAME_KEY, activityName)
      predictAndProcess(pathID, buttonText, data)
    } catch (e: Exception) {
      /*no op*/
    }
  }

  private fun predictAndProcess(pathID: String, buttonText: String, viewData: JSONObject) {
    runOnNonUiThread(
        Runnable {
          try {
            val appName = getAppName(FacebookSdk.getApplicationContext()).toLowerCase()
            val dense = getDenseFeatures(viewData, appName)
            val textFeature = getTextFeature(buttonText, activityName, appName)
            if (dense == null) {
              return@Runnable
            }
            val predictedEvents =
                predict(
                    ModelManager.Task.MTML_APP_EVENT_PREDICTION,
                    arrayOf(dense),
                    arrayOf(textFeature))
                    ?: return@Runnable
            val predictedEvent = predictedEvents[0]
            PredictionHistoryManager.addPrediction(pathID, predictedEvent)
            if (predictedEvent != OTHER_EVENT) {
              processPredictedResult(predictedEvent, buttonText, dense)
            }
          } catch (e: Exception) {
            /*no op*/
          }
        })
  }

  companion object {
    private const val API_ENDPOINT = "%s/suggested_events"
    const val OTHER_EVENT = "other"
    private val viewsAttachedListener: MutableSet<Int> = HashSet()

    @JvmStatic
    internal fun attachListener(hostView: View, rootView: View, activityName: String) {
      val key = hostView.hashCode()
      if (!viewsAttachedListener.contains(key)) {
        ViewHierarchy.setOnClickListener(
            hostView, ViewOnClickListener(hostView, rootView, activityName))
        viewsAttachedListener.add(key)
      }
    }

    // return True if successfully found history prediction
    private fun queryHistoryAndProcess(pathID: String, buttonText: String): Boolean {
      // not found
      val queriedEvent = PredictionHistoryManager.queryEvent(pathID) ?: return false
      if (queriedEvent != OTHER_EVENT) {
        runOnNonUiThread { processPredictedResult(queriedEvent, buttonText, floatArrayOf()) }
      }
      return true
    }

    private fun processPredictedResult(
        predictedEvent: String,
        buttonText: String,
        dense: FloatArray
    ) {
      if (isProductionEvents(predictedEvent)) {
        val logger = InternalAppEventsLogger(FacebookSdk.getApplicationContext())
        logger.logEventFromSE(predictedEvent, buttonText)
      } else if (isEligibleEvents(predictedEvent)) {
        sendPredictedResult(predictedEvent, buttonText, dense)
      }
    }

    private fun sendPredictedResult(eventToPost: String, buttonText: String, dense: FloatArray) {
      val publishParams = Bundle()
      try {
        publishParams.putString("event_name", eventToPost)
        val metadata = JSONObject()
        val denseSB = StringBuilder()
        for (f in dense) {
          denseSB.append(f).append(",")
        }
        metadata.put("dense", denseSB.toString())
        metadata.put("button_text", buttonText)
        publishParams.putString("metadata", metadata.toString())
        val postRequest =
            newPostRequest(
                null,
                String.format(Locale.US, API_ENDPOINT, FacebookSdk.getApplicationId()),
                null,
                null)
        postRequest.parameters = publishParams
        postRequest.executeAndWait()
      } catch (e: JSONException) {
        /*no op*/
      }
    }
  }
}
