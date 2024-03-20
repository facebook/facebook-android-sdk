/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.Utility.convertJSONArrayToHashSet
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray
import kotlin.collections.HashSet

@AutoHandleExceptions
object SensitiveParamsManager {
    private var enabled = false
    private var sensitiveParameters: MutableMap<String, HashSet<String>> = HashMap()
    private const val SENSITIVE_PARAMS_KEY = "_filteredKey"

    @JvmStatic
    fun enable() {
        loadSensitiveParameters()
        if (!sensitiveParameters.isNullOrEmpty()) {
            enabled = true
        }
    }

    @JvmStatic
    fun disable() {
        enabled = false
        sensitiveParameters = HashMap()
    }

    private fun loadSensitiveParameters() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        try {
            sensitiveParameters = HashMap()
            val sensitiveParamsFromServer = settings.sensitiveParams
            if (sensitiveParamsFromServer != null && sensitiveParamsFromServer.length() != 0) {
                for (i in 0 until sensitiveParamsFromServer.length()) {
                    val jsonObject = sensitiveParamsFromServer.getJSONObject(i)
                    val hasEventName = jsonObject.has("key")
                    val hasSensitiveParams = jsonObject.has("value")
                    if (hasEventName && hasSensitiveParams) {
                        val eventName = jsonObject.getString("key")
                        val sensitiveParams = jsonObject.getJSONArray("value")
                        eventName?.let {
                            sensitiveParams?.let {
                                convertJSONArrayToHashSet(sensitiveParams)?.let {
                                    sensitiveParameters[eventName] = it
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            /* swallow */
        }
    }

    @JvmStatic
    fun processFilterSensitiveParams(parameters: MutableMap<String, String?>, eventName: String) {
        if (!enabled) {
            return
        }
        if (!sensitiveParameters.containsKey(eventName)) {
            return
        }
        
        val filteredParamsJSON = JSONArray()
        try {
            val sensitiveParamsForEvent = sensitiveParameters.get(key = eventName)
            val keys: List<String> = ArrayList(parameters.keys)
            for (key in keys) {
                if (!sensitiveParamsForEvent.isNullOrEmpty() && sensitiveParamsForEvent.contains(key)) {
                    parameters.remove(key)
                    filteredParamsJSON.put(key)
                }
            }
        } catch (e: Exception) {
            /* swallow */
        }
        
        if (filteredParamsJSON.length() > 0) {
            parameters[SENSITIVE_PARAMS_KEY] = filteredParamsJSON.toString()
        }
    }
}
