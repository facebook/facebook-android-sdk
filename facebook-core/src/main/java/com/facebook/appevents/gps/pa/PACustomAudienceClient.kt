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
import android.net.Uri
import android.os.OutcomeReceiver
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
import com.facebook.appevents.internal.Constants
import com.facebook.appevents.restrictivedatafilter.RestrictiveDataManager
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.Executors

@AutoHandleExceptions
object PACustomAudienceClient {
    private val TAG = "Fledge: " + PACustomAudienceClient::class.java.simpleName
    private const val BUYER = "facebook.com"
    private const val BASE_URI = "https://www.facebook.com/privacy_sandbox/pa/logic"
    private const val DELIMITER = "@"
    // Sync with RestrictiveDataManager.REPLACEMENT_STRING
    private const val REPLACEMENT_STRING = "_removed_"
    private var enabled = false
    private var customAudienceManager: CustomAudienceManager? = null

    @JvmStatic
    @TargetApi(34)
    fun enable() {
        val context = FacebookSdk.getApplicationContext()
        try {
            customAudienceManager = CustomAudienceManager.get(context)
            if (customAudienceManager != null) {
                enabled = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get CustomAudienceManager: " + e.message)
        } catch (e: NoClassDefFoundError) {
            Log.w(TAG, "Failed to get CustomAudienceManager: " + e.message)
        } catch (e: NoSuchMethodError) {
            Log.w(TAG, "Failed to get CustomAudienceManager: " + e.message)
        }
    }

    @TargetApi(34)
    fun joinCustomAudience(appId: String, event: AppEvent) {
        if (!enabled) return

        val callback: OutcomeReceiver<Any, Exception> =
            object : OutcomeReceiver<Any, Exception> {
                override fun onResult(result: Any) {
                    Log.i(TAG, "Successfully joined custom audience")
                }

                override fun onError(error: Exception) {
                    Log.e(TAG, error.toString())
                }
            }

        try {
            val caName = validateAndCreateCAName(appId, event) ?: return

            // Each custom audience has to be attached with at least one ad to be valid, so we need to create a dummy ad and attach it to the ca.
            val dummyAd = AdData.Builder()
                .setRenderUri(Uri.parse("$BASE_URI/ad"))
                .setMetadata("{'isRealAd': false}")
                .build()
            val trustedBiddingData = TrustedBiddingData.Builder()
                .setTrustedBiddingUri(Uri.parse("$BASE_URI?trusted_bidding"))
                .setTrustedBiddingKeys(listOf(""))
                .build()

            val ca: CustomAudience =
                CustomAudience.Builder()
                    .setName(caName)
                    .setBuyer(AdTechIdentifier.fromString(BUYER))
                    .setDailyUpdateUri(Uri.parse("$BASE_URI?daily"))
                    .setBiddingLogicUri(Uri.parse("$BASE_URI?bidding"))
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
            Log.w(TAG, "Failed to join Custom Audience: " + e.message)
        }
    }

    private fun validateAndCreateCAName(appId: String, event: AppEvent): String? {
        val eventName = event.getJSONObject().get(Constants.EVENT_NAME_EVENT_KEY)
        if (eventName == REPLACEMENT_STRING) {
            return null
        }

        return appId + DELIMITER + eventName
    }
}
