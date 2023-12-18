/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import java.util.EnumSet
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FetchedAppSettingsManagerTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"supports_implicit_sdk_logging\": true,\n" +
          "  \"suggested_events_setting\": \"{}\",\n" +
          "  \"aam_rules\": \"{}\",\n" +
          "  \"app_events_feature_bitmask\": 65541,\n" +
          "  \"app_events_session_timeout\": 60,\n" +
          "  \"seamless_login\": 1,\n" +
          "  \"smart_login_bookmark_icon_url\": \"swag\",\n" +
          "  \"smart_login_menu_icon_url\": \"yolo\",\n" +
          "  \"android_dialog_configs\": \"garbage\",\n" +
          "  \"protected_mode_rules\": {\"blocklist_events\": [\"test_event_for_block_list_1\", \"test_event_for_block_list_2\"], \n" +  
          "  \"redacted_events\": [{\"key\":\"FilteredEvent\", \"value\":[\"abc\", \"def\"]}, {\"key\":\"RedactedEvent\", \"value\":[\"opq\", \"xyz\"]}]},\n" +
          "  \"auto_log_app_events_default\": true,\n" +
          "  \"auto_log_app_events_enabled\": true\n" +
          "}"

  private val invalidValueTypesJson =
      "{\n" +
          "  \"smart_login_bookmark_icon_url\": true,\n" +
          "  \"supports_implicit_sdk_logging\": \"true\",\n" +
          "  \"suggested_events_setting\": \"[]\",\n" +
          "  \"aam_rules\": \"hello\",\n" +
          "  \"protected_mode_rules\": \"hello\",\n" +
          "  \"app_events_session_timeout\": 6.4\n" +
          "}"

  @Test
  fun `parse valid json`() {
    val test = JSONObject(validJson)
    val result = FetchedAppSettingsManager.parseAppSettingsFromJSON("aa", test)

    assertThat(result.supportsImplicitLogging()).isTrue
    assertEquals("{}", result.suggestedEventsSetting)
    assertEquals("{}", result.rawAamRules)
    assertEquals(60, result.sessionTimeoutInSeconds)
    assertEquals(EnumSet.of(SmartLoginOption.Enabled), result.smartLoginOptions)
    assertEquals("swag", result.smartLoginBookmarkIconURL)
    assertEquals("yolo", result.smartLoginMenuIconURL)
    assertThat(result.dialogConfigurations.isEmpty()).isTrue
    assertThat(result.migratedAutoLogValues).isNotEmpty
    assertThat(result.migratedAutoLogValues?.get("auto_log_app_events_default")).isTrue
    assertThat(result.migratedAutoLogValues?.get("auto_log_app_events_enabled")).isTrue
    assertThat(result.blocklistEvents).isNotNull
    assertEquals(JSONArray(listOf("test_event_for_block_list_1", "test_event_for_block_list_2")), result.blocklistEvents)

    val expectedRedactedEvents = JSONArray(listOf(mapOf("key" to "FilteredEvent", "value" to listOf("abc", "def")), mapOf("key" to "RedactedEvent", "value" to listOf("opq", "xyz"))))
    assertThat(result.redactedEvents).isNotNull
    assertEquals(result.redactedEvents?.length(), expectedRedactedEvents.length())
    for (i in 0 until result.redactedEvents!!.length()) {
      val obj = result.redactedEvents?.getJSONObject(i)
      assertEquals(obj?.getString("key"), expectedRedactedEvents.getJSONObject(i).getString("key"))
      assertEquals(obj?.getJSONArray("value"), expectedRedactedEvents.getJSONObject(i).getJSONArray("value"))
    }
    
    // defaults
    assertThat(result.nuxEnabled).isFalse
    assertEquals("", result.nuxContent)
    assertThat(result.dialogConfigurations.isEmpty()).isTrue
    assertEquals("", result.restrictiveDataSetting)
  }

  @Test
  fun `parse invalid value types`() {
    val test = JSONObject(invalidValueTypesJson)
    val result = FetchedAppSettingsManager.parseAppSettingsFromJSON("aa", test)

    assertTrue(
        result.supportsImplicitLogging()) // actually allows case-insensitive value of true/false
    assertEquals(
        "[]", result.suggestedEventsSetting) // raw string is saved, callers job to figure out :)
    assertEquals("hello", result.rawAamRules)
    assertEquals(6, result.sessionTimeoutInSeconds) // will cast to int even tho its float/double
    assertEquals("", result.smartLoginMenuIconURL)
  }
}
