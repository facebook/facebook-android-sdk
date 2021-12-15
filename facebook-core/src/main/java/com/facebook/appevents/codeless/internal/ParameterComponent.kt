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
