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

    /* the parameters will be filtered out for all events */
    private var defaultSensitiveParameters: HashSet<String> = HashSet()

    /* the parameters will be filtered out based on the event name */
    private var sensitiveParameters: MutableMap<String, HashSet<String>> = HashMap()

    private const val DEFAULT_SENSITIVE_PARAMS_KEY = "_MTSDK_Default_"
    private const val SENSITIVE_PARAMS_KEY = "_filteredKey" /* send back to Meta server */

    @JvmStatic
    fun enable() {
        loadSensitiveParameters()
        if (defaultSensitiveParameters.isNullOrEmpty() && sensitiveParameters.isNullOrEmpty()) {
            enabled = false
            return
        }

        /* enable only when there is non empty default sensitive params or non empty specific
         * sensitive params
         */
        enabled = true
    }

    @JvmStatic
    fun disable() {
        enabled = false
        sensitiveParameters = HashMap()
        defaultSensitiveParameters = HashSet()
    }

    private fun loadSensitiveParameters() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        try {
            defaultSensitiveParameters = HashSet()
            sensitiveParameters = HashMap()
            val sensitiveParamsFromServer = settings.sensitiveParams
            if (sensitiveParamsFromServer != null && sensitiveParamsFromServer.length() != 0) {
                for (i in 0 until sensitiveParamsFromServer.length()) {
                    val jsonObject = sensitiveParamsFromServer.getJSONObject(i)
                    val hasEventName = jsonObject.has("key")
                    val hasSensitiveParams = jsonObject.has("value")
                    if (hasEventName && hasSensitiveParams) {
                        /*  This indicates that the sensitive params are from the specific event 
                         *  name or for all events which are the default sensitive params.
                         */
                        val sensitiveParamsScope = jsonObject.getString("key")
                        val sensitiveParams = jsonObject.getJSONArray("value")
                        sensitiveParamsScope?.let {
                            sensitiveParams?.let {
                                convertJSONArrayToHashSet(sensitiveParams)?.let {
                                    if (sensitiveParamsScope.equals(DEFAULT_SENSITIVE_PARAMS_KEY)) {
                                        defaultSensitiveParameters = it
                                    } else {
                                        sensitiveParameters[sensitiveParamsScope] = it
                                    }
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
        if (defaultSensitiveParameters.isNullOrEmpty() && !sensitiveParameters.containsKey(eventName)) {
            return
        }

        val filteredParamsJSON = JSONArray()
        try {
            val sensitiveParamsForEvent = sensitiveParameters.get(key = eventName)
            val keys: List<String> = ArrayList(parameters.keys)
            for (key in keys) {
                if (shouldFilterOut(key, sensitiveParamsForEvent)) {
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

    private fun shouldFilterOut(parameterKey: String, sensitiveParamsForEvent: HashSet<String>?) : Boolean {
        return defaultSensitiveParameters.contains(parameterKey)
                || (!sensitiveParamsForEvent.isNullOrEmpty() && sensitiveParamsForEvent.contains(parameterKey))
    }
}
