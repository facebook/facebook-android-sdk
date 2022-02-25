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

import android.os.Parcel
import com.facebook.FacebookTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class CameraEffectArgumentsTest : FacebookTestCase() {
  private lateinit var cameraEffectArguments: CameraEffectArguments
  private val itemArray = arrayOf("item1", "item2")
  private val expectedKeySet = setOf("string_key", "string_array_key")

  @Before
  fun init() {
    cameraEffectArguments =
        CameraEffectArguments.Builder()
            .putArgument("string_key", "string_value")
            .putArgument("string_array_key", itemArray)
            .build()
  }

  @Test
  fun `test builder`() {
    assertThat(cameraEffectArguments).isNotNull
  }

  @Test
  fun `test getString`() {
    assertThat(cameraEffectArguments.getString("string_key")).isEqualTo("string_value")
    assertThat(cameraEffectArguments.getString("does_not_exist")).isNull()
  }

  @Test
  fun `test getStringArray`() {
    assertThat(cameraEffectArguments.getStringArray("string_array_key")).isEqualTo(itemArray)
    assertThat(cameraEffectArguments.getStringArray("does_not_exist")).isNull()
  }

  @Test
  fun `test get`() {
    assertThat(cameraEffectArguments.get("string_key")).isEqualTo("string_value")
    assertThat(cameraEffectArguments.get("string_array_key")).isEqualTo(itemArray)
  }

  @Test
  fun `test keySet`() {
    assertThat(cameraEffectArguments.keySet()).isEqualTo(expectedKeySet)
  }

  @Test
  fun `test serialize with parcel`() {
    val parcel = Parcel.obtain()
    parcel.writeParcelable(cameraEffectArguments, 0)
    parcel.setDataPosition(0)

    val recoveredContent =
        parcel.readParcelable<CameraEffectArguments>(CameraEffectArguments::class.java.classLoader)
    checkNotNull(recoveredContent)
    assertThat(recoveredContent.getString("string_key")).isEqualTo("string_value")
    assertThat(recoveredContent.getStringArray("string_array_key")).isEqualTo(itemArray)
    assertThat(recoveredContent.keySet()).isEqualTo(expectedKeySet)

    parcel.recycle()
  }

  @Test
  fun `test builder read from existing content`() {
    val recoveredContent = CameraEffectArguments.Builder().readFrom(cameraEffectArguments).build()
    assertThat(recoveredContent.getString("string_key")).isEqualTo("string_value")
    assertThat(recoveredContent.getStringArray("string_array_key")).isEqualTo(itemArray)
    assertThat(recoveredContent.keySet()).isEqualTo(expectedKeySet)
  }
}
