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
    val json = CameraEffectJSONUtility.convertToJSON(cameraEffectArguments)
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
    val cameraEffectArguments = CameraEffectJSONUtility.convertToCameraEffectArguments(json)
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
