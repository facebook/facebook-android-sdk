/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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

    override val tokenSource: AccessTokenSource = AccessTokenSource.CHROME_CUSTOM_TAB

    override val nameForLogging: String = "testlogin"

    public override fun getParameters(request: LoginClient.Request): Bundle {
      return super.getParameters(request)
    }

    public override fun getRedirectUrl(): String {
      return super.getRedirectUrl()
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
    val uri = Uri.parse(testHandler.getRedirectUrl())
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
