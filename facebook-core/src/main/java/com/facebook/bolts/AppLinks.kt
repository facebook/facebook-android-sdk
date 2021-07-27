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
package com.facebook.bolts

import android.content.Intent
import android.os.Bundle

/**
 * Provides a set of utility methods for working with incoming Intents that may contain App Link
 * data.
 */
object AppLinks {
  const val KEY_NAME_APPLINK_DATA = "al_applink_data"
  const val KEY_NAME_EXTRAS = "extras"

  /**
   * Gets the App Link data for an intent, if there is any. This is the authorized function to check
   * if an intent is AppLink. If null is returned it is not.
   *
   * @param intent the incoming intent.
   * @return a bundle containing the App Link data for the intent, or `null` if none is specified.
   */
  @JvmStatic
  fun getAppLinkData(intent: Intent): Bundle? {
    return intent.getBundleExtra(KEY_NAME_APPLINK_DATA)
  }

  /**
   * Gets the App Link extras for an intent, if there is any.
   *
   * @param intent the incoming intent.
   * @return a bundle containing the App Link extras for the intent, or `null` if none is specified.
   */
  @JvmStatic
  fun getAppLinkExtras(intent: Intent): Bundle? {
    val appLinkData = getAppLinkData(intent) ?: return null
    return appLinkData.getBundle(KEY_NAME_EXTRAS)
  }
}
