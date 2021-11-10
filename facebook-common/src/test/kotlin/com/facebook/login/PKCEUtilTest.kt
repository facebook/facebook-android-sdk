package com.facebook.login

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PKCEUtilTest : FacebookPowerMockTestCase() {
  // 128 characters valid code verifier
  val VALID_CODE_VERIFIER =
      "MAmh_Ym~fzC9X0QAtsdI~vHaToXbio7J.aHQIV0.VX-_o5MqtReS2t~3Hw-8RD-vL7mV.KyJGR0xXnBjN9BuMldSH5RM4xBa8rRhu2Yju828y6FzJXB2BCEW7~XrcMkp"

  @Test
  fun `test isValidCodeVerifier with valid code verifier`() {
    assertThat(PKCEUtil.isValidCodeVerifier(VALID_CODE_VERIFIER)).isTrue
  }

  @Test
  fun `test isValidCodeVerifier with invalid empty or null code verifier`() {
    assertThat(PKCEUtil.isValidCodeVerifier("")).isFalse
    assertThat(PKCEUtil.isValidCodeVerifier(null)).isFalse
  }

  @Test
  fun `test isValidCodeVerifier with less than 43 characters invalid code verifier`() {
    assertThat(PKCEUtil.isValidCodeVerifier("abc")).isFalse
  }

  @Test
  fun `test isValidCodeVerifier with more than 128 characters invalid code verifier`() {
    assertThat(PKCEUtil.isValidCodeVerifier(VALID_CODE_VERIFIER + "a")).isFalse
  }

  @Test
  fun `test isValidCodeVerifier with not-allowed character`() {
    assertThat(PKCEUtil.isValidCodeVerifier("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa=")).isFalse
  }

  @Test
  fun `test generateCodeVerifier and make sure generated code verifier is valid`() {
    val codeVerifier = PKCEUtil.generateCodeVerifier()
    assertThat(PKCEUtil.isValidCodeVerifier(codeVerifier)).isTrue
  }
}
