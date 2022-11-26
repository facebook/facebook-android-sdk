/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import com.facebook.FacebookSdk
import com.facebook.appevents.codeless.internal.ViewHierarchy
import com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY
import com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY
import com.facebook.internal.Utility.jsonStrToMap
import com.facebook.internal.Utility.mapToJsonStr
import com.facebook.internal.Utility.sha256hash
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal object PredictionHistoryManager {
  private val clickedViewPaths: MutableMap<String, String> = mutableMapOf()
  private const val SUGGESTED_EVENTS_HISTORY = "SUGGESTED_EVENTS_HISTORY"
  private const val CLICKED_PATH_STORE = "com.facebook.internal.SUGGESTED_EVENTS_HISTORY"
  private lateinit var shardPreferences: SharedPreferences
  private val initialized = AtomicBoolean(false)
  private fun initAndWait() {
    if (initialized.get()) {
      return
    }
    shardPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(CLICKED_PATH_STORE, Context.MODE_PRIVATE)
    clickedViewPaths.putAll(
        jsonStrToMap(shardPreferences.getString(SUGGESTED_EVENTS_HISTORY, "") ?: ""))
    initialized.set(true)
  }

  @JvmStatic
  fun addPrediction(pathID: String, predictedEvent: String) {
    if (!initialized.get()) {
      initAndWait()
    }
    clickedViewPaths[pathID] = predictedEvent
    shardPreferences
        .edit()
        .putString(SUGGESTED_EVENTS_HISTORY, mapToJsonStr(clickedViewPaths.toMap()))
        .apply()
  }

  @JvmStatic
  fun getPathID(view: View, text: String): String? {
    var view: View? = view
    val pathRoute = JSONObject()
    try {
      pathRoute.put(TEXT_KEY, text)
      val currentPath = JSONArray()
      while (view != null) {
        currentPath.put(view.javaClass.simpleName)
        view = ViewHierarchy.getParentOfView(view) as View?
      }
      pathRoute.put(CLASS_NAME_KEY, currentPath)
    } catch (je: JSONException) {
      /*no op*/
    }
    return sha256hash(pathRoute.toString())
  }

  @JvmStatic
  fun queryEvent(pathID: String): String? {
    return if (clickedViewPaths.containsKey(pathID)) clickedViewPaths[pathID] else null
  }
}
