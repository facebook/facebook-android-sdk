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
import com.facebook.share.model.ShareMessengerURLActionButton.WebviewHeightRatio
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ShareMessengerURLActionButtonTest : FacebookTestCase() {
  private val url = Uri.parse("https://facebook.com")
  private val fallbackUrl = Uri.parse("https://facebook.com/fallback")
  private val webviewHeightRatio = WebviewHeightRatio.WebviewHeightRatioTall
  private lateinit var shareMessengerURLActionButton: ShareMessengerURLActionButton

  @Before
  fun init() {
    shareMessengerURLActionButton =
        ShareMessengerURLActionButton.Builder()
            .setUrl(url)
            .setFallbackUrl(fallbackUrl)
            .setIsMessengerExtensionURL(true)
            .setShouldHideWebviewShareButton(true)
            .setWebviewHeightRatio(webviewHeightRatio)
            .build()
  }

  @Test
  fun `test builder`() {
    assertThat(shareMessengerURLActionButton).isNotNull
  }

  @Test
  fun `test url`() {
    assertThat(shareMessengerURLActionButton.url).isEqualTo(url)
  }

  @Test
  fun `test fallbackUrl`() {
    assertThat(shareMessengerURLActionButton.fallbackUrl).isEqualTo(fallbackUrl)
  }

  @Test
  fun `test isMessengerExtensionURL`() {
    assertThat(shareMessengerURLActionButton.isMessengerExtensionURL).isTrue
  }

  @Test
  fun `test shouldHideWebviewShareButton`() {
    assertThat(shareMessengerURLActionButton.shouldHideWebviewShareButton).isTrue
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareMessengerURLActionButton, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<ShareMessengerURLActionButton>(
            ShareMessengerURLActionButton::class.java.classLoader)

    checkNotNull(recoveredContent)
    assertThat(recoveredContent.url).isEqualTo(url)
    assertThat(recoveredContent.fallbackUrl).isEqualTo(fallbackUrl)
    assertThat(recoveredContent.isMessengerExtensionURL).isTrue
    assertThat(recoveredContent.shouldHideWebviewShareButton).isTrue
    assertThat(recoveredContent.webviewHeightRatio)
        .isEqualTo(WebviewHeightRatio.WebviewHeightRatioTall)

    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent =
        ShareMessengerURLActionButton.Builder().readFrom(shareMessengerURLActionButton).build()
    assertThat(recoveredContent.url).isEqualTo(url)
    assertThat(recoveredContent.fallbackUrl).isEqualTo(fallbackUrl)
    assertThat(recoveredContent.isMessengerExtensionURL).isTrue
    assertThat(recoveredContent.shouldHideWebviewShareButton).isTrue
    assertThat(recoveredContent.webviewHeightRatio)
        .isEqualTo(WebviewHeightRatio.WebviewHeightRatioTall)
  }
}
