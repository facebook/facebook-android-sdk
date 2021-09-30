package com.facebook.login

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class NonceUtilTest : FacebookPowerMockTestCase() {

  @Test
  fun `test valid nonce`() {
    assertThat(NonceUtil.isValidNonce(AuthenticationTokenTestUtil.NONCE)).isTrue
  }

  @Test
  fun `test invalid nonce`() {
    assertThat(NonceUtil.isValidNonce("nonce ")).isFalse
  }

  @Test
  fun `test empty nonce`() {
    assertThat(NonceUtil.isValidNonce("")).isFalse
  }
}
