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
