/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
