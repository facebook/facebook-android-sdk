/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.preference.PreferenceManager
import com.facebook.FacebookSdk
import com.facebook.bolts.AppLinks

internal class SourceApplicationInfo
private constructor(val callingApplicationPackage: String?, val isOpenedByAppLink: Boolean) {
  override fun toString(): String {
    var openType = "Unclassified"
    if (isOpenedByAppLink) {
      openType = "Applink"
    }
    return if (callingApplicationPackage != null) {
      "$openType($callingApplicationPackage)"
    } else openType
  }

  fun writeSourceApplicationInfoToDisk() {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
    val editor = sharedPreferences.edit()
    editor.putString(CALL_APPLICATION_PACKAGE_KEY, callingApplicationPackage)
    editor.putBoolean(OPENED_BY_APP_LINK_KEY, isOpenedByAppLink)
    editor.apply()
  }

  object Factory {
    @JvmStatic
    fun create(activity: Activity): SourceApplicationInfo? {
      var openedByAppLink = false
      var callingApplicationPackage: String? = ""
      val callingApplication = activity.callingActivity
      if (callingApplication != null) {
        callingApplicationPackage = callingApplication.packageName
        if (callingApplicationPackage == activity.packageName) {
          // open by own app.
          return null
        }
      }

      // Tap icon to open an app will still get the old intent if the activity was opened by
      // an intent before. Introduce an extra field in the intent to force clear the
      // sourceApplication.
      val openIntent = activity.intent
      if (openIntent != null &&
          !openIntent.getBooleanExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, false)) {
        openIntent.putExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, true)
        val appLinkData = AppLinks.getAppLinkData(openIntent)
        if (appLinkData != null) {
          openedByAppLink = true
          val appLinkReferrerData = appLinkData.getBundle("referer_app_link")
          if (appLinkReferrerData != null) {
            val appLinkReferrerPackage = appLinkReferrerData.getString("package")
            callingApplicationPackage = appLinkReferrerPackage
          }
        }
      }
      openIntent?.putExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, true)
      return SourceApplicationInfo(callingApplicationPackage, openedByAppLink)
    }
  }

  companion object {
    private const val SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT =
        "_fbSourceApplicationHasBeenSet"
    private const val CALL_APPLICATION_PACKAGE_KEY =
        "com.facebook.appevents.SourceApplicationInfo.callingApplicationPackage"
    private const val OPENED_BY_APP_LINK_KEY =
        "com.facebook.appevents.SourceApplicationInfo.openedByApplink"

    @JvmStatic
    fun getStoredSourceApplicatioInfo(): SourceApplicationInfo? {
      val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
      if (!sharedPreferences.contains(CALL_APPLICATION_PACKAGE_KEY)) {
        return null
      }
      val callingApplicationPackage =
          sharedPreferences.getString(CALL_APPLICATION_PACKAGE_KEY, null)
      val openedByAppLink = sharedPreferences.getBoolean(OPENED_BY_APP_LINK_KEY, false)
      return SourceApplicationInfo(callingApplicationPackage, openedByAppLink)
    }

    @JvmStatic
    fun clearSavedSourceApplicationInfoFromDisk() {
      val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
      val editor = sharedPreferences.edit()
      editor.remove(CALL_APPLICATION_PACKAGE_KEY)
      editor.remove(OPENED_BY_APP_LINK_KEY)
      editor.apply()
    }
  }
}
