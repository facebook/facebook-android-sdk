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
