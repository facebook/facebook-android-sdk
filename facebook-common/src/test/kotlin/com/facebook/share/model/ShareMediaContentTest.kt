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
import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock

class ShareMediaContentTest : FacebookTestCase() {
  private lateinit var testPhoto: SharePhoto
  private lateinit var testVideo: ShareVideo
  private lateinit var mockUnknownMedium: ShareMedia<*, *>

  override fun setUp() {
    super.setUp()
    testPhoto = SharePhoto.Builder().setImageUrl(Uri.parse("https://test.net/image1.png")).build()
    testVideo = ShareVideo.Builder().setLocalUrl(Uri.parse("https://test.net/video1.mp4")).build()
    mockUnknownMedium = mock()
  }

  @Test
  fun `test building media content with photo`() {
    val content = ShareMediaContent.Builder().addMedium(testPhoto).build()

    val contentUrl = (content.media.first() as SharePhoto?)?.imageUrl
    assertThat(contentUrl).isEqualTo(testPhoto.imageUrl)
  }

  @Test
  fun `test building media content with video`() {
    val content = ShareMediaContent.Builder().addMedium(testVideo).build()

    val contentUrl = (content.media.first() as ShareVideo?)?.localUrl
    assertThat(contentUrl).isEqualTo(testVideo.localUrl)
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test adding unknown medium`() {
    ShareMediaContent.Builder().addMedium(mockUnknownMedium).build()
  }

  @Test
  fun `test building media content with a list of media`() {
    val content = ShareMediaContent.Builder().addMedia(listOf(testPhoto, testVideo)).build()
    assertThat(content.media.size).isEqualTo(2)
  }

  @Test
  fun `test restoring media content from a parcel`() {
    val parcel = Parcel.obtain()
    val content = ShareMediaContent.Builder().addMedia(listOf(testPhoto, testVideo)).build()
    parcel.writeParcelable(content, 0)
    parcel.setDataPosition(0)

    val restoredContent =
        parcel.readParcelable<ShareMediaContent>(ShareMediaContent::class.java.classLoader)

    assertThat(restoredContent?.media?.size).isEqualTo(2)
    parcel.recycle()
  }

  @Test
  fun `test read from an existing share media content`() {
    val content = ShareMediaContent.Builder().addMedia(listOf(testPhoto)).build()
    val newContent = ShareMediaContent.Builder().readFrom(content).build()

    assertThat((newContent.media.first() as SharePhoto?)?.imageUrl).isEqualTo(testPhoto.imageUrl)
  }

  @Test
  fun `test builder setting media will clear existing media`() {
    val content =
        ShareMediaContent.Builder().addMedium(testPhoto).setMedia(listOf(testVideo)).build()

    assertThat(content.media.first() is ShareVideo).isTrue
    assertThat(content.media.size).isEqualTo(1)
  }
}
