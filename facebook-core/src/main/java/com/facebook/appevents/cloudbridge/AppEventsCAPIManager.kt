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

import android.content.Context
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.LoggingBehavior
import com.facebook.internal.Logger
import com.facebook.internal.Utility.convertJSONArrayToList
import com.facebook.internal.Utility.convertJSONObjectToHashMap
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.net.MalformedURLException
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

  internal var savedCloudBridgeCredentials: Map<String, Any>?
    @JvmStatic
    @AutoHandleExceptions
    get() {
      val context = FacebookSdk.getApplicationContext()
      val sharedPref =
          context.getSharedPreferences(
              FacebookSdk.CLOUDBRIDGE_SAVED_CREDENTIALS, Context.MODE_PRIVATE)
              ?: return null

      val datasetID = sharedPref.getString(SettingsAPIFields.DATASETID.rawValue, null)
      val url = sharedPref.getString(SettingsAPIFields.URL.rawValue, null)
      val accessKey = sharedPref.getString(SettingsAPIFields.ACCESSKEY.rawValue, null)

      // individual cloud bridge settings should never be null
      if (datasetID.isNullOrBlank() || url.isNullOrBlank() || accessKey.isNullOrBlank()) {
        return null
      }

      val savedSettings: MutableMap<String, String> = mutableMapOf()
      savedSettings[SettingsAPIFields.URL.rawValue] = url
      savedSettings[SettingsAPIFields.DATASETID.rawValue] = datasetID
      savedSettings[SettingsAPIFields.ACCESSKEY.rawValue] = accessKey

      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG.toString(),
          " \n\nLoading Cloudbridge settings from saved Prefs: \n================\n DATASETID: %s\n URL: %s \n ACCESSKEY: %s \n\n ",
          datasetID,
          url,
          accessKey)

      return savedSettings
    }
    set(valuesToSave) {
      val context = FacebookSdk.getApplicationContext()
      val sharedPref =
          context.getSharedPreferences(
              FacebookSdk.CLOUDBRIDGE_SAVED_CREDENTIALS, Context.MODE_PRIVATE)
              ?: return

      if (valuesToSave == null) {
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()
        return
      }
      val datasetID = valuesToSave[SettingsAPIFields.DATASETID.rawValue]
      val url = valuesToSave[SettingsAPIFields.URL.rawValue]
      val accessKey = valuesToSave[SettingsAPIFields.ACCESSKEY.rawValue]

      // individual cloud bridge settings should never be null
      if (datasetID == null || url == null || accessKey == null) {
        return
      }

      val editor = sharedPref.edit()
      editor.putString(SettingsAPIFields.DATASETID.rawValue, datasetID.toString())
      editor.putString(SettingsAPIFields.URL.rawValue, url.toString())
      editor.putString(SettingsAPIFields.ACCESSKEY.rawValue, accessKey.toString())
      editor.apply()

      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG.toString(),
          " \n\nSaving Cloudbridge settings from saved Prefs: \n================\n DATASETID: %s\n URL: %s \n ACCESSKEY: %s \n\n ",
          datasetID,
          url,
          accessKey)
    }

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

      val cbCredentials = savedCloudBridgeCredentials

      // load from cached behaviour
      if (cbCredentials != null) {

        val extractURL = URL(cbCredentials[SettingsAPIFields.URL.rawValue].toString())

        AppEventsConversionsAPITransformerWebRequests.configure(
            cbCredentials[SettingsAPIFields.DATASETID.rawValue].toString(),
            extractURL.protocol + "://" + extractURL.host,
            cbCredentials[SettingsAPIFields.ACCESSKEY.rawValue].toString())

        this.isEnabled = true
      }

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
      config = convertJSONObjectToHashMap(JSONObject(data.firstOrNull()))

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

    try {
      AppEventsConversionsAPITransformerWebRequests.configure(datasetID, url, accessKey)

      savedCloudBridgeCredentials = config
    } catch (e: MalformedURLException) {
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          TAG,
          "CloudBridge Settings API response doesn't have valid url\n %s ",
          e.stackTraceToString())

      return
    }

    this.isEnabled =
        if (config[SettingsAPIFields.ENABLED.rawValue] != null)
            config[SettingsAPIFields.ENABLED.rawValue] as Boolean
        else false
  }
}
