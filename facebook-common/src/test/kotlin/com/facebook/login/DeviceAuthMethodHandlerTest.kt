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

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.facebook.AccessTokenSource
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DeviceAuthMethodHandlerTest : FacebookPowerMockTestCase() {
  private class TestHandler(
      loginClient: LoginClient,
      private val mockDeviceAuthDialog: DeviceAuthDialog
  ) : DeviceAuthMethodHandler(loginClient) {
    override fun createDeviceAuthDialog(): DeviceAuthDialog = mockDeviceAuthDialog
  }

  private lateinit var mockDeviceAuthDialog: DeviceAuthDialog
  private lateinit var mockLoginClient: LoginClient
  private lateinit var mockLoginRequest: LoginClient.Request
  private lateinit var mockActivity: FragmentActivity
  private lateinit var testHandler: TestHandler

  override fun setup() {
    super.setup()
    mockDeviceAuthDialog = mock()
    mockLoginClient = mock()
    mockActivity = mock()
    mockLoginRequest = mock()
    whenever(mockLoginClient.activity).thenReturn(mockActivity)
    testHandler = TestHandler(mockLoginClient, mockDeviceAuthDialog)
  }

  @Test
  fun `test try authorize will start login with auth dialog`() {
    whenever(mockActivity.isFinishing).thenReturn(false)
    val mockFragmentManager = mock<FragmentManager>()
    whenever(mockActivity.supportFragmentManager).thenReturn(mockFragmentManager)

    testHandler.tryAuthorize(mockLoginRequest)

    verify(mockDeviceAuthDialog).show(eq(mockFragmentManager), any())
    verify(mockDeviceAuthDialog).startLogin(mockLoginRequest)
  }

  @Test
  fun `test if the activity is not available for displaying dialogs`() {
    val request = mock<LoginClient.Request>()
    whenever(mockActivity.isFinishing).thenReturn(true)

    testHandler.tryAuthorize(request)

    verify(mockDeviceAuthDialog, never()).startLogin(request)
  }

  @Test
  fun `test the handler keeps one background executor`() {
    val executor1 = DeviceAuthMethodHandler.getBackgroundExecutor()
    val executor2 = DeviceAuthMethodHandler.getBackgroundExecutor()
    assertThat(executor1).isSameAs(executor2)
  }

  @Test
  fun `test on cancel will trigger a cancel outcome`() {
    whenever(mockLoginClient.pendingRequest).thenReturn(mockLoginRequest)

    testHandler.onCancel()

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val outcome = outcomeCaptor.firstValue
    assertThat(outcome.code).isEqualTo(LoginClient.Result.Code.CANCEL)
  }

  @Test
  fun `test on error will trigger an error outcome`() {
    whenever(mockLoginClient.pendingRequest).thenReturn(mockLoginRequest)

    testHandler.onError(FacebookException())

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val outcome = outcomeCaptor.firstValue
    assertThat(outcome.code).isEqualTo(LoginClient.Result.Code.ERROR)
  }

  @Test
  fun `test on success will pass the access token to login client`() {
    whenever(mockLoginClient.pendingRequest).thenReturn(mockLoginRequest)

    testHandler.onSuccess(
        "access_token", // accessToken
        "123456789", // applicationId
        "987654321", // userId
        listOf("email"), // permissions
        listOf(), // declinedPermissions
        listOf(), // expiredPermissions
        AccessTokenSource.DEVICE_AUTH, // accessTokenSource
        Date(Long.MAX_VALUE), // expirationTime
        Date(), // lastRefreshTime
        Date(Long.MAX_VALUE) // dataAccessExpirationTime
        )

    val outcomeCaptor = argumentCaptor<LoginClient.Result>()
    verify(mockLoginClient).completeAndValidate(outcomeCaptor.capture())
    val outcome = outcomeCaptor.firstValue
    assertThat(outcome.code).isEqualTo(LoginClient.Result.Code.SUCCESS)
    val token = checkNotNull(outcome.token)
    assertThat(token.token).isEqualTo("access_token")
  }
}
