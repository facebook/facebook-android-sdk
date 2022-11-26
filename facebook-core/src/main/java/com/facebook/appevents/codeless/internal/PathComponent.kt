/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
