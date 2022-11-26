/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class LegacyNativeDialogParametersTest : FacebookPowerMockTestCase() {
  private lateinit var testCallId: UUID

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
  fun `test creating legacy parameters for sharing a link`() {
    val url = "https://facebook.com"
    val shareLinkContent = ShareLinkContent.Builder().setContentUrl(Uri.parse(url)).build()

    val params = LegacyNativeDialogParameters.create(testCallId, shareLinkContent, true)

    checkNotNull(params)
    assertThat(params.getString(ShareConstants.LEGACY_LINK)).isEqualTo(url)
  }

  @Test
  fun `test creating legacy parameters for sharing photos`() {
    val sharePhotoUrls = listOf("https://facebook.com/1.png", "https://facebook.com/2.png")
    val sharePhotos = sharePhotoUrls.map { SharePhoto.Builder().setImageUrl(Uri.parse(it)).build() }
    val sharePhotoContent = SharePhotoContent.Builder().setPhotos(sharePhotos).build()

    val params = LegacyNativeDialogParameters.create(testCallId, sharePhotoContent, true)

    checkNotNull(params)
    assertThat(params.getStringArrayList(ShareConstants.LEGACY_PHOTOS))
        .containsExactlyInAnyOrder(*sharePhotoUrls.toTypedArray())
  }
}
