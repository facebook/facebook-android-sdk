/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import android.net.Uri
import java.util.Collections

/**
 * Contains App Link metadata relevant for navigation on this device derived from the HTML at a
 * given URL.
 *
 * @param sourceUrl the URL from which this App Link was derived.
 * @param webUrl the fallback web URL, if any was specified, for this App Link.
 * @param targets the ordered list of Targets for this platform.
 */
class AppLink(val sourceUrl: Uri, targets: List<Target>?, val webUrl: Uri) {

  /**
   * Represents a target defined in App Link metadata, consisting of at least a package name, and
   * optionally a URL, class name (for explicit intent handling), and an app name.
   */
  class Target
  /**
   * Creates a Target with the given metadata.
   *
   * @param packageName the package name for the target.
   * @param className the class name to be used when creating an explicit intent.
   * @param url the URL to be used as the data in the constructed intent.
   * @param appName the name of the app.
   */
  (
      /** the package name of the app. */
      val packageName: String,
      /** the class name to be used when creating an explicit intent from this target. */
      val className: String,
      /**
       * the URL that will be used as the data in an intent constructed from this target. If no url
       * is specified, the intent will use the URL that was the source of this metadata.
       */
      val url: Uri,
      /** the app name. */
      val appName: String
  )

  /** the ordered list of Targets for this platform. */
  val targets: List<Target> = targets ?: emptyList()
    get() = Collections.unmodifiableList(field)
}
