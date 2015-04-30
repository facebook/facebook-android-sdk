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

package com.facebook.internal;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import com.facebook.FacebookActivity;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import com.facebook.LoggingBehavior;

import java.util.Collection;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public final class Validate {

    private static final String TAG = Validate.class.getName();

    private static final String NO_INTERNET_PERMISSION_REASON =
            "No internet permissions granted for the app, please add " +
            "<uses-permission android:name=\"android.permission.INTERNET\" /> " +
            "to your AndroidManifest.xml.";

    private static final String FACEBOOK_ACTIVITY_NOT_FOUND_REASON =
            "FacebookActivity is not declared in the AndroidManifest.xml, please add " +
            "com.facebook.FacebookActivity to your AndroidManifest.xml file. See " +
            "https://developers.facebook.com/docs/android/getting-started for more info.";

    private static final String CONTENT_PROVIDER_NOT_FOUND_REASON =
            "A ContentProvider for this app was not set up in the AndroidManifest.xml, please " +
            "add %s as a provider to your AndroidManifest.xml file. See " +
            "https://developers.facebook.com/docs/sharing/android for more info.";

    private static final String CONTENT_PROVIDER_BASE = "com.facebook.app.FacebookContentProvider";

    public static void notNull(Object arg, String name) {
        if (arg == null) {
            throw new NullPointerException("Argument '" + name + "' cannot be null");
        }
    }

    public static <T> void notEmpty(Collection<T> container, String name) {
        if (container.isEmpty()) {
            throw new IllegalArgumentException("Container '" + name + "' cannot be empty");
        }
    }

    public static <T> void containsNoNulls(Collection<T> container, String name) {
        Validate.notNull(container, name);
        for (T item : container) {
            if (item == null) {
                throw new NullPointerException("Container '" + name +
                        "' cannot contain null values");
            }
        }
    }

    public static void containsNoNullOrEmpty(Collection<String> container, String name) {
        Validate.notNull(container, name);
        for (String item : container) {
            if (item == null) {
                throw new NullPointerException("Container '" + name +
                        "' cannot contain null values");
            }
            if (item.length() == 0) {
                throw new IllegalArgumentException("Container '" + name +
                        "' cannot contain empty values");
            }
        }
    }

    public static <T> void notEmptyAndContainsNoNulls(Collection<T> container, String name) {
        Validate.containsNoNulls(container, name);
        Validate.notEmpty(container, name);
    }

    public static void runningOnUiThread() {
        if (!Looper.getMainLooper().equals(Looper.myLooper())) {
            throw new FacebookException("This method should be called from the UI thread");
        }
    }

    public static void notNullOrEmpty(String arg, String name) {
        if (Utility.isNullOrEmpty(arg)) {
            throw new IllegalArgumentException("Argument '" + name + "' cannot be null or empty");
        }
    }

    public static void oneOf(Object arg, String name, Object... values) {
        for (Object value : values) {
            if (value != null) {
                if (value.equals(arg)) {
                    return;
                }
            } else {
                if (arg == null) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Argument '" + name +
                "' was not one of the allowed values");
    }

    public static void sdkInitialized() {
        if (!FacebookSdk.isInitialized()) {
            throw new FacebookSdkNotInitializedException(
                    "The SDK has not been initialized, make sure to call " +
                    "FacebookSdk.sdkInitialize() first.");
        }
    }

    public static String hasAppID() {
        String id = FacebookSdk.getApplicationId();
        if (id == null) {
            throw new IllegalStateException("No App ID found, please set the App ID.");
        }
        return id;
    }

    public static void hasInternetPermissions(Context context) {
        Validate.hasInternetPermissions(context, true);
    }

    public static void hasInternetPermissions(Context context, boolean shouldThrow) {
        Validate.notNull(context, "context");
        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_DENIED) {
            if (shouldThrow) {
                throw new IllegalStateException(NO_INTERNET_PERMISSION_REASON);
            } else {
                Log.w(TAG, NO_INTERNET_PERMISSION_REASON);
            }
        }
    }

    public static void hasFacebookActivity(Context context) {
        Validate.hasFacebookActivity(context, true);
    }

    public static void hasFacebookActivity(Context context, boolean shouldThrow) {
        Validate.notNull(context, "context");
        PackageManager pm = context.getPackageManager();
        ActivityInfo activityInfo = null;
        if (pm != null) {
            ComponentName componentName =
                    new ComponentName(context, FacebookActivity.class);
            try {
                activityInfo = pm.getActivityInfo(componentName, PackageManager.GET_ACTIVITIES);
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        if (activityInfo == null) {
            if (shouldThrow) {
                throw new IllegalStateException(FACEBOOK_ACTIVITY_NOT_FOUND_REASON);
            } else {
                Log.w(TAG, FACEBOOK_ACTIVITY_NOT_FOUND_REASON);
            }
        }
    }

    public static void hasContentProvider(Context context) {
        Validate.notNull(context, "context");
        String appId = Validate.hasAppID();
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            String providerName = CONTENT_PROVIDER_BASE + appId;
            if (pm.resolveContentProvider(providerName, 0) == null) {
                throw new IllegalStateException(
                        String.format(CONTENT_PROVIDER_NOT_FOUND_REASON, providerName));
            }
        }
    }
}
