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
