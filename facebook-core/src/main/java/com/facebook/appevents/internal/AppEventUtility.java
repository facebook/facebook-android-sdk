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

package com.facebook.appevents.internal;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import androidx.annotation.Nullable;
import com.facebook.FacebookSdk;
import com.facebook.core.BuildConfig;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppEventUtility {
  private static final String regex = "[-+]*\\d+([\\,\\.]\\d+)*([\\.\\,]\\d+)?";

  public static void assertIsNotMainThread() {
    if (BuildConfig.DEBUG) {
      if (isMainThread()) {
        throw new AssertionError("Call cannot be made on the main thread");
      }
    }
  }

  public static void assertIsMainThread() {
    if (BuildConfig.DEBUG) {
      if (!isMainThread()) {
        throw new AssertionError("Call must be made on the main thread");
      }
    }
  }

  public static double normalizePrice(String value) {
    try {
      final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
      final Matcher matcher = pattern.matcher(value);

      if (matcher.find()) {
        String firstValue = matcher.group(0);
        return NumberFormat.getNumberInstance(Utility.getCurrentLocale())
            .parse(firstValue)
            .doubleValue();
      } else {
        return 0.0;
      }
    } catch (ParseException e) {
      return 0.0;
    }
  }

  public static String bytesToHex(byte[] bytes) {
    StringBuffer sb = new StringBuffer();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static boolean isEmulator() {
    return Build.FINGERPRINT.startsWith("generic")
        || Build.FINGERPRINT.startsWith("unknown")
        || Build.MODEL.contains("google_sdk")
        || Build.MODEL.contains("Emulator")
        || Build.MODEL.contains("Android SDK built for x86")
        || Build.MANUFACTURER.contains("Genymotion")
        || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
        || "google_sdk".equals(Build.PRODUCT);
  }

  private static boolean isMainThread() {
    return Looper.myLooper() == Looper.getMainLooper();
  }

  public static String getAppVersion() {
    Context context = FacebookSdk.getApplicationContext();
    try {
      PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
      return pInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      return "";
    }
  }

  @AutoHandleExceptions
  @Nullable
  public static View getRootView(@Nullable Activity activity) {
    if (activity == null) {
      return null;
    }
    Window window = activity.getWindow();
    if (window == null) {
      return null;
    }
    return window.getDecorView().getRootView();
  }
}
