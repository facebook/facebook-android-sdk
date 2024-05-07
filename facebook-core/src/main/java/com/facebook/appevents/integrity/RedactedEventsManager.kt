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

@AutoHandleExceptions
object RedactedEventsManager {
    private var enabled = false
    private var redactedEvents: MutableMap<String, HashSet<String>> = HashMap()
    
    @JvmStatic
    fun enable() {
        loadRedactedEvents()
        if (redactedEvents.isNotEmpty()) {
            enabled = true
        }
    }

    @JvmStatic
    fun disable() {
        enabled = false
        redactedEvents = HashMap()
    }
    
    private fun loadRedactedEvents() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        try {
            redactedEvents = HashMap()
            val redactedEventsFromServer = settings.redactedEvents
            if (redactedEventsFromServer !== null && redactedEventsFromServer.length() != 0) {
                for (i in 0 until redactedEventsFromServer.length()) {
                    val jsonObject = redactedEventsFromServer.getJSONObject(i)
                    val hasKey = jsonObject.has("key")
                    val hasValue = jsonObject.has("value")
                    if (hasKey && hasValue) {
                        val redactedString = jsonObject.getString("key")
                        val eventsNeedToBeRedacted = jsonObject.getJSONArray("value")
                        redactedString?.let { /* redacted String */
                            convertJSONArrayToHashSet(eventsNeedToBeRedacted)?.let { /* event names under the specific redacted String */
                                redactedEvents[redactedString] = it
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
    fun processEventsRedaction(eventName: String): String {
        if (enabled) {
            getRedactionString(eventName)?.let {
                return it
            }
        }
        return eventName
    }

    private fun getRedactionString(eventName: String): String? {
        redactedEvents.keys.forEach { redactionString ->
            redactedEvents[redactionString]?.let {
                if (it.contains(eventName)) {
                    return redactionString
                }
            }
        }
        return null
    }
}
