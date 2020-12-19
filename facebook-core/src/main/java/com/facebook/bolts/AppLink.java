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

package com.facebook.bolts;

import android.net.Uri;
import java.util.Collections;
import java.util.List;

/**
 * Contains App Link metadata relevant for navigation on this device derived from the HTML at a
 * given URL.
 */
public class AppLink {
  /**
   * Represents a target defined in App Link metadata, consisting of at least a package name, and
   * optionally a URL, class name (for explicit intent handling), and an app name.
   */
  public static class Target {
    private final Uri url;
    private final String packageName;
    private final String className;
    private final String appName;

    /**
     * Creates a Target with the given metadata.
     *
     * @param packageName the package name for the target.
     * @param className the class name to be used when creating an explicit intent.
     * @param url the URL to be used as the data in the constructed intent.
     * @param appName the name of the app.
     */
    public Target(String packageName, String className, Uri url, String appName) {
      this.packageName = packageName;
      this.className = className;
      this.url = url;
      this.appName = appName;
    }

    /**
     * @return the URL that will be used as the data in an intent constructed from this target. If
     *     no url is specified, the intent will use the URL that was the source of this metadata.
     */
    public Uri getUrl() {
      return url;
    }

    /** @return the app name. */
    public String getAppName() {
      return appName;
    }

    /** @return the class name to be used when creating an explicit intent from this target. */
    public String getClassName() {
      return className;
    }

    /** @return the package name of the app. */
    public String getPackageName() {
      return packageName;
    }
  }

  private Uri sourceUrl;
  private List<Target> targets;
  private Uri webUrl;

  /**
   * Creates an AppLink with the given metadata.
   *
   * @param sourceUrl the URL from which this App Link was derived.
   * @param targets an ordered list of Targets for this platform that will be used when navigating
   *     with this App Link.
   * @param webUrl the fallback web URL, if any, for this App Link.
   */
  public AppLink(Uri sourceUrl, List<Target> targets, Uri webUrl) {
    this.sourceUrl = sourceUrl;
    if (targets == null) {
      targets = Collections.emptyList();
    }
    this.targets = targets;
    this.webUrl = webUrl;
  }

  /** @return the URL from which this App Link was derived. */
  public Uri getSourceUrl() {
    return sourceUrl;
  }

  /** @return the ordered list of Targets for this platform. */
  public List<Target> getTargets() {
    return Collections.unmodifiableList(targets);
  }

  /** @return the fallback web URL, if any was specified, for this App Link. */
  public Uri getWebUrl() {
    return webUrl;
  }
}
