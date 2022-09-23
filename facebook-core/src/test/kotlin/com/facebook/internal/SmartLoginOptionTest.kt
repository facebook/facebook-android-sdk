/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SmartLoginOptionTest {

  @Test
  fun `test SmartLoginOption parseOptions method`() {
    assertEquals(SmartLoginOption.parseOptions(0), EnumSet.noneOf(SmartLoginOption::class.java))
    assertEquals(SmartLoginOption.parseOptions(1), EnumSet.of(SmartLoginOption.Enabled))
    assertEquals(SmartLoginOption.parseOptions(2), EnumSet.of(SmartLoginOption.RequireConfirm))
    assertNotEquals(SmartLoginOption.parseOptions(3), EnumSet.of(SmartLoginOption.Enabled))
  }
}
