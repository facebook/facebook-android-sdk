/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
