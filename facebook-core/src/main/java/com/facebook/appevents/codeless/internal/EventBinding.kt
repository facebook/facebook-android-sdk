/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import java.util.Collections
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class EventBinding(
    val eventName: String,
    val method: MappingMethod,
    val type: ActionType,
    val appVersion: String,
    private val path: List<PathComponent>,
    private val parameters: List<ParameterComponent>,
    val componentId: String,
    val pathType: String,
    val activityName: String
) {
  val viewPath: List<PathComponent>
    get() = Collections.unmodifiableList(path)
  val viewParameters: List<ParameterComponent>
    get() = Collections.unmodifiableList(parameters)

  enum class MappingMethod {
    MANUAL,
    INFERENCE
  }

  enum class ActionType {
    CLICK,
    SELECTED,
    TEXT_CHANGED
  }

  companion object {
    @JvmStatic
    fun parseArray(array: JSONArray?): List<EventBinding> {
      val eventBindings = arrayListOf<EventBinding>()
      if (array != null) {
        try {
          for (i in 0 until array.length()) {
            val eventBinding = getInstanceFromJson(array.getJSONObject(i))
            eventBindings.add(eventBinding)
          }
        } catch (e: JSONException) {
          // Ignore
        } catch (e: IllegalArgumentException) {
          // Ignore
        }
      }
      return eventBindings
    }

    @Throws(JSONException::class, IllegalArgumentException::class)
    @JvmStatic
    fun getInstanceFromJson(mapping: JSONObject): EventBinding {
      val eventName = mapping.getString("event_name")
      val method = MappingMethod.valueOf(mapping.getString("method").uppercase(Locale.ENGLISH))
      val type = ActionType.valueOf(mapping.getString("event_type").uppercase(Locale.ENGLISH))
      val appVersion = mapping.getString("app_version")
      val jsonPathArray = mapping.getJSONArray("path")
      val path = arrayListOf<PathComponent>()
      for (i in 0 until jsonPathArray.length()) {
        val jsonPath = jsonPathArray.getJSONObject(i)
        val component = PathComponent(jsonPath)
        path.add(component)
      }
      val pathType =
          mapping.optString(Constants.EVENT_MAPPING_PATH_TYPE_KEY, Constants.PATH_TYPE_ABSOLUTE)
      val jsonParameterArray = mapping.optJSONArray("parameters")
      val parameters = arrayListOf<ParameterComponent>()
      if (null != jsonParameterArray) {
        for (i in 0 until jsonParameterArray.length()) {
          val jsonParameter = jsonParameterArray.getJSONObject(i)
          val component = ParameterComponent(jsonParameter)
          parameters.add(component)
        }
      }
      val componentId = mapping.optString("component_id")
      val activityName = mapping.optString("activity_name")
      return EventBinding(
          eventName,
          method,
          type,
          appVersion,
          path,
          parameters,
          componentId,
          pathType,
          activityName)
    }
  }
}
