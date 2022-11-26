/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
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
    whenever(FacebookSdk.isFullyInitialized()).thenReturn(true)
    mockApplicationContext = mock(Context::class.java)
    whenever(
            mockApplicationContext.getSharedPreferences(
                FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE))
        .thenReturn(mockSharedPreference)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)

    mockReferrerDetails = mock(ReferrerDetails::class.java)
    mockInstallReferrerClient = mock(InstallReferrerClient::class.java)
    whenever(mockInstallReferrerClient.installReferrer).thenReturn(mockReferrerDetails)
    mockInstallReferrerClientBuilder = mock(InstallReferrerClient.Builder::class.java)
    whenever(mockInstallReferrerClientBuilder.build()).thenReturn(mockInstallReferrerClient)
    mockStatic(InstallReferrerClient::class.java)
    whenever(InstallReferrerClient.newBuilder(mockApplicationContext))
        .thenReturn(mockInstallReferrerClientBuilder)
  }

  @Test
  fun `test connection with fb referer return`() {
    var connectionCounter = 0
    var didReceivedReferrerUrl = false
    val referrerUrl = "facebook.com"
    whenever(mockReferrerDetails.installReferrer).thenReturn(referrerUrl)
    whenever(mockInstallReferrerClient.startConnection(anyObject())).then {
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
    assertThat(didReceivedReferrerUrl).isTrue
  }

  @Test
  fun `test connection twice`() {
    var connectionCounter = 0
    whenever(mockInstallReferrerClient.startConnection(anyObject())).then {
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
    whenever(mockInstallReferrerClient.startConnection(anyObject())).then {
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
