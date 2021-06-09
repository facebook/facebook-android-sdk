package com.facebook.internal.instrument.crashreport

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import com.facebook.util.common.anyObject
import com.nhaarman.mockitokotlin2.any
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentUtility::class, InstrumentData.Builder::class, FacebookSdk::class)
class CrashHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var root: File
  private lateinit var directory: File

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    directory = File(rootName, "instrument")
    directory.mkdirs()
    root = File(rootName)

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(InstrumentData.Builder::class.java)

    PowerMockito.mockStatic(InstrumentUtility::class.java)
    PowerMockito.`when`(InstrumentUtility.getInstrumentReportDir()).thenReturn(directory)
    PowerMockito.`when`(InstrumentUtility.listExceptionReportFiles()).thenCallRealMethod()
    PowerMockito.`when`(InstrumentUtility.listExceptionAnalysisReportFiles()).thenCallRealMethod()
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
  fun `test not to send report if app events disabled or data processing restricted`() {
    var hitSendReports = false
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    PowerMockito.`when`(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject()))
        .thenAnswer {
          hitSendReports = true
          null
        }
    CrashHandler.enable()
    Assert.assertFalse(hitSendReports)
  }

  @Test
  fun `test enable CrashHandler will set to be default handler`() {
    CrashHandler.enable()
    val handler = Thread.getDefaultUncaughtExceptionHandler()
    Assert.assertNotNull(handler)
    Assert.assertTrue(handler is CrashHandler)
  }

  @Test
  fun `test crash handler save the exception report`() {
    PowerMockito.`when`(InstrumentUtility.isSDKRelatedException(anyObject())).thenCallRealMethod()
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.`when`(InstrumentData.Builder.build(any<Throwable>(), any<InstrumentData.Type>()))
        .thenReturn(mockInstrumentData)
    val e = Exception()
    val trace =
        arrayOf(
            StackTraceElement(
                "com.facebook.appevents.codeless.CodelessManager", "onActivityResumed", "file", 10))
    e.stackTrace = trace

    CrashHandler.enable()

    val handler = Thread.getDefaultUncaughtExceptionHandler()
    handler?.uncaughtException(Thread.currentThread(), e)
    Mockito.verify(mockInstrumentData).save()
  }

  @Test
  fun `test send reports`() {
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    InstrumentUtility.writeFile("crash_log_1.json", "{\"callstack\":[],\"timestamp\":1}")
    InstrumentUtility.writeFile("crash_log_2.json", "{\"callstack\":[],\"timestamp\":2}")
    InstrumentUtility.writeFile("crash_log_3.json", "{\"callstack\":[],\"timestamp\":3}")
    var crashLogs: JSONArray? = null
    PowerMockito.`when`(InstrumentUtility.sendReports(anyObject(), anyObject(), anyObject()))
        .thenAnswer {
          crashLogs = it.arguments[1] as JSONArray
          null
        }
    CrashHandler.enable()
    // check the order of the reports is correct
    val crashLogsTimeStamps =
        (0 until (crashLogs?.length() ?: 0)).map {
          JSONObject(crashLogs?.getString(it) ?: "{}").getInt("timestamp")
        }
    Assert.assertArrayEquals(intArrayOf(3, 2, 1), crashLogsTimeStamps.toIntArray())
  }
}
