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
  fun `test initialize LoginConfiguration with valid nonce`() {
    val config = LoginConfiguration(listOf("user_email"), AuthenticationTokenTestUtil.NONCE)
    assertThat(config.nonce).isEqualTo(AuthenticationTokenTestUtil.NONCE)
  }
}
