/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

object InstallReferrerUtil {
  private const val IS_REFERRER_UPDATED = "is_referrer_updated"

  @JvmStatic
  fun tryUpdateReferrerInfo(callback: Callback) {
    if (!isUpdated) {
      tryConnectReferrerInfo(callback)
    }
  }

  private fun tryConnectReferrerInfo(callback: Callback) {
    val referrerClient =
        InstallReferrerClient.newBuilder(FacebookSdk.getApplicationContext()).build()
    val installReferrerStateListener =
        object : InstallReferrerStateListener {
          @AutoHandleExceptions
          override fun onInstallReferrerSetupFinished(responseCode: Int) {
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
          }

          override fun onInstallReferrerServiceDisconnected() = Unit
        }
    try {
      referrerClient.startConnection(installReferrerStateListener)
    } catch (e: Exception) {
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
