/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import org.json.JSONObject

class ParameterComponent(component: JSONObject) {
  val name: String = component.getString(PARAMETER_NAME_KEY)
  val value: String = component.optString(PARAMETER_VALUE_KEY)
  val path: List<PathComponent>
  val pathType: String =
      component.optString(Constants.EVENT_MAPPING_PATH_TYPE_KEY, Constants.PATH_TYPE_ABSOLUTE)

  companion object {
    private const val PARAMETER_NAME_KEY = "name"
    private const val PARAMETER_PATH_KEY = "path"
    private const val PARAMETER_VALUE_KEY = "value"
  }

  init {
    val pathComponents = arrayListOf<PathComponent>()
    val jsonPathArray = component.optJSONArray(PARAMETER_PATH_KEY)
    if (jsonPathArray != null) {
      for (i in 0 until jsonPathArray.length()) {
        val pathComponent = PathComponent(jsonPathArray.getJSONObject(i))
        pathComponents.add(pathComponent)
      }
    }
    path = pathComponents
  }
}
