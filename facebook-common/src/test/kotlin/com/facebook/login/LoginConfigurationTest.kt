/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LoginConfigurationTest : FacebookPowerMockTestCase() {

  @Test
  fun `test initialize LoginConfiguration without nonce`() {
    val config = LoginConfiguration(listOf("user_email"))
    assertThat(config.nonce).isNotEmpty
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test initialize LoginConfiguration with invalid nonce`() {
    LoginConfiguration(listOf("user_email"), "nonce ")
  }

  @Test
  fun `test initialize LoginConfiguration with correct fields`() {
    val codeVerifier = PKCEUtil.generateCodeVerifier()
    val config =
        LoginConfiguration(listOf("user_email"), AuthenticationTokenTestUtil.NONCE, codeVerifier)
    assertThat(config.permissions).isNotNull
    assertThat(config.permissions).contains(LoginConfiguration.OPENID)
    assertThat(config.nonce).isEqualTo(AuthenticationTokenTestUtil.NONCE)
    assertThat(config.codeVerifier).isEqualTo(codeVerifier)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test initialize LoginConfiguration with invalid code verifier`() {
    LoginConfiguration(listOf("user_email"), AuthenticationTokenTestUtil.NONCE, "")
  }
}
