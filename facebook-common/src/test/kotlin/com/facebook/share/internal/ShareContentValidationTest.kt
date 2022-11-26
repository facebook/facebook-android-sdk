/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.net.Uri
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.Validate
import com.facebook.share.internal.ShareContentValidation.validateForApiShare
import com.facebook.share.internal.ShareContentValidation.validateForMessage
import com.facebook.share.internal.ShareContentValidation.validateForNativeShare
import com.facebook.share.internal.ShareContentValidation.validateForWebShare
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareVideoContent
import org.junit.Test
import org.powermock.core.classloader.annotations.PrepareForTest

/** Tests for [ShareContentValidation] */
@PrepareForTest(Validate::class)
class ShareContentValidationTest : FacebookPowerMockTestCase() {
  // Share by Message
  @Test(expected = FacebookException::class)
  fun testItValidatesNullForMessage() {
    validateForMessage(null)
  }

  // -PhotoContent
  @Test(expected = FacebookException::class)
  fun testItValidatesNullImageForPhotoShareByMessage() {
    val spcBuilder = SharePhotoContent.Builder()
    val sharePhoto = SharePhoto.Builder().setImageUrl(null).setBitmap(null).build()
    val sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build()
    validateForMessage(sharePhotoContent)
  }

  @Test(expected = FacebookException::class)
  fun testItValidatesEmptyListOfPhotoForPhotoShareByMessage() {
    val sharePhoto = SharePhotoContent.Builder().build()
    validateForMessage(sharePhoto)
  }

  @Test(expected = FacebookException::class)
  fun testItValidatesMaxSizeOfPhotoShareByMessage() {
    val sharePhotoContent =
        SharePhotoContent.Builder()
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-1.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-2.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-3.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-4.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-5.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-6.gif"))
            .addPhoto(buildSharePhoto("https://facebook.com/awesome-7.gif"))
            .build()
    validateForMessage(sharePhotoContent)
  }

  // -ShareVideoContent
  @Test(expected = FacebookException::class)
  fun testItValidatesEmptyPreviewPhotoForShareVideoContentByMessage() {
    val sharePhoto = ShareVideoContent.Builder().setPreviewPhoto(null).build()
    validateForMessage(sharePhoto)
  }

  // Share by Native (Is the same as Message)
  @Test(expected = FacebookException::class)
  fun testItValidatesNullContentForNativeShare() {
    validateForNativeShare(null)
  }

  // Share by Web
  @Test(expected = FacebookException::class)
  fun testItValidatesNullContentForWebShare() {
    validateForWebShare(null)
  }

  @Test
  fun testItDoesAcceptSharePhotoContentByWeb() {
    val sharePhoto = buildSharePhoto("https://facebook.com/awesome.gif")
    val sharePhotoContent = SharePhotoContent.Builder().addPhoto(sharePhoto).build()
    validateForWebShare(sharePhotoContent)
  }

  @Test(expected = FacebookException::class)
  fun testItDoesNotAcceptShareVideoContentByWeb() {
    val previewPhoto = buildSharePhoto("https://facebook.com/awesome.gif")
    val shareVideoContent = ShareVideoContent.Builder().setPreviewPhoto(previewPhoto).build()
    validateForWebShare(shareVideoContent)
  }

  // Share by Api
  @Test(expected = FacebookException::class)
  fun testItValidatesNullContentForApiShare() {
    validateForApiShare(null)
  }

  @Test(expected = FacebookException::class)
  fun testItValidatesNullImageForSharePhotoContentByApi() {
    val spcBuilder = SharePhotoContent.Builder()
    val sharePhoto = SharePhoto.Builder().setImageUrl(null).build()
    val sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build()
    validateForApiShare(sharePhotoContent)
  }

  private fun buildSharePhoto(url: String): SharePhoto {
    return SharePhoto.Builder().setImageUrl(Uri.parse(url)).build()
  }
}
