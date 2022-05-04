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

package com.facebook.internal.instrument.crashshield

import android.os.Handler
import android.os.Looper
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class,
    InstrumentData::class,
    InstrumentData.Builder::class,
    CrashShieldHandler::class,
    Looper::class,
    Handler::class)
class CrashShieldHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var mockInstrumentData: InstrumentData

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(mockInstrumentData.save()).then {
      return@then Unit
    }
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)
    whenever(InstrumentData.Builder.build(any<Throwable>(), any<InstrumentData.Type>()))
        .thenReturn(mockInstrumentData)
    PowerMockito.spy(CrashShieldHandler::class.java)
    whenever(CrashShieldHandler.isDebug()).thenReturn(false)
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117690812
  @Test
  fun `test handler disabled by default`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.disable()
    CrashShieldHandler.handleThrowable(exception, probe)
    assertThat(CrashShieldHandler.isObjectCrashing(probe)).isFalse
  }

  @Test
  fun `test handler enable`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.enable()
    CrashShieldHandler.handleThrowable(exception, probe)
    assertThat(CrashShieldHandler.isObjectCrashing(probe)).isTrue
    verify(mockInstrumentData, times(1)).save()
  }

  @Ignore // TODO: Re-enable when flakiness is fixed T117670320
  @Test
  fun `test reset`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.enable()
    CrashShieldHandler.handleThrowable(exception, probe)
    assertThat(CrashShieldHandler.isObjectCrashing(probe)).isTrue
    CrashShieldHandler.reset()
    assertThat(CrashShieldHandler.isObjectCrashing(probe)).isFalse
  }
}
