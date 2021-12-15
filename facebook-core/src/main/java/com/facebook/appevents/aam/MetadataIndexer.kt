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

package com.facebook.appevents.aam

import android.app.Activity
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import com.facebook.FacebookSdk
import com.facebook.appevents.aam.MetadataRule.Companion.getRules
import com.facebook.appevents.aam.MetadataRule.Companion.updateRules
import com.facebook.internal.AttributionIdentifiers.Companion.isTrackingLimited
import com.facebook.internal.FetchedAppSettingsManager.queryAppSettings
import com.facebook.internal.Utility.logd
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.Exception

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object MetadataIndexer {
  private val TAG = MetadataIndexer::class.java.canonicalName
  private var enabled = false
  @UiThread
  @JvmStatic
  fun onActivityResumed(activity: Activity) {
    try {
      if (!enabled || getRules().isEmpty()) {
        return
      }
      MetadataViewObserver.startTrackingActivity(activity)
    } catch (e: Exception) {}
  }

  private fun updateRules() {
    val settings = queryAppSettings(FacebookSdk.getApplicationId(), false) ?: return
    val rawRule = settings.rawAamRules ?: return
    updateRules(rawRule)
  }

  @JvmStatic
  fun enable() {
    try {
      FacebookSdk.getExecutor().execute {
        val context = FacebookSdk.getApplicationContext()
        if (!isTrackingLimited(context)) {
          updateRules()
          enabled = true
        }
      }
    } catch (e: Exception) {
      logd(TAG, e)
    }
  }
}
