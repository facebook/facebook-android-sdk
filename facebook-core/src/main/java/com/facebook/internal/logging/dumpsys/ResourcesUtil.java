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
package com.facebook.core.internal.logging.dumpsys;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Internal copy"))
class ResourcesUtil {
  private ResourcesUtil() {}

  public static String getIdStringQuietly(Object idContext, @Nullable Resources r, int resourceId) {
    try {
      return getIdString(r, resourceId);
    } catch (Resources.NotFoundException e) {
      return getFallbackIdString(resourceId);
    }
  }

  public static String getIdString(@Nullable Resources r, int resourceId)
      throws Resources.NotFoundException {
    if (r == null) {
      return getFallbackIdString(resourceId);
    }

    String prefix;
    String prefixSeparator;
    switch (getResourcePackageId(resourceId)) {
      case 0x7f:
        prefix = "";
        prefixSeparator = "";
        break;
      default:
        prefix = r.getResourcePackageName(resourceId);
        prefixSeparator = ":";
        break;
    }

    String typeName = r.getResourceTypeName(resourceId);
    String entryName = r.getResourceEntryName(resourceId);

    StringBuilder sb =
        new StringBuilder(
            1
                + prefix.length()
                + prefixSeparator.length()
                + typeName.length()
                + 1
                + entryName.length());
    sb.append("@");
    sb.append(prefix);
    sb.append(prefixSeparator);
    sb.append(typeName);
    sb.append("/");
    sb.append(entryName);

    return sb.toString();
  }

  private static String getFallbackIdString(int resourceId) {
    return "#" + Integer.toHexString(resourceId);
  }

  private static int getResourcePackageId(int id) {
    return (id >>> 24) & 0xff;
  }
}
