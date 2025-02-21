/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.facebook.appevents.gps.pa

import android.adservices.common.AdData
import android.adservices.common.AdSelectionSignals
import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.JoinCustomAudienceRequest
import android.adservices.customaudience.TrustedBiddingData
import android.annotation.TargetApi
import android.os.Bundle
import android.os.OutcomeReceiver
import android.util.Log
import androidx.core.net.toUri
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.gps.GpsDebugLogger
import com.facebook.appevents.internal.Constants
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONException
import java.util.concurrent.Executors

@AutoHandleExceptions
object PACustomAudienceClient {
    private val TAG = "Fledge: " + PACustomAudienceClient::class.java.simpleName
    private const val BUYER = "facebook.com"
    private const val DELIMITER = "@"
    private const val GPS_PREFIX = "gps"

    // Sync with RestrictiveDataManager.REPLACEMENT_STRING
    private const val REPLACEMENT_STRING = "_removed_"
    private var enabled = false
    private var isInitialized = false
    private var customAudienceManager: CustomAudienceManager? = null
    private lateinit var gpsDebugLogger: GpsDebugLogger
    private lateinit var baseUri: String

    @JvmStatic
    @TargetApi(34)
    fun enable() {
        isInitialized = true
        val context = FacebookSdk.getApplicationContext()
        gpsDebugLogger = GpsDebugLogger(context)
        baseUri = "https://www.${FacebookSdk.getFacebookDomain()}/privacy_sandbox/pa/logic"

        var errorMsg: String? = null
        try {
            customAudienceManager = CustomAudienceManager.get(context)
            if (customAudienceManager != null) {
                enabled = true
            }
        } catch (e: Exception) {
            errorMsg = e.toString()
            Log.w(TAG, "Failed to get CustomAudienceManager: $e")
        } catch (e: NoClassDefFoundError) {
            errorMsg = e.toString()
            Log.w(TAG, "Failed to get CustomAudienceManager: $e")
        } catch (e: NoSuchMethodError) {
            errorMsg = e.toString()
            Log.w(TAG, "Failed to get CustomAudienceManager: $e")
        }

        if (enabled == false) {
            gpsDebugLogger.log(
                Constants.GPS_PA_FAILED,
                Bundle().apply { putString(Constants.GPS_PA_FAILED_REASON, errorMsg) })
        }
    }

    fun joinCustomAudience(appId: String?, eventName: String?) {
        if (!isInitialized) {
            enable()
        }

        if (!enabled) return

        joinCustomAudienceImpl(appId, eventName)
    }


    fun joinCustomAudience(appId: String?, event: AppEvent?) {
        if (!isInitialized) {
            enable()
        }

        if (!enabled) return

        var eventName: String? = null
        try {
            eventName = event?.getJSONObject()?.getString(Constants.EVENT_NAME_EVENT_KEY)
        } catch (e: JSONException) {
            Log.w(TAG, "Failed to get event name from event.")
        }

        joinCustomAudienceImpl(appId, eventName)
    }

    @TargetApi(34)
    private fun joinCustomAudienceImpl(appId: String?, eventName: String?) {
        val caName = validateAndCreateCAName(appId, eventName)
        if (caName == null) {
            return
        }

        val callback: OutcomeReceiver<Any, Exception> =
            object : OutcomeReceiver<Any, Exception> {
                override fun onResult(result: Any) {
                    Log.i(TAG, "Successfully joined custom audience")
                    gpsDebugLogger.log(Constants.GPS_PA_SUCCEED, null)
                }

                override fun onError(error: Exception) {
                    Log.e(TAG, error.toString())
                    gpsDebugLogger.log(
                        Constants.GPS_PA_FAILED,
                        Bundle().apply {
                            putString(
                                Constants.GPS_PA_FAILED_REASON,
                                error.toString()
                            )
                        })
                }
            }

        try {
            // Each custom audience has to be attached with at least one ad to be valid, so we need to create a dummy ad and attach it to the ca.
            val dummyAd = AdData.Builder()
                .setRenderUri("$baseUri/ad".toUri())
                .setMetadata("{'isRealAd': false}")
                .build()
            val trustedBiddingData = TrustedBiddingData.Builder()
                .setTrustedBiddingUri("$baseUri?trusted_bidding".toUri())
                .setTrustedBiddingKeys(listOf(""))
                .build()

            val ca: CustomAudience =
                CustomAudience.Builder()
                    .setName(caName)
                    .setBuyer(AdTechIdentifier.fromString(BUYER))
                    .setDailyUpdateUri("$baseUri?daily&app_id=$appId".toUri())
                    .setBiddingLogicUri("$baseUri?bidding".toUri())
                    .setTrustedBiddingData(trustedBiddingData)
                    .setUserBiddingSignals(AdSelectionSignals.fromString("{}"))
                    .setAds(listOf(dummyAd)).build()
            val request: JoinCustomAudienceRequest =
                JoinCustomAudienceRequest.Builder().setCustomAudience(ca).build()

            customAudienceManager?.joinCustomAudience(
                request,
                Executors.newSingleThreadExecutor(),
                callback
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to join Custom Audience: $e")
            gpsDebugLogger.log(
                Constants.GPS_PA_FAILED,
                Bundle().apply { putString(Constants.GPS_PA_FAILED_REASON, e.toString()) })
        }
    }

    private fun validateAndCreateCAName(appId: String?, eventName: String?): String? {
        if (appId == null || eventName == null) {
            return null
        }
        if (eventName == REPLACEMENT_STRING || eventName.contains(GPS_PREFIX)) {
            return null
        }

        return appId + DELIMITER + eventName
    }
}
