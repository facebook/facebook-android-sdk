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
import com.facebook.FacebookPowerMockTestCase
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.Date
import kotlin.math.abs
import org.assertj.core.api.Assertions.assertThat
import org.robolectric.Robolectric

abstract class LoginHandlerTestCase : FacebookPowerMockTestCase() {
  protected lateinit var activity: FragmentActivity
  protected lateinit var mockLoginClient: LoginClient

  override fun setup() {
    super.setup()
    mockLoginClient = mock()
    activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
    whenever(mockLoginClient.activity).thenReturn(activity)
  }

  @JvmOverloads
  protected open fun createRequest(previousAccessTokenString: String? = null): LoginClient.Request {
    return LoginClient.Request(
        LoginBehavior.NATIVE_WITH_FALLBACK,
        HashSet(PERMISSIONS),
        DefaultAudience.FRIENDS,
        "rerequest",
        "1234",
        "5678",
        LoginTargetApp.FACEBOOK,
        AuthenticationTokenTestUtil.NONCE,
        CODE_VERIFIER,
        CODE_CHALLENGE,
        CodeChallengeMethod.S256)
  }

  protected open fun createIGAppRequest(): LoginClient.Request {
    return LoginClient.Request(
        LoginBehavior.NATIVE_WITH_FALLBACK,
        HashSet(PERMISSIONS),
        DefaultAudience.FRIENDS,
        "rerequest",
        "1234",
        "5678",
        LoginTargetApp.INSTAGRAM)
  }

  protected open fun createIGWebRequest(): LoginClient.Request {
    return LoginClient.Request(
        LoginBehavior.WEB_ONLY,
        HashSet(PERMISSIONS),
        DefaultAudience.FRIENDS,
        "rerequest",
        "1234",
        "5678",
        LoginTargetApp.INSTAGRAM)
  }

  protected open fun createRequestWithNonce(): LoginClient.Request {
    val codeVerifier = PKCEUtil.generateCodeVerifier()
    return LoginClient.Request(
        LoginBehavior.NATIVE_WITH_FALLBACK,
        HashSet(PERMISSIONS),
        DefaultAudience.FRIENDS,
        "rerequest",
        "1234",
        "5678",
        null,
        AuthenticationTokenTestUtil.NONCE,
        codeVerifier,
        PKCEUtil.generateCodeChallenge(codeVerifier, CodeChallengeMethod.S256),
        CodeChallengeMethod.S256)
  }

  protected fun assertDateDiffersWithinDelta(
      expected: Date,
      actual: Date,
      expectedDifference: Long,
      deltaInMsec: Long
  ) {
    val delta = abs(expected.time - actual.time) - expectedDifference
    assertThat(delta).isLessThan(deltaInMsec)
  }

  protected val encodedAuthTokenString: String
    get() = getEncodedAuthTokenStringForTest()

  companion object {
    const val ACCESS_TOKEN = "An access token"
    const val USER_ID = "123"
    const val EXPIRES_IN_DELTA = (3600 * 24 * 60).toLong()
    @JvmField val PERMISSIONS = setOf("go outside", "come back in")
    const val ERROR_MESSAGE = "This is bad!"
    const val CODE_VERIFIER = "codeVerifier"
    const val CODE_CHALLENGE = "codeChallenge"
  }
}
