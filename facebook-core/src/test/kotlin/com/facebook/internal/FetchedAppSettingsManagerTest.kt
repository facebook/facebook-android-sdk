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
                "  \"redacted_events\": [{\"key\":\"FilteredEvent\", \"value\":[\"abc\", \"def\"]}, {\"key\":\"RedactedEvent\", \"value\":[\"opq\", \"xyz\"]}],\n" +
                "  \"sensitive_params\": [{\"key\":\"test_event_1\", \"value\":[\"last name\", \"first name\"]}, {\"key\":\"test_event_2\", \"value\":[\"address\", \"ssn\"]}]},\n" +
                "  \"auto_log_app_events_default\": true,\n" +
                "  \"auto_log_app_events_enabled\": true,\n" +
                "  \"app_events_config\":{\"default_ate_status\":2,\"advertiser_id_collection_enabled\":true,\"event_collection_enabled\":true,\"iap_manual_and_auto_log_dedup_keys\":[{\"key\":\"prod_keys\",\"value\":[{\"key\":\"fb_iap_product_id\",\"value\":[{\"key\":0,\"value\":\"fb_content_id\"},{\"key\":1,\"value\":\"fb_product_item_id\"},{\"key\":2,\"value\":\"fb_iap_product_id\"}]},{\"key\":\"fb_iap_product_title\",\"value\":[{\"key\":0,\"value\":\"fb_content_title\"},{\"key\":1,\"value\":\"fb_product_title\"},{\"key\":2,\"value\":\"fb_iap_product_title\"}]},{\"key\":\"fb_iap_product_description\",\"value\":[{\"key\":0,\"value\":\"fb_description\"},{\"key\":1,\"value\":\"fb_iap_product_description\"}]},{\"key\":\"fb_iap_purchase_token\",\"value\":[{\"key\":0,\"value\":\"fb_iap_purchase_token\"},{\"key\":1,\"value\":\"fb_transaction_id\"},{\"key\":2,\"value\":\"fb_order_id\"}]},{\"key\":\"_valueToSum\",\"value\":[{\"key\":0,\"value\":\"_valueToSum\"},{\"key\":1,\"value\":\"fb_product_price_amount\"}]},{\"key\":\"fb_currency\",\"value\":[{\"key\":0,\"value\":\"fb_currency\"},{\"key\":1,\"value\":\"fb_product_price_currency\"}]}]},{\"key\":\"test_keys\"}]},\"id\":\"227791440613076\"\n" +
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
        assertEquals(
            JSONArray(
                listOf(
                    "test_event_for_block_list_1",
                    "test_event_for_block_list_2"
                )
            ), result.blocklistEvents
        )

        val expectedRedactedEvents = JSONArray(
            listOf(
                mapOf("key" to "FilteredEvent", "value" to listOf("abc", "def")),
                mapOf("key" to "RedactedEvent", "value" to listOf("opq", "xyz"))
            )
        )
        assertThat(result.redactedEvents).isNotNull
        assertEquals(result.redactedEvents?.length(), expectedRedactedEvents.length())
        for (i in 0 until result.redactedEvents!!.length()) {
            val obj = result.redactedEvents?.getJSONObject(i)
            assertEquals(
                obj?.getString("key"),
                expectedRedactedEvents.getJSONObject(i).getString("key")
            )
            assertEquals(
                obj?.getJSONArray("value"),
                expectedRedactedEvents.getJSONObject(i).getJSONArray("value")
            )
        }

        val expectedSensitiveParams = JSONArray(
            listOf(
                mapOf(
                    "key" to "test_event_1",
                    "value" to listOf("last name", "first name")
                ), mapOf("key" to "test_event_2", "value" to listOf("address", "ssn"))
            )
        )
        assertThat(result.sensitiveParams).isNotNull
        assertEquals(result.sensitiveParams?.length(), expectedSensitiveParams.length())
        for (i in 0 until result.sensitiveParams!!.length()) {
            val obj = result.sensitiveParams?.getJSONObject(i)
            assertEquals(
                obj?.getString("key"),
                expectedSensitiveParams.getJSONObject(i).getString("key")
            )
            assertEquals(
                obj?.getJSONArray("value"),
                expectedSensitiveParams.getJSONObject(i).getJSONArray("value")
            )
        }

        // defaults
        assertThat(result.nuxEnabled).isFalse
        assertEquals("", result.nuxContent)
        assertThat(result.dialogConfigurations.isEmpty()).isTrue
        assertEquals("", result.restrictiveDataSetting)

        val expectedDedupeParameters = listOf(
            Pair(
                "fb_iap_product_id",
                listOf("fb_content_id", "fb_product_item_id", "fb_iap_product_id")
            ),
            Pair(
                "fb_iap_product_title",
                listOf("fb_content_title", "fb_product_title", "fb_iap_product_title")
            ),
            Pair(
                "fb_iap_product_description",
                listOf("fb_description", "fb_iap_product_description")
            ),
            Pair(
                "fb_iap_purchase_token",
                listOf("fb_iap_purchase_token", "fb_transaction_id", "fb_order_id")
            )
        )
        assertEquals(expectedDedupeParameters, result.prodDedupeParameters)

        val expectedCurrencyParameters = listOf("fb_currency", "fb_product_price_currency")
        assertEquals(expectedCurrencyParameters, result.currencyDedupeParameters)

        val expectedPurchaseAmountParameters = listOf("_valueToSum", "fb_product_price_amount")
        assertEquals(expectedPurchaseAmountParameters, result.purchaseValueDedupeParameters)
    }

    @Test
    fun `parse invalid value types`() {
        val test = JSONObject(invalidValueTypesJson)
        val result = FetchedAppSettingsManager.parseAppSettingsFromJSON("aa", test)

        assertTrue(
            result.supportsImplicitLogging()
        ) // actually allows case-insensitive value of true/false
        assertEquals(
            "[]", result.suggestedEventsSetting
        ) // raw string is saved, callers job to figure out :)
        assertEquals("hello", result.rawAamRules)
        assertEquals(
            6,
            result.sessionTimeoutInSeconds
        ) // will cast to int even tho its float/double
        assertEquals("", result.smartLoginMenuIconURL)
    }
}
