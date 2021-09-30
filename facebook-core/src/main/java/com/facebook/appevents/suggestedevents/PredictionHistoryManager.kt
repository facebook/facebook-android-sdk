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
