package com.facebook.internal.instrument.anrreport

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.internal.instrument.InstrumentUtility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(ANRDetector::class, InstrumentUtility::class, FacebookSdk::class)
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
    PowerMockito.spy(InstrumentUtility::class.java)
    PowerMockito.doReturn(directory).`when`(InstrumentUtility::class.java, "getInstrumentReportDir")
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
    PowerMockito.doAnswer {
          hitSendReports += 1
          null
        }
        .`when`(
            InstrumentUtility::class.java,
            "sendReports",
            anyOrNull<String>(),
            any<JSONArray>(),
            anyOrNull<GraphRequest.Callback>())
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
    PowerMockito.doAnswer {
          hitSendReports += 1
          null
        }
        .`when`(
            InstrumentUtility::class.java,
            "sendReports",
            anyOrNull<String>(),
            any<JSONArray>(),
            anyOrNull<GraphRequest.Callback>())

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
    PowerMockito.doAnswer {
          anrLogs = it.arguments[1] as JSONArray
          null
        }
        .`when`(
            InstrumentUtility::class.java,
            "sendReports",
            anyOrNull<String>(),
            any<JSONArray>(),
            anyOrNull<GraphRequest.Callback>())

    ANRHandler.sendANRReports()
    // check the order of the reports is correct
    val anrLogsTimeStamps =
        (0 until (anrLogs?.length() ?: 0)).map {
          JSONObject(anrLogs?.getString(it) ?: "{}").getInt("timestamp")
        }
    Assert.assertArrayEquals(intArrayOf(3, 2, 1), anrLogsTimeStamps.toIntArray())
  }
}
