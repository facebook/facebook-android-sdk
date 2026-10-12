/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.google.android.gms.ads.identifier

import android.content.Context

/**
 * Test double for Google's AdvertisingIdClient, so AttributionIdentifiers' reflective lookup runs
 * for real. play-services-ads-identifier is not on the test classpath. play-services-base is (via
 * the test-only play-services-gcm dependency), but Google Play services isn't installed under
 * Robolectric, so the SDK's pre-check fails. That is the same outcome as an app without
 * play-services-base; the class-absent case itself is covered by the device test.
 */
class AdvertisingIdClient {
  class Info(private val id: String?, private val limitAdTracking: Boolean) {
    fun getId(): String? = id

    fun isLimitAdTrackingEnabled(): Boolean = limitAdTracking
  }

  companion object {
    @JvmField var nextInfo: Info? = null
    @JvmField var failure: Exception? = null
    @JvmField var callCount = 0

    @JvmStatic
    fun getAdvertisingIdInfo(context: Context): Info? {
      callCount++
      failure?.let { throw it }
      return nextInfo
    }

    fun reset() {
      nextInfo = null
      failure = null
      callCount = 0
    }
  }
}
