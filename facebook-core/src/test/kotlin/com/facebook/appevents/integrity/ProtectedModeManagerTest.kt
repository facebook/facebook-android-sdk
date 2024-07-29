/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.*
import org.assertj.core.api.Assertions
import org.junit.Assert.assertTrue
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ProtectedModeManager::class, FacebookSdk::class, FetchedAppSettingsManager::class)
class ProtectedModeManagerTest : FacebookPowerMockTestCase() {

    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockStandardParamsFromServer: JSONArray
    private val mockAppID = "123"
    private val emptyJSONArray = JSONArray()


    @Before
    override fun setup() {
        super.setup()
        PowerMockito.mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)

        mockStandardParamsFromServer = JSONArray()
        mockStandardParamsFromServer.put("standard_param_from_server_1")
        mockStandardParamsFromServer.put("standard_param_from_server_2")
    }

    @After
    fun tearDown() {
        ProtectedModeManager.disable()
    }

    fun initMockFetchedAppSettings(mockStandardParams:JSONArray?) {
        val mockFetchedAppSettings = FetchedAppSettings(
                false,
                "",
                false,
                1,
                SmartLoginOption.parseOptions(0),
                emptyMap(),
                false,
                mockFacebookRequestErrorClassification,
                "",
                "",
                false,
                codelessEventsEnabled = false,
                eventBindings = emptyJSONArray,
                sdkUpdateMessage = "",
                trackUninstallEnabled = false,
                monitorViaDialogEnabled = false,
                rawAamRules = "",
                suggestedEventsSetting = "",
                restrictiveDataSetting = "",
                protectedModeStandardParamsSetting = mockStandardParams,
                MACARuleMatchingSetting = emptyJSONArray,
                migratedAutoLogValues = null,
                blocklistEvents = emptyJSONArray,
                redactedEvents = emptyJSONArray,
                sensitiveParams = emptyJSONArray
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
                .thenReturn(mockFetchedAppSettings)
    }

    @Test
    fun `test default standard params list`() {
        val expectedParameters = hashSetOf(
            "_currency",
            "_valueToSum",
            "fb_availability",
            "fb_body_style",
            "fb_checkin_date",
            "fb_checkout_date",
            "fb_city",
            "fb_condition_of_vehicle",
            "fb_content_ids",
            "fb_content_type",
            "fb_contents",
            "fb_country",
            "fb_currency",
            "fb_delivery_category",
            "fb_departing_arrival_date",
            "fb_departing_departure_date",
            "fb_destination_airport",
            "fb_destination_ids",
            "fb_dma_code",
            "fb_drivetrain",
            "fb_exterior_color",
            "fb_fuel_type",
            "fb_hotel_score",
            "fb_interior_color",
            "fb_lease_end_date",
            "fb_lease_start_date",
            "fb_listing_type",
            "fb_make",
            "fb_mileage.unit",
            "fb_mileage.value",
            "fb_model",
            "fb_neighborhood",
            "fb_num_adults",
            "fb_num_children",
            "fb_num_infants",
            "fb_num_items",
            "fb_order_id",
            "fb_origin_airport",
            "fb_postal_code",
            "fb_predicted_ltv",
            "fb_preferred_baths_range",
            "fb_preferred_beds_range",
            "fb_preferred_neighborhoods",
            "fb_preferred_num_stops",
            "fb_preferred_price_range",
            "fb_preferred_star_ratings",
            "fb_price",
            "fb_property_type",
            "fb_region",
            "fb_returning_arrival_date",
            "fb_returning_departure_date",
            "fb_state_of_vehicle",
            "fb_suggested_destinations",
            "fb_suggested_home_listings",
            "fb_suggested_hotels",
            "fb_suggested_jobs",
            "fb_suggested_local_service_businesses",
            "fb_suggested_location_based_items",
            "fb_suggested_vehicles",
            "fb_transmission",
            "fb_travel_class",
            "fb_travel_end",
            "fb_travel_start",
            "fb_trim",
            "fb_user_bucket",
            "fb_value",
            "fb_vin",
            "fb_year",
            "lead_event_source",
            "predicted_ltv",
            "product_catalog_id",

            // AppCustomEventField list
            "app_user_id",
            "appVersion",
            "_eventName",
            "_eventName_md5",
            /* duplicated _currency */
            "_implicitlyLogged",
            "_inBackground",
            "_isTimedEvent",
            "_logTime",
            /* duplicated fb_order_id */
            "_session_id",
            "_ui",
            /* duplicated _valueToSum */
            "_valueToUpdate",
            "_is_fb_codeless",
            "_is_suggested_event",
            "_fb_pixel_referral_id",
            "fb_pixel_id",
            "trace_id",
            "subscription_id",
            /* duplicated predicted_ltv */
            "event_id",
            "_restrictedParams",
            "_onDeviceParams",
            "purchase_valid_result_type",
            "core_lib_included",
            "login_lib_included",
            "share_lib_included",
            "place_lib_included",
            "messenger_lib_included",
            "applinks_lib_included",
            "marketing_lib_included",
            "_codeless_action",
            "sdk_initialized",
            "billing_client_lib_included",
            "billing_service_lib_included",
            "user_data_keys",
            "device_push_token",
            "fb_mobile_pckg_fp",
            "fb_mobile_app_cert_hash",
            "aggregate_id",
            "anonymous_id",
            "campaign_ids",

            // AppEventsIgnoredParams List
            "fb_post_attachment",
            "receipt_data",

            // List from dev doc
            "ad_type",
            "fb_content",
            "fb_content_id",
            /* duplicated fb_content_type */
            /* duplicated fb_currency */
            "fb_description",
            "fb_level",
            "fb_max_rating_value",
            /* duplicated fb_num_items */
            /* duplicated fb_order_id */
            "fb_payment_info_available",
            "fb_registration_method",
            /* duplicated fb_search_string */
            "fb_success",
            "pm",
            "_audiencePropertyIds",
            "cs_maca"
        )
        assertEquals(expectedParameters, ProtectedModeManager.defaultStandardParameterNames)
    }

    @Test
    fun `test null as parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = null
        val expectedParameters = null

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test empty parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle()
        val expectedParameters = Bundle()

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test all standard parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("pm", "1")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test filter out non-standard parameters when enable and server return standard params list`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("non_standard_param_1", "value_1")
            putString("non_standard_param_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
            putString("pm", "1")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    // This should not happen in the current design, the server will drop empty standard params.
    // Adding this test case to ensure empty standard params from server will not crash the App in
    // case.
    fun `test filter out non-standard parameters when enable and server return empty standard params list`() {
        initMockFetchedAppSettings(emptyJSONArray)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("fb_product_price_amount", "0.990")
            putString("quantity", "1")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("pm", "1")
        }

        // We use static standard params list defined in FB SDK.
        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test filter out non-standard parameters when enable and server do not return standard params`() {
        initMockFetchedAppSettings(null)
        ProtectedModeManager.enable()
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("pm", "1")
        }

        // We use static standard params list defined in FB SDK.
        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test not filter out non-standard parameters when disable`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        val mockParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }
        val expectedParameters = Bundle().apply {
            putString("fb_currency", "USD")
            putString("fb_price", "0.99")
            putString("standard_param_from_server_1", "value_1")
            putString("standard_param_from_server_2", "value_2")
        }

        ProtectedModeManager.processParametersForProtectedMode(mockParameters)
        assertEqual(mockParameters, expectedParameters)
    }

    @Test
    fun `test protected mode is applied`() {
        initMockFetchedAppSettings(mockStandardParamsFromServer)
        val mockParametersWithPM = Bundle().apply {
            putString("fb_currency", "USD")
            putString("pm", "1")
        }

        var protectedModeIsApplied = ProtectedModeManager.protectedModeIsApplied(mockParametersWithPM)
        Assertions.assertThat(protectedModeIsApplied).isTrue

        val mockParametersWithoutPM = Bundle().apply {
            putString("fb_currency", "USD")
        }

        protectedModeIsApplied = ProtectedModeManager.protectedModeIsApplied(mockParametersWithoutPM)
        Assertions.assertThat(protectedModeIsApplied).isFalse

        val mockParametersWithPMNotTrue = Bundle().apply {
            putString("fb_currency", "USD")
            putString("pm", "0")
        }

        protectedModeIsApplied = ProtectedModeManager.protectedModeIsApplied(mockParametersWithPMNotTrue)
        Assertions.assertThat(protectedModeIsApplied).isFalse
    }

    private fun isEqual(mockBundle: Bundle?, expectedBundle: Bundle?): Boolean {
        if (mockBundle == null && expectedBundle == null) {
            return true
        }
        val s1 = mockBundle?.keySet() ?: return false
        val s2 = expectedBundle?.keySet() ?: return false

        if (!s1.equals(s2)) {
            return false
        }

        for (s in s1) {
            val v1 = mockBundle.get(s) ?: return false
            val v2 = expectedBundle.get(s) ?: return false
            if (v1 != v2) {
                return false
            }
        }
        return true
    }

    private fun assertEqual(mockBundle: Bundle?, expectedBundle: Bundle?) {
        assertTrue(isEqual(mockBundle,expectedBundle ))
    }
}
