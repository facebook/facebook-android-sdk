/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.os.Parcel
import android.os.Parcelable
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShareMessengerActionButtonTest : FacebookTestCase() {
  class TestShareMessengerActionButton : ShareMessengerActionButton {
    private constructor(builder: Builder) : super(builder)

    internal constructor(parcel: Parcel) : super(parcel)

    class Builder : ShareMessengerActionButton.Builder<TestShareMessengerActionButton, Builder>() {
      override fun build(): TestShareMessengerActionButton {
        return TestShareMessengerActionButton(this)
      }
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<TestShareMessengerActionButton> =
          object : Parcelable.Creator<TestShareMessengerActionButton> {
            override fun createFromParcel(parcel: Parcel): TestShareMessengerActionButton {
              return TestShareMessengerActionButton(parcel)
            }

            override fun newArray(size: Int): Array<TestShareMessengerActionButton?> {
              return arrayOfNulls(size)
            }
          }
    }
  }

  private val title = "title"
  private lateinit var shareMessengerActionButton: TestShareMessengerActionButton

  @Before
  fun init() {
    shareMessengerActionButton = TestShareMessengerActionButton.Builder().setTitle(title).build()
  }

  @Test
  fun `test builder`() {
    assertThat(shareMessengerActionButton).isNotNull
  }

  @Test
  fun `test title`() {
    assertThat(shareMessengerActionButton.title).isEqualTo(title)
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareMessengerActionButton, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<TestShareMessengerActionButton>(
            TestShareMessengerActionButton::class.java.classLoader)

    checkNotNull(recoveredContent)
    assertThat(recoveredContent.title).isEqualTo(title)

    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent =
        TestShareMessengerActionButton.Builder().readFrom(shareMessengerActionButton).build()
    assertThat(recoveredContent.title).isEqualTo(title)
  }
}
