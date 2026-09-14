/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.internal.Constants
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@PrepareForTest(FacebookSdk::class)
class AppEventTest : FacebookPowerMockTestCase() {
    @Before
    fun init() {
        mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
        whenever(FacebookSdk.isInitialized()).thenReturn(true)
    }

    @Test
    fun testOperationalParameters() {
        val appEvent = AppEventTestUtilities.getTestAppEvent()
        val json = appEvent.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertThat(json?.getString("key3")).isEqualTo("value3")
        assertThat(json?.getString("key4")).isEqualTo("value4")
    }

    @Test
    fun testAppEventSerializedChecksum() {
        val appEvent1 = AppEventTestUtilities.getTestAppEvent()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
        objectOutputStream.writeObject(appEvent1)
        val byteArrayInputStream = ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        val objectInputStream = ObjectInputStream(byteArrayInputStream)
        val appEvent2 = objectInputStream.readObject() as AppEvent

        // A secondary validation ensure that the json string matches the original
        assertThat(
            appEvent1.getJSONObject().toString() == appEvent2.getJSONObject().toString()
        ).isTrue
    }

    @Test
    fun testInboundUrlIsAddedToEvent() {
        val inboundUrl = "fb123://applinks/product?id=42"

        val appEvent = createAppEvent(inboundUrl = inboundUrl)

        assertThat(appEvent.getJSONObject().getString(Constants.EVENT_PARAM_INBOUND_URL))
            .isEqualTo(inboundUrl)
    }

    @Test
    fun testEmptyInboundUrlIsNotAddedToEvent() {
        val appEvent = createAppEvent(inboundUrl = "")

        assertThat(appEvent.getJSONObject().has(Constants.EVENT_PARAM_INBOUND_URL)).isFalse()
    }

    @Test
    fun testSdkInboundUrlOverridesDeveloperParameter() {
        val parameters = Bundle().apply {
            putString(Constants.EVENT_PARAM_INBOUND_URL, "developer-value")
        }
        val inboundUrl = "fb123://applinks/sdk-value"

        val appEvent = createAppEvent(parameters, inboundUrl)

        assertThat(appEvent.getJSONObject().getString(Constants.EVENT_PARAM_INBOUND_URL))
            .isEqualTo(inboundUrl)
    }

    @Test
    fun testInboundUrlSurvivesSerialization() {
        val inboundUrl = "fb123://applinks/persisted"
        val original = createAppEvent(inboundUrl = inboundUrl)
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(original) }

        val restored =
            ObjectInputStream(ByteArrayInputStream(output.toByteArray())).use {
                it.readObject() as AppEvent
            }

        assertThat(restored.getJSONObject().getString(Constants.EVENT_PARAM_INBOUND_URL))
            .isEqualTo(inboundUrl)
    }

    private fun createAppEvent(
        parameters: Bundle? = null,
        inboundUrl: String? = null,
    ): AppEvent =
        AppEvent(
            contextName = "test-context",
            eventName = "test-event",
            valueToSum = null,
            parameters = parameters,
            isImplicitlyLogged = false,
            isInBackground = false,
            currentSessionId = null,
            inboundUrl = inboundUrl,
        )

    @Test
    fun testActivityLabelWrittenToJson() {
        val appEvent =
            AppEvent(
                "contextName",
                "eventName",
                null,
                null,
                false,
                false,
                null,
                activityLabel = "My Screen"
            )
        assertThat(appEvent.getJSONObject().getString(Constants.EVENT_PARAM_ACTIVITY_LABEL))
            .isEqualTo("My Screen")
    }

    @Test
    fun testActivityLabelOmittedWhenNull() {
        val appEvent = AppEvent("contextName", "eventName", null, null, false, false, null)
        assertThat(appEvent.getJSONObject().has(Constants.EVENT_PARAM_ACTIVITY_LABEL)).isFalse()
    }

    @Test
    fun testActivityLabelOmittedWhenEmpty() {
        val appEvent =
            AppEvent("contextName", "eventName", null, null, false, false, null, activityLabel = "")
        assertThat(appEvent.getJSONObject().has(Constants.EVENT_PARAM_ACTIVITY_LABEL)).isFalse()
    }

    @Test
    fun testActivityLabelOverridesDeveloperParam() {
        val parameters =
            Bundle().apply { putString(Constants.EVENT_PARAM_ACTIVITY_LABEL, "developer_value") }
        val appEvent =
            AppEvent(
                "contextName",
                "eventName",
                null,
                parameters,
                false,
                false,
                null,
                activityLabel = "Resolved Label"
            )
        assertThat(appEvent.getJSONObject().getString(Constants.EVENT_PARAM_ACTIVITY_LABEL))
            .isEqualTo("Resolved Label")
    }

    @Test
    fun testActivityLabelSerializationRoundTrip() {
        val appEvent =
            AppEvent(
                "contextName",
                "eventName",
                null,
                null,
                false,
                false,
                null,
                activityLabel = "Settings"
            )
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(appEvent) }
        val restored =
            ObjectInputStream(ByteArrayInputStream(output.toByteArray())).use {
                it.readObject() as AppEvent
            }
        assertThat(restored.getJSONObject().getString(Constants.EVENT_PARAM_ACTIVITY_LABEL))
            .isEqualTo("Settings")
    }
}
