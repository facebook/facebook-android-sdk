/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphRequestBatch
import com.facebook.core.BuildConfig
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.disableFeature
import com.facebook.internal.FeatureManager.getFeature
import com.facebook.internal.Utility.isDataProcessingRestricted
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ExceptionAnalyzer {
  private var enabled = false

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  @JvmStatic
  internal fun isDebug(): Boolean = BuildConfig.DEBUG

  @JvmStatic
  fun enable() {
    enabled = true
    if (FacebookSdk.getAutoLogAppEventsEnabled()) {
      sendExceptionAnalysisReports()
    }
  }

  @JvmStatic
  fun execute(e: Throwable?) {
    if (!enabled || isDebug() || e == null) {
      return
    }
    val disabledFeatures: MutableSet<String?> = HashSet()
    e.stackTrace.forEach {
      val feature = getFeature(it.className)
      if (feature !== FeatureManager.Feature.Unknown) {
        disableFeature(feature)
        disabledFeatures.add(feature.toString())
      }
    }
    if (FacebookSdk.getAutoLogAppEventsEnabled() && disabledFeatures.isNotEmpty()) {
      InstrumentData.Builder.build(JSONArray(disabledFeatures)).save()
    }
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  internal fun sendExceptionAnalysisReports() {
    if (isDataProcessingRestricted) {
      return
    }
    val reports = InstrumentUtility.listExceptionAnalysisReportFiles()
    val requests: MutableList<GraphRequest> = ArrayList()
    for (report in reports) {
      val instrumentData = InstrumentData.Builder.load(report)
      if (instrumentData.isValid) {
        val params = JSONObject()
        try {
          params.put("crash_shield", instrumentData.toString())
          val request =
              GraphRequest.newPostRequest(
                  null,
                  String.format("%s" + "/instruments", FacebookSdk.getApplicationId()),
                  params) { response ->
                    try {
                      if (response.error == null &&
                          response.jsonObject?.getBoolean("success") == true) {
                        instrumentData.clear()
                      }
                    } catch (e: JSONException) {
                      /* no op */
                    }
                  }
          requests.add(request)
        } catch (e: JSONException) {
          /* no op */
        }
      }
    }
    if (requests.isEmpty()) {
      return
    }
    val requestBatch = GraphRequestBatch(requests)
    requestBatch.executeAsync()
  }
}
