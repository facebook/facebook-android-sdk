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

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData
import com.facebook.util.common.anyObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, InstrumentData.Builder::class, CrashShieldHandler::class)
class CrashShieldHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var mockInstrumentData: InstrumentData

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.`when`(mockInstrumentData.save()).then {
      return@then Unit
    }
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)
    PowerMockito.`when`(InstrumentData.Builder.build(anyObject(), anyObject()))
        .thenReturn(mockInstrumentData)

    PowerMockito.mockStatic(CrashShieldHandler::class.java)
    PowerMockito.`when`(CrashShieldHandler.scheduleCrashInDebug(anyObject())).then {}
    PowerMockito.`when`(CrashShieldHandler.handleThrowable(anyObject(), anyObject()))
        .thenCallRealMethod()
    PowerMockito.`when`(CrashShieldHandler.isObjectCrashing(anyObject())).thenCallRealMethod()
    PowerMockito.`when`(CrashShieldHandler.enable()).thenCallRealMethod()
    PowerMockito.`when`(CrashShieldHandler.disable()).thenCallRealMethod()
    PowerMockito.`when`(CrashShieldHandler.resetCrashingObjects()).thenCallRealMethod()
    PowerMockito.`when`(CrashShieldHandler.reset()).thenCallRealMethod()
  }

  @Test
  fun `test handler disabled by default`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.disable()
    CrashShieldHandler.handleThrowable(exception, probe)
    Assert.assertFalse(CrashShieldHandler.isObjectCrashing(probe))
  }

  @Test
  fun `test handler enable`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.enable()
    CrashShieldHandler.handleThrowable(exception, probe)
    Assert.assertTrue(CrashShieldHandler.isObjectCrashing(probe))
    verify(mockInstrumentData, times(1)).save()
  }

  @Test
  fun `test reset`() {
    val probe = Object()
    val exception = RuntimeException()
    CrashShieldHandler.enable()
    CrashShieldHandler.handleThrowable(exception, probe)
    Assert.assertTrue(CrashShieldHandler.isObjectCrashing(probe))
    CrashShieldHandler.reset()
    Assert.assertFalse(CrashShieldHandler.isObjectCrashing(probe))
  }
}
