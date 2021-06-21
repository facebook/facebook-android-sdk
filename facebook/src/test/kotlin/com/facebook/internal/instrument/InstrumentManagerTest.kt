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

package com.facebook.internal.instrument

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import com.facebook.internal.instrument.anrreport.ANRHandler
import com.facebook.internal.instrument.crashreport.CrashHandler
import com.facebook.internal.instrument.crashshield.CrashShieldHandler
import com.facebook.internal.instrument.errorreport.ErrorReportHandler
import com.facebook.internal.instrument.threadcheck.ThreadCheckHandler
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    FeatureManager::class,
    CrashHandler::class,
    ExceptionAnalyzer::class,
    CrashShieldHandler::class,
    ThreadCheckHandler::class,
    ErrorReportHandler::class,
    ANRHandler::class,
)
class InstrumentManagerTest : FacebookPowerMockTestCase() {
  private var isLogAppEventsEnable = false
  private var isCrashHandlerEnable = false
  private var isExceptionAnalyzerEnable = false
  private var isErrorReportHandlerEnable = false
  private var isThreadCheckHandlerEnable = false
  private var isCrashShieldHandlerEnable = false
  private var isAnrHandlerEnable = false

  private lateinit var listOfCallbacks: ArrayList<FeatureManager.Callback>

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenAnswer {
      return@thenAnswer isLogAppEventsEnable
    }
    listOfCallbacks = arrayListOf()
    PowerMockito.mockStatic(FeatureManager::class.java)
    PowerMockito.`when`(FeatureManager.checkFeature(any(), any())).then {
      val callback = it.arguments[1] as FeatureManager.Callback
      listOfCallbacks.add(callback)
      return@then Unit
    }

    PowerMockito.mockStatic(CrashHandler::class.java)
    val mockCrashHandlerCompanion = mock<CrashHandler.Companion>()
    Whitebox.setInternalState(CrashHandler::class.java, "Companion", mockCrashHandlerCompanion)
    PowerMockito.doAnswer {
          isCrashHandlerEnable = true
          Unit
        }
        .`when`(mockCrashHandlerCompanion)
        .enable()

    PowerMockito.mockStatic(ExceptionAnalyzer::class.java)
    PowerMockito.doAnswer {
          isExceptionAnalyzerEnable = true
          Unit
        }
        .`when`(ExceptionAnalyzer::class.java, "enable")
    PowerMockito.mockStatic(CrashShieldHandler::class.java)
    PowerMockito.doAnswer {
          isCrashShieldHandlerEnable = true
          Unit
        }
        .`when`(CrashShieldHandler::class.java, "enable")
    PowerMockito.mockStatic(ThreadCheckHandler::class.java)
    PowerMockito.doAnswer {
          isThreadCheckHandlerEnable = true
          Unit
        }
        .`when`(ThreadCheckHandler::class.java, "enable")
    PowerMockito.mockStatic(ErrorReportHandler::class.java)
    PowerMockito.doAnswer {
          isErrorReportHandlerEnable = true
          Unit
        }
        .`when`(ErrorReportHandler::class.java, "enable")
    PowerMockito.mockStatic(ANRHandler::class.java)
    PowerMockito.doAnswer {
          isAnrHandlerEnable = true
          Unit
        }
        .`when`(ANRHandler::class.java, "enable")
  }

  @Test
  fun `test start with auto logging events disable`() {
    isLogAppEventsEnable = false
    InstrumentManager.start()
    Assert.assertTrue(listOfCallbacks.isEmpty())
  }

  @Test
  fun `test start with all features enable`() {
    isLogAppEventsEnable = true
    PowerMockito.`when`(FeatureManager.isEnabled(any())).thenReturn(true)
    InstrumentManager.start()
    listOfCallbacks.forEach { it.onCompleted(true) }
    Assert.assertTrue(isCrashHandlerEnable)
    Assert.assertTrue(isExceptionAnalyzerEnable)
    Assert.assertTrue(isErrorReportHandlerEnable)
    Assert.assertTrue(isThreadCheckHandlerEnable)
    Assert.assertTrue(isCrashShieldHandlerEnable)
    Assert.assertTrue(isAnrHandlerEnable)
  }
}
