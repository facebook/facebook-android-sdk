/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
