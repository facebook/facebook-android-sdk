package com.facebook.internal.instrument.errorreport

import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.instrument.InstrumentUtility
import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentUtility::class)
class ErrorReportDataTest : FacebookPowerMockTestCase() {

  private val validJson =
      "{\n" + "  \"timestamp\": 10,\n" + "  \"error_message\": \"yoloswag\"\n" + "}"
  private val jsonWithoutTimestamp = "{\n" + "  \"error_message\": \"error\"\n" + "}"

  @Before
  fun init() {
    mockStatic(InstrumentUtility::class.java)
    val jsonObject = JSONObject(validJson)
    `when`(InstrumentUtility.readFile(isA(String::class.java), isA(Boolean::class.java)))
        .thenReturn(jsonObject)
  }

  @Test
  fun `test params from json`() {
    val data = ErrorReportData(File("swag"))
    val result = data.parameters
    assertEquals("yoloswag", result?.optString("error_message"))
    assertEquals(10L, result?.optLong("timestamp"))
  }

  @Test
  fun `test compare to`() {
    `when`(InstrumentUtility.readFile(isA(String::class.java), isA(Boolean::class.java)))
        .thenReturn(JSONObject(validJson), JSONObject(jsonWithoutTimestamp))
    val data1 = ErrorReportData(File("swag1"))
    val data2 = ErrorReportData(File("swag2"))
    assertEquals(-1, data1.compareTo(data2))
  }
}
