/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.Utility.convertJSONArrayToHashSet
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray
import kotlin.collections.HashSet

@AutoHandleExceptions
object BannedParamManager {
    private var enabled = false

    /* the parameters will be filtered out based on the param key */
    private var bannedParamsConfig: HashSet<String> = HashSet()

    @JvmStatic
    fun enable() {
        if(enabled) {
            return
        }
        loadConfigs()
        enabled = bannedParamsConfig.isNotEmpty()
    }

    @JvmStatic
    fun disable() {
        enabled = false
        bannedParamsConfig = HashSet()
    }

    private fun loadConfigs() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        bannedParamsConfig = loadSet(settings.bannedParams)
    }

    private fun loadSet(paramValues: JSONArray?): HashSet<String> {
        return try {
            convertJSONArrayToHashSet(paramValues)?: HashSet()
        } catch (e: Exception){
            /* swallow */
            HashSet()
        }
    }

    @JvmStatic
    fun processFilterBannedParams(parameters: Bundle?) {
        if (!enabled || parameters == null) {
            return
        }
        bannedParamsConfig.forEach { paramToRemove ->
            parameters?.remove(paramToRemove)
        }
    }

}
