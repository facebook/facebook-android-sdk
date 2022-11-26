/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument

import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.Utility
import java.io.File
import java.lang.RuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentUtility::class, Utility::class)
class InstrumentDataTest : FacebookPowerMockTestCase() {

  private val validJson =
      "{\n" +
          "  \"timestamp\": 10,\n" +
          "  \"app_version\": \"0.0.1\",\n" +
          "  \"reason\": \"i do not know\",\n" +
          "  \"callstack\": \"placeholder\"\n" +
          "}"

  @Before
  fun init() {
    val fileData = JSONObject(validJson)
    mockStatic(InstrumentUtility::class.java)
    whenever(InstrumentUtility.readFile(isA(String::class.java), isA(Boolean::class.java)))
        .thenReturn(fileData)
    whenever(InstrumentUtility.getCause(isA(Throwable::class.java))).thenCallRealMethod()
    whenever(InstrumentUtility.getStackTrace(isA(Throwable::class.java))).thenCallRealMethod()

    mockStatic(Utility::class.java)
    whenever(Utility.getAppVersion()).thenReturn("0.0.1")
  }

  @Test
  fun `test creating instrument data with an anr`() {
    val cause = "test_cause"
    val stacktrace = "test_st"
    val data = InstrumentData.Builder.build(cause, stacktrace)
    assertThat(data.isValid).isTrue
    val parameterString = data.toString()
    assertNotNull(parameterString)
    val parameters = JSONObject(parameterString)
    assertEquals(parameters.get("type"), InstrumentData.Type.AnrReport.toString())
    assertEquals(parameters.get("reason"), cause)
    assertEquals(parameters.get("callstack"), stacktrace)
  }

  @Test
  fun `test creating instrument data with an exception`() {
    val ex = NotImplementedError()
    val data = InstrumentData.Builder.build(ex, InstrumentData.Type.CrashReport)
    assertThat(data.isValid).isTrue
    val parameterString = data.toString()
    assertNotNull(parameterString)
    val parameters = JSONObject(parameterString)
    assertEquals(parameters.get("type"), InstrumentData.Type.CrashReport.toString())
    assertEquals(parameters.get("reason"), InstrumentUtility.getCause(ex))
    assertEquals(parameters.get("callstack"), InstrumentUtility.getStackTrace(ex))
  }

  @Test
  fun `test creating instrument data with file`() {
    val testFile = File("thread_check_log_001.json")
    val data = InstrumentData.Builder.load(testFile)
    assertThat(data.isValid).isTrue
    val parameterString = data.toString()
    assertNotNull(parameterString)
    val parameters = JSONObject(parameterString)
    assertEquals(parameters.get("type"), InstrumentData.Type.ThreadCheck.toString())
    assertEquals(parameters.get("timestamp"), 10)
    assertEquals(parameters.get("app_version"), "0.0.1")
    assertEquals(parameters.get("reason"), "i do not know")
    assertEquals(parameters.get("callstack"), "placeholder")
  }

  @Test
  fun `test creating instrument data with a features array`() {
    val featureArray = arrayOf("a", "b", "c")
    val features = JSONArray(featureArray)
    val data = InstrumentData.Builder.build(features)
    assertThat(data.isValid).isTrue
    val parameterString = data.toString()
    assertNotNull(parameterString)
    val parameters = JSONObject(parameterString)
    assertEquals(parameters.get("feature_names"), features)
  }

  @Test
  fun `test invalid instrument analysis data`() {
    val testFile = File("analysis_log_1.json")
    val data = InstrumentData.Builder.load(testFile)
    assertThat(data.isValid).isFalse
  }

  @Test
  fun `test save with exception report`() {
    var didWriteFile = false
    whenever(InstrumentUtility.writeFile(isA(String::class.java), isA(String::class.java))).then {
      didWriteFile = true
      return@then Unit
    }
    InstrumentData.Builder.build(RuntimeException(), InstrumentData.Type.CrashShield).save()
    assertThat(didWriteFile).isTrue
  }
}
