/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import android.text.InputType
import android.widget.TextView
import com.facebook.appevents.codeless.CodelessTestBase
import com.facebook.appevents.codeless.internal.SensitiveUserDataUtils.isSensitiveUserData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
    assertThat(isSensitiveUserData(textView)).isTrue

    // input type == Text
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)
    assertThat(isSensitiveUserData(textView)).isFalse

    // input type == Person Name
    whenever(textView.inputType).thenReturn(InputType.TYPE_TEXT_VARIATION_PERSON_NAME)
    assertThat(isSensitiveUserData(textView)).isTrue

    // input type == Postal Address
    whenever(textView.inputType).thenReturn(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS)
    assertThat(isSensitiveUserData(textView)).isTrue

    // input type == Phone
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_PHONE)
    assertThat(isSensitiveUserData(textView)).isTrue

    // Credit Card
    whenever(textView.inputType).thenReturn(InputType.TYPE_CLASS_TEXT)
    whenever(textView.text).thenReturn("4030122707427751")
    assertThat(isSensitiveUserData(textView)).isTrue
  }
}
