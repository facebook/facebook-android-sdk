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

package com.facebook.internal.instrument.threadcheck

import android.util.Log
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.instrument.InstrumentData
import com.facebook.util.common.anyObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentData.Builder::class, Log::class)
class ThreadCheckHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var mockInstrumentData: InstrumentData
  @Before
  fun init() {
    mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)
    PowerMockito.`when`(InstrumentData.Builder.build(anyObject(), anyObject()))
        .thenReturn(mockInstrumentData)
    PowerMockito.mockStatic(Log::class.java)
  }

  @Test
  fun `test ui thread violation`() {
    var logMessage: String = ""
    PowerMockito.`when`(Log.e(anyString(), anyString(), anyObject())).then {
      logMessage = it.arguments[1] as String
      return@then 0
    }
    ThreadCheckHandler.enable()
    ThreadCheckHandler.uiThreadViolationDetected(this.javaClass, "testMethod", "testMethod()")
    Mockito.verify(mockInstrumentData, Mockito.times(1)).save()
    Assert.assertNotNull(logMessage)
    Assert.assertTrue(logMessage.contains("@UiThread"))
  }

  @Test
  fun `test worker thread violation`() {
    var logMessage: String = ""
    PowerMockito.`when`(Log.e(anyString(), anyString(), anyObject())).then {
      logMessage = it.arguments[1] as String
      return@then 0
    }
    ThreadCheckHandler.enable()
    ThreadCheckHandler.workerThreadViolationDetected(this.javaClass, "testMethod", "testMethod()")
    Mockito.verify(mockInstrumentData, Mockito.times(1)).save()
    Assert.assertNotNull(logMessage)
    Assert.assertTrue(logMessage.contains("@WorkerThread"))
  }
}
