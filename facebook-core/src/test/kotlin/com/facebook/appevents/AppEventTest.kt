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
package com.facebook.appevents

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.internal.Constants
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class AppEventTest : FacebookPowerMockTestCase() {
  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
  }

  @Test
  fun testChecksumOfAppEventName() {
    val appEvent = AppEventTestUtilities.getTestAppEvent()
    val eventNameMd5 = appEvent.getJSONObject().getString(Constants.EVENT_NAME_MD5_EVENT_KEY)
    assertThat(eventNameMd5).isEqualTo("e0cf6877da9df873a85a2137fb5d2e26")
  }

  @Test
  fun testChecksumOfAppEvent() {
    val appEvent = AppEventTestUtilities.getTestAppEvent()
    assertThat(appEvent.isChecksumValid).isTrue
    appEvent.getJSONObject().put("new_key", "corrupted")
    assertThat(appEvent.isChecksumValid).isFalse
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
    assertThat(appEvent2.isChecksumValid).isTrue

    // A secondary validation ensure that the json string matches the original
    assertThat(appEvent1.getJSONObject().toString() == appEvent2.getJSONObject().toString()).isTrue
  }
}
