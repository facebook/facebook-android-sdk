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

package com.facebook.bolts

import android.content.Intent
import android.os.Bundle
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AppLinksTest : FacebookTestCase() {
  companion object {
    private const val TEST_KEY = "test key"
    private const val TEST_DATA = "test value"
  }

  @Test
  fun `test get app link data`() {
    val intent = Intent()
    val bundle = Bundle()
    val extraBundle = Bundle()
    extraBundle.putString(TEST_KEY, TEST_DATA)
    bundle.putBundle(AppLinks.KEY_NAME_EXTRAS, extraBundle)
    intent.putExtra(AppLinks.KEY_NAME_APPLINK_DATA, bundle)

    assertThat(AppLinks.getAppLinkData(intent)).isEqualTo(bundle)
    assertThat(AppLinks.getAppLinkExtras(intent)?.getString(TEST_KEY)).isEqualTo(TEST_DATA)
  }

  @Test
  fun `test empty intent won't crash`() {
    val intent = Intent()
    assertThat(AppLinks.getAppLinkExtras(intent)).isNull()
  }
}
