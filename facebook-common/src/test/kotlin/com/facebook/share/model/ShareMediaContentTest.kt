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
