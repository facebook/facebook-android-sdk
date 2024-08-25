/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import com.facebook.FacebookSdk
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import org.json.JSONObject

@AutoHandleExceptions
class AppLinkManager private constructor() {

  private val preferences: SharedPreferences by lazy {
    FacebookSdk.getApplicationContext().getSharedPreferences(APPLINK_INFO, Context.MODE_PRIVATE)
  }

  companion object {
    const val APPLINK_INFO = "com.facebook.sdk.APPLINK_INFO"
    const val APPLINK_DATA_KEY = "al_applink_data"
    const val CAMPAIGN_IDS_KEY = "campaign_ids"

    @Volatile
    private var instance: AppLinkManager? = null

    fun getInstance(): AppLinkManager? =
      instance ?: synchronized(this) {
        if (!FacebookSdk.isInitialized()) {
          return null
        }
        instance ?: AppLinkManager().also { instance = it }
      }
  }

  fun handleURL(activity: Activity) {
    val uri = activity.intent.data ?: return
    processCampaignIds(uri)
  }

  fun processCampaignIds(uri: Uri) {
    val applinkData = uri.getQueryParameter(APPLINK_DATA_KEY) ?: return
    try {
      val json = JSONObject(applinkData)
      val campaignIDs = json.getString(CAMPAIGN_IDS_KEY) ?: return
      preferences.edit().putString(CAMPAIGN_IDS_KEY, campaignIDs).apply()
    } catch (_: Exception) {}
  }

  fun getInfo(key: String): String? {
    return preferences.getString(key, null)
  }

  fun setupLifecycleListener(application: Application) {
    application.registerActivityLifecycleCallbacks(
      object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
          // no-op
        }

        override fun onActivityStarted(activity: Activity) {
          getInstance()?.handleURL(activity)
        }

        override fun onActivityResumed(activity: Activity) {
          getInstance()?.handleURL(activity)
        }

        override fun onActivityPaused(activity: Activity) {
          // no-op
        }

        override fun onActivityStopped(activity: Activity) {
          // no-op
        }

        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
          // no-op
        }

        override fun onActivityDestroyed(activity: Activity) {
          // no-op
        }
      })
  }
}
