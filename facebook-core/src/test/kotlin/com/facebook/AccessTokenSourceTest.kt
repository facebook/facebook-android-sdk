/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AccessTokenSourceTest : FacebookTestCase() {
  @Test
  fun `test token sources from Instagram`() {
    assertThat(AccessTokenSource.INSTAGRAM_APPLICATION_WEB.fromInstagram()).isTrue()
    assertThat(AccessTokenSource.INSTAGRAM_CUSTOM_CHROME_TAB.fromInstagram()).isTrue()
    assertThat(AccessTokenSource.INSTAGRAM_WEB_VIEW.fromInstagram()).isTrue()
  }

  @Test
  fun `test token sources from Facebook`() {
    assertThat(AccessTokenSource.FACEBOOK_APPLICATION_WEB.fromInstagram()).isFalse()
    assertThat(AccessTokenSource.FACEBOOK_APPLICATION_NATIVE.fromInstagram()).isFalse()
    assertThat(AccessTokenSource.FACEBOOK_APPLICATION_SERVICE.fromInstagram()).isFalse()
    assertThat(AccessTokenSource.WEB_VIEW.fromInstagram()).isFalse()
    assertThat(AccessTokenSource.CHROME_CUSTOM_TAB.fromInstagram()).isFalse()
  }

  @Test
  fun `test non-extensible token type`() {
    assertThat(AccessTokenSource.NONE.canExtendToken()).isFalse()
  }
}
