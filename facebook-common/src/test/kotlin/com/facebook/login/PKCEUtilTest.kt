package com.facebook.login

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class PKCEUtilTest : FacebookPowerMockTestCase() {
  // 128 characters valid code verifier
  val VALID_CODE_VERIFIER =
      "MAmh_Ym~fzC9X0QAtsdI~vHaToXbio7J.aHQIV0.VX-_o5MqtReS2t~3Hw-8RD-vL7mV.KyJGR0xXnBjN9BuMldSH5RM4xBa8rRhu2Yju828y6FzJXB2BCEW7~XrcMkp"

  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    whenever(FacebookSdk.getApplicationId()).thenReturn(AuthenticationTokenTestUtil.APP_ID)
  }

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

  @Test
  fun `test generateCodeChallenge with plain method`() {
    val codeChallenge =
        PKCEUtil.generateCodeChallenge(VALID_CODE_VERIFIER, CodeChallengeMethod.PLAIN)
    assertThat(codeChallenge).isEqualTo(VALID_CODE_VERIFIER)
  }

  @Test(expected = FacebookException::class)
  fun `test generateCodeChallenge with invalid code verifier`() {
    PKCEUtil.generateCodeChallenge("abc", CodeChallengeMethod.PLAIN)
  }

  @Test
  fun `test generateCodeChallenge with correct code verifier and S256`() {
    val expectedCodeChallenge = "Kwymmz9hbJb0Df_F6y-LaDxntHMdlm8vgM5T0320czQ"
    val codeChallenge =
        PKCEUtil.generateCodeChallenge(VALID_CODE_VERIFIER, CodeChallengeMethod.S256)
    assertThat(codeChallenge).isEqualTo(expectedCodeChallenge)
  }

  @Test
  fun `test createCodeExchangeRequest to make sure create correctly`() {
    val paramCode = "code"
    val paramRedirectUri = "redirect_uri"
    val paramCodeVerifier = "code_verifier"
    val paramAppId = AuthenticationTokenTestUtil.APP_ID

    val graphRequest =
        PKCEUtil.createCodeExchangeRequest(paramCode, paramRedirectUri, paramCodeVerifier)
    val parameters = graphRequest.parameters

    // Make sure correct HttpMethod and parameters are called
    assertThat(parameters["code"]).isEqualTo(paramCode)
    assertThat(parameters["redirect_uri"]).isEqualTo(paramRedirectUri)
    assertThat(parameters["code_verifier"]).isEqualTo(paramCodeVerifier)
    assertThat(parameters["client_id"]).isEqualTo(paramAppId)
  }
}
