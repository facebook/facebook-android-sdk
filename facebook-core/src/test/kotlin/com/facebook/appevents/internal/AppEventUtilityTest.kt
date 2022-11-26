/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import java.util.Locale
import org.junit.Assert
import org.junit.Test

class AppEventUtilityTest {
  @Test
  fun testNormalizePrice() {
    Locale.setDefault(Locale.US)
    var price = "$1,234.567"
    Assert.assertEquals(1_234.567, AppEventUtility.normalizePrice(price), 0.001)
    price = "price: $1,234.567. Good deal!"
    Assert.assertEquals(1_234.567, AppEventUtility.normalizePrice(price), 0.001)
    price = "price: $1,234.567. Good deal! Add 2 more? "
    Assert.assertEquals(1_234.567, AppEventUtility.normalizePrice(price), 0.001)
    price = "price: $123 for 2 items"
    Assert.assertEquals(123.0, AppEventUtility.normalizePrice(price), 0.001)
    price = "Click to check price!"
    Assert.assertEquals(0.0, AppEventUtility.normalizePrice(price), 0.001)
    price = "$-1.23"
    Assert.assertEquals(-1.23, AppEventUtility.normalizePrice(price), 0.001)
    Locale.setDefault(Locale.FRANCE)
    price = "$1,234"
    Assert.assertEquals(1.234, AppEventUtility.normalizePrice(price), 0.001)
    Locale.setDefault(Locale.ITALY)
    price = "$1.234,567"
    Assert.assertEquals(1_234.567, AppEventUtility.normalizePrice(price), 0.001)
  }
}
