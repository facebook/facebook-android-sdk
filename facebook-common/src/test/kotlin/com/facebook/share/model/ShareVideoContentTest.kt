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

class ShareVideoContentTest : FacebookTestCase() {
  private lateinit var content: ShareVideoContent

  override fun setUp() {
    super.setUp()
    val shareVideo = ShareVideo.Builder().setLocalUrl(Uri.parse("/tmp/test.avi")).build()
    val previewPhoto = SharePhoto.Builder().setImageUrl(Uri.parse("/tmp/test.jpg")).build()
    content =
        ShareVideoContent.Builder()
            .setVideo(shareVideo)
            .setContentTitle("test title")
            .setContentDescription("test description")
            .setPreviewPhoto(previewPhoto)
            .build()
  }

  @Test
  fun `test build share video content with ShareVideo object`() {
    assertThat(content.video?.localUrl.toString()).isEqualTo("/tmp/test.avi")
    assertThat(content.contentDescription).isEqualTo("test description")
    assertThat(content.contentTitle).isEqualTo("test title")
    assertThat(content.previewPhoto?.imageUrl.toString()).isEqualTo("/tmp/test.jpg")
  }

  @Test
  fun `test parcelize`() {
    val parcel = Parcel.obtain()
    content.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)

    val recoveredContent = ShareVideoContent.CREATOR.createFromParcel(parcel)
    assertThat(recoveredContent.video?.localUrl.toString()).isEqualTo("/tmp/test.avi")
    assertThat(recoveredContent.contentDescription).isEqualTo("test description")
    assertThat(recoveredContent.contentTitle).isEqualTo("test title")
    assertThat(recoveredContent.previewPhoto?.imageUrl.toString()).isEqualTo("/tmp/test.jpg")
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = ShareVideoContent.Builder().readFrom(content).build()
    assertThat(recoveredContent.video?.localUrl.toString()).isEqualTo("/tmp/test.avi")
    assertThat(recoveredContent.contentDescription).isEqualTo("test description")
    assertThat(recoveredContent.contentTitle).isEqualTo("test title")
    assertThat(recoveredContent.previewPhoto?.imageUrl.toString()).isEqualTo("/tmp/test.jpg")
  }
}
