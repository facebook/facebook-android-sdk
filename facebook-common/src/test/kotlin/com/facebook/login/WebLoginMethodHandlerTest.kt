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

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.FacebookServiceException
import com.facebook.MockSharedPreference
import com.facebook.internal.ServerProtocol
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class WebLoginMethodHandlerTest : FacebookPowerMockTestCase() {
  private lateinit var mockLoginClient: LoginClient
  private lateinit var testHandler: TestWebLoginMethodHandler
  private lateinit var testRequest: LoginClient.Request
  private lateinit var mockActivity: FragmentActivity
  private lateinit var mockSharedPreference: MockSharedPreference

  internal class TestWebLoginMethodHandler(loginClient: LoginClient) :
      WebLoginMethodHandler(loginClient) {
    override fun tryAuthorize(request: LoginClient.Request): Int = 0

    override fun describeContents(): Int = 0

    override fun getTokenSource(): AccessTokenSource = AccessTokenSource.CHROME_CUSTOM_TAB

    override val nameForLogging: String = "testlogin"

    public override fun getParameters(request: LoginClient.Request): Bundle {
      return super.getParameters(request)
    }

    public override fun onComplete(
        request: LoginClient.Request,
        values: Bundle?,
        error: FacebookException?
    ) {
      super.onComplete(request, values, error)
    }
  }

  override fun setup() {
    super.setup()
    mockActivity = mock()
    mockSharedPreference = MockSharedPreference()
    whenever(mockActivity.getSharedPreferences(any<String>(), any()))
        .thenReturn(mockSharedPreference)
    mockLoginClient = mock()
    whenever(mockLoginClient.activity).thenReturn(mockActivity)
    testHandler = TestWebLoginMethodHandler(mockLoginClient)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123456789")
    testRequest =
        LoginClient.Request(
            LoginBehavior.DIALOG_ONLY,
            setOf("email", "public_profile"),
            DefaultAudience.EVERYONE,
            "testAuthType",
            "123456789",
            "testAuthId")
  }

  @Test
  fun `test build parameters from request`() {
    val parameters = testHandler.getParameters(testRequest)
    assertThat(parameters.getString(ServerProtocol.DIALOG_PARAM_SCOPE))
        .isEqualTo("email,public_profile")
    assertThat(parameters.getString(ServerProtocol.DIALOG_PARAM_CBT)).isNotNull
    assertThat(parameters.getString(ServerProtocol.DIALOG_PARAM_IES)).isNotNull
    assertThat(parameters.getString(ServerProtocol.DIALOG_PARAM_STATE)).contains("testAuthId")
  }

  @Test
  fun `test onComplete will save access token and validate outcome`() {
    val values = LoginMethodHandlerTest.createValidWebLoginResultBundle()
    testHandler.onComplete(testRequest, values, null)
    assertThat(mockSharedPreference.getString("TOKEN", "")).isEqualTo("access_token")
    verify(mockLoginClient).completeAndValidate(any())
  }

  @Test
  fun `test redirect url is a valid uri`() {
    val uri = Uri.parse(testHandler.redirectUrl)
    assertThat(uri.scheme).isEqualTo("fb123456789")
    assertThat(uri.authority).isEqualTo("authorize")
  }

  @Test
  fun `test onComplete if a facebook service exception is received`() {
    val errorMessage = "unknown error"
    val requestError = FacebookRequestError(0xff, "test", errorMessage)
    val error = FacebookServiceException(requestError, null)
    testHandler.onComplete(testRequest, null, error)
    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val capturedOutcome = outcomeCaptor.firstValue
    val outcomeErrorMessage = checkNotNull(capturedOutcome.errorMessage)
    assertThat(outcomeErrorMessage).isEqualTo(requestError.toString())
  }
}
