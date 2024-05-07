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
import kotlin.collections.HashSet

@AutoHandleExceptions
object BlocklistEventsManager {
    private var enabled = false
    private var blocklist: MutableSet<String> = HashSet()
    
    @JvmStatic
    fun enable() {
        loadBlocklistEvents()
        if (!blocklist.isNullOrEmpty()) {
            enabled = true
        }
    }

    @JvmStatic
    fun disable() {
        enabled = false
        blocklist = HashSet()
    }

    private fun loadBlocklistEvents() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return

        convertJSONArrayToHashSet(settings.blocklistEvents)?.let {
            blocklist = it
        }
    }

    /** check if the event is in the blocklist */
    @JvmStatic
    fun isInBlocklist(eventName: String): Boolean {
        if (!enabled) {
            return false
        }
        return blocklist.contains(eventName)
    }
}
