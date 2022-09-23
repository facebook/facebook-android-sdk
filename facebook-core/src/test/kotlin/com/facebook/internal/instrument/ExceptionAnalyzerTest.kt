/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import android.content.Context
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequestBatch
import com.facebook.MockSharedPreference
import com.facebook.internal.Utility
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    ExceptionAnalyzer::class,
    InstrumentUtility::class,
    Utility::class,
    GraphRequestBatch::class)
class ExceptionAnalyzerTest : FacebookPowerMockTestCase() {
  private var instrumentDataWritten = "{}"
  private val preference = MockSharedPreference()
  private lateinit var directory: File
  private lateinit var root: File

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    directory = File(rootName, "instrument")
    directory.mkdirs()
    root = File(rootName)

    val context = PowerMockito.mock(Context::class.java)
    PowerMockito.`when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(preference)
    PowerMockito.spy(FacebookSdk::class.java)
    PowerMockito.spy(ExceptionAnalyzer::class.java)
    PowerMockito.`when`(ExceptionAnalyzer.isDebug()).thenReturn(false)
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationId", "123456789")
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationContext", context)
    Whitebox.setInternalState(ExceptionAnalyzer::class.java, "enabled", true)
    PowerMockito.spy(InstrumentUtility::class.java)
    PowerMockito.spy(Utility::class.java)
    PowerMockito.doReturn(false).`when`(Utility::class.java, "isDataProcessingRestricted")
    PowerMockito.doReturn(directory).`when`(InstrumentUtility::class.java, "getInstrumentReportDir")
  }

  @After
  fun clean() {
    root.deleteRecursively()
  }

  @Test
  fun `test execute`() {
    PowerMockito.doReturn(true).`when`(FacebookSdk::class.java, "getAutoLogAppEventsEnabled")
    PowerMockito.doAnswer {
          instrumentDataWritten = it.arguments[1] as String
          return@doAnswer Unit
        }
        .`when`(InstrumentUtility::class.java, "writeFile", anyString(), anyString())

    val e = Exception()
    val trace =
        arrayOf(
            StackTraceElement(
                "com.facebook.appevents.codeless.CodelessManager", "onActivityResumed", "file", 10))
    e.stackTrace = trace
    ExceptionAnalyzer.execute(e)
    Assert.assertEquals(
        FacebookSdk.getSdkVersion(), preference.getString("FBSDKFeatureCodelessEvents", null))
    val instrumentData = JSONObject(instrumentDataWritten)
    val featureNames = instrumentData.getJSONArray("feature_names")
    val featureToExam = featureNames.getString(0)
    Assert.assertNotNull(featureToExam)
    Assert.assertEquals("CodelessEvents", featureToExam)
  }

  @Test
  fun `test send error reports`() {
    InstrumentUtility.writeFile("analysis_log_1.json", "{\"feature_names\":[],\"timestamp\":1}")
    val mockGraphRequestBatch = PowerMockito.mock(GraphRequestBatch::class.java)
    PowerMockito.whenNew(GraphRequestBatch::class.java)
        .withAnyArguments()
        .thenReturn(mockGraphRequestBatch)
    ExceptionAnalyzer.sendExceptionAnalysisReports()
    verify(mockGraphRequestBatch, times(1)).executeAsync()
  }
}
