/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */


package com.facebook.appevents.gps.pa

import android.adservices.common.AdTechIdentifier
import android.adservices.customaudience.CustomAudience
import android.adservices.customaudience.CustomAudienceManager
import android.adservices.customaudience.JoinCustomAudienceRequest
import android.annotation.TargetApi
import android.net.Uri
import android.os.OutcomeReceiver
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.Executors

@AutoHandleExceptions
object PACustomAudienceClient {
    private val TAG = "Fledge: " + PACustomAudienceClient::class.java.simpleName
    private const val BUYER = "facebook.com"
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
    fun joinCustomAudience() {
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
            val ca: CustomAudience =
                CustomAudience.Builder().setName("").setBuyer(AdTechIdentifier.fromString(BUYER))
                    .setDailyUpdateUri(Uri.parse("")).setBiddingLogicUri(Uri.parse(""))
                    .build()
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
}
