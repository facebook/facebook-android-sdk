/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.model

import android.graphics.Bitmap
import android.net.Uri
import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShareCameraEffectContentTest : FacebookTestCase() {
  private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
  private val uri = Uri.parse("https://image.png")

  private val effectId = "effectId"
  private lateinit var cameraEffectArguments: CameraEffectArguments
  private lateinit var cameraEffectTextures: CameraEffectTextures
  private lateinit var shareCameraEffectContent: ShareCameraEffectContent

  override fun setUp() {
    super.setUp()

    cameraEffectArguments =
        CameraEffectArguments.Builder()
            .putArgument("string_key", "string_value")
            .putArgument("string_array_key", arrayOf("item1", "item2"))
            .build()

    cameraEffectTextures =
        CameraEffectTextures.Builder().putTexture("bitmap", bitmap).putTexture("uri", uri).build()

    shareCameraEffectContent =
        ShareCameraEffectContent.Builder()
            .setEffectId(effectId)
            .setArguments(cameraEffectArguments)
            .setTextures(cameraEffectTextures)
            .build()
  }

  @Test
  fun `test builder`() {
    assertThat(shareCameraEffectContent).isNotNull
  }

  @Test
  fun `test getEffectId`() {
    assertThat(shareCameraEffectContent.effectId).isEqualTo(effectId)
  }

  @Test
  fun `test getArguments`() {
    assertThat(shareCameraEffectContent.arguments).isEqualTo(cameraEffectArguments)
  }

  @Test
  fun `test getTextures`() {
    assertThat(shareCameraEffectContent.textures).isEqualTo(cameraEffectTextures)
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(shareCameraEffectContent, 0)
    parcel.setDataPosition(0)

    val recoveredContent: ShareCameraEffectContent? =
        parcel.readParcelable(ShareCameraEffectContent::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.effectId).isEqualTo(effectId)
    assertThat(recoveredContent.arguments?.keySet()).isEqualTo(cameraEffectArguments.keySet())
    assertThat(recoveredContent.textures?.keySet()).isEqualTo(cameraEffectTextures.keySet())
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent =
        ShareCameraEffectContent.Builder().readFrom(shareCameraEffectContent).build()
    assertThat(recoveredContent.effectId).isEqualTo(effectId)
    assertThat(recoveredContent.arguments).isEqualTo(cameraEffectArguments)
    assertThat(recoveredContent.textures).isEqualTo(cameraEffectTextures)
  }
}
