package com.facebook.internal.instrument

import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import java.io.File
import java.io.FileOutputStream
import java.util.*
import org.json.JSONArray
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.powermock.api.mockito.PowerMockito.*
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(InstrumentUtility::class, FileOutputStream::class)
class InstrumentUtilityTest : FacebookPowerMockTestCase() {

  private lateinit var directory: File

  @Before
  fun init() {
    val rootName = UUID.randomUUID().toString()
    directory = File(rootName, "instrument")
    directory.mkdirs()
    mockStatic(InstrumentUtility::class.java)
    `when`(InstrumentUtility.getInstrumentReportDir()).thenReturn(directory)
    `when`(InstrumentUtility.writeFile(isA(String::class.java), isA(String::class.java)))
        .thenCallRealMethod()
    `when`(InstrumentUtility.readFile(isA(String::class.java), isA(Boolean::class.java)))
        .thenCallRealMethod()
    `when`(InstrumentUtility.getStackTrace(any<Thread>())).thenCallRealMethod()
    `when`(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenCallRealMethod()
    `when`(InstrumentUtility.listAnrReportFiles()).thenCallRealMethod()
    `when`(InstrumentUtility.listExceptionReportFiles()).thenCallRealMethod()
    `when`(InstrumentUtility.listExceptionAnalysisReportFiles()).thenCallRealMethod()
    `when`(InstrumentUtility.deleteFile(isA(String::class.java))).thenCallRealMethod()
  }

  @After
  fun tearDown() {
    directory.deleteRecursively()
  }

  @Test
  fun `writing and reading json string`() {
    InstrumentUtility.writeFile("error_log_1337.json", "{\"anything\":\"swag\"}")
    val result = InstrumentUtility.readFile("error_log_1337.json", false)
    assertEquals("{\"anything\":\"swag\"}", result.toString())
  }

  @Test
  fun `writing and reading non-json string`() {
    InstrumentUtility.writeFile("error_log_1337.json", "anything")
    val result = InstrumentUtility.readFile("error_log_1337.json", false)
    assertNull(result)
  }

  @Test
  fun `getting stack trace from a specific thread`() {
    val trace =
        arrayOf(
            StackTraceElement(
                "com.facebook.appevents.codeless.CodelessManager", "onActivityResumed", "file", 10))
    mockStatic(Thread::class.java)
    val thread: Thread = mock(Thread::class.java)
    `when`(thread.stackTrace).thenReturn(trace)
    val result = InstrumentUtility.getStackTrace(thread)
    val expected = JSONArray()
    expected.put("com.facebook.appevents.codeless.CodelessManager.onActivityResumed(file:10)")
    assertEquals(expected.toString(), result)
  }

  @Test
  fun `Checking if the thread is SDK related`() {
    mockStatic(Thread::class.java)
    val thread: Thread = mock(Thread::class.java)

    var trace =
        arrayOf(StackTraceElement("com.cfsample.coffeeshop.AnrActivity", "onClick", "file", 10))
    `when`(thread.stackTrace).thenReturn(trace)
    assertFalse(InstrumentUtility.isSDKRelatedThread(thread))

    // Exclude onClick(), onItemClick() or onTouch() when they are calling app itself's click
    // listeners
    trace =
        arrayOf(
            StackTraceElement("com.cfsample.coffeeshop.AnrActivity", "onClick", "file", 10),
            StackTraceElement(
                "com.facebook.appevents.suggestedevents.ViewOnClickListener",
                "onClick",
                "ViewOnClickListener.java",
                10),
            StackTraceElement(
                "com.facebook.appevents.codeless.CodelessLoggingEventListener",
                "onItemClick",
                "CodelessLoggingEventListener.java",
                10),
            StackTraceElement(
                "com.facebook.appevents.codeless.RCTCodelessLoggingEventListener",
                "onTouch",
                "RCTCodelessLoggingEventListener.java",
                10),
        )
    `when`(thread.stackTrace).thenReturn(trace)
    assertFalse(InstrumentUtility.isSDKRelatedThread(thread))

    // If onClick() calls process() and there is an ANR in process(), it's SDK related
    trace =
        arrayOf(
            StackTraceElement(
                "com.facebook.appevents.suggestedevents.ViewOnClickListener",
                "process",
                "ViewOnClickListener.java",
                10),
            StackTraceElement(
                "com.facebook.appevents.suggestedevents.ViewOnClickListener",
                "onClick",
                "ViewOnClickListener.java",
                10),
            StackTraceElement(
                "com.nhaarman.mockitokotlin2.any", "onClick", "ViewOnClickListener.java", 10),
        )
    `when`(thread.stackTrace).thenReturn(trace)
    assertTrue(InstrumentUtility.isSDKRelatedThread(thread))
  }

  @Test
  fun `listing anr report files`() {
    InstrumentUtility.writeFile("anr_log_1.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("anr_log_2.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("shouldbeignored_1.json", "{\"anything\":\"swag\"}")
    val result = InstrumentUtility.listAnrReportFiles()
    assertEquals(2, result.size)

    InstrumentUtility.deleteFile("anr_log_1.json")
    InstrumentUtility.deleteFile("anr_log_2.json")
    val result1 = InstrumentUtility.listAnrReportFiles()
    assertEquals(0, result1.size)
  }

  @Test
  fun `listing exception report files`() {
    InstrumentUtility.writeFile("crash_log_1.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("shield_log_1.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("thread_check_log_1.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("shouldbeignored_1.json", "{\"anything\":\"swag\"}")
    val result = InstrumentUtility.listExceptionReportFiles()
    assertEquals(3, result.size)

    InstrumentUtility.deleteFile("crash_log_1.json")
    InstrumentUtility.deleteFile("shield_log_1.json")
    InstrumentUtility.deleteFile("thread_check_log_1.json")
    val result1 = InstrumentUtility.listExceptionReportFiles()
    assertEquals(0, result1.size)
  }

  @Test
  fun `listing exception analysis files`() {
    InstrumentUtility.writeFile("analysis_log_1.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("analysis_log_2.json", "{\"anything\":\"swag\"}")
    InstrumentUtility.writeFile("shouldbeignored_1.json", "{\"anything\":\"swag\"}")
    val result = InstrumentUtility.listExceptionAnalysisReportFiles()
    assertEquals(2, result.size)

    InstrumentUtility.deleteFile("analysis_log_1.json")
    InstrumentUtility.deleteFile("analysis_log_2.json")
    val result1 = InstrumentUtility.listExceptionAnalysisReportFiles()
    assertEquals(0, result1.size)
  }
}
