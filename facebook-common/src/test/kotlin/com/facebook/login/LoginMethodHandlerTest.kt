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
import android.os.Parcel
import com.facebook.AccessToken
import com.facebook.AuthenticationToken
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookRequestError
import com.facebook.FacebookSdk
import com.facebook.FacebookServiceException
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.internal.NativeProtocol
import com.facebook.internal.ServerProtocol
import com.facebook.internal.security.OidcSecurityUtil
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.security.PublicKey
import java.util.HashSet
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.times
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, OidcSecurityUtil::class, PKCEUtil::class, GraphRequest::class)
class LoginMethodHandlerTest : FacebookPowerMockTestCase() {

  private lateinit var mockLoginClient: LoginClient
  private lateinit var testHandler: TestLoginMethodHandler
  private lateinit var request: LoginClient.Request
  private lateinit var bundle: Bundle
  private lateinit var graphRequestMock: GraphRequest
  private lateinit var graphResponseMock: GraphResponse

  private class TestLoginMethodHandler(loginClient: LoginClient) : LoginMethodHandler(loginClient) {
    var tryAuthorizeCalledTimes = 0
    var capturedAuthorizeRequest: LoginClient.Request? = null
    override fun describeContents(): Int = 0

    override fun tryAuthorize(request: LoginClient.Request): Int {
      tryAuthorizeCalledTimes += 1
      capturedAuthorizeRequest = request
      return 0
    }

    override fun putChallengeParam(param: JSONObject) {
      super.putChallengeParam(param)
      param.put("challenge", "challenge-value")
    }

    // will be migrated as a property when LoginMethodHandler is migrated to Kotlin
    override val nameForLogging: String = "test_login_handler"

    public override fun getClientState(authId: String): String {
      return super.getClientState(authId)
    }

    public override fun processCodeExchange(request: LoginClient.Request, values: Bundle): Bundle {
      return super.processCodeExchange(request, values)
    }

    public override fun addLoggingExtra(key: String?, value: Any?) {
      super.addLoggingExtra(key, value)
    }
  }

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(GraphRequest::class.java)
    PowerMockito.mockStatic(PKCEUtil::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123456789")
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    PowerMockito.`when`(OidcSecurityUtil.getRawKeyFromEndPoint(any()))
        .thenReturn(AuthenticationTokenTestUtil.PUBLIC_KEY_STRING)
    val pubKey = mock<PublicKey>()
    PowerMockito.`when`(
            OidcSecurityUtil.getPublicKeyFromString(AuthenticationTokenTestUtil.PUBLIC_KEY_STRING))
        .thenReturn(pubKey)
    PowerMockito.`when`(OidcSecurityUtil.verify(eq(pubKey), any(), any())).thenReturn(true)
    mockLoginClient = mock()
    testHandler = TestLoginMethodHandler(mockLoginClient)
    val codeVerifier = "code_verifier"
    request =
        LoginClient.Request(
            LoginBehavior.NATIVE_WITH_FALLBACK,
            HashSet(LoginHandlerTestCase.PERMISSIONS),
            DefaultAudience.FRIENDS,
            "rerequest",
            "1234",
            "5678",
            null,
            AuthenticationTokenTestUtil.NONCE,
            codeVerifier,
            PKCEUtil.generateCodeChallenge(codeVerifier, CodeChallengeMethod.S256),
            CodeChallengeMethod.S256)
    bundle = Bundle()
    graphRequestMock = mock()
    graphResponseMock = mock()
  }

  @Test
  fun `test client state contains name for logging, auth id and challenge params`() {
    val clientState = testHandler.getClientState("1234")
    assertThat(clientState).contains("1234")
    assertThat(clientState).contains(testHandler.nameForLogging)
    assertThat(clientState).contains("challenge-value")
  }

  @Test
  fun `test createAuthenticationTokenFromNativeLogin with empty bundle`() {
    val authenticationToken =
        LoginMethodHandler.createAuthenticationTokenFromNativeLogin(
            Bundle(), AuthenticationTokenTestUtil.NONCE)
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun `test createAuthenticationTokenFromNativeLogin with valid bundle`() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val tokenString = authenticationToken.token
    val bundle = Bundle()
    bundle.putString(NativeProtocol.EXTRA_AUTHENTICATION_TOKEN, tokenString)
    val createdAuthenticationToken =
        LoginMethodHandler.createAuthenticationTokenFromNativeLogin(
            bundle, AuthenticationTokenTestUtil.NONCE)
    assertThat(createdAuthenticationToken).isEqualTo(authenticationToken)
  }

