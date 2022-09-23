/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import com.facebook.FacebookTestCase
import com.facebook.share.model.CameraEffectArguments
import java.lang.IllegalArgumentException
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class CameraEffectJSONUtilityTest : FacebookTestCase() {
  @Test
  fun `test convert camera effect arguments to json`() {
    val cameraEffectArguments =
        CameraEffectArguments.Builder()
            .putArgument("name", "test")
            .putArgument("permissions", arrayOf("email", "profile"))
            .build()
    val json = checkNotNull(CameraEffectJSONUtility.convertToJSON(cameraEffectArguments))
    assertThat(json.getString("name")).isEqualTo("test")
    assertThat(json.getJSONArray("permissions")).isEqualTo(JSONArray(arrayOf("email", "profile")))
  }

  @Test
  fun `test convert json to camera effect arguments`() {
    val json =
        JSONObject(
            mapOf(
                "name" to "test",
                "permissions" to arrayOf("email", "profile"),
                "empty" to JSONObject.NULL))
    val cameraEffectArguments =
        checkNotNull(CameraEffectJSONUtility.convertToCameraEffectArguments(json))
    assertThat(cameraEffectArguments.getString("name")).isEqualTo("test")
    assertThat(cameraEffectArguments.getStringArray("permissions"))
        .containsExactlyInAnyOrder("email", "profile")
  }

  @Test(expected = IllegalArgumentException::class)
  fun `test convert json to camera effect arguments with illegal value`() {
    val json =
        JSONObject(
            mapOf(
                "name" to "test", "permissions" to arrayOf("email", "profile"), "illegal" to 1.2f))
    CameraEffectJSONUtility.convertToCameraEffectArguments(json)
  }
}
