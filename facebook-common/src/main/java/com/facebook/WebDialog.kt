/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

object WebDialog {
  /**
   * Gets the theme used by [com.facebook.internal.WebDialog]
   *
   * @return the theme
   */
  @JvmStatic
  fun getWebDialogTheme(): Int {
    return com.facebook.internal.WebDialog.getWebDialogTheme()
  }
  /**
   * Sets the theme used by [com.facebook.internal.WebDialog]
   *
   * @param theme A theme to use
   */
  @JvmStatic
  fun setWebDialogTheme(theme: Int) {
    com.facebook.internal.WebDialog.setWebDialogTheme(theme)
  }
}
