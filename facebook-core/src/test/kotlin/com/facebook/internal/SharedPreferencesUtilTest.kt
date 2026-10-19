/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.preference.PreferenceManager
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.robolectric.RuntimeEnvironment

class SharedPreferencesUtilTest : FacebookTestCase() {
  private val context: Context = RuntimeEnvironment.application

  @Suppress("DEPRECATION")
  @Test
  fun `test returns the same preferences as PreferenceManager`() {
    assertThat(SharedPreferencesUtil.getDefaultSharedPreferences(context))
        .isSameAs(PreferenceManager.getDefaultSharedPreferences(context))
  }

  @Suppress("DEPRECATION")
  @Test
  fun `test reads values written through PreferenceManager`() {
    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("key", "value").commit()

    assertThat(SharedPreferencesUtil.getDefaultSharedPreferences(context).getString("key", null))
        .isEqualTo("value")
    assertThat(
            context
                .getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
                .getString("key", null))
        .isEqualTo("value")
  }
}
