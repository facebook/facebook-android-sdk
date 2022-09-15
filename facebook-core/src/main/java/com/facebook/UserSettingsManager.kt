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

package com.facebook

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import com.facebook.GraphRequest.Companion.newGraphPathRequest
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.isAutoAppLinkSetup
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONException
import org.json.JSONObject

@AutoHandleExceptions
internal object UserSettingsManager {
  private val TAG = UserSettingsManager::class.java.name
  private val isInitialized = AtomicBoolean(false)
  private val isFetchingCodelessStatus = AtomicBoolean(false)
  private const val EVENTS_CODELESS_SETUP_ENABLED = "auto_event_setup_enabled"
  private const val TIMEOUT_7D =
      (7 * 24 * 60 * 60 * 1000 // Millisecond
          )
          .toLong()
  private const val ADVERTISER_ID_KEY = "advertiser_id"
  private const val APPLICATION_FIELDS = GraphRequest.FIELDS_PARAM
  private val autoInitEnabled = UserSetting(true, FacebookSdk.AUTO_INIT_ENABLED_PROPERTY)
  private val autoLogAppEventsEnabled =
      UserSetting(true, FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY)
  private val advertiserIDCollectionEnabled =
      UserSetting(true, FacebookSdk.ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY)
  private val codelessSetupEnabled = UserSetting(false, EVENTS_CODELESS_SETUP_ENABLED)

  // Monitor enabled user setting from AndroidManifest
  private val monitorEnabled = UserSetting(true, FacebookSdk.MONITOR_ENABLED_PROPERTY)

  // Cache
  private const val USER_SETTINGS = "com.facebook.sdk.USER_SETTINGS"
  private const val USER_SETTINGS_BITMASK = "com.facebook.sdk.USER_SETTINGS_BITMASK"
  private lateinit var userSettingPref: SharedPreferences

  // Parameter names of settings in cache
  private const val LAST_TIMESTAMP = "last_timestamp"
  private const val VALUE = "value"

  // Warning message for App Event Flags
  private const val AUTOLOG_APPEVENT_NOT_SET_WARNING =
      ("Please set a value for AutoLogAppEventsEnabled. Set the flag to TRUE if you want " +
          "to collect app install, app launch and in-app purchase events automatically. To " +
          "request user consent before collecting data, set the flag value to FALSE, then " +
          "change to TRUE once user consent is received. " +
          "Learn more: https://developers.facebook.com/docs/app-events/getting-started-app-events-android#disable-auto-events.")
  private const val ADVERTISERID_COLLECTION_NOT_SET_WARNING =
      ("You haven't set a value for AdvertiserIDCollectionEnabled. Set the flag to TRUE " +
          "if you want to collect Advertiser ID for better advertising and analytics " +
          "results. To request user consent before collecting data, set the flag value to " +
          "FALSE, then change to TRUE once user consent is received. " +
          "Learn more: https://developers.facebook.com/docs/app-events/getting-started-app-events-android#disable-auto-events.")
  private const val ADVERTISERID_COLLECTION_FALSE_WARNING =
      ("The value for AdvertiserIDCollectionEnabled is currently set to FALSE so you're " +
          "sending app events without collecting Advertiser ID. This can affect the quality " +
          "of your advertising and analytics results.")

  // Warning message for Auto App Link Setting
  private const val AUTO_APP_LINK_WARNING =
      "You haven't set the Auto App Link URL scheme: fb<YOUR APP ID> in AndroidManifest"

  private fun initializeIfNotInitialized() {
    if (!FacebookSdk.isInitialized()) {
      return
    }
    if (!isInitialized.compareAndSet(false, true)) {
      return
    }
    userSettingPref =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(USER_SETTINGS, Context.MODE_PRIVATE)
    initializeUserSetting(autoLogAppEventsEnabled, advertiserIDCollectionEnabled, autoInitEnabled)
    initializeCodelessSetupEnabledAsync()
    logWarnings()
    logIfSDKSettingsChanged()
  }

  private fun initializeUserSetting(vararg userSettings: UserSetting) {
    for (userSetting in userSettings) {
      if (userSetting === codelessSetupEnabled) {
        initializeCodelessSetupEnabledAsync()
      } else {
        if (userSetting.value == null) {
          readSettingFromCache(userSetting)
          if (userSetting.value == null) {
            loadSettingFromManifest(userSetting)
          }
        } else {
          // if flag has been set before initialization, load setting to cache
          writeSettingToCache(userSetting)
        }
      }
    }
  }

