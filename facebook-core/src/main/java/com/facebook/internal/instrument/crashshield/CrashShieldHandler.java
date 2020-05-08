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

package com.facebook.internal.instrument.crashshield;

import android.os.Handler;
import android.os.Looper;

import com.facebook.FacebookSdk;
import com.facebook.core.BuildConfig;
import com.facebook.internal.instrument.ExceptionAnalyzer;
import com.facebook.internal.instrument.InstrumentData;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class CrashShieldHandler {

    private static final Set<Object> sCrashingObjects
            = Collections.newSetFromMap(new WeakHashMap<Object, Boolean>());
    private static boolean enabled = false;

    public static void enable() {
        enabled = true;
    }

    public static void handleThrowable(Throwable e, Object o) {
        if (!enabled) {
            return;
        }

        sCrashingObjects.add(o);
        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            ExceptionAnalyzer.execute(e);
            InstrumentData.Builder.build(e, InstrumentData.Type.CrashShield).save();
        }
        scheduleCrashInDebug(e);
    }

    public static boolean isObjectCrashing(Object o) {
        return sCrashingObjects.contains(o);
    }

    public static void methodFinished(Object o) { }

    public static void reset() {
        resetCrashingObjects();
    }

    public static void resetCrashingObjects() {
        sCrashingObjects.clear();
    }

    private static void scheduleCrashInDebug(final Throwable e) {
        if (BuildConfig.DEBUG) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {

                @NoAutoExceptionHandling
                @Override
                public void run() {
                    // throw on main thread during development to avoid catching by crash shield
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
