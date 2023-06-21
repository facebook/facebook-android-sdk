/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import androidx.core.os.bundleOf
import com.facebook.FacebookTestCase
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MACARuleMatchingManagerTest: FacebookTestCase() {

  @Test
  fun `test string comparison with null`() {
    val json = JSONObject()
    json.put("eq", "platinum")
    assertFalse(
      MACARuleMatchingManager.stringComparison(
        variable = "card_type",
        values = json,
        data = null
      )
    )
  }

  @Test
  fun `test string comparison for contains`() {
    val json = JSONObject()
    json.put("contains", "xxxxx")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "URL",
        values = json,
        data = bundleOf(
          "event" to "CompleteRegistration",
          "url" to "www.xxxxx.com"
        )
      )
    )
  }

  @Test
  fun `test string comparison for i_contains`() {
    val json = JSONObject()
    json.put("i_contains", "xxxxx")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "URL",
        values = json,
        data = bundleOf(
          "event" to "CompleteRegistration",
          "url" to "www.xxXxx.com"
        )
      )
    )
  }

  @Test
  fun `test string comparison for regex_match`() {
    val json = JSONObject()
    json.put("regex_match", "eylea.us/support/?\$|eylea.us/support/?")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "URL",
        values = json,
        data = bundleOf(
          "event" to "CompleteRegistration",
          "url" to "eylea.us/support"
        )
      )
    )
  }

  @Test
  fun `test string comparison for eq`() {
    val json = JSONObject()
    json.put("eq", "CompleteRegistration")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "event",
        values = json,
        data = bundleOf(
          "event" to "CompleteRegistration",
          "url" to "eylea.us/support"
        )
      )
    )
  }

  @Test
  fun `test string comparison for neq`() {
    val json = JSONObject()
    json.put("neq", "0")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "1"
        )
      )
    )
  }

  @Test
  fun `test string comparison for lt`() {
    val json = JSONObject()
    json.put("lt", "10")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "1"
        )
      )
    )
  }

  @Test
  fun `test string comparison for lte`() {
    val json = JSONObject()
    json.put("lte", "30")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "30"
        )
      )
    )
  }

  @Test
  fun `test string comparison for gt`() {
    val json = JSONObject()
    json.put("gt", "0")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "1"
        )
      )
    )
  }

  @Test
  fun `test string comparison for gte`() {
    val json = JSONObject()
    json.put("gte", "100")
    assertTrue(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "100"
        )
      )
    )
  }

  @Test
  fun `test string comparison for invalid op`() {
    val json = JSONObject()
    json.put("none", "0")
    assertFalse(
      MACARuleMatchingManager.stringComparison(
        variable = "value",
        values = json,
        data = bundleOf(
          "value" to "1"
        )
      )
    )
  }
}