/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import java.util.EnumSet

enum class SmartLoginOption(val value: Long) {
  None(0),
  Enabled(1),
  RequireConfirm(2);

  companion object {
    private val ALL: EnumSet<SmartLoginOption> = EnumSet.allOf(SmartLoginOption::class.java)

    @JvmStatic
    fun parseOptions(bitmask: Long): EnumSet<SmartLoginOption> {
      val result = EnumSet.noneOf(SmartLoginOption::class.java)
      for (opt in ALL) {
        if (bitmask and opt.value != 0L) {
          result.add(opt)
        }
      }
      return result
    }
  }
}
