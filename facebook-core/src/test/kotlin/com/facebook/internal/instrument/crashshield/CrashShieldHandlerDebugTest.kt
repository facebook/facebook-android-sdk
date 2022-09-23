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
import com.facebook.util.common.anyObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
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
class CrashShieldHandlerDebugTest : FacebookPowerMockTestCase() {
  private lateinit var mockInstrumentData: InstrumentData
  private lateinit var mockLooper: Looper
  private lateinit var mockHandler: Handler

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
    whenever(CrashShieldHandler.isDebug()).thenReturn(true)
    mockLooper = PowerMockito.mock(Looper::class.java)
    mockHandler = PowerMockito.mock(Handler::class.java)
    PowerMockito.mockStatic(Looper::class.java)
    whenever(Looper.getMainLooper()).thenReturn(mockLooper)
    PowerMockito.whenNew(Handler::class.java).withAnyArguments().thenReturn(mockHandler)
  }

  @Test(expected = RuntimeException::class)
  fun `test throw exceptions in debug mode`() {
    val probe = Object()
    val exception = RuntimeException()
    var runnableToBePosted: Runnable? = null
    PowerMockito.doAnswer {
          runnableToBePosted = it.arguments[0] as Runnable
          return@doAnswer true
        }
        .`when`(mockHandler)
        .post(anyObject())
    CrashShieldHandler.enable()
    CrashShieldHandler.handleThrowable(exception, probe)

    Assert.assertNotNull(runnableToBePosted)
    try {
      runnableToBePosted?.run()
    } catch (e: RuntimeException) {
      Assert.assertEquals(exception, e.cause)
      throw e
    }
  }
}
