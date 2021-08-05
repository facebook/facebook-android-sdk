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
package com.facebook.appevents.iap

import android.content.pm.PackageManager
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.FeatureManager
import com.facebook.internal.FeatureManager.isEnabled
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object InAppPurchaseManager {
  private const val GOOGLE_BILLINGCLIENT_VERSION = "com.google.android.play.billingclient.version"
  private val enabled = AtomicBoolean(false)

  @JvmStatic
  fun enableAutoLogging() {
    enabled.set(true)
    startTracking()
  }

  @JvmStatic
  fun startTracking() {
    if (enabled.get()) {
      if (usingBillingLib2Plus() && isEnabled(FeatureManager.Feature.IapLoggingLib2)) {
        InAppPurchaseAutoLogger.startIapLogging(getApplicationContext())
      } else {
        InAppPurchaseActivityLifecycleTracker.startIapLogging()
      }
    }
  }

  private fun usingBillingLib2Plus(): Boolean {
    return try {
      val context = getApplicationContext()
      val info =
          context.packageManager.getApplicationInfo(
              context.packageName, PackageManager.GET_META_DATA)
      if (info != null) {
        val version = info.metaData.getString(GOOGLE_BILLINGCLIENT_VERSION)
        val versionArray = if (version === null) return false else version.split(".", limit = 3)
        return versionArray[0].toInt() >= 2
      }
      false
    } catch (e: Exception) {
      false
    }
  }
}
