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

package com.facebook.appevents.ml;

import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.facebook.FacebookSdk;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.io.File;
import java.nio.charset.Charset;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
public class Utils {

  private static final String DIR_NAME = "facebook_ml/";

  static int[] vectorize(final String texts, int maxLen) {
    int[] ret = new int[maxLen];
    String normalizedStr = normalizeString(texts);
    byte[] strBytes = normalizedStr.getBytes(Charset.forName("UTF-8"));
    for (int i = 0; i < maxLen; i++) {
      if (i < strBytes.length) {
        ret[i] = strBytes[i] & 0xFF;
      } else {
        ret[i] = 0;
      }
    }
    return ret;
  }

  static String normalizeString(final String str) {
    String trim = str.trim();
    String[] strArray = trim.split("\\s+");
    String joinedString = TextUtils.join(" ", strArray);
    return joinedString;
  }

  @Nullable
  public static File getMlDir() {
    File dir = new File(FacebookSdk.getApplicationContext().getFilesDir(), DIR_NAME);
    if (dir.exists() || dir.mkdirs()) {
      return dir;
    }
    return null;
  }
}
