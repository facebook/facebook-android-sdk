/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.threadcheck

import android.util.Log
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.instrument.InstrumentData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentData::class, InstrumentData.Builder::class, Log::class)
class ThreadCheckHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var mockInstrumentData: InstrumentData
  @Before
  fun init() {
    mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)
    whenever(InstrumentData.Builder.build(any<Throwable>(), any<InstrumentData.Type>()))
        .thenReturn(mockInstrumentData)
    PowerMockito.mockStatic(Log::class.java)
  }

  @Test
  fun `test ui thread violation`() {
    var logMessage: String = ""
    whenever(Log.e(anyString(), anyString(), any())).then {
      logMessage = it.arguments[1] as String
      return@then 0
    }
    ThreadCheckHandler.enable()
    ThreadCheckHandler.uiThreadViolationDetected(this.javaClass, "testMethod", "testMethod()")
    verify(mockInstrumentData, times(1)).save()
    Assert.assertNotNull(logMessage)
    assertThat(logMessage).contains("@UiThread")
  }

  @Test
  fun `test worker thread violation`() {
    var logMessage: String = ""
    whenever(Log.e(anyString(), anyString(), any())).then {
      logMessage = it.arguments[1] as String
      return@then 0
    }
    ThreadCheckHandler.enable()
    ThreadCheckHandler.workerThreadViolationDetected(this.javaClass, "testMethod", "testMethod()")
    verify(mockInstrumentData, times(1)).save()
    Assert.assertNotNull(logMessage)
    assertThat(logMessage).contains("@WorkerThread")
  }
}
