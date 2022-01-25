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
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.pm.ServiceInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.AuthenticationToken
import com.facebook.CustomTabMainActivity
import com.facebook.FacebookActivity
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookSdk
import com.facebook.TestUtils
import com.facebook.internal.Validate
import com.facebook.internal.security.OidcSecurityUtil
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(Validate::class, FacebookSdk::class, OidcSecurityUtil::class)
class CustomTabLoginMethodHandlerTest : LoginHandlerTestCase() {
  private lateinit var request: LoginClient.Request
  private lateinit var mockAccessTokenCompanion: AccessToken.Companion

  @Before
  fun setUp() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)

    mockAccessTokenCompanion = mock()
    whenever(mockAccessTokenCompanion.getCurrentAccessToken()).thenReturn(null)
    Whitebox.setInternalState(AccessToken::class.java, "Companion", mockAccessTokenCompanion)

    val fragment: Fragment = mock<LoginFragment>()
    whenever(mockLoginClient.getFragment()).thenReturn(fragment)
    request = createRequest()

    // mock and bypass signature verification
    PowerMockito.mockStatic(OidcSecurityUtil::class.java)
    whenever(OidcSecurityUtil.getRawKeyFromEndPoint(any())).thenReturn("key")
    whenever(OidcSecurityUtil.getPublicKeyFromString(any())).thenReturn(mock())
    whenever(OidcSecurityUtil.verify(any(), any(), any())).thenReturn(true)
  }

  @Test
  fun testCustomTabHandlesSuccess() {
    testCustomTabHandles(encodedAuthTokenString)
  }

  @Test
  fun testCustomTabHandlesSuccessWithEmptyAuthenticationToken() {
    val result = testCustomTabHandles("")
    val authenticationToken = result.authenticationToken
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun testCustomTabHandlesSuccessWithNoAuthenticationToken() {
    val result = testCustomTabHandles(null)
    val authenticationToken = result.authenticationToken
    assertThat(authenticationToken).isNull()
  }

  @Test
  fun testIdTokenWithNonceCustomTabHandlesSuccess() {
    mockCustomTabRedirectActivity(true)
    val requestWithNonce = createRequestWithNonce()
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    val expectedIdTokenString = getEncodedAuthTokenStringForTest()
    val bundle = Bundle()
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, expectedIdTokenString)
    bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA))
    bundle.putString("signed_request", SIGNED_REQUEST_STR)
    handler.onComplete(requestWithNonce, bundle, null)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultArgumentCaptor.capture())
    val result = checkNotNull(resultArgumentCaptor.firstValue)
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.SUCCESS)
    val idToken = checkNotNull(result.authenticationToken)
    assertThat(idToken.token).isEqualTo(expectedIdTokenString)
    val token = checkNotNull(result.token)
    assertThat(token.token).isEqualTo(ACCESS_TOKEN)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    TestUtils.assertSamePermissions(PERMISSIONS, token.permissions)
  }

  @Test
  fun testIGCustomTabHandlesSuccess() {
    mockCustomTabRedirectActivity(true)
    val igRequest = createIGWebRequest()
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    val bundle = Bundle()
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("graph_domain", "instagram")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)
    handler.onComplete(igRequest, bundle, null)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue
    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.SUCCESS)
    val token = checkNotNull(result.token)
    assertThat(token.token).isEqualTo(ACCESS_TOKEN)
    assertThat(token.userId).isEqualTo(USER_ID)
    assertThat(token.graphDomain).isEqualTo("instagram")
    assertThat(token.source).isEqualTo(AccessTokenSource.INSTAGRAM_CUSTOM_CHROME_TAB)
    TestUtils.assertSamePermissions(PERMISSIONS, token.permissions)
  }

  @Test
  fun testCustomTabHandlesCancel() {
    mockCustomTabRedirectActivity(true)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    handler.onComplete(request, null, FacebookOperationCanceledException())
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultArgumentCaptor.capture())
    val result = checkNotNull(resultArgumentCaptor.firstValue)
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.CANCEL)
    assertThat(result.token).isNull()
    assertThat(result.errorMessage).isNotNull
  }

  @Test
  fun testCustomTabHandlesError() {
    mockCustomTabRedirectActivity(true)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    handler.onComplete(request, null, FacebookException(ERROR_MESSAGE))
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultArgumentCaptor.capture())
    val result = checkNotNull(resultArgumentCaptor.firstValue)
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.ERROR)
    assertThat(result.token).isNull()
    assertThat(result.errorMessage).isNotNull
    assertThat(result.errorMessage).isEqualTo(ERROR_MESSAGE)
  }

  @Test
  fun testTryAuthorizeNeedsRedirectActivity() {
    mockChromeCustomTabsSupported(true, CHROME_PACKAGE)
    mockCustomTabRedirectActivity(true)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.tryAuthorize(request)).isEqualTo(1)
  }

  @Test
  fun testTryAuthorizeWithChromePackage() {
    mockCustomTabRedirectActivity(true)
    mockChromeCustomTabsSupported(true, CHROME_PACKAGE)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.tryAuthorize(request)).isEqualTo(1)
  }

  @Test
  fun testTryAuthorizeWithChromeBetaPackage() {
    mockCustomTabRedirectActivity(true)
    mockChromeCustomTabsSupported(true, BETA_PACKAGE)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.tryAuthorize(request)).isEqualTo(1)
  }

  @Test
  fun testTryAuthorizeWithChromeDevPackage() {
    mockCustomTabRedirectActivity(true)
    mockChromeCustomTabsSupported(true, DEV_PACKAGE)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.tryAuthorize(request)).isEqualTo(1)
  }

  private fun mockChromeCustomTabsSupported(supported: Boolean, packageName: String) {
    val resolveInfos: MutableList<ResolveInfo> = ArrayList()
    val resolveInfo = ResolveInfo()
    val serviceInfo = ServiceInfo()
    serviceInfo.packageName = packageName
    resolveInfo.serviceInfo = serviceInfo
    if (supported) {
      resolveInfos.add(resolveInfo)
    }
    val packageManager = mock<PackageManager>()
    whenever(packageManager.queryIntentServices(any(), any())).thenReturn(resolveInfos)
    activity = mock<FacebookActivity>()
    whenever(mockLoginClient.activity).thenReturn(activity)
    whenever(activity.packageManager).thenReturn(packageManager)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationContext()).thenReturn(activity)
  }

  private fun mockCustomTabRedirectActivity(hasActivity: Boolean) {
    PowerMockito.mockStatic(Validate::class.java)
    whenever(Validate.hasCustomTabRedirectActivity(anyOrNull(), anyOrNull()))
        .thenReturn(hasActivity)
  }

  private fun testCustomTabHandles(authenticationTokenString: String?): LoginClient.Result {
    val requestWithNonce = createRequestWithNonce()
    mockCustomTabRedirectActivity(true)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    val bundle = Bundle()
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString(AuthenticationToken.AUTHENTICATION_TOKEN_KEY, authenticationTokenString)
    bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA))
    bundle.putString("code", "Something else")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)
    handler.onComplete(requestWithNonce, bundle, null)
    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultArgumentCaptor.capture())
    val result = checkNotNull(resultArgumentCaptor.firstValue)
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.SUCCESS)
    val token = checkNotNull(result.token)
    assertThat(token.token).isEqualTo(ACCESS_TOKEN)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    TestUtils.assertSamePermissions(PERMISSIONS, token.permissions)
    return result
  }

  @Test
  fun `test receiving no browser exception as activity result`() {
    mockCustomTabRedirectActivity(true)
    val data = Intent()
    data.putExtra(CustomTabMainActivity.NO_ACTIVITY_EXCEPTION, true)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.onActivityResult(1, Activity.RESULT_CANCELED, data)).isFalse
  }

  @Test
  fun `test receiving user cancel result`() {
    mockCustomTabRedirectActivity(true)
    whenever(mockLoginClient.getPendingRequest()).thenReturn(request)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.onActivityResult(1, Activity.RESULT_CANCELED, null)).isFalse
    val resultCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(resultCaptor.capture())
    val result = resultCaptor.firstValue
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.CANCEL)
    assertThat(result.errorMessage).isEqualTo(LoginMethodHandler.USER_CANCELED_LOG_IN_ERROR_MESSAGE)
  }

  @Test
  fun `test receiving user ok result`() {
    mockCustomTabRedirectActivity(true)
    whenever(mockLoginClient.getPendingRequest()).thenReturn(request)
    val handler = CustomTabLoginMethodHandler(mockLoginClient)
    assertThat(handler.onActivityResult(1, Activity.RESULT_OK, null)).isTrue
  }

  companion object {
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJjb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
    private const val CHROME_PACKAGE = "com.android.chrome"
    private const val DEV_PACKAGE = "com.chrome.dev"
    private const val BETA_PACKAGE = "com.chrome.beta"
  }
}
