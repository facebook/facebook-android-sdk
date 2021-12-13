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

package com.facebook

import android.graphics.Bitmap
import com.facebook.gamingservices.GamingContext
import com.facebook.gamingservices.model.CustomUpdateContent
import com.facebook.gamingservices.model.CustomUpdateLocalizedText
import com.facebook.gamingservices.model.CustomUpdateMedia
import com.facebook.gamingservices.model.CustomUpdateMediaInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomUpdateContentTest : FacebookPowerMockTestCase() {
  @Test
  fun `constructor with media`() {
    val content =
        CustomUpdateContent.Builder(
                GamingContext("1233"),
                CustomUpdateLocalizedText("text"),
                CustomUpdateMedia(CustomUpdateMediaInfo("https://www.facebook.com/cool.gif")))
            .build()
    assertThat(content.contextTokenId).isEqualTo("1233")
    assertThat(content.text.default).isEqualTo("text")
  }

  fun `constructor with media gif and video`() {
    try {
      val builder =
          CustomUpdateContent.Builder(
                  GamingContext("1233"),
                  CustomUpdateLocalizedText("text"),
                  CustomUpdateMedia(
                      CustomUpdateMediaInfo("https://www.facebook.com/cool.gif"),
                      CustomUpdateMediaInfo("https://www.facebook.com/cool.mp4")))
              .build()
    } catch (e: IllegalArgumentException) {
      assertThat(e).isNotNull()
      assertThat(e.message).startsWith("Invalid CustomUpdateMedia")
    }
  }

  @Test
  fun `constructor with media and empty gif and empty video`() {
    try {
      val builder =
          CustomUpdateContent.Builder(
                  GamingContext("1233"),
                  CustomUpdateLocalizedText("text"),
                  CustomUpdateMedia(null, null))
              .build()
    } catch (e: IllegalArgumentException) {
      assertThat(e).isNotNull()
      assertThat(e.message).startsWith("Invalid CustomUpdateMedia")
    }
  }

  @Test
  fun `constructor with image`() {
    val content =
        CustomUpdateContent.Builder(
                GamingContext("1233"),
                CustomUpdateLocalizedText("text"),
                Bitmap.createBitmap(200, 200, Bitmap.Config.ALPHA_8))
            .build()
    assertThat(content.contextTokenId).isEqualTo("1233")
    assertThat(content.image).isNotNull()
  }

  @Test
  fun `bitmap is converted to Base64`() {
    val content =
        CustomUpdateContent.Builder(
                GamingContext("1233"),
                CustomUpdateLocalizedText("text"),
                Bitmap.createBitmap(200, 200, Bitmap.Config.ALPHA_8))
            .build()
    assertThat(content.image).isNotNull()
    assertThat(content.image).startsWith("data:image/png;base64,")
  }

  @Test
  fun `toGraphRequestContent returns valid JSON`() {
    val localizations = hashMapOf<String, String>()
    localizations.put("es_LA", "abc")
    val content =
        CustomUpdateContent.Builder(
                GamingContext("1233"),
                CustomUpdateLocalizedText("text", localizations),
                CustomUpdateMedia(CustomUpdateMediaInfo("https://www.facebook.com/cool.gif")))
            .build()
    val json = content.toGraphRequestContent()
    assertThat(json.getString("context_token_id")).isEqualTo("1233")
    assertThat(json.getString("text"))
        .isEqualTo("""{"default":"text","localizations":{"es_LA":"abc"}}""")
    assertThat(json.getString("media"))
        .isEqualTo("""{"gif":{"url":"https:\/\/www.facebook.com\/cool.gif"}}""")
  }
}
