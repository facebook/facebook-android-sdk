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
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEvent
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
    CustomAudience::class,
    JoinCustomAudienceRequest::class,
    AdSelectionSignals::class,
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

        val ad = mock(AdData::class.java)
        val adBuilder = mock(AdData.Builder::class.java)
        whenever(adBuilder.setMetadata(any<String>())).thenReturn(adBuilder)
        whenever(adBuilder.setRenderUri(any<Uri>())).thenReturn(adBuilder)
        whenever(adBuilder.build()).thenReturn(ad)
        whenNew(AdData.Builder::class.java).withAnyArguments().thenReturn(adBuilder)

        val trustedBiddingData = mock(TrustedBiddingData::class.java)
        val trustedBiddingDataBuilder = mock(TrustedBiddingData.Builder::class.java)
        whenever(trustedBiddingDataBuilder.setTrustedBiddingUri(any<Uri>())).thenReturn(trustedBiddingDataBuilder)
        whenever(trustedBiddingDataBuilder.setTrustedBiddingKeys(any<List<String>>())).thenReturn(trustedBiddingDataBuilder)
        whenever(trustedBiddingDataBuilder.build()).thenReturn(trustedBiddingData)
        whenNew(TrustedBiddingData.Builder::class.java).withAnyArguments().thenReturn(trustedBiddingDataBuilder)

        val adSelectionSignals = mock(AdSelectionSignals::class.java)
        mockStatic(AdSelectionSignals::class.java)
        whenever(AdSelectionSignals.fromString(any<String>())).thenReturn(adSelectionSignals)

        val ca = mock(CustomAudience::class.java)
        val caBuilder = mock(CustomAudience.Builder::class.java)
        whenever(caBuilder.setName(any<String>())).thenReturn(caBuilder)
        whenever(caBuilder.setBuyer(any<AdTechIdentifier>())).thenReturn(caBuilder)
        whenever(caBuilder.setDailyUpdateUri(any<Uri>())).thenReturn(caBuilder)
        whenever(caBuilder.setBiddingLogicUri(any<Uri>())).thenReturn(caBuilder)
        whenever(caBuilder.setAds(any<List<AdData>>())).thenReturn(caBuilder)
        whenever(caBuilder.setTrustedBiddingData(any<TrustedBiddingData>())).thenReturn(caBuilder)
        whenever(caBuilder.setUserBiddingSignals(any<AdSelectionSignals>())).thenReturn(caBuilder)
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
        PACustomAudienceClient.joinCustomAudience("1234", createEvent("test_event"))

        verify(customAudienceManager, times(0))?.joinCustomAudience(
            any<JoinCustomAudienceRequest>(),
            any<Executor>(),
            any<OutcomeReceiver<Any, Exception>>()
        )
    }

    @Test
    fun testAvailableCustomAudienceManager() {
        mockStatic(CustomAudienceManager::class.java)
        whenever(CustomAudienceManager.get(any<Context>())).thenReturn(customAudienceManager)

        PACustomAudienceClient.enable()
        PACustomAudienceClient.joinCustomAudience("1234", createEvent("test_event"))
        verify(customAudienceManager, times(1))?.joinCustomAudience(
            any<JoinCustomAudienceRequest>(),
            any<Executor>(),
            any<OutcomeReceiver<Any, Exception>>()
        )
    }

    @Test
    fun testInvalidCAName() {
        mockStatic(CustomAudienceManager::class.java)
        whenever(CustomAudienceManager.get(any<Context>())).thenReturn(customAudienceManager)

        PACustomAudienceClient.enable()
        PACustomAudienceClient.joinCustomAudience("1234", createEvent("_removed_"))
        verify(customAudienceManager, times(0))?.joinCustomAudience(
            any<JoinCustomAudienceRequest>(),
            any<Executor>(),
            any<OutcomeReceiver<Any, Exception>>()
        )
    }

    private fun createEvent(eventName: String): AppEvent {
        val params = Bundle()
        return AppEvent(
            "context_name", eventName, 0.0, params, false,
            isInBackground = false,
            currentSessionId = null
        )
    }

}