  @Test
  fun `test createAuthenticationTokenFromWebBundle with invalid bundle`() {
    val authenticationToken =
        LoginMethodHandler.createAuthenticationTokenFromWebBundle(
            Bundle(), AuthenticationTokenTestUtil.NONCE)
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun `test createAuthenticationTokenFromWebBundle with valid bundle`() {
    val authenticationToken = AuthenticationTokenTestUtil.getAuthenticationTokenForTest()
    val tokenString = authenticationToken.token
    val bundle = Bundle()
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, tokenString)
    val createdAuthenticationToken =
        LoginMethodHandler.createAuthenticationTokenFromWebBundle(
            bundle, AuthenticationTokenTestUtil.NONCE)
    checkNotNull(createdAuthenticationToken)
    assertThat(createdAuthenticationToken.token).isEqualTo(tokenString)
  }

  @Test
  fun `test createAccessTokenFromNativeLogin with empty bundle`() {
    val accessToken =
        LoginMethodHandler.createAccessTokenFromNativeLogin(
            Bundle(), mock(), FacebookSdk.getApplicationId())
    assertThat(accessToken).isNull()
  }

  @Test
  fun `test createAccessTokenFromNativeLogin with valid bundle`() {
    val bundle = Bundle()
    bundle.putStringArrayList(NativeProtocol.EXTRA_PERMISSIONS, arrayListOf("email"))
    bundle.putString(NativeProtocol.EXTRA_ACCESS_TOKEN, "access_token")
    bundle.putString(NativeProtocol.EXTRA_USER_ID, "user_id")
    bundle.putString(NativeProtocol.RESULT_ARGS_GRAPH_DOMAIN, "test.facebook.com")
    bundle.putLong(NativeProtocol.EXTRA_EXPIRES_SECONDS_SINCE_EPOCH, 36000L)
    bundle.putLong(NativeProtocol.EXTRA_DATA_ACCESS_EXPIRATION_TIME, 36000L)
    val accessToken =
        LoginMethodHandler.createAccessTokenFromNativeLogin(
            bundle, mock(), FacebookSdk.getApplicationId())
    assertThat(accessToken?.token).isEqualTo("access_token")
  }

  @Test
  fun `test createAccessTokenFromWebBundle with invalid bundle`() {
    val accessToken =
        LoginMethodHandler.createAccessTokenFromWebBundle(
            listOf(), Bundle(), mock(), FacebookSdk.getApplicationId())
    assertThat(accessToken).isNull()
  }

  @Test
  fun `test createAccessTokenFromWebBundle with valid bundle`() {
    val bundle = createValidWebLoginResultBundle()
    val accessToken =
        LoginMethodHandler.createAccessTokenFromWebBundle(
            listOf(), bundle, mock(), FacebookSdk.getApplicationId())
    assertThat(accessToken?.userId).isEqualTo(WEB_LOGIN_TEST_USER_ID)
  }

  @Test
  fun `test by default not to track multiple intents`() {
    assertThat(testHandler.shouldKeepTrackOfMultipleIntents()).isFalse
  }

  @Test
  fun `test write to parcel will write logging extras`() {
    testHandler.addLoggingExtra("extra_logging_key", "extra_logging_value")
    val parcel = mock<Parcel>()
    testHandler.writeToParcel(parcel, 0)
    verify(parcel).writeString("extra_logging_key")
    verify(parcel).writeString("extra_logging_value")
  }

  @Test
  fun `test processCodeExchange without code param`() {
    whenever(PKCEUtil.createCodeExchangeRequest(any(), any(), any())).thenReturn(graphRequestMock)
    try {
      testHandler.processCodeExchange(request, bundle)
      Assert.fail()
    } catch (ex: FacebookException) {
      assertThat(ex.message).isEqualTo("No code param found from the request")
    }
    verify(graphRequestMock, times(0)).executeAndWait()
  }

