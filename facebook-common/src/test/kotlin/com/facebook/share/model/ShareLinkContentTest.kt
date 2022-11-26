/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.net.Uri
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShareLinkContentTest : FacebookTestCase() {
  @Test
  fun `test create link content`() {
    val url = "https://facebook.com"
    val quote = "test quote"

    val linkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse(url)).setQuote(quote).build()

    assertThat(linkContent.quote).isEqualTo(quote)
    assertThat(linkContent.contentUrl).isEqualTo(Uri.parse(url))
  }

  @Test
  fun `test read link content from parcel`() {
    val url = "https://facebook.com"
    val quote = "test quote"

    val linkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse(url)).setQuote(quote).build()
    val parcel = Parcel.obtain()
    linkContent.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    val restoredLinkContent = ShareLinkContent.CREATOR.createFromParcel(parcel)

    assertThat(restoredLinkContent.quote).isEqualTo(quote)
    assertThat(restoredLinkContent.contentUrl).isEqualTo(Uri.parse(url))
  }
}
