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
import com.facebook.AccessToken
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookSdk
import com.facebook.TestUtils
import com.facebook.internal.FacebookDialogFragment
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PowerMockIgnore("org.mockito.*", "org.robolectric.*")
@PrepareForTest(AccessToken::class, FacebookSdk::class, LoginClient::class)
class WebViewLoginMethodHandlerTest : LoginHandlerTestCase() {
  companion object {
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ" +
            "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
  }

  @Test
  fun testWebViewHandlesSuccess() {
    mockTryAuthorize()
    val bundle = Bundle()
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("expires_in", String.format("%d", EXPIRES_IN_DELTA))
    bundle.putString("code", "Something else")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val handler = WebViewLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.onWebDialogComplete(request, bundle, null)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue
    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.SUCCESS)

    val token = checkNotNull(result.token)
    assertThat(token).isNotNull
    assertThat(token.token).isEqualTo(ACCESS_TOKEN)
    assertDateDiffersWithinDelta(Date(), token.expires, EXPIRES_IN_DELTA * 1000, 1000)
    TestUtils.assertSamePermissions(PERMISSIONS, token.permissions)
  }

  @Test
  fun testIGWebViewHandlesSuccess() {
    mockTryAuthorize()
    val bundle = Bundle()
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("graph_domain", "instagram")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val handler = WebViewLoginMethodHandler(mockLoginClient)

    val igRequest = createIGWebRequest()
    handler.tryAuthorize(igRequest)
    handler.onWebDialogComplete(igRequest, bundle, null)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue
    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.SUCCESS)

    val token = checkNotNull(result.token)
    assertThat(token).isNotNull
    assertThat(token.token).isEqualTo(ACCESS_TOKEN)
    assertThat(token.userId).isEqualTo(USER_ID)
    assertThat(token.graphDomain).isEqualTo("instagram")
    assertThat(token.source).isEqualTo(AccessTokenSource.INSTAGRAM_WEB_VIEW)
    TestUtils.assertSamePermissions(PERMISSIONS, token.permissions)
  }

  @Test
  fun testWebViewHandlesCancel() {
    val handler = WebViewLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.onWebDialogComplete(request, null, FacebookOperationCanceledException())

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.CANCEL)
    assertThat(result.token).isNull()
    assertThat(result.errorMessage).isNotNull
  }

  @Test
  fun testWebViewHandlesError() {
    val handler = WebViewLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.onWebDialogComplete(request, null, FacebookException(ERROR_MESSAGE))

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())
    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.ERROR)
    assertThat(result.token).isNull()
    assertThat(result.errorMessage).isNotNull
    assertThat(result.errorMessage).isEqualTo(ERROR_MESSAGE)
  }

  @Test
  fun testFromDialog() {
    val permissions = arrayListOf("stream_publish", "go_outside_and_play")
    val token = "AnImaginaryTokenValue"

    val bundle = Bundle()
    bundle.putString("access_token", token)
    bundle.putString("expires_in", "60")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val accessToken =
        checkNotNull(
            LoginMethodHandler.createAccessTokenFromWebBundle(
                permissions, bundle, AccessTokenSource.WEB_VIEW, "1234"))

    TestUtils.assertSamePermissions(permissions, accessToken)
    assertThat(accessToken).isNotNull
    assertThat(accessToken.token).isEqualTo(token)
    assertThat(accessToken.source).isEqualTo(AccessTokenSource.WEB_VIEW)
    assertThat(!accessToken.isExpired).isTrue
  }

  @Test
  fun testFromSSOWithExpiresString() {
    val permissions = arrayListOf("stream_publish", "go_outside_and_play")
    val token = "AnImaginaryTokenValue"

    val bundle = Bundle()
    bundle.putString("access_token", token)
    bundle.putString("expires_in", "60")
    bundle.putString("extra_extra", "Something unrelated")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val accessToken =
        checkNotNull(
            LoginMethodHandler.createAccessTokenFromWebBundle(
                permissions, bundle, AccessTokenSource.FACEBOOK_APPLICATION_WEB, "1234"))

    TestUtils.assertSamePermissions(permissions, accessToken)
    assertThat(accessToken).isNotNull
    assertThat(accessToken.token).isEqualTo(token)
    assertThat(accessToken.source).isEqualTo(AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    assertThat(!accessToken.isExpired).isTrue
  }

  @Test
  fun testFromSSOWithExpiresLong() {
    val permissions = arrayListOf("stream_publish", "go_outside_and_play")
    val token = "AnImaginaryTokenValue"

    val bundle = Bundle()
    bundle.putString("access_token", token)
    bundle.putString("expires_in", "60")
    bundle.putString("extra_extra", "Something unrelated")
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val accessToken =
        checkNotNull(
            LoginMethodHandler.createAccessTokenFromWebBundle(
                permissions, bundle, AccessTokenSource.FACEBOOK_APPLICATION_WEB, "1234"))

    TestUtils.assertSamePermissions(permissions, accessToken)
    assertThat(accessToken).isNotNull
    assertThat(accessToken.token).isEqualTo(token)
    assertThat(accessToken.source).isEqualTo(AccessTokenSource.FACEBOOK_APPLICATION_WEB)
    assertThat(!accessToken.isExpired).isTrue
  }

  fun mockTryAuthorize() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)

    val mockCompanion = mock<AccessToken.Companion>()
    WhiteboxImpl.setInternalState(AccessToken::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.getCurrentAccessToken()).thenReturn(null)
    val dialogFragment = mock<FacebookDialogFragment>()
    PowerMockito.whenNew(FacebookDialogFragment::class.java)
        .withAnyArguments()
        .thenReturn(dialogFragment)
  }
}
