/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import androidx.fragment.app.FragmentActivity
import com.facebook.FacebookPowerMockTestCase
import com.facebook.login.AuthenticationTokenTestUtil.getEncodedAuthTokenStringForTest
import java.util.Date
import kotlin.math.abs
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
        CodeChallengeMethod.S256,
        "https://example.com/redirect/my_uri")
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
