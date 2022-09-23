/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenAnswer {
      return@thenAnswer isLogAppEventsEnable
    }
    listOfCallbacks = arrayListOf()
    PowerMockito.mockStatic(FeatureManager::class.java)
    whenever(FeatureManager.checkFeature(any(), any())).then {
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
    whenever(ExceptionAnalyzer.enable()).thenAnswer {
      isExceptionAnalyzerEnable = true
      Unit
    }
    PowerMockito.mockStatic(CrashShieldHandler::class.java)

    whenever(CrashShieldHandler.enable()).thenAnswer {
      isCrashShieldHandlerEnable = true
      Unit
    }
    PowerMockito.mockStatic(ThreadCheckHandler::class.java)
    whenever(ThreadCheckHandler.enable()).thenAnswer {
      isThreadCheckHandlerEnable = true
      Unit
    }
    PowerMockito.mockStatic(ErrorReportHandler::class.java)
    whenever(ErrorReportHandler.enable()).thenAnswer {
      isErrorReportHandlerEnable = true
      Unit
    }
    PowerMockito.mockStatic(ANRHandler::class.java)
    whenever(ANRHandler.enable()).thenAnswer {
      isAnrHandlerEnable = true
      Unit
    }
  }

  @Test
  fun `test start with auto logging events disable`() {
    isLogAppEventsEnable = false
    InstrumentManager.start()
    assertThat(listOfCallbacks.isEmpty()).isTrue
  }

  @Test
  fun `test start with all features enable`() {
    isLogAppEventsEnable = true
    whenever(FeatureManager.isEnabled(any())).thenReturn(true)
    InstrumentManager.start()
    listOfCallbacks.forEach { it.onCompleted(true) }
    assertThat(isCrashHandlerEnable).isTrue
    assertThat(isExceptionAnalyzerEnable).isTrue
    assertThat(isErrorReportHandlerEnable).isTrue
    assertThat(isThreadCheckHandlerEnable).isTrue
    assertThat(isCrashShieldHandlerEnable).isTrue
    assertThat(isAnrHandlerEnable).isTrue
  }
}
