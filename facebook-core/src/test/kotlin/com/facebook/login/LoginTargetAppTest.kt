/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login

import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LoginTargetAppTest : FacebookTestCase() {
  @Test
  fun `test conversion to string`() {
    assertThat(LoginTargetApp.FACEBOOK.toString()).isEqualTo("facebook")
    assertThat(LoginTargetApp.INSTAGRAM.toString()).isEqualTo("instagram")
  }

  @Test
  fun `test conversion from string`() {
    assertThat(LoginTargetApp.fromString("facebook")).isEqualTo(LoginTargetApp.FACEBOOK)
    assertThat(LoginTargetApp.fromString("instagram")).isEqualTo(LoginTargetApp.INSTAGRAM)
  }
}