  private fun initializeCodelessSetupEnabledAsync() {
    readSettingFromCache(codelessSetupEnabled)
    val currTime = System.currentTimeMillis()
    if (codelessSetupEnabled.value != null && currTime - codelessSetupEnabled.lastTS < TIMEOUT_7D) {
      return
    } else {
      codelessSetupEnabled.value = null
      codelessSetupEnabled.lastTS = 0
    }
    if (!isFetchingCodelessStatus.compareAndSet(false, true)) {
      return
    }
    // fetch data through Graph request if cache is unavailable
    FacebookSdk.getExecutor().execute {
      if (advertiserIDCollectionEnabled.getValue()) {
        val appSettings = queryAppSettings(FacebookSdk.getApplicationId(), false)
        if (appSettings != null && appSettings.codelessEventsEnabled) {
          var advertiserId: String? = null
          val context = FacebookSdk.getApplicationContext()
          val identifiers = getAttributionIdentifiers(context)
          if (identifiers != null && identifiers.androidAdvertiserId != null) {
            advertiserId = identifiers.androidAdvertiserId
          }
          if (advertiserId != null) {
            val codelessSettingsParams = Bundle()
            codelessSettingsParams.putString(ADVERTISER_ID_KEY, advertiserId)
            codelessSettingsParams.putString(APPLICATION_FIELDS, EVENTS_CODELESS_SETUP_ENABLED)

            val codelessRequest = newGraphPathRequest(null, "app", null)
            codelessRequest.parameters = codelessSettingsParams
            val response = codelessRequest.executeAndWait().getJSONObject()

            if (response != null) {
              codelessSetupEnabled.value = response.optBoolean(EVENTS_CODELESS_SETUP_ENABLED, false)
              codelessSetupEnabled.lastTS = currTime
              writeSettingToCache(codelessSetupEnabled)
            }
          }
        }
      }
      isFetchingCodelessStatus.set(false)
    }
  }

  private fun writeSettingToCache(userSetting: UserSetting) {
    validateInitialized()
    try {
      val jsonObject = JSONObject()
      jsonObject.put(VALUE, userSetting.value)
      jsonObject.put(LAST_TIMESTAMP, userSetting.lastTS)
      userSettingPref.edit().putString(userSetting.key, jsonObject.toString()).apply()
      logIfSDKSettingsChanged()
    } catch (e: Exception) {
      logd(TAG, e)
    }
  }

  private fun readSettingFromCache(userSetting: UserSetting) {
    validateInitialized()
    try {
      val settingStr = userSettingPref.getString(userSetting.key, "") ?: ""
      if (settingStr.isNotEmpty()) {
        val setting = JSONObject(settingStr)
        userSetting.value = setting.getBoolean(VALUE)
        userSetting.lastTS = setting.getLong(LAST_TIMESTAMP)
      }
    } catch (je: JSONException) {
      logd(TAG, je)
    }
  }

