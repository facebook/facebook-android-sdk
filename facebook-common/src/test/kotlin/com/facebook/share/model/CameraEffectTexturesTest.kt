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
import org.junit.Before
import org.junit.Test

class CameraEffectTexturesTest : FacebookTestCase() {
  private val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
  private val uri = Uri.parse("https://image.png")

  private lateinit var cameraEffectTextures: CameraEffectTextures

  @Before
  fun init() {
    cameraEffectTextures =
        CameraEffectTextures.Builder().putTexture("bitmap", bitmap).putTexture("uri", uri).build()
  }

  @Test
  fun `test builder`() {
    assertThat(cameraEffectTextures).isNotNull
  }

  @Test
  fun `test getTextureBitmap`() {
    assertThat(cameraEffectTextures.getTextureBitmap("bitmap")).isEqualTo(bitmap)
    assertThat(cameraEffectTextures.getTextureBitmap("uri")).isNull()
    assertThat(cameraEffectTextures.getTextureBitmap("key_does_not_exist")).isNull()
  }

  @Test
  fun `test getTextureUri`() {
    assertThat(cameraEffectTextures.getTextureUri("uri")).isEqualTo(uri)
    assertThat(cameraEffectTextures.getTextureUri("bitmap")).isNull()
    assertThat(cameraEffectTextures.getTextureUri("key_does_not_exist")).isNull()
  }

  @Test
  fun `test get`() {
    assertThat(cameraEffectTextures.get("uri")).isEqualTo(uri)
    assertThat(cameraEffectTextures.get("bitmap")?.javaClass).isEqualTo(Bitmap::class.java)
  }

  @Test
  fun `test keySet`() {
    assertThat(cameraEffectTextures.keySet()).isEqualTo(setOf("bitmap", "uri"))
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(cameraEffectTextures, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<CameraEffectTextures>(CameraEffectTextures::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.getTextureBitmap("bitmap")).isNotNull
    assertThat(recoveredContent.getTextureUri("uri")).isEqualTo(uri)
    assertThat(recoveredContent.keySet()).isEqualTo(setOf("bitmap", "uri"))
    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = CameraEffectTextures.Builder().readFrom(cameraEffectTextures).build()
    assertThat(recoveredContent.getTextureBitmap("bitmap")).isNotNull
    assertThat(recoveredContent.getTextureUri("uri")).isEqualTo(uri)
    assertThat(recoveredContent.keySet()).isEqualTo(setOf("bitmap", "uri"))
  }
}
