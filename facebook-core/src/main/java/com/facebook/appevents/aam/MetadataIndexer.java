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

package com.facebook.appevents.aam;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.annotation.UiThread;

import com.facebook.FacebookSdk;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;

import java.util.concurrent.atomic.AtomicBoolean;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
final public class MetadataIndexer {
    private static final String TAG = MetadataIndexer.class.getCanonicalName();
    private static final AtomicBoolean enabled = new AtomicBoolean(false);

    @UiThread
    public static void onActivityResumed(final Activity activity) {
        try {
            if (!enabled.get() || MetadataRule.getRules().isEmpty()) {
                return;
            }

            MetadataViewObserver.startTrackingActivity(activity);
        } catch (Exception e) {
        }
    }

    private static void updateRules() {
        FetchedAppSettings settings = FetchedAppSettingsManager.queryAppSettings(
                FacebookSdk.getApplicationId(), false);
        if (settings == null) {
            return;
        }

        String rawRule = settings.getRawAamRules();
        if (rawRule == null) {
            return;
        }
        MetadataRule.updateRules(rawRule);
    }

    public static void enable() {
        try {
            FacebookSdk.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    Context context = FacebookSdk.getApplicationContext();
                    if (!AttributionIdentifiers.isTrackingLimited(context)) {
                        enabled.set(true);
                        updateRules();
                    }
                }
            });
        } catch (Exception e) {
            Utility.logd(TAG, e);
        }
    }
}
