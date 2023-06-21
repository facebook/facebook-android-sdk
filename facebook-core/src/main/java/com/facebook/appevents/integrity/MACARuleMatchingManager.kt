/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONObject

@AutoHandleExceptions
object MACARuleMatchingManager {
  private var enabled = false

  @JvmStatic
  fun enable() {
    enabled = true
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
    val dataValue = (data?.get(variable.lowercase()) ?: data?.get(variable)) ?: return false

    return when (op) {
      "contains" -> {
        dataValue.toString().contains(ruleValue)
      }
      "i_contains" -> {
        dataValue.toString().lowercase().contains(ruleValue.lowercase())
      }
      "i_not_contains" -> {
        !dataValue.toString().lowercase().contains(ruleValue.lowercase())
      }
      "regex_match" -> {
        ruleValue.toRegex().matches(dataValue.toString())
      }
      "eq" -> {
        dataValue.toString() == ruleValue
      }
      "neq" -> {
        dataValue.toString() != ruleValue
      }
      "lt" -> {
        dataValue.toString().toDouble() < ruleValue.toDouble()
      }
      "lte" -> {
        dataValue.toString().toDouble() <= ruleValue.toDouble()
      }
      "gt" -> {
        dataValue.toString().toDouble() > ruleValue.toDouble()
      }
      "gte" -> {
        dataValue.toString().toDouble() >= ruleValue.toDouble()
      }
      else -> false
    }
  }
}