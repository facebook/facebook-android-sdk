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
