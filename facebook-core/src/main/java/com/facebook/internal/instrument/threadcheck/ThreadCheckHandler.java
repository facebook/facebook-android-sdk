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

package com.facebook.internal.instrument.threadcheck;

import android.os.Looper;
import android.util.Log;
import androidx.annotation.RestrictTo;
import com.facebook.internal.instrument.InstrumentData;
import java.util.Locale;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ThreadCheckHandler {

  private static final String TAG = ThreadCheckHandler.class.getCanonicalName();
  private static boolean enabled = false;

  private ThreadCheckHandler() {}

  public static void enable() {
    enabled = true;
  }

  public static void uiThreadViolationDetected(
      Class<?> clazz, String methodName, String methodDesc) {
    log("@UiThread", clazz, methodName, methodDesc);
  }

  public static void workerThreadViolationDetected(
      Class<?> clazz, String methodName, String methodDesc) {
    log("@WorkerThread", clazz, methodName, methodDesc);
  }

  private static void log(String annotation, Class<?> clazz, String methodName, String methodDesc) {
    if (!enabled) {
      return;
    }

    String message =
        String.format(
            Locale.US,
            "%s annotation violation detected in %s.%s%s. Current looper is %s and main looper is %s.",
            annotation,
            clazz.getName(),
            methodName,
            methodDesc,
            Looper.myLooper(),
            Looper.getMainLooper());
    Exception e = new Exception();
    Log.e(TAG, message, e);
    InstrumentData.Builder.build(e, InstrumentData.Type.ThreadCheck).save();
  }
}