  private fun loadSettingFromManifest(userSetting: UserSetting) {
    validateInitialized()
    try {
      val ctx = FacebookSdk.getApplicationContext()
      val ai = ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA)
      if (ai?.metaData != null && ai.metaData.containsKey(userSetting.key)) {
        // default value should not be used
        userSetting.value = ai.metaData.getBoolean(userSetting.key, userSetting.defaultVal)
      }
    } catch (e: PackageManager.NameNotFoundException) {
      logd(TAG, e)
    }
  }

  private fun logWarnings() {
    try {
      val ctx = FacebookSdk.getApplicationContext()
      val ai = ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA)
      if (ai?.metaData != null) {
        // Log warnings for App Event Flags
        if (!ai.metaData.containsKey(FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY)) {
          Log.w(TAG, AUTOLOG_APPEVENT_NOT_SET_WARNING)
        }
        if (!ai.metaData.containsKey(FacebookSdk.ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY)) {
          Log.w(TAG, ADVERTISERID_COLLECTION_NOT_SET_WARNING)
        }
        if (!getAdvertiserIDCollectionEnabled()) {
          Log.w(TAG, ADVERTISERID_COLLECTION_FALSE_WARNING)
        }
      }
    } catch (e: PackageManager.NameNotFoundException) {
      /* no op */
    }
  }

  private fun logIfSDKSettingsChanged() {
    if (!isInitialized.get()) {
      return
    }
    if (!FacebookSdk.isInitialized()) {
      return
    }
    val ctx = FacebookSdk.getApplicationContext()
    var bitmask = 0
    var bit = 0
    bitmask = bitmask or ((if (autoInitEnabled.getValue()) 1 else 0) shl bit++)
    bitmask = bitmask or ((if (autoLogAppEventsEnabled.getValue()) 1 else 0) shl bit++)
    bitmask = bitmask or ((if (advertiserIDCollectionEnabled.getValue()) 1 else 0) shl bit++)
    bitmask = bitmask or ((if (monitorEnabled.getValue()) 1 else 0) shl bit++)
    val previousBitmask = userSettingPref.getInt(USER_SETTINGS_BITMASK, 0)
    if (previousBitmask != bitmask) {
      userSettingPref.edit().putInt(USER_SETTINGS_BITMASK, bitmask).apply()
      var initialBitmask = 0
      var usageBitmask = 0
      try {
        val ai =
            ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA)
        if (ai?.metaData != null) {
          val keys =
              arrayOf(
                  FacebookSdk.AUTO_INIT_ENABLED_PROPERTY,
                  FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY,
                  FacebookSdk.ADVERTISER_ID_COLLECTION_ENABLED_PROPERTY,
                  FacebookSdk.MONITOR_ENABLED_PROPERTY)
          val defaultValues = booleanArrayOf(true, true, true, true)
          for (i in keys.indices) {
            usageBitmask = usageBitmask or ((if (ai.metaData.containsKey(keys[i])) 1 else 0) shl i)
            val initialValue = ai.metaData.getBoolean(keys[i], defaultValues[i])
            initialBitmask = initialBitmask or ((if (initialValue) 1 else 0) shl i)
          }
        }
      } catch (e: PackageManager.NameNotFoundException) {
        /* no op */
      }
      val logger = InternalAppEventsLogger(ctx)
      val parameters = Bundle()
      parameters.putInt("usage", usageBitmask)
      parameters.putInt("initial", initialBitmask)
      parameters.putInt("previous", previousBitmask)
      parameters.putInt("current", bitmask)
      logger.logChangedSettingsEvent(parameters)
    }
  }

  @JvmStatic
  fun logIfAutoAppLinkEnabled() {
    try {
      val ctx = FacebookSdk.getApplicationContext()
      val ai = ctx.packageManager.getApplicationInfo(ctx.packageName, PackageManager.GET_META_DATA)
      if (ai?.metaData != null &&
          ai.metaData.getBoolean("com.facebook.sdk.AutoAppLinkEnabled", false)) {
        val logger = InternalAppEventsLogger(ctx)
        val params = Bundle()
        if (!isAutoAppLinkSetup) {
          params.putString("SchemeWarning", AUTO_APP_LINK_WARNING)
          Log.w(TAG, AUTO_APP_LINK_WARNING)
        }
        logger.logEvent("fb_auto_applink", params)
      }
    } catch (e: PackageManager.NameNotFoundException) {
      /* no op */
    }
  }

  /** Sanity check that if UserSettingsManager initialized successfully */
  private fun validateInitialized() {
    if (!isInitialized.get()) {
      throw FacebookSdkNotInitializedException(
          "The UserSettingManager has not been initialized successfully")
    }
  }

  @JvmStatic
  fun setAutoInitEnabled(flag: Boolean) {
    autoInitEnabled.value = flag
    autoInitEnabled.lastTS = System.currentTimeMillis()
    if (isInitialized.get()) {
      writeSettingToCache(autoInitEnabled)
    } else {
      initializeIfNotInitialized()
    }
  }

  @JvmStatic
  fun getAutoInitEnabled(): Boolean {
    initializeIfNotInitialized()
    return autoInitEnabled.getValue()
  }

  @JvmStatic
  fun setAutoLogAppEventsEnabled(flag: Boolean) {
    autoLogAppEventsEnabled.value = flag
    autoLogAppEventsEnabled.lastTS = System.currentTimeMillis()
    if (isInitialized.get()) {
      writeSettingToCache(autoLogAppEventsEnabled)
    } else {
      initializeIfNotInitialized()
    }
  }

  @JvmStatic
  fun getAutoLogAppEventsEnabled(): Boolean {
    initializeIfNotInitialized()
    return autoLogAppEventsEnabled.getValue()
  }

  @JvmStatic
  fun setAdvertiserIDCollectionEnabled(flag: Boolean) {
    advertiserIDCollectionEnabled.value = flag
    advertiserIDCollectionEnabled.lastTS = System.currentTimeMillis()
    if (isInitialized.get()) {
      writeSettingToCache(advertiserIDCollectionEnabled)
    } else {
      initializeIfNotInitialized()
    }
  }

  @JvmStatic
  fun getAdvertiserIDCollectionEnabled(): Boolean {
    initializeIfNotInitialized()
    return advertiserIDCollectionEnabled.getValue()
  }

  @JvmStatic
  fun getCodelessSetupEnabled(): Boolean {
    initializeIfNotInitialized()
    return codelessSetupEnabled.getValue()
  }

  @JvmStatic
  fun setMonitorEnabled(flag: Boolean) {
    monitorEnabled.value = flag
    monitorEnabled.lastTS = System.currentTimeMillis()
    if (isInitialized.get()) {
      writeSettingToCache(monitorEnabled)
    } else {
      initializeIfNotInitialized()
    }
  }

  @JvmStatic
  fun getMonitorEnabled(): Boolean {
    initializeIfNotInitialized()
    return monitorEnabled.getValue()
  }

  private class UserSetting(var defaultVal: Boolean, var key: String) {
    var value: Boolean? = null
    var lastTS: Long = 0
    fun getValue(): Boolean {
      return value ?: defaultVal
    }
  }
}
