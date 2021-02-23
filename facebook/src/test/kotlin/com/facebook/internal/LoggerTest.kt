package com.facebook.internal

import android.util.Log
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Log::class)
class LoggerTest : FacebookPowerMockTestCase() {
  private val tag = "TEST_TAG"
  private val content = "test content"
  private val fbToken = "fb_token"
  private val igToken = "ig_token"
  private val contentWithFBToken = "{\"access_token\":\"$fbToken\", \"content\":\"Facebook\"}"
  private val contentWithIGToken = "{\"access_token\":\"$igToken\", \"content\":\"Instagram\"}"

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.mockStatic(Log::class.java)

    PowerMockito.`when`(FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.APP_EVENTS))
        .thenReturn(true)
    PowerMockito.`when`(FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_INFO))
        .thenReturn(false)

    Logger.registerAccessToken(fbToken)
    Logger.registerStringToReplace(igToken, "removed")
  }

  @Test
  fun `test easy log`() {
    var callCount = 0
    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(content)))
        .then { callCount++ }
    Logger.log(LoggingBehavior.APP_EVENTS, tag, content)
    assertEquals(1, callCount)
  }

  @Test
  fun `test format log`() {
    var callCount = 0
    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(content)))
        .then { callCount++ }
    Logger.log(LoggingBehavior.APP_EVENTS, tag, "%s %s", "test", "content")
    assertEquals(1, callCount)
  }

  @Test
  fun `test disable behaviour log`() {
    var callCount = 0
    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(content)))
        .then { callCount++ }
    Logger.log(LoggingBehavior.GRAPH_API_DEBUG_INFO, tag, content)
    assertEquals(0, callCount)
  }

  @Test
  fun `test register access token`() {
    var callCount = 0
    val newContent = contentWithFBToken.replace(fbToken, "ACCESS_TOKEN_REMOVED")

    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(newContent)))
        .then { callCount++ }

    Logger.log(LoggingBehavior.APP_EVENTS, tag, contentWithFBToken)
    assertEquals(1, callCount)
  }

  @Test
  fun `test register string`() {
    var callCount = 0
    val newContent = contentWithIGToken.replace(igToken, "removed")

    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(newContent)))
        .then { callCount++ }
    Logger.log(LoggingBehavior.APP_EVENTS, tag, contentWithIGToken)
    assertEquals(1, callCount)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test invalid priority`() {
    val logger = Logger(LoggingBehavior.APP_EVENTS, tag)
    logger.priority = 0
  }

  @Test
  fun `test new logger`() {
    var callCount = 0
    var newContent = contentWithFBToken.replace(fbToken, "ACCESS_TOKEN_REMOVED")

    val logger = Logger(LoggingBehavior.APP_EVENTS, tag)
    logger.append(contentWithFBToken)
    assertEquals(newContent, logger.contents)

    newContent = newContent + String.format("  %s:\t%s\n", "key", "value")
    logger.appendKeyValue("key", "value")
    assertEquals(newContent, logger.contents)

    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(newContent)))
        .then { callCount++ }

    logger.log()
    assertEquals(1, callCount)
  }

  @Test
  fun `test log multiple time`() {
    var callCount = 0
    var newContent = contentWithFBToken.replace(fbToken, "ACCESS_TOKEN_REMOVED")

    val logger = Logger(LoggingBehavior.APP_EVENTS, tag)
    logger.append(contentWithFBToken)
    assertEquals(newContent, logger.contents)

    PowerMockito.`when`(Log.println(eq(Log.DEBUG), eq(Logger.LOG_TAG_BASE + tag), eq(newContent)))
        .then { callCount++ }

    logger.log()
    assertEquals(1, callCount)

    assertEquals("", logger.contents)
    logger.log()
    assertEquals(1, callCount)
  }
}
