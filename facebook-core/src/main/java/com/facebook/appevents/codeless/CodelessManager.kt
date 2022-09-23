/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.FacebookSdk.getApplicationId
import com.facebook.FacebookSdk.getCodelessSetupEnabled
import com.facebook.FacebookSdk.getExecutor
import com.facebook.GraphRequest
import com.facebook.appevents.codeless.internal.Constants
import com.facebook.appevents.internal.AppEventUtility.isEmulator
import com.facebook.core.BuildConfig
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import com.facebook.internal.FetchedAppSettingsManager.getAppSettingsWithoutQuery
import com.facebook.internal.Utility.currentLocale
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Volatile
import org.json.JSONArray

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CodelessManager {
  private val viewIndexingTrigger = ViewIndexingTrigger()
  private var sensorManager: SensorManager? = null
  private var viewIndexer: ViewIndexer? = null
  private var deviceSessionID: String? = null
  private val isCodelessEnabled = AtomicBoolean(true)
  private val isAppIndexingEnabled = AtomicBoolean(false)

  @Volatile private var isCheckingSession = false
  @JvmStatic
  fun onActivityResumed(activity: Activity) {
    if (!isCodelessEnabled.get()) {
      return
    }
    CodelessMatcher.getInstance().add(activity)
    val applicationContext = activity.applicationContext
    val appId = getApplicationId()
    val appSettings = getAppSettingsWithoutQuery(appId)
    if (appSettings?.codelessEventsEnabled == true || isDebugOnEmulator()) {
      val sensorManager =
          applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager? ?: return
      this.sensorManager = sensorManager
      val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      val viewIndexer = ViewIndexer(activity)
      this.viewIndexer = viewIndexer
      viewIndexingTrigger.setOnShakeListener {
        val codelessEventsEnabled = appSettings != null && appSettings.codelessEventsEnabled
        val codelessSetupEnabled = (getCodelessSetupEnabled() || BuildConfig.DEBUG && isEmulator())
        if (codelessEventsEnabled && codelessSetupEnabled) {
          checkCodelessSession(appId)
        }
      }
      sensorManager.registerListener(
          viewIndexingTrigger, accelerometer, SensorManager.SENSOR_DELAY_UI)
      if (appSettings != null && appSettings.codelessEventsEnabled) {
        viewIndexer.schedule()
      }
    }
    if (isDebugOnEmulator() && !isAppIndexingEnabled.get()) {
      // Check session on start when app launched
      // on emulator and built in DEBUG mode
      checkCodelessSession(appId)
    }
  }

  @JvmStatic
  fun onActivityPaused(activity: Activity) {
    if (!isCodelessEnabled.get()) {
      return
    }
    CodelessMatcher.getInstance().remove(activity)
    viewIndexer?.unschedule()
    sensorManager?.unregisterListener(viewIndexingTrigger)
  }

  @JvmStatic
  fun onActivityDestroyed(activity: Activity) {
    CodelessMatcher.getInstance().destroy(activity)
  }

  @JvmStatic
  fun enable() {
    isCodelessEnabled.set(true)
  }

  @JvmStatic
  fun disable() {
    isCodelessEnabled.set(false)
  }

  private fun checkCodelessSession(applicationId: String?) {
    if (isCheckingSession) {
      return
    }
    isCheckingSession = true
    getExecutor().execute {
      val requestParameters = Bundle()
      val context = getApplicationContext()
      val identifiers = getAttributionIdentifiers(context)
      val extInfoArray = JSONArray()
      extInfoArray.put(if (Build.MODEL != null) Build.MODEL else "")
      if (identifiers?.androidAdvertiserId != null) {
        extInfoArray.put(identifiers.androidAdvertiserId)
      } else {
        extInfoArray.put("")
      }
      extInfoArray.put(if (BuildConfig.DEBUG) "1" else "0")
      extInfoArray.put(if (isEmulator()) "1" else "0")
      // Locale
      val locale = currentLocale
      extInfoArray.put(locale.language + "_" + locale.country)
      val extInfo = extInfoArray.toString()
      requestParameters.putString(Constants.DEVICE_SESSION_ID, getCurrentDeviceSessionID())
      requestParameters.putString(Constants.EXTINFO, extInfo)
      val request =
          GraphRequest.newPostRequestWithBundle(
              null,
              String.format(Locale.US, "%s/app_indexing_session", applicationId),
              requestParameters,
              null)
      val res = request.executeAndWait()
      val jsonRes = res.getJSONObject()
      isAppIndexingEnabled.set(
          jsonRes != null && jsonRes.optBoolean(Constants.APP_INDEXING_ENABLED, false))
      if (!isAppIndexingEnabled.get()) {
        deviceSessionID = null
      } else {
        viewIndexer?.schedule()
      }
      isCheckingSession = false
    }
  }

  private fun isDebugOnEmulator(): Boolean {
    return BuildConfig.DEBUG && isEmulator()
  }

  @JvmStatic
  internal fun getCurrentDeviceSessionID(): String {
    if (null == deviceSessionID) {
      deviceSessionID = UUID.randomUUID().toString()
    }
    return deviceSessionID as String
  }

  @JvmStatic
  internal fun getIsAppIndexingEnabled(): Boolean {
    return isAppIndexingEnabled.get()
  }

  @JvmStatic
  internal fun updateAppIndexing(appIndexingEnabled: Boolean) {
    isAppIndexingEnabled.set(appIndexingEnabled)
  }
}
