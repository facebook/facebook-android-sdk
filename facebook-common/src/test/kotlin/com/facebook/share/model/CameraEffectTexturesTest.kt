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
    assertThat(recoveredContent?.getTextureBitmap("bitmap")).isNotNull
    assertThat(recoveredContent?.getTextureUri("uri")).isEqualTo(uri)
    assertThat(recoveredContent?.keySet()).isEqualTo(setOf("bitmap", "uri"))
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
