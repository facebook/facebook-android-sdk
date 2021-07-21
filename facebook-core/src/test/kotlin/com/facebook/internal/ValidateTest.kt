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
package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.Validate.hasAppID
import com.facebook.internal.Validate.notEmpty
import com.facebook.internal.Validate.notNull
import com.facebook.internal.Validate.notNullOrEmpty
import com.facebook.internal.Validate.oneOf
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ValidateTest : FacebookPowerMockTestCase() {
  private val appID = "123"
  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
  }

  @Test
  fun testNotNullOnNonNull() {
    notNull("A string", "name")
  }

  @Test(expected = NullPointerException::class)
  fun testNotNullOnNull() {
    notNull(null, "name")
  }

  @Test
  fun testNotEmptyOnNonEmpty() {
    notEmpty(listOf("hi"), "name")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testNotEmptylOnEmpty() {
    notEmpty(listOf<String>(), "name")
  }

  @Test
  fun testNotNullOrEmptyOnNonEmpty() {
    notNullOrEmpty("hi", "name")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testNotNullOrEmptyOnEmpty() {

    notNullOrEmpty("", "name")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testNotNullOrEmptyOnNull() {
    notNullOrEmpty(null, "name")
  }

  @Test
  fun testOneOfOnValid() {
    oneOf("hi", "name", "hi", "there")
  }

  @Test(expected = IllegalArgumentException::class)
  fun testOneOfOnInvalid() {

    oneOf("hit", "name", "hi", "there")
  }

  @Test
  fun testOneOfOnValidNull() {
    oneOf(null, "name", "hi", "there", null)
  }

  @Test(expected = IllegalArgumentException::class)
  fun testOneOfOnInvalidNull() {

    oneOf(null, "name", "hi", "there")
  }

  @Test
  fun testHasAppID() {
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(appID)
    assertEquals(appID, hasAppID())
  }

  @Test(expected = IllegalStateException::class)
  fun testHasNoAppID() {
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(null)
    hasAppID()
  }
}
