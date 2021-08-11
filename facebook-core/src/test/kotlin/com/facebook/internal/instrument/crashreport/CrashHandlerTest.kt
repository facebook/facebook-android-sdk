package com.facebook.internal.instrument.crashreport

import android.content.Context
import android.content.SharedPreferences
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import java.io.File
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(InstrumentData::class, FacebookSdk::class)
class CrashHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var root: File
  private lateinit var mockGraphRequestCompanionObject: GraphRequest.Companion

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    root = File(rootName)
    root.mkdir()

    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123")

    val mockContext: Context = mock()
    val mockSharedPreferences: SharedPreferences = mock()
    PowerMockito.`when`(mockSharedPreferences.getString(FacebookSdk.DATA_PROCESSION_OPTIONS, null))
        .thenReturn(null)
    PowerMockito.`when`(mockContext.cacheDir).thenReturn(root)
    PowerMockito.`when`(
            mockContext.getSharedPreferences(
                FacebookSdk.DATA_PROCESSING_OPTIONS_PREFERENCES, Context.MODE_PRIVATE))
        .thenReturn(mockSharedPreferences)
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(mockContext)

    PowerMockito.mockStatic(InstrumentData::class.java)
    mockGraphRequestCompanionObject = mock()
    Whitebox.setInternalState(
        GraphRequest::class.java, "Companion", mockGraphRequestCompanionObject)
  }

  @After
  fun cleanTestDirectory() {
    root.deleteRecursively()
  }

  @Test
  fun `test not to send report if app events disabled or data processing restricted`() {
    var hitSendReports = false
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    PowerMockito.`when`(mockGraphRequestCompanionObject.newPostRequest(any(), any(), any(), any()))
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
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    PowerMockito.whenNew(InstrumentData::class.java)
        .withAnyArguments()
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
    var crashLogs: JSONObject? = null
    val mockRequest: GraphRequest = mock()
    PowerMockito.`when`(
            mockGraphRequestCompanionObject.newPostRequest(isNull(), any(), any(), any()))
        .thenAnswer {
          crashLogs = it.arguments[2] as JSONObject?
          mockRequest
        }
    CrashHandler.enable()
    val tokener = JSONTokener(crashLogs?.get("crash_reports") as String)
    val logArray = JSONArray(tokener)
    val crashLogsTimeStamps =
        (0 until logArray?.length()).map {
          JSONObject(logArray?.getString(it) ?: "{}").getInt("timestamp")
        }
    Assert.assertArrayEquals(intArrayOf(3, 2, 1), crashLogsTimeStamps.toIntArray())
  }
}
