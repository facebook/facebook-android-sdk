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
import android.content.Context
import android.net.Uri
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import java.util.concurrent.Executor

@PrepareForTest(
    FacebookSdk::class,
    Context::class,
    CustomAudienceManager::class,
    AdTechIdentifier::class,
    CustomAudience.Builder::class,
    CustomAudience::class,
    JoinCustomAudienceRequest.Builder::class,
    JoinCustomAudienceRequest::class,
    PACustomAudienceClient::class
)
class PACustomAudienceClientTest : FacebookPowerMockTestCase() {
    private var customAudienceManager: CustomAudienceManager? = null

    @Before
    fun setUp() {
        val context = mock(Context::class.java)
        mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationContext()).thenReturn(context)

        customAudienceManager = mock(CustomAudienceManager::class.java)

        val adTech = mock(AdTechIdentifier::class.java)
        mockStatic(AdTechIdentifier::class.java)
        whenever(AdTechIdentifier.fromString(any<String>())).thenReturn(adTech)

        val ca = mock(CustomAudience::class.java)
        val caBuilder = mock(CustomAudience.Builder::class.java)
        whenever(caBuilder.setName(any<String>())).thenReturn(caBuilder)
        whenever(caBuilder.setBuyer(any<AdTechIdentifier>())).thenReturn(caBuilder)
        whenever(caBuilder.setDailyUpdateUri(any<Uri>())).thenReturn(caBuilder)
        whenever(caBuilder.setBiddingLogicUri(any<Uri>())).thenReturn(caBuilder)
        whenever(caBuilder.build()).thenReturn(ca)
        whenNew(CustomAudience.Builder::class.java).withAnyArguments().thenReturn(caBuilder)

        val request = mock(JoinCustomAudienceRequest::class.java)
        val requestBuilder = mock(JoinCustomAudienceRequest.Builder::class.java)
        whenNew(JoinCustomAudienceRequest.Builder::class.java).withAnyArguments()
            .thenReturn(requestBuilder)
        whenever(requestBuilder.setCustomAudience(any<CustomAudience>())).thenReturn(requestBuilder)
        whenever(requestBuilder.build()).thenReturn(request)
    }

    @Test
    fun testUnavailableCustomAudienceManager() {
        mockStatic(CustomAudienceManager::class.java)
        whenever(CustomAudienceManager.get(any<Context>())).thenReturn(null)

        PACustomAudienceClient.enable()
        PACustomAudienceClient.joinCustomAudience()

        verify(customAudienceManager, times(0))?.joinCustomAudience(any<JoinCustomAudienceRequest>(),
            any<Executor>(),
            any<OutcomeReceiver<Any, Exception>>())
    }

    @Test
    fun testAvailableCustomAudienceManager() {
        mockStatic(CustomAudienceManager::class.java)
        whenever(CustomAudienceManager.get(any<Context>())).thenReturn(customAudienceManager)

        PACustomAudienceClient.enable()
        PACustomAudienceClient.joinCustomAudience()
        verify(customAudienceManager, times(1))?.joinCustomAudience(any<JoinCustomAudienceRequest>(),
            any<Executor>(),
            any<OutcomeReceiver<Any, Exception>>())
    }

}