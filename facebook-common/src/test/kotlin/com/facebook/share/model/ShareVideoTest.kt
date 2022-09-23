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
import org.junit.Before
import org.junit.Test

class ShareVideoTest : FacebookTestCase() {
  private lateinit var shareVideo: ShareVideo
  @Before
  fun init() {
    shareVideo = ShareVideo.Builder().setLocalUrl(Uri.parse("https://facebook.com")).build()
  }

  @Test
  fun `test build video`() {
    assertThat(shareVideo).isNotNull
  }

  @Test
  fun `test get video`() {
    assertThat(shareVideo.localUrl.toString()).isEqualTo("https://facebook.com")
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareVideo, 0)
    parcel.setDataPosition(0)

    val recoveredContent = parcel.readParcelable<ShareVideo>(ShareVideo::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.localUrl).isEqualTo(shareVideo.localUrl)
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = ShareVideo.Builder().readFrom(shareVideo).build()
    assertThat(recoveredContent.localUrl).isEqualTo(shareVideo.localUrl)
  }
}
