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
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideo
import com.facebook.share.model.ShareVideoContent
import com.nhaarman.mockitokotlin2.whenever
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class NativeDialogParametersTest : FacebookPowerMockTestCase() {
  lateinit var testCallId: UUID

  override fun setup() {
    super.setup()
    testCallId = UUID.randomUUID()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `test creating parameters for sharing link content`() {
    val contentUrl = "https://facebook.com"
    val quote = "test quote"
    val linkContent =
        ShareLinkContent.Builder().setContentUrl(Uri.parse(contentUrl)).setQuote(quote).build()

    val params = NativeDialogParameters.create(testCallId, linkContent, true)

    checkNotNull(params)
    assertThat(params.getString(ShareConstants.CONTENT_URL)).isEqualTo(contentUrl)
    assertThat(params.getString(ShareConstants.MESSENGER_URL)).isEqualTo(contentUrl)
    assertThat(params.getString(ShareConstants.TARGET_DISPLAY)).isEqualTo(contentUrl)
    assertThat(params.getString(ShareConstants.QUOTE)).isEqualTo(quote)
  }

  @Test
  fun `test creating parameters for sharing photo content`() {
    val photoUrls = listOf("https://test.net/image1.png", "https://test.net/image2.png")
    val sharePhotos = photoUrls.map { SharePhoto.Builder().setImageUrl(Uri.parse(it)).build() }
    val photoContent = SharePhotoContent.Builder().setPhotos(sharePhotos).build()

    val params = NativeDialogParameters.create(testCallId, photoContent, true)

    checkNotNull(params)
    assertThat(params.getStringArrayList(ShareConstants.PHOTOS))
        .containsExactlyInAnyOrder(*photoUrls.toTypedArray())
  }

  @Test
  fun `test creating parameters for sharing video content`() {
    val videoUrl = "https://test.net/video1.avi"
    val videoTitle = "Test title"
    val shareVideo = ShareVideo.Builder().setLocalUrl(Uri.parse(videoUrl)).build()
    val videoContent =
        ShareVideoContent.Builder().setVideo(shareVideo).setContentTitle(videoTitle).build()

    val params = NativeDialogParameters.create(testCallId, videoContent, true)

    checkNotNull(params)
    assertThat(params.getString(ShareConstants.VIDEO_URL)).isEqualTo(videoUrl)
    assertThat(params.getString(ShareConstants.TITLE)).isEqualTo(videoTitle)
    assertThat(params.getString(ShareConstants.DESCRIPTION)).isNull()
  }

  @Test
  fun `test creating parameters for sharing media content`() {
    val photoUrl = "https://test.net/image1.png"
    val videoUrl = "https://test.net/video1.avi"
    val photo = SharePhoto.Builder().setImageUrl(Uri.parse(photoUrl)).build()
    val video = ShareVideo.Builder().setLocalUrl(Uri.parse(videoUrl)).build()
    val shareMediaContent = ShareMediaContent.Builder().addMedia(listOf(photo, video)).build()

    val params = NativeDialogParameters.create(testCallId, shareMediaContent, true)

    checkNotNull(params)
    val mediaInfos = checkNotNull(params.getParcelableArrayList<Bundle>(ShareConstants.MEDIA))
    val mediaInfoUrls = mediaInfos.map { it.getString("uri") }
    assertThat(mediaInfoUrls).containsExactlyInAnyOrder(photoUrl, videoUrl)
  }

  @Test
  fun `test creating parameters for sharing story content`() {
    val backgroundPhotoUrl = "https://test.net/image1.png"
    val stickerUrl = "https://test.net/image2.png"
    val attributionLink = "https://test.net"
    val backgroundColorList = listOf("color1", "color2")
    val shareStoryContent =
        ShareStoryContent.Builder()
            .setBackgroundAsset(
                SharePhoto.Builder().setImageUrl(Uri.parse(backgroundPhotoUrl)).build())
            .setBackgroundColorList(backgroundColorList)
            .setStickerAsset(SharePhoto.Builder().setImageUrl(Uri.parse(stickerUrl)).build())
            .setAttributionLink(attributionLink)
            .build()

    val params = NativeDialogParameters.create(testCallId, shareStoryContent, true)

    checkNotNull(params)
    assertThat(params.getParcelable<Bundle>(ShareConstants.STORY_BG_ASSET)?.getString("uri"))
        .isEqualTo(backgroundPhotoUrl)
    assertThat(
            params
                .getParcelable<Bundle>(ShareConstants.STORY_INTERACTIVE_ASSET_URI)
                ?.getString("uri"))
        .isEqualTo(stickerUrl)
    assertThat(params.getStringArrayList(ShareConstants.STORY_INTERACTIVE_COLOR_LIST))
        .containsExactlyInAnyOrder(*backgroundColorList.toTypedArray())
    assertThat(params.getString(ShareConstants.STORY_DEEP_LINK_URL)).isEqualTo(attributionLink)
  }
}
