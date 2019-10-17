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

package com.facebook.applinks;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.facebook.FacebookSdk;
import com.facebook.appevents.InternalAppEventsLogger;

import org.json.JSONObject;

public final class AppLinks {

    public final static String AUTO_APPLINK_DATA_KEY = "fb_aut_applink_data";

    private static Class<? extends Activity> autoAppLinkActivity;
    private static final String AUTO_APPLINK_EVENT = "fb_auto_applink";
    private static final String AUTO_APPLINK_KEY = "com.facebook.sdk.AutoAppLinkActivity";

    private AppLinks() {}

    @SuppressWarnings("unchecked")
    private static synchronized boolean initialize(Context context) {
        if (!FacebookSdk.isInitialized()) {
            return false;
        }
        if (autoAppLinkActivity != null) {
            return true;
        }
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(
                            context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (ai != null && ai.metaData != null && ai.metaData.containsKey(AUTO_APPLINK_KEY)) {
                String name = ai.metaData.getString(AUTO_APPLINK_KEY);
                autoAppLinkActivity = (Class<? extends Activity>) Class.forName(name);
            }
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            /* no op */
        } catch (ClassNotFoundException e) {
            /* no op */
        }
        return false;
    }

    /**
     * Handle the received Auto App Link and navigate to the registered display activity with the
     * Auto App Link data. If the received app link is not Auto App Link or the display activity is
     * not registered, the function does nothing and returns false.
     *
     * @param activity the activity that receives deep link
     * @return true if successfully handle auto app link and false otherwise
     */
    public static boolean handleAutoAppLink(final Activity activity) {
        if (activity == null || !initialize(activity)) {
            return false;
        }

        AppLinkData data = AppLinkData.createFromAlApplinkData(activity.getIntent());
        if (data == null || !data.isAutoAppLink()) {
            return false;
        }

        JSONObject appLinkData = data.getAppLinkData();
        if (appLinkData == null) {
            return false;
        }

        Intent intent = new Intent(activity, autoAppLinkActivity);
        intent.putExtra(AUTO_APPLINK_DATA_KEY, appLinkData.toString());
        InternalAppEventsLogger logger = new InternalAppEventsLogger(FacebookSdk.getApplicationContext());
        logger.logEvent(AUTO_APPLINK_EVENT, new Bundle());
        activity.startActivity(intent);
        return true;
    }
}
