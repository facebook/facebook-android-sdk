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
