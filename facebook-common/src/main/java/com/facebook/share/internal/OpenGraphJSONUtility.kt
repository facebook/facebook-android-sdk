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

import com.facebook.share.model.ShareOpenGraphAction
import com.facebook.share.model.ShareOpenGraphObject
import com.facebook.share.model.SharePhoto
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * Utility methods for JSON representation of Open Graph models.
 */
object OpenGraphJSONUtility {
  /**
   * Converts an action to a JSONObject, return null if the action is null.
   *
   * NOTE: All images are removed from the JSON representation and must be added to the builder
   * separately.
   *
   * @param action [com.facebook.share.model.ShareOpenGraphAction] to be converted.
   * @return [org.json.JSONObject] representing the action.
   * @throws JSONException
   */
  @Throws(JSONException::class)
  @JvmStatic
  fun toJSONObject(
      action: ShareOpenGraphAction?,
      photoJSONProcessor: PhotoJSONProcessor?
  ): JSONObject? {
    if (action == null) {
      return null
    }
    val result = JSONObject()
    val keys = action.keySet()
    for (key in keys) {
      result.put(key, toJSONValue(action[key], photoJSONProcessor))
    }
    return result
  }

  @Throws(JSONException::class)
  private fun toJSONObject(
      graphObject: ShareOpenGraphObject,
      photoJSONProcessor: PhotoJSONProcessor?
  ): JSONObject {
    val result = JSONObject()
    val keys = graphObject.keySet()
    for (key in keys) {
      result.put(key, toJSONValue(graphObject[key], photoJSONProcessor))
    }
    return result
  }

  @Throws(JSONException::class)
  private fun toJSONArray(list: List<*>, photoJSONProcessor: PhotoJSONProcessor?): JSONArray {
    val result = JSONArray()
    for (item in list) {
      result.put(toJSONValue(item, photoJSONProcessor))
    }
    return result
  }

  /**
   * Converts an value to a JSON value, return [org.json.JSONObject.NULL] if the value is null.
   * Return null if error happens.
   *
   * NOTE: All images are removed from the JSON representation and must be added to the builder
   * separately.
   *
   * @param value the value to be converted.
   * @param photoJSONProcessor [PhotoJSONProcessor] object for processing photo objects.
   * @return [org.json.JSONObject] representing the action.
   * @throws JSONException
   */
  @Throws(JSONException::class)
  @JvmStatic
  fun toJSONValue(value: Any?, photoJSONProcessor: PhotoJSONProcessor?): Any? {
    if (value == null) {
      return JSONObject.NULL
    }
    if (value is String ||
        value is Boolean ||
        value is Double ||
        value is Float ||
        value is Int ||
        value is Long) {
      return value
    }
    try {
      if (value is SharePhoto) {
        return photoJSONProcessor?.toJSONObject(value)
      }
      if (value is ShareOpenGraphObject) {
        return toJSONObject(value, photoJSONProcessor)
      }
      if (value is List<*>) {
        return toJSONArray(value, photoJSONProcessor)
      }
    } catch (e: Exception) {
      // crash from PhotoJSONProcessor. Swallow it
    }
    return null
  }

  fun interface PhotoJSONProcessor {
    fun toJSONObject(photo: SharePhoto): JSONObject?
  }
}
