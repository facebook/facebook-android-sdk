/**
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

package com.facebook.internal.instrument.crashreport;

import android.os.Process;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.facebook.internal.instrument.InstrumentUtility;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getCanonicalName();

    @Nullable private static CrashHandler instance;

    @Nullable private final Thread.UncaughtExceptionHandler mPreviousHandler;
    private boolean mEndApplication;

    private CrashHandler(@Nullable  Thread.UncaughtExceptionHandler oldHandler) {
        mPreviousHandler = oldHandler;
        mEndApplication = false;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (InstrumentUtility.isSDKRelatedException(e)) {
            CrashReportData crashData = new CrashReportData(e);
            crashData.save();
        }
        if (mPreviousHandler != null) {
            mPreviousHandler.uncaughtException(t, e);
        }
        if (mEndApplication) {
            killProcess();
        }
    }

    public static synchronized void enable() {
        if (instance != null) {
            Log.w(TAG, "Already enabled!");
            return;
        }
        Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        instance = new CrashHandler(oldHandler);
        Thread.setDefaultUncaughtExceptionHandler(instance);
    }

    public void endApplication() {
        mEndApplication = true;
    }

    private static void killProcess() {
        try {
            Process.killProcess(Process.myPid());
            System.exit(10);
        } catch (Throwable internalEx) { /* no op */ }
    }
}
