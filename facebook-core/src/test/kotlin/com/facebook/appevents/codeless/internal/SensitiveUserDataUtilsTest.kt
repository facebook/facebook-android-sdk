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
package com.facebook.appevents.codeless.internal

import android.text.InputType
import android.widget.TextView
import com.facebook.appevents.codeless.CodelessTestBase
import com.facebook.appevents.codeless.internal.SensitiveUserDataUtils.isSensitiveUserData
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SensitiveUserDataUtilsTest : CodelessTestBase() {
  private lateinit var textView: TextView

  @Before
  override fun setup() {
    super.setup()
    textView = mock()
  }

  @Test
  fun testIsSensitiveUserData() {
    whenever(textView.text).thenReturn("")

    // input type == Password
    whenever(textView.inputType).thenReturn(InputType.TYPE_TEXT_VARIATION_PASSWORD)
    Assert.assertTrue(isSensitiveUserData(textView))

    // input type == Text
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)
    Assert.assertFalse(isSensitiveUserData(textView))

    // input type == Person Name
    whenever(textView.inputType).thenReturn(InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
    Assert.assertTrue(isSensitiveUserData(textView))

    // input type == Postal Address
    whenever(textView.inputType).thenReturn(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS)
    Assert.assertTrue(isSensitiveUserData(textView))

    // input type == Phone
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_PHONE)
    Assert.assertTrue(isSensitiveUserData(textView))

    // Credit Card
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)
    whenever(textView.text).thenReturn("4030122707427751")
    Assert.assertTrue(isSensitiveUserData(textView))
  }
}
