/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import android.view.View
import com.facebook.FacebookSdk
import com.facebook.core.BuildConfig
import com.facebook.internal.Utility.currentLocale
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.text.NumberFormat
import java.text.ParseException
import java.util.regex.Pattern

object AppEventUtility {
  private const val PRICE_REGEX = """[-+]*\d+([.,]\d+)*([.,]\d+)?"""

  @JvmStatic
  fun assertIsNotMainThread() {
    if (BuildConfig.DEBUG) {
      if (isMainThread) {
        throw AssertionError("Call cannot be made on the main thread")
      }
    }
  }

  @JvmStatic
  fun assertIsMainThread() {
    if (BuildConfig.DEBUG) {
      if (!isMainThread) {
        throw AssertionError("Call must be made on the main thread")
      }
    }
  }

  @JvmStatic
  fun normalizePrice(value: String?): Double {
    return try {
      val pattern = Pattern.compile(PRICE_REGEX, Pattern.MULTILINE)
      val matcher = pattern.matcher(value)
      if (matcher.find()) {
        val firstValue = matcher.group(0)
        NumberFormat.getNumberInstance(currentLocale).parse(firstValue).toDouble()
      } else {
        0.0
      }
    } catch (e: ParseException) {
      0.0
    }
  }

  @JvmStatic
  fun bytesToHex(bytes: ByteArray): String {
    val sb = StringBuffer()
    for (b in bytes) {
      sb.append(String.format("%02x", b))
    }
    return sb.toString()
  }

  @JvmStatic
  fun isEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.MODEL.contains("google_sdk") ||
        Build.MODEL.contains("Emulator") ||
        Build.MODEL.contains("Android SDK built for x86") ||
        Build.MANUFACTURER.contains("Genymotion") ||
        Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
        "google_sdk" == Build.PRODUCT
  }

  private val isMainThread: Boolean
    @JvmStatic private get() = Looper.myLooper() == Looper.getMainLooper()

  @JvmStatic
  fun getAppVersion(): String {
    val context = FacebookSdk.getApplicationContext()
    return try {
      val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
      packageInfo.versionName
    } catch (e: PackageManager.NameNotFoundException) {
      ""
    }
  }

  @AutoHandleExceptions
  @JvmStatic
  fun getRootView(activity: Activity?): View? {
    return try {
      if (activity == null) {
        return null
      }
      val window = activity.window ?: return null
      window.decorView.rootView
    } catch (e: Exception) {
      null
    }
  }
}
