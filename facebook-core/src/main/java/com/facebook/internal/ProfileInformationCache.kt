/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 *
 * Caches the profile information associated to a specific access token. This minimizes the number
 * of request we need to make to the server.
 */
internal object ProfileInformationCache {
  private val infoCache = ConcurrentHashMap<String, JSONObject>()

  @JvmStatic
  fun getProfileInformation(accessToken: String): JSONObject? {
    return infoCache[accessToken]
  }

  @JvmStatic
  fun putProfileInformation(key: String, value: JSONObject) {
    infoCache[key] = value
  }
}
