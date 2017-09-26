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

package com.facebook.appevents.internal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.facebook.FacebookSdk;

import bolts.AppLinks;

class SourceApplicationInfo {
    private static final String SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT =
            "_fbSourceApplicationHasBeenSet";
    private static final String CALL_APPLICATION_PACKAGE_KEY
            = "com.facebook.appevents.SourceApplicationInfo.callingApplicationPackage";
    private static final String OPENED_BY_APP_LINK_KEY
            = "com.facebook.appevents.SourceApplicationInfo.openedByApplink";

    private String callingApplicationPackage;
    private boolean openedByApplink;

    private SourceApplicationInfo(String callingApplicationPackage, boolean openedByApplink) {
        this.callingApplicationPackage = callingApplicationPackage;
        this.openedByApplink = openedByApplink;
    }

    public static SourceApplicationInfo getStoredSourceApplicatioInfo() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());

        if (!sharedPreferences.contains(CALL_APPLICATION_PACKAGE_KEY)) {
            return null;
        }

        String callingApplicationPackage = sharedPreferences.getString(
                CALL_APPLICATION_PACKAGE_KEY,
                null);
        boolean openedByApplink = sharedPreferences.getBoolean(OPENED_BY_APP_LINK_KEY, false);

        return new SourceApplicationInfo(callingApplicationPackage, openedByApplink);
    }

    public static void clearSavedSourceApplicationInfoFromDisk() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(CALL_APPLICATION_PACKAGE_KEY);
        editor.remove(OPENED_BY_APP_LINK_KEY);
        editor.apply();
    }

    public String getCallingApplicationPackage() {
        return callingApplicationPackage;
    }

    public boolean isOpenedByApplink() {
        return openedByApplink;
    }

    @Override
    public String toString() {
        String openType = "Unclassified";
        if (openedByApplink) {
            openType = "Applink";
        }

        if (callingApplicationPackage != null) {
            return openType + "(" + callingApplicationPackage + ")";
        }
        return openType;
    }

    public void writeSourceApplicationInfoToDisk() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(
                        FacebookSdk.getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(CALL_APPLICATION_PACKAGE_KEY, this.callingApplicationPackage);
        editor.putBoolean(OPENED_BY_APP_LINK_KEY, this.openedByApplink);
        editor.apply();
    }

    public static class Factory {
        public static SourceApplicationInfo create(Activity activity) {
            boolean openedByApplink = false;

            ComponentName callingApplication = activity.getCallingActivity();
            if (callingApplication == null) {
                return null;
            }

            String callingApplicationPackage = callingApplication.getPackageName();
            if (callingApplicationPackage.equals(activity.getPackageName())) {
                // opened by own app.
                return null;
            }

            // Tap icon to open an app will still get the old intent if the activity was opened by
            // an intent before. Introduce an extra field in the intent to force clear the
            // sourceApplication.
            Intent openIntent = activity.getIntent();
            if (openIntent != null
                && !openIntent.getBooleanExtra(
                    SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT,
                    false)) {
                openIntent.putExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, true);
                Bundle applinkData = AppLinks.getAppLinkData(openIntent);
                if (applinkData != null) {
                    openedByApplink = true;
                    Bundle applinkReferrerData = applinkData.getBundle("referer_app_link");
                    if (applinkReferrerData != null) {
                        String applinkReferrerPackage = applinkReferrerData.getString("package");
                        callingApplicationPackage = applinkReferrerPackage;
                    }
                }
            }

            // Mark this intent has been used to avoid use this intent again and again.
            openIntent.putExtra(SOURCE_APPLICATION_HAS_BEEN_SET_BY_THIS_INTENT, true);
            return new SourceApplicationInfo(callingApplicationPackage, openedByApplink);
        }
    }
}
