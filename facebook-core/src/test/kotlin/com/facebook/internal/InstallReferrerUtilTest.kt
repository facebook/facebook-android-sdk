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
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.MockSharedPreference
import com.facebook.util.common.anyObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class, InstallReferrerClient::class, InstallReferrerClient.Builder::class)
class InstallReferrerUtilTest : FacebookPowerMockTestCase() {
  private lateinit var mockApplicationContext: Context
  private lateinit var mockInstallReferrerClient: InstallReferrerClient
  private lateinit var mockInstallReferrerClientBuilder: InstallReferrerClient.Builder
  private lateinit var mockReferrerDetails: ReferrerDetails
  private lateinit var mockSharedPreference: MockSharedPreference
  private val emptyCallback =
      object : InstallReferrerUtil.Callback {
        override fun onReceiveReferrerUrl(s: String?) = Unit
      }

  @Before
  fun init() {
    mockSharedPreference = MockSharedPreference()
    mockStatic(FacebookSdk::class.java)
    `when`(FacebookSdk.isFullyInitialized()).thenReturn(true)
    mockApplicationContext = mock(Context::class.java)
    `when`(
            mockApplicationContext.getSharedPreferences(
                FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE))
        .thenReturn(mockSharedPreference)
    `when`(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)

    mockReferrerDetails = mock(ReferrerDetails::class.java)
    mockInstallReferrerClient = mock(InstallReferrerClient::class.java)
    `when`(mockInstallReferrerClient.installReferrer).thenReturn(mockReferrerDetails)
    mockInstallReferrerClientBuilder = mock(InstallReferrerClient.Builder::class.java)
    `when`(mockInstallReferrerClientBuilder.build()).thenReturn(mockInstallReferrerClient)
    mockStatic(InstallReferrerClient::class.java)
    `when`(InstallReferrerClient.newBuilder(mockApplicationContext))
        .thenReturn(mockInstallReferrerClientBuilder)
  }

  @Test
  fun `test connection with fb referer return`() {
    var connectionCounter = 0
    var didReceivedReferrerUrl = false
    val referrerUrl = "facebook.com"
    `when`(mockReferrerDetails.installReferrer).thenReturn(referrerUrl)
    `when`(mockInstallReferrerClient.startConnection(anyObject())).then {
      connectionCounter += 1
      val listener = it.getArgument<InstallReferrerStateListener>(0)
      listener.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)
      return@then null
    }

    val callback =
        object : InstallReferrerUtil.Callback {
          override fun onReceiveReferrerUrl(s: String?) {
            assertEquals(s, referrerUrl)
            didReceivedReferrerUrl = true
          }
        }

    InstallReferrerUtil.tryUpdateReferrerInfo(callback)
    assertEquals(
        1,
        connectionCounter,
    )
    assertTrue(didReceivedReferrerUrl)
  }

  @Test
  fun `test connection twice`() {
    var connectionCounter = 0
    `when`(mockInstallReferrerClient.startConnection(anyObject())).then {
      connectionCounter += 1
      val listener = it.getArgument<InstallReferrerStateListener>(0)
      listener.onInstallReferrerSetupFinished(InstallReferrerClient.InstallReferrerResponse.OK)
      return@then null
    }
    InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback)
    InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback)
    assertEquals(1, connectionCounter)
  }

  @Test
  fun `test service unavailable`() {
    var connectionCounter = 0
    `when`(mockInstallReferrerClient.startConnection(anyObject())).then {
      connectionCounter += 1
      val listener = it.getArgument<InstallReferrerStateListener>(0)
      listener.onInstallReferrerSetupFinished(
          InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE)
      return@then null
    }
    repeat(3) { InstallReferrerUtil.tryUpdateReferrerInfo(emptyCallback) }
    assertEquals(3, connectionCounter)
  }
}
