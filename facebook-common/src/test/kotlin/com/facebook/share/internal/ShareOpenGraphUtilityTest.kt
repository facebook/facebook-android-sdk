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
import com.facebook.TestUtils
import com.facebook.share.internal.OpenGraphJSONUtility.toJSONObject
import com.facebook.share.model.ShareOpenGraphAction
import com.facebook.share.model.ShareOpenGraphObject
import org.json.JSONObject
import org.junit.Test

class ShareOpenGraphUtilityTest : FacebookTestCase() {
  @Test
  fun testToJSONObject() {
    val actual = toJSONObject(action, null)
    val expected = actionJSONObject
    TestUtils.assertEquals(actual, expected)
  }

  private val action: ShareOpenGraphAction
    private get() =
        ShareOpenGraphAction.Builder()
            .putString(TYPE_KEY, "myActionType")
            .putObject(
                "myObject",
                ShareOpenGraphObject.Builder()
                    .putString("myString", "value")
                    .putInt("myInt", 42)
                    .putBoolean("myBoolean", true)
                    .putStringArrayList(
                        "myStringArray", arrayListOf("string1", "string2", "string3"))
                    .putObject(
                        "myObject", ShareOpenGraphObject.Builder().putDouble("myPi", 3.14).build())
                    .build())
            .build()

  private val actionJSONObject: JSONObject = JSONObject(ACTION_JSON_STRING)

  companion object {
    private const val TYPE_KEY = "type"
    private val ACTION_JSON_STRING =
        """{
            "type": "myActionType",
            "myObject": {
              "fbsdk:create_object":true,
              "myString": "value",
              "myInt": 42,
              "myBoolean": true,
              "myStringArray": [
                "string1",
                "string2",
                "string3"
              ],
              "myObject": {
                "fbsdk:create_object":true,
                "myPi": 3.14
              }
            }
          }""".trimIndent()
  }
}
