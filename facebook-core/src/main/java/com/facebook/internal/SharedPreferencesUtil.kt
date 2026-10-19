/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SharedPreferencesUtil {
  /**
   * Same file and mode as the deprecated
   * `android.preference.PreferenceManager.getDefaultSharedPreferences`, so existing data is read.
   */
  @JvmStatic
  fun getDefaultSharedPreferences(context: Context): SharedPreferences =
      context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
}
