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
