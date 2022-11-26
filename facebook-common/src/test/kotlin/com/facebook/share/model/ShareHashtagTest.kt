/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShareHashtagTest : FacebookTestCase() {
  private lateinit var shareHashtag: ShareHashtag
  @Before
  fun init() {
    shareHashtag = ShareHashtag.Builder().setHashtag("test").build()
  }

  @Test
  fun `test build hashtag`() {
    assertThat(shareHashtag).isNotNull
  }

  @Test
  fun `test get hashtag`() {
    assertThat(shareHashtag.hashtag).isEqualTo("test")
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareHashtag, 0)
    parcel.setDataPosition(0)

    val recoveredContent = parcel.readParcelable<ShareHashtag>(ShareHashtag::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.hashtag).isEqualTo(shareHashtag.hashtag)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = ShareHashtag.Builder().readFrom(shareHashtag).build()
    assertThat(recoveredContent.hashtag).isEqualTo(shareHashtag.hashtag)
  }
}
