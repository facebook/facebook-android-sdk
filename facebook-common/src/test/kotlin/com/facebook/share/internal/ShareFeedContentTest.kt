/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
