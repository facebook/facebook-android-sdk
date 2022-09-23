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
