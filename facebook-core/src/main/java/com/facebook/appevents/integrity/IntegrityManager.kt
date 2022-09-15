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

package com.facebook.appevents.integrity

import com.facebook.FacebookSdk
import com.facebook.appevents.ml.ModelManager
import com.facebook.appevents.ml.ModelManager.predict
import com.facebook.internal.FetchedAppGateKeepersManager.getGateKeeperForKey
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONObject

@AutoHandleExceptions
object IntegrityManager {
  const val INTEGRITY_TYPE_NONE = "none"
  const val INTEGRITY_TYPE_ADDRESS = "address"
  const val INTEGRITY_TYPE_HEALTH = "health"
  private const val RESTRICTIVE_ON_DEVICE_PARAMS_KEY = "_onDeviceParams"
  private var enabled = false
  private var isSampleEnabled = false

  @JvmStatic
  fun enable() {
    enabled = true
    isSampleEnabled =
        getGateKeeperForKey("FBSDKFeatureIntegritySample", FacebookSdk.getApplicationId(), false)
  }

  /** Process integrity parameters */
  @JvmStatic
  fun processParameters(parameters: MutableMap<String, String>) {
    if (!enabled || parameters.isEmpty()) {
      return
    }
    try {
      val keys = parameters.keys.toList()
      val restrictiveParamJson = JSONObject()
      for (key in keys) {
        val value = checkNotNull(parameters[key])
        if (shouldFilter(key) || shouldFilter(value)) {
          parameters.remove(key)
          restrictiveParamJson.put(key, if (isSampleEnabled) value else "")
        }
      }
      if (restrictiveParamJson.length() != 0) {
        parameters[RESTRICTIVE_ON_DEVICE_PARAMS_KEY] = restrictiveParamJson.toString()
      }
    } catch (e: Exception) {
      /* swallow */
    }
  }

  private fun shouldFilter(input: String): Boolean {
    val predictResult = getIntegrityPredictionResult(input)
    return INTEGRITY_TYPE_NONE != predictResult
  }

  private fun getIntegrityPredictionResult(textFeature: String): String {
    val dense = FloatArray(30) { 0f }
    val res = predict(ModelManager.Task.MTML_INTEGRITY_DETECT, arrayOf(dense), arrayOf(textFeature))
    return res?.get(0) ?: INTEGRITY_TYPE_NONE
  }
}
