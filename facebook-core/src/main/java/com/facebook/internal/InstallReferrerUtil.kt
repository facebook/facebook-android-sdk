/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object InstallReferrerUtil {
  private const val IS_REFERRER_UPDATED = "is_referrer_updated"
  private const val REFERRER_FETCH_TIMEOUT = 5L

  @JvmStatic
  fun tryUpdateReferrerInfo(callback: Callback) {
    if (!isUpdated) {
      tryConnectReferrerInfo(callback)
    }
  }

  @JvmStatic
  fun tryUpdateReferrerInfoBlocking(callback: Callback) {
    if (!isUpdated) {
      val latch = CountDownLatch(1)
      tryConnectReferrerInfo(callback, latch)
      try {
        latch.await(REFERRER_FETCH_TIMEOUT, TimeUnit.SECONDS)
      } catch (e: InterruptedException) { /* proceed with whatever we have */ }
    }
  }

  private fun tryConnectReferrerInfo(callback: Callback, latch: CountDownLatch? = null) {
    val referrerClient =
        InstallReferrerClient.newBuilder(FacebookSdk.getApplicationContext()).build()
    val installReferrerStateListener =
        object : InstallReferrerStateListener {
          @AutoHandleExceptions
          override fun onInstallReferrerSetupFinished(responseCode: Int) {
            try {
              when (responseCode) {
                InstallReferrerResponse.OK -> {
                  val response: ReferrerDetails =
                      try {
                        referrerClient.installReferrer
                      } catch (e: RemoteException) {
                        return
                      }
                  val referrerUrl = response.installReferrer
                  if (referrerUrl != null &&
                      (referrerUrl.contains("fb") || referrerUrl.contains("facebook"))) {
                    callback.onReceiveReferrerUrl(referrerUrl)
                  }
                  // Even if we are not interested in the url, there is no reason to update again
                  updateReferrer()
                }
                InstallReferrerResponse.FEATURE_NOT_SUPPORTED ->
                    updateReferrer() // No point retrying if feature not supported
                InstallReferrerResponse.SERVICE_UNAVAILABLE -> {}
              }
              try {
                referrerClient.endConnection()
              } catch (e: Exception) {
                // Silent endConnection errors for unit test and else
              }
            } finally {
              latch?.countDown()
            }
          }

          override fun onInstallReferrerServiceDisconnected() {
            latch?.countDown()
          }
        }
    try {
      referrerClient.startConnection(installReferrerStateListener)
    } catch (e: Exception) {
      latch?.countDown()
      return
    }
  }

  private fun updateReferrer() {
    val preferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
    preferences.edit().putBoolean(IS_REFERRER_UPDATED, true).apply()
  }

  private val isUpdated: Boolean
    get() {
      val preferences =
          FacebookSdk.getApplicationContext()
              .getSharedPreferences(FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
      return preferences.getBoolean(IS_REFERRER_UPDATED, false)
    }

  fun interface Callback {
    fun onReceiveReferrerUrl(s: String?)
  }
}
