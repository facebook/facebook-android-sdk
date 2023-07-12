/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Build
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.Utility
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray
import org.json.JSONObject

@AutoHandleExceptions
object MACARuleMatchingManager {
  private var enabled = false

  private var MACARules: JSONArray? = null

  private var keys = arrayOf(
    "event",
    "_locale",
    "_appVersion",
    "_deviceOS",
    "_platform",
    "_deviceModel",
    "_nativeAppID",
    "_nativeAppShortVersion",
    "_timezone",
    "_carrier",
    "_deviceOSTypeName",
    "_deviceOSVersion",
    "_remainingDiskGB",
  )

  @JvmStatic
  fun enable() {
    loadMACARules()
    if (MACARules != null) {
      enabled = true
    }
  }

  private fun loadMACARules() {
    val settings = FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
      ?: return
    MACARules = settings.MACARuleMatchingSetting
  }

  @JvmStatic
  fun getKey(logic: JSONObject): String? {
    val its = logic.keys()
    while (its.hasNext()) {
      return its.next()
    }
    return null
  }

  @JvmStatic
  fun stringComparison(
    variable: String,
    values: JSONObject,
    data: Bundle?
  ): Boolean {
    val op = getKey(values) ?: return false
    val ruleValue = values.get(op).toString()
    val ruleArray = getStringArrayList(values.optJSONArray(op))

    if (op == "exists") {
      return data?.containsKey(variable) == ruleValue.toBoolean()
    }

    val dataValue = (data?.get(variable.lowercase()) ?: data?.get(variable)) ?: return false

    return when (op) {
      "contains" -> {
        dataValue.toString().contains(ruleValue)
      }
      "i_contains" -> {
        dataValue.toString().lowercase().contains(ruleValue.lowercase())
      }
      "not_contains" -> {
        !dataValue.toString().contains(ruleValue)
      }
      "i_not_contains" -> {
        !dataValue.toString().lowercase().contains(ruleValue.lowercase())
      }
      "starts_with" -> {
        dataValue.toString().startsWith(ruleValue)
      }
      "i_starts_with" -> {
        dataValue.toString().lowercase().startsWith(ruleValue.lowercase())
      }
      "i_str_eq" -> {
        dataValue.toString().lowercase() == ruleValue.lowercase()
      }
      "i_str_neq" -> {
        dataValue.toString().lowercase() != ruleValue.lowercase()
      }
      "in", "is_any" -> {
        val arr = ruleArray ?: return false
        arr.contains(dataValue.toString())
      }
      "i_str_in", "i_is_any" -> {
        val arr = ruleArray ?: return false
        arr.any { str -> str.lowercase() == dataValue.toString().lowercase() }
      }
      "not_in", "is_not_any" -> {
        val arr = ruleArray ?: return false
        arr.contains(dataValue.toString())
      }
      "i_str_not_in", "i_is_not_any" -> {
        val arr = ruleArray ?: return false
        arr.all { str -> str.lowercase() != dataValue.toString().lowercase() }
      }
      "regex_match" -> {
        ruleValue.toRegex().matches(dataValue.toString())
      }
      "eq", "=", "==" -> {
        dataValue.toString() == ruleValue
      }
      "neq", "ne", "!=" -> {
        dataValue.toString() != ruleValue
      }
      "lt", "<" -> {
        dataValue.toString().toDouble() < ruleValue.toDouble()
      }
      "lte", "le", "<=" -> {
        dataValue.toString().toDouble() <= ruleValue.toDouble()
      }
      "gt", ">" -> {
        dataValue.toString().toDouble() > ruleValue.toDouble()
      }
      "gte", "ge", ">=" -> {
        dataValue.toString().toDouble() >= ruleValue.toDouble()
      }
      else -> false
    }
  }

  @JvmStatic
  fun getStringArrayList(
    jsonArray: JSONArray?
  ): ArrayList<String>? {
    if (jsonArray == null) {
      return null
    }
    val res = arrayListOf<String>()
    for (i in 0 until jsonArray.length()) {
      res.add(jsonArray.get(i).toString())
    }
    return res
  }

  @JvmStatic
  fun isMatchCCRule(
    ruleString: String?,
    data: Bundle?
  ): Boolean {
    if (ruleString == null || data == null) {
      return false
    }

    val ruleJson = JSONObject(ruleString)
    val op = getKey(ruleJson) ?: return false
    val values = ruleJson.get(op)

    return when (op) {
      "and" -> {
        val v = (values as JSONArray?) ?: return false
        for (i in 0 until v.length()) {
          val thisRes = isMatchCCRule(v.get(i).toString(), data)
          if (!thisRes) {
            return false
          }
        }
        return true
      }
      "or" -> {
        val v = (values as JSONArray?) ?: return false
        for (i in 0 until v.length()) {
          val thisRes = isMatchCCRule(v.get(i).toString(), data)
          if (thisRes) {
            return true
          }
        }
        return false
      }
      "not" -> !isMatchCCRule(values.toString(), data)
      else -> {
        val v = (values as JSONObject?) ?: return false
        return stringComparison(op, v, data)
      }
    }
  }

  @JvmStatic
  fun getMatchPropertyIDs(params: Bundle?): String {
    if (MACARules == null || MACARules?.length() == 0) {
      return "[]"
    }
    val rules = MACARules as JSONArray
    val res = mutableListOf<Long>()
    for (i in 0 until rules.length()) {
      val entry = rules.optString(i) ?: continue
      val json = JSONObject(entry)
      val pid = json.optLong("id")
      if (pid == 0L) continue
      val rule = json.optString("rule") ?: continue
      if (isMatchCCRule(rule, params)) {
        res.add(pid)
      }
    }
    return JSONArray(res).toString()
  }

  @JvmStatic
  fun processParameters(params: Bundle?, event: String) {
    if (!enabled || params == null) {
      return
    }

    try {
      generateInfo(params, event)
      params.putString("_audiencePropertyIds", getMatchPropertyIDs(params))
      params.putString("cs_maca", "1")
      removeGeneratedInfo(params)
    } catch (_: Exception) {}
  }

  @JvmStatic
  fun generateInfo(params: Bundle, event: String) {
    params.putString("event", event)
    params.putString("_locale",
      (Utility.locale?.language ?: "")+ '_' + (Utility.locale?.country ?: ""))
    params.putString("_appVersion", Utility.versionName ?: "")
    params.putString("_deviceOS", "ANDROID")
    params.putString("_platform", "mobile")
    params.putString("_deviceModel", Build.MODEL ?: "")
    params.putString("_nativeAppID", FacebookSdk.getApplicationId())
    params.putString("_nativeAppShortVersion", Utility.versionName ?: "")
    params.putString("_timezone", Utility.deviceTimeZoneName)
    params.putString("_carrier", Utility.carrierName)
    params.putString("_deviceOSTypeName", "ANDROID")
    params.putString("_deviceOSVersion", Build.VERSION.RELEASE)
    params.putLong("_remainingDiskGB", Utility.availableExternalStorageGB)
  }

  @JvmStatic
  fun removeGeneratedInfo(params: Bundle) {
    for (k in keys) {
      params.remove(k)
    }
  }
}