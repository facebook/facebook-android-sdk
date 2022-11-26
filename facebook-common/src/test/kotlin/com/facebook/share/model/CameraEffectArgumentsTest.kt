/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
