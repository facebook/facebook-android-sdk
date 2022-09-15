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
