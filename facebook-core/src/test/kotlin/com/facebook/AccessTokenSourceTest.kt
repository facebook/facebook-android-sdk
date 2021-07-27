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
