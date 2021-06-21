package com.facebook.internal.instrument.anrreport

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import com.facebook.util.common.anyObject
import java.io.File
import java.util.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    ANRDetector::class, InstrumentUtility::class, InstrumentData.Builder::class, FacebookSdk::class)
class ANRHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var root: File
  private lateinit var directory: File

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    directory = File(rootName, "instrument")
    directory.mkdirs()
    root = File(rootName)

    PowerMockito.mockStatic(ANRDetector::class.java)

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)

    PowerMockito.mockStatic(InstrumentUtility::class.java)
    PowerMockito.`when`(InstrumentUtility.getInstrumentReportDir()).thenReturn(directory)
    PowerMockito.`when`(InstrumentUtility.listAnrReportFiles()).thenCallRealMethod()
    PowerMockito.`when`(
            InstrumentUtility.writeFile(
                ArgumentMatchers.isA(String::class.java), ArgumentMatchers.isA(String::class.java)))
        .thenCallRealMethod()
    PowerMockito.`when`(
            InstrumentUtility.readFile(
                ArgumentMatchers.isA(String::class.java),
                ArgumentMatchers.isA(Boolean::class.java)))
        .thenCallRealMethod()
    PowerMockito.`when`(InstrumentData.Builder.load(anyObject())).thenCallRealMethod()
  }

  @After
  fun cleanTestDirectory() {
    root.deleteRecursively()
  }

  @Test
  fun `test to only execute enable() once`() {
    var hitSendReports = 0
    var hitStartANRDetection = 0
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    PowerMockito.`when`(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject()))
        .thenAnswer {
          hitSendReports += 1
          null
        }
    PowerMockito.`when`(ANRDetector.start()).then {
      hitStartANRDetection += 1
      return@then Unit
    }

    ANRHandler.enable()
    ANRHandler.enable()
    ANRHandler.enable()
    Assert.assertEquals(1, hitSendReports)
    Assert.assertEquals(1, hitStartANRDetection)
  }

  @Test
  fun `test not to send report if app events disabled or data processing restricted`() {
    var hitSendReports = 0
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

    PowerMockito.`when`(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject()))
        .thenAnswer {
          hitSendReports += 1
          null
        }

    ANRHandler.enable()
    ANRHandler.enable()
    ANRHandler.enable()
    Assert.assertEquals(0, hitSendReports)
  }

  @Test
  fun `test send reports`() {
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    InstrumentUtility.writeFile(
        "anr_log_1.json", "{\"reason\":\"yoloswag\",\"callstack\":[],\"timestamp\":1}")
    InstrumentUtility.writeFile(
        "anr_log_2.json", "{\"reason\":\"yoloswag\",\"callstack\":[],\"timestamp\":2}")
    InstrumentUtility.writeFile(
        "anr_log_3.json", "{\"reason\":\"yoloswag\",\"callstack\":[],\"timestamp\":3}")
    var anrLogs: JSONArray? = null
    PowerMockito.`when`(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject()))
        .thenAnswer {
          anrLogs = it.arguments[1] as JSONArray
          null
        }
    ANRHandler.sendANRReports()
    // check the order of the reports is correct
    val anrLogsTimeStamps =
        (0 until (anrLogs?.length() ?: 0)).map {
          JSONObject(anrLogs?.getString(it) ?: "{}").getInt("timestamp")
        }
    Assert.assertArrayEquals(intArrayOf(3, 2, 1), anrLogsTimeStamps.toIntArray())
  }
}
