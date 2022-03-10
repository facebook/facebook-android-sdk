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

import android.graphics.Bitmap
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShareStoryContentTest : FacebookTestCase() {
  private lateinit var background: SharePhoto
  private lateinit var sticker: SharePhoto
  private lateinit var attributionLink: String
  private lateinit var backgroundColorList: List<String>
  private lateinit var storyContent: ShareStoryContent

  override fun setUp() {
    super.setUp()
    sticker =
        SharePhoto.Builder().setBitmap(Bitmap.createBitmap(2, 3, Bitmap.Config.ALPHA_8)).build()
    background =
        SharePhoto.Builder().setBitmap(Bitmap.createBitmap(5, 7, Bitmap.Config.ALPHA_8)).build()
    attributionLink = "https://facebook.com"
    backgroundColorList = listOf("red", "blue")

    storyContent =
        ShareStoryContent.Builder()
            .setBackgroundAsset(background)
            .setStickerAsset(sticker)
            .setAttributionLink(attributionLink)
            .setBackgroundColorList(backgroundColorList)
            .build()
  }

  @Test
  fun `test builder creates the correct content`() {
    validateContentIsExpected(storyContent)
  }

  @Test
  fun `test parcelizing story content`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(storyContent, 0)
    parcel.setDataPosition(0)
    val recoveredStoryContent =
        parcel.readParcelable<ShareStoryContent>(ShareStoryContent::class.java.classLoader)
    checkNotNull(recoveredStoryContent)
    validateContentIsExpected(recoveredStoryContent)
    parcel.recycle()
  }

  @Test
  fun `test building from an existing story content`() {
    val recoveredStoryContent = ShareStoryContent.Builder().readFrom(storyContent).build()
    validateContentIsExpected(recoveredStoryContent)
  }

  private fun validateContentIsExpected(content: ShareStoryContent) {
    assertThat(content.attributionLink).isEqualTo(attributionLink)
    assertThat(content.backgroundColorList)
        .containsExactlyInAnyOrder(*backgroundColorList.toTypedArray())
    val stickBitmap = checkNotNull(sticker.bitmap)
    assertThat(content.stickerAsset?.bitmap?.width).isEqualTo(stickBitmap.width)
    assertThat(content.stickerAsset?.bitmap?.height).isEqualTo(stickBitmap.height)
    val contentBackgroundAssert = content.backgroundAsset as SharePhoto?
    assertThat(contentBackgroundAssert?.bitmap?.width).isEqualTo(background.bitmap?.width)
    assertThat(contentBackgroundAssert?.bitmap?.height).isEqualTo(background.bitmap?.height)
  }
}
