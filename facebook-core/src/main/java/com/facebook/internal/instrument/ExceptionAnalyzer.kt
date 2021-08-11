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
