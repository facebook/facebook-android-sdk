/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
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
}
