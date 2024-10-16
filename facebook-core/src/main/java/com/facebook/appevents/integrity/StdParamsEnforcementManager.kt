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
object StdParamsEnforcementManager {
    private var enabled = false

    /* the parameters will be filtered out based on the param value */
    private var regexRestrictionsConfig: MutableMap<String, HashSet<String>> = HashMap()
    private var enumRestrictionsConfig: MutableMap<String, HashSet<String>> = HashMap()

    @JvmStatic
    fun enable() {
        if(enabled) {
            return
        }
        loadConfigs()
        enabled = !(regexRestrictionsConfig.isEmpty() && enumRestrictionsConfig.isEmpty())
    }

    @JvmStatic
    fun disable() {
        enabled = false
        regexRestrictionsConfig = HashMap()
        enumRestrictionsConfig = HashMap()
    }

    private fun loadConfigs() {
        val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        configureSchemaRestrictions(settings.schemaRestrictions)
    }

    private fun configureSchemaRestrictions(schema: JSONArray?) {
        if(schema == null || enabled){
            return
        }
        for (i in 0 until schema.length()) {
            val restrictionJson = schema.getJSONObject(i)
            val key = restrictionJson.getString("key")
            if(key.isNullOrEmpty()) {
                continue
            }
            try {
                val value = restrictionJson.getJSONArray("value")
                for (j in 0 until value.length()) {
                    val requireExactMatch = value.getJSONObject(j).getBoolean("require_exact_match")
                    val potentialMatches = value.getJSONObject(j).getJSONArray("potential_matches")
                    val paramSet = loadSet(potentialMatches)
                    if (requireExactMatch) {
                        enumRestrictionsConfig[key] = enumRestrictionsConfig[key]?.apply { addAll(paramSet) } ?: paramSet
                    } else {
                        regexRestrictionsConfig[key] = regexRestrictionsConfig[key]?.apply { addAll(paramSet) } ?: paramSet
                    }
                }
            }catch (e: Exception) {
                /* don't enforce if schema consumption fails */
                enumRestrictionsConfig.remove(key)
                regexRestrictionsConfig.remove(key)
            }

        }
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
    fun processFilterParamSchemaBlocking(parameters: Bundle?) {
        if (!enabled || parameters == null) {
            return
        }
        val paramsToRemove = mutableListOf<String>()

        for (key in parameters.keySet()) {
            val value: String = parameters.get(key).toString()

            // check for schema restrictions
            val regexKeyExists = regexRestrictionsConfig[key] != null
            val enumKeyExists = enumRestrictionsConfig[key] != null

            // If no schema restriction exists, do not filter
            if (!regexKeyExists && !enumKeyExists) {
                continue
            }
            val regexMatches = isAnyRegexMatched(value, regexRestrictionsConfig[key])
            val enumMatches = isAnyEnumMatched(value, enumRestrictionsConfig[key])
            if (!regexMatches && !enumMatches) {
                // Filter if no rule matches
                paramsToRemove.add(key)
            }
        }

        paramsToRemove.forEach { paramToRemove ->
            parameters.remove(paramToRemove)
        }
    }

    private fun isAnyRegexMatched(value: String, expressions: Set<String>?): Boolean {
        return expressions?.any { value.matches(Regex(it)) } ?: false
    }

    private fun isAnyEnumMatched(value: String, enumValues: Set<String>?): Boolean {
        return enumValues?.any { it.lowercase() == value.lowercase() } ?: false
    }

}