  @Test
  fun `test processCodeExchange with bad request`() {
    bundle.putString("code", "code")

    val requestErrorMock = mock<FacebookRequestError>()
    whenever(requestErrorMock.errorMessage).thenReturn("test")
    whenever(graphRequestMock.executeAndWait()).thenReturn(graphResponseMock)
    whenever(graphResponseMock.error).thenReturn(requestErrorMock)
    whenever(PKCEUtil.createCodeExchangeRequest(any(), any(), any())).thenReturn(graphRequestMock)
    try {
      testHandler.processCodeExchange(request, bundle)
      Assert.fail()
    } catch (ex: FacebookServiceException) {
      assertThat(ex.requestError.errorMessage).isEqualTo("test")
    }
  }

  @Test
  fun `test processCodeExchange with no access_token found from response`() {
    bundle.putString("code", "code")
    val jsonMock = mock<JSONObject>()

    whenever(graphRequestMock.executeAndWait()).thenReturn(graphResponseMock)
    whenever(graphResponseMock.error).thenReturn(null) // make sure no request error
    whenever(graphResponseMock.getJSONObject()).thenReturn(jsonMock)
    whenever(PKCEUtil.createCodeExchangeRequest(any(), any(), any())).thenReturn(graphRequestMock)
    try {
      testHandler.processCodeExchange(request, bundle)
      Assert.fail()
    } catch (ex: FacebookException) {
      assertThat(ex.message).isEqualTo("No access token found from result")
    }
  }

  @Test
  fun `test processCodeExchange successfully with access_token and id_token included`() {
    bundle.putString("code", "code")
    val responseJSONObject = JSONObject()
    responseJSONObject.put(ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN, "123")
    responseJSONObject.put(ServerProtocol.DIALOG_PARAM_AUTHENTICATION_TOKEN, "456")

    whenever(graphRequestMock.executeAndWait()).thenReturn(graphResponseMock)
    whenever(graphResponseMock.error).thenReturn(null) // make sure no request error
    whenever(graphResponseMock.getJSONObject()).thenReturn(responseJSONObject)
    whenever(PKCEUtil.createCodeExchangeRequest(any(), any(), any())).thenReturn(graphRequestMock)
    try {
      val values = testHandler.processCodeExchange(request, bundle)
      assertThat(values[ServerProtocol.DIALOG_PARAM_ACCESS_TOKEN]).isEqualTo("123")
      assertThat(values[ServerProtocol.DIALOG_PARAM_AUTHENTICATION_TOKEN]).isEqualTo("456")
    } catch (ex: FacebookException) {
      Assert.fail()
    }
  }

  @Test
  fun `test processCodeExchange with invalid JSON from response`() {
    bundle.putString("code", "code")

    whenever(graphRequestMock.executeAndWait()).thenReturn(graphResponseMock)
    whenever(graphResponseMock.error).thenReturn(null) // make sure no request error
    whenever(graphResponseMock.getJSONObject()).thenReturn(null) // null JSON or invalid json
    whenever(PKCEUtil.createCodeExchangeRequest(any(), any(), any())).thenReturn(graphRequestMock)
    try {
      testHandler.processCodeExchange(request, bundle)
      Assert.fail()
    } catch (ex: FacebookException) {
      assertThat(ex.message).isEqualTo("No access token found from result")
    }
  }

  companion object {
    // user_id = 54321 for this base64 code
    private const val SIGNATURE_AND_PAYLOAD = "signature.eyJ1c2VyX2lkIjo1NDMyMX0="
    const val WEB_LOGIN_TEST_USER_ID = "54321"

    internal fun createValidWebLoginResultBundle(): Bundle {
      val bundle = Bundle()
      bundle.putLong(AccessToken.EXPIRES_IN_KEY, 36000L)
      bundle.putLong(AccessToken.DATA_ACCESS_EXPIRATION_TIME, 36000L)
      bundle.putString(AccessToken.ACCESS_TOKEN_KEY, "access_token")
      bundle.putString(AccessToken.GRAPH_DOMAIN, "test.facebook.com")
      bundle.putString("signed_request", SIGNATURE_AND_PAYLOAD)
      return bundle
    }
  }
}
