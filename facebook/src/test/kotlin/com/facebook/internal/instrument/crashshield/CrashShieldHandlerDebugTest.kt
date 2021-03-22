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
import com.facebook.util.common.anyObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    FacebookSdk::class,
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
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.`when`(mockInstrumentData.save()).then {
      return@then Unit
    }
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)
    PowerMockito.`when`(InstrumentData.Builder.build(anyObject(), anyObject()))
        .thenReturn(mockInstrumentData)
    PowerMockito.spy(CrashShieldHandler::class.java)
    PowerMockito.`when`(CrashShieldHandler.isDebug()).thenReturn(true)
    mockLooper = PowerMockito.mock(Looper::class.java)
    mockHandler = PowerMockito.mock(Handler::class.java)
    PowerMockito.mockStatic(Looper::class.java)
    PowerMockito.`when`(Looper.getMainLooper()).thenReturn(mockLooper)
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
