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

package com.facebook.internal.instrument;

import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.instrument.crashreport.CrashHandler;
import com.facebook.internal.instrument.crashreport.CrashShieldHandler;
import com.facebook.internal.instrument.errorreport.ErrorReportHandler;
import com.facebook.internal.instrument.threadcheck.ThreadCheckHandler;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class InstrumentManager {

    /**
     * Start Instrument functionality.
     *
     * Note that the function should be called after FacebookSdk is initialized. Otherwise,
     * exception FacebookSdkNotInitializedException will be thrown when loading and sending crash
     * reports.
     */
    public static void start() {
        if (!FacebookSdk.getAutoLogAppEventsEnabled()) {
            return;
        }

        FeatureManager.checkFeature(FeatureManager.Feature.CrashReport,
                new FeatureManager.Callback() {
            @Override
            public void onCompleted(boolean enabled) {
                if (enabled) {
                    CrashHandler.enable();
                    if (FeatureManager.isEnabled(FeatureManager.Feature.CrashShield)) {
                        CrashShieldHandler.enable();
                    }
                    if (FeatureManager.isEnabled(FeatureManager.Feature.ThreadCheck)) {
                        ThreadCheckHandler.enable();
                    }
                }
            }
        });
        FeatureManager.checkFeature(FeatureManager.Feature.ErrorReport,
                new FeatureManager.Callback() {
            @Override
            public void onCompleted(boolean enabled) {
                if (enabled) {
                    ErrorReportHandler.enable();
                }
            }
        });
    }
}
