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

package com.facebook.appevents.codeless.internal

import org.json.JSONObject

class PathComponent internal constructor(component: JSONObject) {
  enum class MatchBitmaskType(val value: Int) {
    ID(1),
    TEXT(1 shl 1),
    TAG(1 shl 2),
    DESCRIPTION(1 shl 3),
    HINT(1 shl 4)
  }

  val className: String = component.getString(PATH_CLASS_NAME_KEY)
  val index: Int = component.optInt(PATH_INDEX_KEY, -1)
  val id: Int = component.optInt(PATH_ID_KEY)
  val text: String = component.optString(PATH_TEXT_KEY)
  val tag: String = component.optString(PATH_TAG_KEY)
  val description: String = component.optString(PATH_DESCRIPTION_KEY)
  val hint: String = component.optString(PATH_HINT_KEY)
  val matchBitmask: Int = component.optInt(PATH_MATCH_BITMASK_KEY)

  companion object {
    private const val PATH_CLASS_NAME_KEY = "class_name"
    private const val PATH_INDEX_KEY = "index"
    private const val PATH_ID_KEY = "id"
    private const val PATH_TEXT_KEY = "text"
    private const val PATH_TAG_KEY = "tag"
    private const val PATH_DESCRIPTION_KEY = "description"
    private const val PATH_HINT_KEY = "hint"
    private const val PATH_MATCH_BITMASK_KEY = "match_bitmask"
  }
}
