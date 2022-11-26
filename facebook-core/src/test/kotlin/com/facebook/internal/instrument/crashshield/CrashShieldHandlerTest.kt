/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.crashshield

import android.os.Handler
import android.os.Looper
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
