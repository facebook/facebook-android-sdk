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
import com.facebook.FacebookTestCase
import com.facebook.share.internal.OpenGraphJSONUtility.toJSONObject
import com.facebook.share.internal.OpenGraphJSONUtility.toJSONValue
import com.facebook.share.model.ShareOpenGraphAction
import com.facebook.share.model.SharePhoto
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class OpenGraphJSONUtilityTest : FacebookTestCase() {
  private lateinit var photoProcessor: OpenGraphJSONUtility.PhotoJSONProcessor
  private lateinit var mockPhotoObject: JSONObject
  override fun setUp() {
    super.setUp()
    mockPhotoObject = JSONObject("{\"data\":\"test\"}")
    photoProcessor = OpenGraphJSONUtility.PhotoJSONProcessor { photo -> mockPhotoObject }
  }

  @Test
  fun `test toJSONValue`() {
    assertThat(toJSONValue(null, photoProcessor)).isEqualTo(JSONObject.NULL)
    assertThat(toJSONValue("test", photoProcessor)).isEqualTo("test")
    assertThat(toJSONValue(SharePhoto.Builder().setImageUrl(Uri.EMPTY).build(), photoProcessor))
        .isEqualTo(mockPhotoObject)
    assertThat(toJSONValue(SharePhoto.Builder().setImageUrl(Uri.EMPTY).build(), null)).isNull()
    assertThat(toJSONValue(listOf("123", "456"), photoProcessor))
        .isEqualTo(JSONArray(listOf("123", "456")))
    assertThat(toJSONValue(Any(), photoProcessor)).isNull()
  }

  @Test
  fun `test toJSONObject with action`() {
    val action =
        ShareOpenGraphAction.Builder()
            .putString("str", "test")
            .putPhoto("photo", SharePhoto.Builder().setImageUrl(Uri.EMPTY).build())
            .putInt("int", 123)
            .build()
    val jsonObject = checkNotNull(toJSONObject(action, photoProcessor))
    assertThat(jsonObject.getString("str")).isEqualTo("test")
    assertThat(jsonObject.getInt("int")).isEqualTo(123)
    assertThat(jsonObject.getJSONObject("photo")).isEqualTo(mockPhotoObject)
  }
}
