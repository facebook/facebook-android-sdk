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

class SharePhotoContentTest : FacebookTestCase() {
  private lateinit var photos: List<SharePhoto>
  override fun setUp() {
    super.setUp()
    photos =
        listOf(
            SharePhoto.Builder()
                .setImageUrl(Uri.parse("file://tmp/photo1.jpg"))
                .setCaption("photo 1")
                .build(),
            SharePhoto.Builder()
                .setImageUrl(Uri.parse("file://tmp/photo2.jpg"))
                .setCaption("photo 2")
                .build(),
        )
  }

  @Test
  fun `test add photo`() {
    val photoContent = SharePhotoContent.Builder().addPhoto(photos[0]).addPhoto(null).build()
    assertThat(photoContent.photos.size).isEqualTo(1)
    assertThat(photoContent.photos[0].imageUrl).isEqualTo(photos[0].imageUrl)
    assertThat(photoContent.photos[0].caption).isEqualTo(photos[0].caption)
  }

  @Test
  fun `test add photos`() {
    val photoContent = SharePhotoContent.Builder().addPhotos(photos).build()
    assertThat(photoContent.photos.size).isEqualTo(2)
    assertThat(photoContent.photos[0].imageUrl).isEqualTo(photos[0].imageUrl)
    assertThat(photoContent.photos[0].caption).isEqualTo(photos[0].caption)
    assertThat(photoContent.photos[1].imageUrl).isEqualTo(photos[1].imageUrl)
    assertThat(photoContent.photos[1].caption).isEqualTo(photos[1].caption)
  }

  @Test
  fun `test set photos`() {
    val photoContent =
        SharePhotoContent.Builder()
            .addPhotos(photos)
            .setPhotos(listOf(photos[1], photos[0]))
            .build()
    assertThat(photoContent.photos.size).isEqualTo(2)
    assertThat(photoContent.photos[1].imageUrl).isEqualTo(photos[0].imageUrl)
    assertThat(photoContent.photos[1].caption).isEqualTo(photos[0].caption)
    assertThat(photoContent.photos[0].imageUrl).isEqualTo(photos[1].imageUrl)
    assertThat(photoContent.photos[0].caption).isEqualTo(photos[1].caption)
  }

  @Test
  fun `test read from existing photo content`() {
    val photoContent = SharePhotoContent.Builder().addPhotos(photos).build()
    val recoveredPhotoContent = SharePhotoContent.Builder().readFrom(photoContent).build()
    assertThat(recoveredPhotoContent.photos).isNotEqualTo(photoContent.photos)
    assertThat(recoveredPhotoContent.photos[0].imageUrl).isEqualTo(photos[0].imageUrl)
    assertThat(recoveredPhotoContent.photos[0].caption).isEqualTo(photos[0].caption)
    assertThat(recoveredPhotoContent.photos[1].imageUrl).isEqualTo(photos[1].imageUrl)
    assertThat(recoveredPhotoContent.photos[1].caption).isEqualTo(photos[1].caption)
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    val photoContent = SharePhotoContent.Builder().addPhotos(photos).build()
    photoContent.writeToParcel(parcel, 0)
    parcel.setDataPosition(0)
    val recoveredPhotoContent = SharePhotoContent.CREATOR.createFromParcel(parcel)
    assertThat(recoveredPhotoContent.photos[0].imageUrl).isEqualTo(photos[0].imageUrl)
    assertThat(recoveredPhotoContent.photos[0].caption).isEqualTo(photos[0].caption)
    assertThat(recoveredPhotoContent.photos[1].imageUrl).isEqualTo(photos[1].imageUrl)
    assertThat(recoveredPhotoContent.photos[1].caption).isEqualTo(photos[1].caption)
  }
}
