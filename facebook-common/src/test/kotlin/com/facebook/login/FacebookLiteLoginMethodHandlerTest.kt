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
import com.facebook.FacebookSdk
import com.facebook.TestUtils
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RuntimeEnvironment

@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "org.powermock.*")
@PrepareForTest(LoginClient::class)
class FacebookLiteLoginMethodHandlerTest : LoginHandlerTestCase() {
  companion object {
    private const val CANCEL_MESSAGE = "Permissions error"
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ" +
            "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
  }

  @Throws(Exception::class)
  @Before
  override fun before() {
    super.before()
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setAutoLogAppEventsEnabled(false)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
  }

  @Test
  fun testFacebookLiteHandlesSuccess() {
    val bundle = Bundle()
    bundle.putLong("expires_in", EXPIRES_IN_DELTA)
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = FacebookLiteLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    PowerMockito.`when`(mockLoginClient.getPendingRequest()).thenReturn(request)

    handler.tryAuthorize(request)
    handler.onActivityResult(0, Activity.RESULT_OK, intent)

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
  fun testFacebookLiteHandlesBack() {
    val handler = FacebookLiteLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.tryAuthorize(request)
    handler.onActivityResult(0, Activity.RESULT_CANCELED, null)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.CANCEL)

    assertThat(result.token).isNull()
    assertThat(result.errorMessage).isEqualTo("Operation canceled")
  }

  @Test
  fun testFacebookLiteHandlesCancel() {
    val bundle = Bundle()
    bundle.putString("error", CANCEL_MESSAGE)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = FacebookLiteLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.tryAuthorize(request)
    handler.onActivityResult(0, Activity.RESULT_CANCELED, intent)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
  }

  @Test
  fun testFacebookLiteHandlesErrorMessage() {
    val bundle = Bundle()
    bundle.putString("error", ERROR_MESSAGE)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = FacebookLiteLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.tryAuthorize(request)
    handler.onActivityResult(0, Activity.RESULT_CANCELED, intent)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
    assertThat(result.code).isEqualTo(LoginClient.Result.Code.CANCEL)

    assertThat(result.token).isNull()
    val errorMessage = checkNotNull(result.errorMessage)
    assertThat(errorMessage).isNotNull
    assertThat(errorMessage.contains(ERROR_MESSAGE)).isTrue
  }
}
