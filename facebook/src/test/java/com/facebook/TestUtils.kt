/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.annotation.TargetApi
import junit.framework.Assert
import org.json.JSONArray
import org.json.JSONObject

object TestUtils {
  @JvmStatic
  @TargetApi(16)
  fun assertEquals(expected: JSONObject?, actual: JSONObject?) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return
    }
    Assert.failNotEquals("", expected, actual)
  }

  private fun areEqual(expected: JSONObject?, actual: JSONObject?): Boolean {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    val expectedKeysIterator = expected.keys()
    val expectedKeys = mutableSetOf<String>()
    while (expectedKeysIterator.hasNext()) {
      expectedKeys.add(expectedKeysIterator.next())
    }
    val actualKeysIterator = actual.keys()
    while (actualKeysIterator.hasNext()) {
      val key = actualKeysIterator.next()
      if (!areEqual(expected.opt(key), actual.opt(key))) {
        return false
      }
      expectedKeys.remove(key)
    }
    return expectedKeys.size == 0
  }

  private fun areEqual(expected: JSONArray?, actual: JSONArray?): Boolean {
    // JSONObject.equals does not do an order-independent comparison, so we need to check values
    // that are JSONObject
    // manually
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    if (expected.length() != actual.length()) {
      return false
    }
    val length = expected.length()
    for (i in 0 until length) {
      if (!areEqual(expected.opt(i), actual.opt(i))) {
        return false
      }
    }
    return true
  }

  private fun areEqual(expected: Any?, actual: Any?): Boolean {
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    if (expected is JSONObject && actual is JSONObject) {
      return areEqual(expected, actual)
    }
    return if (expected is JSONArray && actual is JSONArray) {
      areEqual(expected, actual)
    } else expected == actual
  }
}
