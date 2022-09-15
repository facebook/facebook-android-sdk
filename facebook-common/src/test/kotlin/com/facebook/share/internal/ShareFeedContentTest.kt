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

package com.facebook.share.internal

import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShareFeedContentTest : FacebookTestCase() {
  private lateinit var content: ShareFeedContent

  override fun setUp() {
    super.setUp()
    content =
        ShareFeedContent.Builder().setLink(LINK).setToId(TO_ID).setLinkCaption(LINK_CAPTION).build()
  }

  @Test
  fun `test builder`() {
    assertThat(content.link).isEqualTo(LINK)
    assertThat(content.toId).isEqualTo(TO_ID)
    assertThat(content.linkCaption).isEqualTo(LINK_CAPTION)
    assertThat(content.picture).isNull()
  }

  @Test
  fun `test build from parcel`() {
    val parcel = Parcel.obtain()
    content.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)

    val recoveredContent = ShareFeedContent.CREATOR.createFromParcel(parcel)
    assertThat(recoveredContent.link).isEqualTo(LINK)
    assertThat(recoveredContent.linkCaption).isEqualTo(LINK_CAPTION)
    assertThat(recoveredContent.toId).isEqualTo(TO_ID)
    assertThat(recoveredContent.picture).isNull()
    assertThat(recoveredContent.linkName).isNull()
    assertThat(recoveredContent.linkDescription).isNull()
  }

  companion object {
    const val LINK: String = "https://www.facebook.com"
    const val LINK_CAPTION: String = "test caption"
    const val TO_ID: String = "123456789"
  }
}
