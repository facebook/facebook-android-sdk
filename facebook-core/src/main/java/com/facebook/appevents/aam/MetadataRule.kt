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
