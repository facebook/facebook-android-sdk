/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
