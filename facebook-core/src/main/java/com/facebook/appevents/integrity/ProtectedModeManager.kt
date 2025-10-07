/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.integrity

import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONArray

@AutoHandleExceptions
object ProtectedModeManager {
    private var enabled = false
    private const val PROTECTED_MODE_IS_APPLIED_KEY = "pm"
    private const val PROTECTED_MODE_IS_APPLIED_VALUE = "1"
    private const val PROTECTED_MODE_METADATA_KEY = "pm_metadata"

    val defaultStandardParameterNames: HashSet<String> by lazy {
        hashSetOf(
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
    }

    private var standardParams: HashSet<String>? = null;

    @JvmStatic
    fun enable() {
        enabled = true
        loadStandardParams()
    }

    @JvmStatic
    fun disable() {
        enabled = false
    }

    @JvmStatic
    fun isEnabled(): Boolean {
        return enabled
    }

    private fun loadStandardParams() {
        val settings =
            FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false)
                ?: return
        // Load the standard params from app settings first, if it doesn't exist, then use the default
        // one inside the class
        standardParams = convertJSONArrayToHashSet(settings.protectedModeStandardParamsSetting)
            ?: defaultStandardParameterNames
    }

    private fun convertJSONArrayToHashSet(jsonArray: JSONArray?): HashSet<String>? {
        if (jsonArray == null || jsonArray!!.length() == 0) {
            return null
        }
        val hashSet: HashSet<String> = HashSet()
        for (i in 0 until jsonArray.length()) {
            val element: String = jsonArray.getString(i)
            hashSet.add(element)
        }
        return hashSet
    }

    /** Process parameters for protected mode */
    @JvmStatic
    fun processParametersForProtectedMode(
        parameters: Bundle?
    ) {
        if (!enabled || parameters == null || parameters.isEmpty || standardParams == null) {
            return
        }

        val paramsToRemove = mutableListOf<String>()

        parameters.keySet().forEach { param ->
            if (param !in standardParams!!) {
                paramsToRemove.add(param)
            }
        }

        var anyRemoved = false
        paramsToRemove.forEach { paramToRemove ->
            if (parameters.containsKey(paramToRemove)) {
                parameters.remove(paramToRemove)
                anyRemoved = true
            }
        }
        val pmMetadata = org.json.JSONObject().apply {
            put("cd", anyRemoved)
        }
        parameters.putString(PROTECTED_MODE_METADATA_KEY, pmMetadata.toString())
        parameters.putString(PROTECTED_MODE_IS_APPLIED_KEY, PROTECTED_MODE_IS_APPLIED_VALUE)
    }

    fun protectedModeIsApplied(parameters: Bundle?): Boolean {
        if (parameters == null) {
            // if parameters aren't provided then the flag indicating core setup can't be assumed implicitly
            return false
        }
        return parameters.containsKey(PROTECTED_MODE_IS_APPLIED_KEY) && parameters.get(
            PROTECTED_MODE_IS_APPLIED_KEY
        ) == PROTECTED_MODE_IS_APPLIED_VALUE
    }
}
