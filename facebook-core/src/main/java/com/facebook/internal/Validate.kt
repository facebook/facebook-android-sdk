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
package com.facebook.internal

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Looper
import android.util.Log
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.FacebookSdkNotInitializedException

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object Validate {
  private val TAG = Validate::class.java.name
  private const val NO_INTERNET_PERMISSION_REASON =
      "No internet permissions granted for the app, please add " +
          "<uses-permission android:name=\"android.permission.INTERNET\" /> " +
          "to your AndroidManifest.xml."
  private const val FACEBOOK_ACTIVITY_NOT_FOUND_REASON =
      "FacebookActivity is not declared in the AndroidManifest.xml. If you are using the " +
          "facebook-common module or dependent modules please add " +
          "com.facebook.FacebookActivity to your AndroidManifest.xml file. See " +
          "https://developers.facebook.com/docs/android/getting-started for more info."
  private const val CONTENT_PROVIDER_NOT_FOUND_REASON =
      "A ContentProvider for this app was not set up in the AndroidManifest.xml, please " +
          "add %s as a provider to your AndroidManifest.xml file. See " +
          "https://developers.facebook.com/docs/sharing/android for more info."
  private const val CONTENT_PROVIDER_BASE = "com.facebook.app.FacebookContentProvider"
  const val CUSTOM_TAB_REDIRECT_URI_PREFIX = "fbconnect://cct."

  @JvmStatic
  fun notNull(arg: Any?, name: String) {
    if (arg == null) {
      throw NullPointerException("Argument '$name' cannot be null")
    }
  }

  @JvmStatic
  fun <T> notEmpty(container: Collection<T>, name: String) {
    require(!container.isEmpty()) { "Container '$name' cannot be empty" }
  }

  @JvmStatic
  fun <T> containsNoNulls(container: Collection<T>, name: String) {
    notNull(container, name)
    for (item in container) {
      if (item == null) {
        throw NullPointerException("Container '$name' cannot contain null values")
      }
    }
  }

  @JvmStatic
  fun containsNoNullOrEmpty(container: Collection<String?>, name: String) {
    notNull(container, name)
    for (item in container) {
      if (item == null) {
        throw NullPointerException("Container '$name' cannot contain null values")
      }
      require(item.isNotEmpty()) { "Container '$name' cannot contain empty values" }
    }
  }

  @JvmStatic
  fun <T> notEmptyAndContainsNoNulls(container: Collection<T>, name: String) {
    containsNoNulls(container, name)
    notEmpty(container, name)
  }

  @JvmStatic
  fun runningOnUiThread() {
    if (Looper.getMainLooper() != Looper.myLooper()) {
      throw FacebookException("This method should be called from the UI thread")
    }
  }

  @JvmStatic
  fun notNullOrEmpty(arg: String?, name: String) {
    require(!Utility.isNullOrEmpty(arg)) { "Argument '$name' cannot be null or empty" }
  }

  @JvmStatic
  fun notEmpty(arg: String, name: String) {
    require(arg.isNotEmpty()) { "Argument '$name' cannot be empty" }
  }

  @JvmStatic
  fun oneOf(arg: Any?, name: String, vararg values: Any?) {
    for (value in values) {
      if (value == arg) {
        return
      }
    }
    throw IllegalArgumentException("Argument '$name' was not one of the allowed values")
  }

  @JvmStatic
  fun sdkInitialized() {
    if (!FacebookSdk.isInitialized()) {
      throw FacebookSdkNotInitializedException(
          "The SDK has not been initialized, make sure to call " +
              "FacebookSdk.sdkInitialize() first.")
    }
  }

  @JvmStatic
  fun hasAppID(): String {
    return checkNotNull(FacebookSdk.getApplicationId()) {
      "No App ID found, please set the App ID."
    }
  }

  @JvmStatic
  fun hasClientToken(): String {
    return checkNotNull(FacebookSdk.getClientToken()) {
      "No Client Token found, please set the Client Token."
    }
  }

  @JvmStatic
  fun hasInternetPermissions(context: Context) {
    hasInternetPermissions(context, true)
  }

  @JvmStatic
  fun hasInternetPermissions(context: Context, shouldThrow: Boolean) {
    notNull(context, "context")
    if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) ==
        PackageManager.PERMISSION_DENIED) {
      check(!shouldThrow) { NO_INTERNET_PERMISSION_REASON }
      Log.w(TAG, NO_INTERNET_PERMISSION_REASON)
    }
  }

  @JvmStatic
  fun hasWiFiPermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE)
  }

  @JvmStatic
  fun hasChangeWifiStatePermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.CHANGE_WIFI_STATE)
  }

  @JvmStatic
  fun hasLocationPermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ||
        hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
  }

  @JvmStatic
  fun hasBluetoothPermission(context: Context): Boolean {
    return hasPermission(context, Manifest.permission.BLUETOOTH) &&
        hasPermission(context, Manifest.permission.BLUETOOTH_ADMIN)
  }

  @JvmStatic
  fun hasPermission(context: Context, permission: String): Boolean {
    return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
  }

  @JvmStatic
  fun hasFacebookActivity(context: Context) {
    hasFacebookActivity(context, true)
  }

  @SuppressLint("WrongConstant")
  @JvmStatic
  fun hasFacebookActivity(context: Context, shouldThrow: Boolean) {
    notNull(context, "context")
    val pm = context.packageManager
    var activityInfo: ActivityInfo? = null
    if (pm != null) {
      val componentName = ComponentName(context, "com.facebook.FacebookActivity")
      try {
        activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_ACTIVITIES)
      } catch (e: PackageManager.NameNotFoundException) {
        // ignore
      }
    }
    if (activityInfo == null) {
      check(!shouldThrow) { FACEBOOK_ACTIVITY_NOT_FOUND_REASON }
      Log.w(TAG, FACEBOOK_ACTIVITY_NOT_FOUND_REASON)
    }
  }

  @JvmStatic
  fun hasCustomTabRedirectActivity(context: Context, redirectURI: String?): Boolean {
    notNull(context, "context")
    val pm = context.packageManager
    var infos: List<ResolveInfo>? = null
    if (pm != null) {
      val intent = Intent()
      intent.action = Intent.ACTION_VIEW
      intent.addCategory(Intent.CATEGORY_DEFAULT)
      intent.addCategory(Intent.CATEGORY_BROWSABLE)
      intent.data = Uri.parse(redirectURI)
      infos = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
    }
    var hasActivity = false
    if (infos != null) {
      for (info in infos) {
        val activityInfo = info.activityInfo
        if (activityInfo.name == "com.facebook.CustomTabActivity" &&
            activityInfo.packageName == context.packageName) {
          hasActivity = true
        } else {
          // another application is listening for this url scheme, don't open
          // Custom Tab for security reasons
          return false
        }
      }
    }
    return hasActivity
  }

  @JvmStatic
  fun hasContentProvider(context: Context) {
    notNull(context, "context")
    val appId = hasAppID()
    val pm = context.packageManager
    if (pm != null) {
      val providerName = CONTENT_PROVIDER_BASE + appId
      checkNotNull(pm.resolveContentProvider(providerName, 0)) {
        String.format(CONTENT_PROVIDER_NOT_FOUND_REASON, providerName)
      }
    }
  }
}
