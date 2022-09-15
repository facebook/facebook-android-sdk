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
