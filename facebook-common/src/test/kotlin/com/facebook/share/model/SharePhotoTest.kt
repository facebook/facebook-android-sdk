/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SharePhotoTest : FacebookTestCase() {
  private lateinit var testBitmap: Bitmap
  private lateinit var testImageUri: Uri
  private lateinit var testSharePhoto: SharePhoto
  private val testCaption = "test caption"

  override fun setUp() {
    super.setUp()
    testBitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.ALPHA_8)
    testImageUri = Uri.parse("https://facebook.com/1.png")
    testSharePhoto =
        SharePhoto.Builder()
            .setBitmap(testBitmap)
            .setImageUrl(testImageUri)
            .setCaption(testCaption)
            .setUserGenerated(true)
            .build()
  }

  @Test
  fun `test creating a photo with bitmap`() {
    val sharePhoto =
        SharePhoto.Builder()
            .setBitmap(testBitmap)
            .setCaption(testCaption)
            .setUserGenerated(true)
            .build()

    assertThat(sharePhoto.imageUrl).isNull()
    val bitmap = checkNotNull(sharePhoto.bitmap)
    assertThat(bitmap.width).isEqualTo(1)
    assertThat(bitmap.height).isEqualTo(2)
    assertThat(bitmap.config).isEqualTo(Bitmap.Config.ALPHA_8)
    assertThat(sharePhoto.caption).isEqualTo(testCaption)
    assertThat(sharePhoto.userGenerated).isTrue
  }

  @Test
  fun `test creating a photo with uri`() {
    val sharePhoto =
        SharePhoto.Builder()
            .setImageUrl(testImageUri)
            .setCaption(testCaption)
            .setUserGenerated(false)
            .build()

    assertThat(sharePhoto.imageUrl?.toString()).isEqualTo(testImageUri.toString())
    assertThat(sharePhoto.bitmap).isNull()
    assertThat(sharePhoto.caption).isEqualTo(testCaption)
    assertThat(sharePhoto.userGenerated).isFalse
  }

  @Test
  fun `test creating share photo from an existing share photo`() {
    val sharePhoto = SharePhoto.Builder().readFrom(testSharePhoto).build()

    assertThat(sharePhoto.imageUrl.toString()).isEqualTo(testImageUri.toString())
    val bitmap = checkNotNull(sharePhoto.bitmap)
    assertThat(bitmap.width).isEqualTo(1)
    assertThat(bitmap.height).isEqualTo(2)
    assertThat(bitmap.config).isEqualTo(Bitmap.Config.ALPHA_8)
    assertThat(sharePhoto.caption).isEqualTo(testCaption)
    assertThat(sharePhoto.userGenerated).isTrue
  }

  @Test
  fun `test recovering share photo from parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(testSharePhoto, 0)
    parcel.setDataPosition(0)

    val sharePhoto = parcel.readParcelable<SharePhoto>(SharePhoto::class.java.classLoader)

    checkNotNull(sharePhoto)
    assertThat(sharePhoto.imageUrl.toString()).isEqualTo(testImageUri.toString())
    val bitmap = checkNotNull(sharePhoto.bitmap)
    assertThat(bitmap.width).isEqualTo(1)
    assertThat(bitmap.height).isEqualTo(2)
    assertThat(bitmap.config).isEqualTo(Bitmap.Config.ALPHA_8)
    assertThat(sharePhoto.caption).isEqualTo(testCaption)
    assertThat(sharePhoto.userGenerated).isTrue

    parcel.recycle()
  }
}
