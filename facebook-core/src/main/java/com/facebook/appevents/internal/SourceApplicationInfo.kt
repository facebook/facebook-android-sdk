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
