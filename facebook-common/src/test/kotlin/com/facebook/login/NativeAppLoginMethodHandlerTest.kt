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
import android.os.Parcel
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookSdk
import com.facebook.internal.ServerProtocol
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class NativeAppLoginMethodHandlerTest : LoginHandlerTestCase() {
  private lateinit var mockExecutor: Executor
  private lateinit var testLoginHandler: NativeAppLoginMethodHandler
  private lateinit var testRequest: LoginClient.Request

  class TestNativeAppLoginMethodHandler : NativeAppLoginMethodHandler {
    constructor(loginClient: LoginClient) : super(loginClient)
    constructor(source: Parcel) : super(source)

    var capturedRequest: LoginClient.Request? = null

    override fun tryAuthorize(request: LoginClient.Request): Int {
      capturedRequest = request
      return 1
    }

    override fun describeContents(): Int = 0

    override val nameForLogging: String = "test_native_app_login"
  }

  override fun before() {
    super.before()
    mockExecutor = mock()
    mockLoginClient = mock()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    testLoginHandler = TestNativeAppLoginMethodHandler(mockLoginClient)
    testRequest = createRequest()
    whenever(mockLoginClient.getPendingRequest()).thenReturn(testRequest)
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

  companion object {
    private const val ERROR_FIELD = "error"
    private const val ERROR_CODE_FIELD = "error_code"
    private const val ERROR_MESSAGE_FIELD = "error_message"
    private const val TEST_ERROR_MESSAGE = "test error"
  }
}
