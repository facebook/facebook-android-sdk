/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.aam

import androidx.annotation.RestrictTo
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.CopyOnWriteArraySet
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class MetadataRule
private constructor(val name: String, keyRules: List<String>, val valRule: String) {
  val keyRules = keyRules
    get() = ArrayList(field)
  companion object {
    private val rules: MutableSet<MetadataRule> = CopyOnWriteArraySet()
    private const val FIELD_K = "k"
    private const val FIELD_V = "v"
    private const val FIELD_K_DELIMITER = ","
    @JvmStatic
    fun getRules(): Set<MetadataRule> {
      return HashSet(rules)
    }

    @JvmStatic
    fun updateRules(rulesFromServer: String) {
      try {
        rules.clear()
        val jsonObject = JSONObject(rulesFromServer)
        constructRules(jsonObject)
      } catch (e: JSONException) {}
    }

    @JvmStatic
    fun getEnabledRuleNames(): Set<String> {
      val ruleNames: MutableSet<String> = HashSet()
      for (r in rules) {
        ruleNames.add(r.name)
      }
      return ruleNames
    }

    private fun constructRules(jsonObject: JSONObject) {
      val keys = jsonObject.keys()
      while (keys.hasNext()) {
        val key = keys.next()
        val rule = jsonObject.optJSONObject(key) ?: continue
        val k = rule.optString(FIELD_K)
        val v = rule.optString(FIELD_V)
        if (k.isEmpty()) {
          continue
        }
        rules.add(MetadataRule(key, k.split(FIELD_K_DELIMITER), v))
      }
    }
  }
}
