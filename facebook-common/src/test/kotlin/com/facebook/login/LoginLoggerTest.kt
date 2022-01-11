/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.login

import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.ScheduledExecutorService
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
class LoginLoggerTest : FacebookPowerMockTestCase() {
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
  private lateinit var loginLogger: LoginLogger
  private lateinit var testLoginRequest: LoginClient.Request
  private lateinit var mockExecutor: ScheduledExecutorService

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    mockInternalAppEventsLogger = mock()
    mockExecutor = mock()
    loginLogger = LoginLogger(ApplicationProvider.getApplicationContext(), "123456789")
    Whitebox.setInternalState(loginLogger, "logger", mockInternalAppEventsLogger)
    Whitebox.setInternalState(LoginLogger::class.java, "worker", mockExecutor)
    testLoginRequest =
        LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            setOf("email"),
            DefaultAudience.EVERYONE,
            "rerequest",
            "123456789",
            "5678")
  }

  @Test
  fun `test logStartLogin logging information in login request`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logStartLogin(testLoginRequest)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_START), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    val extras = JSONObject(params.getString(LoginLogger.EVENT_PARAM_EXTRAS) ?: "")
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_LOGIN_BEHAVIOR))
        .isEqualTo(testLoginRequest.loginBehavior.toString())
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_PERMISSIONS)).isEqualTo("email")
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_DEFAULT_AUDIENCE))
        .isEqualTo(testLoginRequest.defaultAudience.toString())
  }

  @Test
  fun `test logStartLogin with a custom event name logging information in login request`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logStartLogin(testLoginRequest, "custom_login_event")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq("custom_login_event"), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    val extras = JSONObject(params.getString(LoginLogger.EVENT_PARAM_EXTRAS) ?: "")
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_LOGIN_BEHAVIOR))
        .isEqualTo(testLoginRequest.loginBehavior.toString())
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_PERMISSIONS)).isEqualTo("email")
    assertThat(extras.getString(LoginLogger.EVENT_EXTRAS_DEFAULT_AUDIENCE))
        .isEqualTo(testLoginRequest.defaultAudience.toString())
  }

  @Test
  fun `test logCompleteLogin with cancel result logging the result logging value`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logCompleteLogin(
        testLoginRequest.authId, mapOf(), LoginClient.Result.Code.CANCEL, null, null)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.CANCEL.loggingValue)
  }

  @Test
  fun `test logCompleteLogin with exception logging the error message`() {
    val errorMessage = "Test login failure"
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logCompleteLogin(
        testLoginRequest.authId,
        mapOf(),
        LoginClient.Result.Code.ERROR,
        null,
        Exception(errorMessage))

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.ERROR.loggingValue)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(errorMessage)
  }

  @Test
  fun `test logCompleteLogin with success result logging the result logging value and starting heartbeat logging`() {
    val errorMessage = "Test login failure"
    val bundleCaptor = argumentCaptor<Bundle>()
    val jobCaptor = argumentCaptor<Runnable>()

    loginLogger.logCompleteLogin(
        testLoginRequest.authId,
        mapOf(),
        LoginClient.Result.Code.SUCCESS,
        null,
        Exception(errorMessage))

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.SUCCESS.loggingValue)
    verify(mockExecutor).schedule(jobCaptor.capture(), any(), any())
    val job = jobCaptor.firstValue
    job.run()
    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_HEARTBEAT), any<Bundle>())
  }

  @Test
  fun `test logCompleteLogin with custom event name`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logCompleteLogin(
        testLoginRequest.authId,
        mapOf(),
        LoginClient.Result.Code.CANCEL,
        null,
        null,
        "custom_event_name")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq("custom_event_name"), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.CANCEL.loggingValue)
  }

  @Test
  fun `test logAuthorizationMethodStart logging the method`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodStart(testLoginRequest.authId, "test_method")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_METHOD_START), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
  }

  @Test
  fun `test logAuthorizationMethodStart with the custom event name logging the method and event name`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodStart(
        testLoginRequest.authId, "test_method", "custom_event_name")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq("custom_event_name"), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
  }

  @Test
  fun `test logAuthorizationMethodComplete logging the result, error message and error code `() {
    val result = "error"
    val errorMessage = "errorMessage"
    val errorCode = "500"
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodComplete(
        testLoginRequest.authId, "test_method", result, errorMessage, errorCode, null)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(LoginLogger.EVENT_NAME_LOGIN_METHOD_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT)).isEqualTo(result)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(errorMessage)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_CODE)).isEqualTo(errorCode)
  }

  @Test
  fun `test logAuthorizationMethodComplete with custom event name logging the result, error message and error code `() {
    val result = "error"
    val errorMessage = "errorMessage"
    val errorCode = "500"
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodComplete(
        testLoginRequest.authId,
        "test_method",
        result,
        errorMessage,
        errorCode,
        null,
        "custom_event_name")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq("custom_event_name"), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT)).isEqualTo(result)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(errorMessage)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_CODE)).isEqualTo(errorCode)
  }

  @Test
  fun `test logAuthorizationMethodNotTried logging the method`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodNotTried(testLoginRequest.authId, "test_method")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(LoginLogger.EVENT_NAME_LOGIN_METHOD_NOT_TRIED), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
  }

  @Test
  fun `test logAuthorizationMethodNotTried with the custom event name logging the method and event name`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logAuthorizationMethodNotTried(
        testLoginRequest.authId, "test_method", "custom_event_name")

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq("custom_event_name"), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo("test_method")
  }

  @Test
  fun `test logLoginStatusStart logging the auth Id`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logLoginStatusStart(testLoginRequest.authId)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(eq(LoginLogger.EVENT_NAME_LOGIN_STATUS_START), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
  }

  @Test
  fun `test logLoginStatusSuccess logging the auth Id and result`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logLoginStatusSuccess(testLoginRequest.authId)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(LoginLogger.EVENT_NAME_LOGIN_STATUS_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.SUCCESS.loggingValue)
  }

  @Test
  fun `test logLoginStatusFailure logging the auth Id and result`() {
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logLoginStatusFailure(testLoginRequest.authId)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(LoginLogger.EVENT_NAME_LOGIN_STATUS_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginLogger.EVENT_EXTRAS_FAILURE)
  }

  @Test
  fun `test logLoginStatusError logging the auth Id, result and error message`() {
    val errorMessage = "error message"
    val error = Exception(errorMessage)
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logLoginStatusError(testLoginRequest.authId, error)

    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(LoginLogger.EVENT_NAME_LOGIN_STATUS_COMPLETE), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_AUTH_LOGGER_ID))
        .isEqualTo(testLoginRequest.authId)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.ERROR.loggingValue)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(error.toString())
  }

  @Test
  fun `test logUnexpectedError logging the error result`() {
    val errorMessage = "error message"
    val eventName = "test_event"
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logUnexpectedError(eventName, errorMessage)

    verify(mockInternalAppEventsLogger).logEventImplicitly(eq(eventName), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.ERROR.loggingValue)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(errorMessage)
  }

  @Test
  fun `test logUnexpectedError with method logging the error result`() {
    val errorMessage = "error message"
    val eventName = "test_event"
    val method = "test_method"
    val bundleCaptor = argumentCaptor<Bundle>()

    loginLogger.logUnexpectedError(eventName, errorMessage, method)

    verify(mockInternalAppEventsLogger).logEventImplicitly(eq(eventName), bundleCaptor.capture())
    val params = bundleCaptor.firstValue
    assertThat(params.getString(LoginLogger.EVENT_PARAM_LOGIN_RESULT))
        .isEqualTo(LoginClient.Result.Code.ERROR.loggingValue)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_ERROR_MESSAGE)).isEqualTo(errorMessage)
    assertThat(params.getString(LoginLogger.EVENT_PARAM_METHOD)).isEqualTo(method)
  }
}
