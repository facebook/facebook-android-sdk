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

  fun `test string comparison for not_contains`() {
    val json = JSONObject()
    json.put("not_contains", "xxxxx")
    assertFalse(
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
  fun `test string comparison for i_not_contains`() {
    val json = JSONObject()
    json.put("i_not_contains", "xxxxx")
    assertFalse(
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

  @Test
  fun `test isMatchCCRule for not existed data value`() {
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"contains":"xxxxx"}}]}]}""",
        data = null
      )
    )
  }

  @Test
  fun `test isMatchCCRule for contains`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"contains":"xxxxx"}}]}]}""",
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxxxx.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for not contains`() {
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"contains":"xxxxx"}}]}]}""",
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxx.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for not_contains`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"not_contains":"xxxxx"}}]}]}""",
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxx.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for i_contains match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_contains":"xxxxx"}}]}]}
          """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxx.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for i_not_contains match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_not_contains":"xxxxx"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xx.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for i_not_contains not match`() {
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_not_contains":"xxxxx"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for starts_with match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"starts_with":"ww"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for i_starts_with match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_starts_with":"WWW"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for i_str_eq match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_str_eq":"www"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "WWW"
        )
      )
    )
  }

  fun `test isMatchCCRule for i_str_neq match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"eq":"Lead"}},{"or":[{"URL":{"i_str_neq":"ww"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "www"
        )
      )
    )
  }

  fun `test isMatchCCRule for in match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"in":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "fb_mobile_activate_app",
          "url" to "www.xxXxxww.com"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"is_any":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "fb_page_view",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for i_str_in match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"i_str_in":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "FB_PAGE_VIEW",
          "url" to "www.xxXxxww.com"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"i_is_any":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "PAGEVIEW",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for not_in`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"not_in":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "fb_mobile_deactivate_app",
          "url" to "www.xxXxxww.com"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"is_not_any":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "fb_page_view",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  fun `test isMatchCCRule for i_str_not_in`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"i_str_not_in":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "fb_mobile_deactivate_app",
          "url" to "www.xxXxxww.com"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"event":{"i_is_not_any":["fb_mobile_activate_app","fb_page_view","PixelInitialized","PageView"]}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "PAgeVIEW",
          "url" to "www.xxXxxww.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for regex match`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"or":[{"URL":{"regex_match":"eylea.us/support/?${'$'}|eylea.us/support/?"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "eylea.us/support"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for regex not match`() {
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """
          {"and":[{"or":[{"URL":{"regex_match":"eylea.us/support/?${'$'}|eylea.us/support/?"}}]}]}
        """.trimIndent(),
        data = bundleOf(
          "event" to "Lead",
          "url" to "eylea.us.support"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for exists`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"url":{"exists":true}}]}""",
        data = bundleOf(
          "event" to "PageLoad",
          "url" to "www.test.com"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"url":{"exists":false}}]}""",
        data = bundleOf(
          "event" to "PageLoad",
          "url" to "www.test.com"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"product":{"exists":true}}]}""",
        data = bundleOf(
          "event" to "PageLoad",
          "url" to "www.test.com"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"product":{"exists":false}}]}""",
        data = bundleOf(
          "event" to "PageLoad",
          "url" to "www.test.com"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for eq`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"eq":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"=":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"==":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for neq`() {
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"neq":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"ne":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
    assertFalse(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"event":{"!=":"PageLoad"}}]}""",
        data = bundleOf(
          "event" to "PageLoad"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for lt`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"lt":"30"}}]}""",
        data = bundleOf(
          "value" to 1
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"<":"30"}}]}""",
        data = bundleOf(
          "value" to 1
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for lte`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"lte":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"le":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"<=":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for gt`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"gt":"30"}}]}""",
        data = bundleOf(
          "value" to 31
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{">":"30"}}]}""",
        data = bundleOf(
          "value" to 31
        )
      )
    )
  }

  @Test
  fun `test isMatchCCRule for gte`() {
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"gte":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{"ge":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
    assertTrue(
      MACARuleMatchingManager.isMatchCCRule(
        ruleString = """{"and":[{"value":{">=":"30"}}]}""",
        data = bundleOf(
          "value" to "30"
        )
      )
    )
  }
}