/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import android.net.Uri

/**
 * Implement this interface to provide an alternate strategy for resolving App Links that may
 * include pre-fetching, caching, or querying for App Link data from an index provided by a service
 * provider.
 */
fun interface AppLinkResolver {
  /**
   * Asynchronously resolves App Link data for a given URL.
   *
   * @param url the URL to resolve into an App Link.
   * @return the [AppLink] for the given URL.
   */
  fun getAppLinkFromUrlInBackground(url: Uri): Task<AppLink>
}
