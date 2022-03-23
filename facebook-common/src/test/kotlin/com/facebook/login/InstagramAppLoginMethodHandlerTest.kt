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
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest

@PowerMockIgnore("org.mockito.*", "org.robolectric.*", "org.powermock.*")
@PrepareForTest(LoginClient::class, FacebookSdk::class)
class InstagramAppLoginMethodHandlerTest : LoginHandlerTestCase() {
  companion object {
    private const val CANCEL_MESSAGE = "Permissions error"
    private const val SIGNED_REQUEST_STR =
        "ggarbage.eyJhbGdvcml0aG0iOiJITUFDSEEyNTYiLCJ" +
            "jb2RlIjoid2h5bm90IiwiaXNzdWVkX2F0IjoxNDIyNTAyMDkyLCJ1c2VyX2lkIjoiMTIzIn0"
  }

  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn("123456789")
  }

  @Test
  fun testInstagramAppHandlesSuccess() {
    val bundle = Bundle()
    bundle.putLong("expires_in", EXPIRES_IN_DELTA)
    bundle.putString("access_token", ACCESS_TOKEN)
    bundle.putString("signed_request", SIGNED_REQUEST_STR)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = InstagramAppLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    whenever(mockLoginClient.pendingRequest).thenReturn(request)

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
  fun testInstagramAppHandlesBack() {
    val handler = InstagramAppLoginMethodHandler(mockLoginClient)

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
  fun testInstagramAppHandlesCancel() {
    val bundle = Bundle()
    bundle.putString("error", CANCEL_MESSAGE)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = InstagramAppLoginMethodHandler(mockLoginClient)

    val request = createRequest()
    handler.tryAuthorize(request)
    handler.onActivityResult(0, Activity.RESULT_CANCELED, intent)

    val resultArgumentCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient, times(1)).completeAndValidate(resultArgumentCaptor.capture())

    val result = resultArgumentCaptor.firstValue

    assertThat(result).isNotNull
  }

  @Test
  fun testInstagramAppHandlesErrorMessage() {
    val bundle = Bundle()
    bundle.putString("error", ERROR_MESSAGE)

    val intent = Intent()
    intent.putExtras(bundle)

    val handler = InstagramAppLoginMethodHandler(mockLoginClient)

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
    assertThat(errorMessage).contains(ERROR_MESSAGE)
  }
}
