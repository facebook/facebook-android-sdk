/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal.instrument.errorreport

import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentUtility
import com.facebook.util.common.anyObject
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    InstrumentUtility::class,
    FacebookSdk::class,
)
class ErrorReportHandlerTest : FacebookPowerMockTestCase() {

  private lateinit var directory: File
  private lateinit var rootDirectory: File
  private val validJson1 =
      "{\n" + "  \"timestamp\": 10,\n" + "  \"error_message\": \"yoloswag\"\n" + "}"
  private val validJson2 =
      "{\n" + "  \"timestamp\": 20,\n" + "  \"error_message\": \"yoloswag\"\n" + "}"
  private val inValidJson = "{\n" + "  \"timestamp\": 30,\n" + "}"

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    directory = File(rootName, "instrument")
    directory.mkdirs()
    rootDirectory = File(rootName)
    mockStatic(InstrumentUtility::class.java)
    whenever(InstrumentUtility.getInstrumentReportDir()).thenReturn(directory)
    whenever(
            InstrumentUtility.writeFile(
                ArgumentMatchers.isA(String::class.java), ArgumentMatchers.isA(String::class.java)))
        .thenCallRealMethod()
    whenever(
            InstrumentUtility.readFile(
                ArgumentMatchers.isA(String::class.java),
                ArgumentMatchers.isA(Boolean::class.java)))
        .thenCallRealMethod()

    InstrumentUtility.writeFile("error_log_1.json", validJson1)
    InstrumentUtility.writeFile("error_log_2.json", validJson2)
    InstrumentUtility.writeFile("error_log_3.json", inValidJson)
    InstrumentUtility.writeFile("log_1337.json", "{\"anything\":\"swag\"}")

    mockStatic(FacebookSdk::class.java)
    val mockContext = mock(Context::class.java)
    val mockSharedPreferences = mock(SharedPreferences::class.java)
    whenever(mockSharedPreferences.getString(FacebookSdk.DATA_PROCESSION_OPTIONS, null))
        .thenReturn(null)
    whenever(
            mockContext.getSharedPreferences(
                FacebookSdk.DATA_PROCESSING_OPTIONS_PREFERENCES, Context.MODE_PRIVATE))
        .thenReturn(mockSharedPreferences)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
  }

  @After
  fun tearDown() {
    rootDirectory.deleteRecursively()
  }

  @Test
  fun `test error report files`() {
    val files = ErrorReportHandler.listErrorReportFiles()
    assertEquals(3, files.size)
  }

  @Test
  fun `test send error log`() {
    var errorLogs: JSONArray? = null
    whenever(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject())).thenAnswer {
      errorLogs = it.getArgument(1) as JSONArray
      null
    }
    ErrorReportHandler.sendErrorReports()
    assertNotNull(errorLogs)
    assertEquals(2, errorLogs?.length())
    assertEquals(20, (errorLogs?.get(0) as ErrorReportData).parameters?.getInt("timestamp"))
    assertEquals(10, (errorLogs?.get(1) as ErrorReportData).parameters?.getInt("timestamp"))
  }
}
