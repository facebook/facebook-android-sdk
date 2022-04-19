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

package com.facebook.appevents.cloudbridge

import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger
import com.facebook.internal.Utility.convertJSONArrayToList
import com.facebook.internal.Utility.convertJSONObjectToHashMap
import java.net.URL
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class SettingsAPIFields(val rawValue: String) {
  URL("endpoint"),
  ENABLED("is_enabled"),
  DATASETID("dataset_id"),
  ACCESSKEY("access_key")
}

object AppEventsCAPIManager {

  private const val SETTINGS_PATH = "/cloudbridge_settings"
  private val TAG = AppEventsCAPIManager::class.java.canonicalName
  internal var isEnabled: Boolean = false

  /** Run this to pull the CAPIG initialization settings from FB Backend. */
  @JvmStatic
  fun enable() {
    try {

      val callback =
          GraphRequest.Callback { response -> getCAPIGSettingsFromGraphResponse(response) }

      val graphReq =
          GraphRequest(
              null, FacebookSdk.getApplicationId() + SETTINGS_PATH, null, HttpMethod.GET, callback)

      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG as String,
          " \n\nCreating Graph Request: \n=============\n%s\n\n ",
          graphReq)

      graphReq.executeAsync()
    } catch (e: JSONException) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG as String,
          " \n\nGraph Request Exception: \n=============\n%s\n\n ",
          e.stackTraceToString())
    }
  }

  internal fun getCAPIGSettingsFromGraphResponse(response: GraphResponse) {
    if (response.error != null) {

      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG as String,
          " \n\nGraph Response Error: \n================\nResponse Error: %s\nResponse Error Exception: %s\n\n ",
          response.error.toString(),
          response.error.exception.toString())

      return
    }

    Logger.log(
        LoggingBehavior.APP_EVENTS,
        TAG as String,
        " \n\nGraph Response Received: \n================\n%s\n\n ",
        response)

    val result = response.getJSONObject()
    val url: String?
    val datasetID: String?
    val accessKey: String?
    val config: Map<String, Any>?
    try {
      val data = convertJSONArrayToList(result?.get("data") as JSONArray)
      config = convertJSONObjectToHashMap(JSONObject(data.first()))

      url = config[SettingsAPIFields.URL.rawValue] as String?
      datasetID = config[SettingsAPIFields.DATASETID.rawValue] as String?
      accessKey = config[SettingsAPIFields.ACCESSKEY.rawValue] as String?
    } catch (e: JSONException) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "CloudBridge Settings API response is not a valid json: \n%s ",
          e.stackTraceToString())
      return
    } catch (e: NullPointerException) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "CloudBridge Settings API response is not a valid json: \n%s ",
          e.stackTraceToString())
      return
    }

    if (url == null || datasetID == null || accessKey == null) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "CloudBridge Settings API response doesn't have valid data")

      return
    }

    val extractURL = URL(url)
    AppEventsConversionsAPITransformerWebRequests.configure(
        datasetID, extractURL.protocol + "://" + extractURL.host, accessKey)

    this.isEnabled =
        if (config[SettingsAPIFields.ENABLED.rawValue] != null)
            config[SettingsAPIFields.ENABLED.rawValue] as Boolean
        else false
  }
}
