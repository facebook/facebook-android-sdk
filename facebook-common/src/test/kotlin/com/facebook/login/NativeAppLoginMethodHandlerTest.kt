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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.internal.ServerProtocol
import com.facebook.internal.security.OidcSecurityUtil
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class)
class NativeAppLoginMethodHandlerTest : LoginHandlerTestCase() {
  private lateinit var mockExecutor: Executor
  private lateinit var testLoginHandler: TestNativeAppLoginMethodHandler
  private lateinit var testRequest: LoginClient.Request

  inner class TestNativeAppLoginMethodHandler(loginClient: LoginClient) :
      NativeAppLoginMethodHandler(loginClient) {

    var capturedRequest: LoginClient.Request? = null
    var mockCodeExchangeResult: Bundle? = null

    override fun tryAuthorize(request: LoginClient.Request): Int {
      capturedRequest = request
      return 1
    }

    override fun processCodeExchange(request: LoginClient.Request, values: Bundle): Bundle {
      val result = mockCodeExchangeResult
      if (result == null) {
        throw FacebookException("No code exchange result available")
      } else {
        return result
      }
    }

    override fun describeContents(): Int = 0

    override val nameForLogging: String = "test_native_app_login"
  }

  override fun setup() {
    super.setup()
    mockExecutor = mock()
    mockLoginClient = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    testLoginHandler = TestNativeAppLoginMethodHandler(mockLoginClient)
    testRequest = createRequest()
    whenever(mockLoginClient.pendingRequest).thenReturn(testRequest)
  }

  @Test
  fun `test no return data available will be treated as canceled`() {
    testLoginHandler.onActivityResult(0, Activity.RESULT_OK, null)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.CANCEL)
  }

  @Test
  fun `test cancel result without extras`() {
    val data = Intent()
    testLoginHandler.onActivityResult(0, Activity.RESULT_CANCELED, data)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.CANCEL)
  }

  @Test
  fun `test cancel result with error`() {
    val data = Intent()
    data.putExtra(ERROR_FIELD, TEST_ERROR_MESSAGE)
    testLoginHandler.onActivityResult(0, Activity.RESULT_CANCELED, data)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.CANCEL)
    assertThat(capturedOutcome.errorMessage).isEqualTo(TEST_ERROR_MESSAGE)
  }

  @Test
  fun `test cancel result with connection error will be treated as error`() {
    val data = Intent()
    data.putExtra(ERROR_CODE_FIELD, ServerProtocol.getErrorConnectionFailure())
    data.putExtra(ERROR_MESSAGE_FIELD, TEST_ERROR_MESSAGE)

    testLoginHandler.onActivityResult(0, Activity.RESULT_CANCELED, data)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.ERROR)
    assertThat(capturedOutcome.errorCode).isEqualTo(ServerProtocol.getErrorConnectionFailure())
    assertThat(capturedOutcome.errorMessage).isEqualTo(TEST_ERROR_MESSAGE)
  }

  @Test
  fun `test error result without extras`() {
    val data = Intent()

    testLoginHandler.onActivityResult(0, 15, data)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.ERROR)
    assertThat(capturedOutcome.errorMessage).contains("Unexpected resultCode")
  }

  @Test
  fun `test ok result without extras`() {
    val data = Intent()

    testLoginHandler.onActivityResult(0, Activity.RESULT_OK, data)

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.ERROR)
    assertThat(capturedOutcome.errorMessage).contains("Unexpected null")
  }

  @Test
  fun `test processing success response with code exchange enabled`() {
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any())).thenReturn(mock())
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
    val authenticationTokenString = AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest()
    val data = Intent()
    data.putExtra("code", "code_exchange")
    val codeExchangeResult = Bundle()
    codeExchangeResult.putString(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, TEST_ACCESS_TOKEN)
    codeExchangeResult.putString(
        ServerProtocol.DIALOG_PARAM_AUTHENTICATION_TOKEN, authenticationTokenString)
    codeExchangeResult.putString("signed_request", SIGNED_REQUEST_STR)

    testLoginHandler.mockCodeExchangeResult = codeExchangeResult

    testLoginHandler.onActivityResult(0, Activity.RESULT_OK, data)

    val runnableCaptor = argumentCaptor<Runnable>()
    verify(mockExecutor).execute(runnableCaptor.capture())
    val capturedRunnable = runnableCaptor.firstValue
    capturedRunnable.run()

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.SUCCESS)
    assertThat(capturedOutcome.token?.token).isEqualTo(TEST_ACCESS_TOKEN)
    assertThat(capturedOutcome.authenticationToken?.token).isEqualTo(authenticationTokenString)
  }

  @Test
  fun `test processing success response with invalid code exchange result`() {
    val data = Intent()
    data.putExtra("code", "code_exchange")
    val authenticationTokenString = AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest()
    val codeExchangeResult = Bundle()
    codeExchangeResult.putString(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, TEST_ACCESS_TOKEN)
    codeExchangeResult.putString(
        ServerProtocol.DIALOG_PARAM_AUTHENTICATION_TOKEN, authenticationTokenString)
    codeExchangeResult.putString("signed_request", "invalid result")

    testLoginHandler.mockCodeExchangeResult = codeExchangeResult

    testLoginHandler.onActivityResult(0, Activity.RESULT_OK, data)

    val runnableCaptor = argumentCaptor<Runnable>()
    verify(mockExecutor).execute(runnableCaptor.capture())
    val capturedRunnable = runnableCaptor.firstValue
    capturedRunnable.run()

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    assertThat(capturedOutcome.code).isEqualTo(LoginClient.Result.Code.ERROR)
  }

  companion object {
    private const val ERROR_FIELD = "error"
    private const val ERROR_CODE_FIELD = "error_code"
    private const val ERROR_MESSAGE_FIELD = "error_message"
    private const val TEST_ERROR_MESSAGE = "test error"
    private const val TEST_ACCESS_TOKEN = "test_access_token"
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJjb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
  }
}
