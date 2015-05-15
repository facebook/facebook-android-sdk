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

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Looper;
import android.util.Log;

import com.facebook.FacebookException;

import java.lang.reflect.Method;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class AttributionIdentifiers {
    private static final String TAG = AttributionIdentifiers.class.getCanonicalName();
    private static final String ATTRIBUTION_ID_CONTENT_PROVIDER =
            "com.facebook.katana.provider.AttributionIdProvider";
    private static final String ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI =
            "com.facebook.wakizashi.provider.AttributionIdProvider";
    private static final String ATTRIBUTION_ID_COLUMN_NAME = "aid";
    private static final String ANDROID_ID_COLUMN_NAME = "androidid";
    private static final String LIMIT_TRACKING_COLUMN_NAME = "limit_tracking";

    // com.google.android.gms.common.ConnectionResult.SUCCESS
    private static final int CONNECTION_RESULT_SUCCESS = 0;

    private static final long IDENTIFIER_REFRESH_INTERVAL_MILLIS = 3600 * 1000;

    private String attributionId;
    private String androidAdvertiserId;
    private boolean limitTracking;
    private long fetchTime;

    private static AttributionIdentifiers recentlyFetchedIdentifiers;

    private static AttributionIdentifiers getAndroidId(Context context) {
        AttributionIdentifiers identifiers = new AttributionIdentifiers();
        try {
            // We can't call getAdvertisingIdInfo on the main thread or the app will potentially
            // freeze, if this is the case throw:
            if (Looper.myLooper() == Looper.getMainLooper()) {
              throw new FacebookException("getAndroidId cannot be called on the main thread.");
            }
            Method isGooglePlayServicesAvailable = Utility.getMethodQuietly(
                    "com.google.android.gms.common.GooglePlayServicesUtil",
                    "isGooglePlayServicesAvailable",
                    Context.class
            );

            if (isGooglePlayServicesAvailable == null) {
                return identifiers;
            }

            Object connectionResult = Utility.invokeMethodQuietly(
                    null, isGooglePlayServicesAvailable, context);
            if (!(connectionResult instanceof Integer)
                    || (Integer) connectionResult != CONNECTION_RESULT_SUCCESS) {
                return identifiers;
            }

            Method getAdvertisingIdInfo = Utility.getMethodQuietly(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                    "getAdvertisingIdInfo",
                    Context.class
            );
            if (getAdvertisingIdInfo == null) {
                return identifiers;
            }
            Object advertisingInfo = Utility.invokeMethodQuietly(
                    null, getAdvertisingIdInfo, context);
            if (advertisingInfo == null) {
                return identifiers;
            }

            Method getId = Utility.getMethodQuietly(advertisingInfo.getClass(), "getId");
            Method isLimitAdTrackingEnabled = Utility.getMethodQuietly(
                    advertisingInfo.getClass(),
                    "isLimitAdTrackingEnabled");
            if (getId == null || isLimitAdTrackingEnabled == null) {
                return identifiers;
            }

            identifiers.androidAdvertiserId =
                    (String) Utility.invokeMethodQuietly(advertisingInfo, getId);
            identifiers.limitTracking = (Boolean) Utility.invokeMethodQuietly(
                    advertisingInfo,
                    isLimitAdTrackingEnabled);
        } catch (Exception e) {
            Utility.logd("android_id", e);
        }
        return identifiers;
    }

    public static AttributionIdentifiers getAttributionIdentifiers(Context context) {
        if (recentlyFetchedIdentifiers != null &&
            System.currentTimeMillis() - recentlyFetchedIdentifiers.fetchTime <
                    IDENTIFIER_REFRESH_INTERVAL_MILLIS) {
            return recentlyFetchedIdentifiers;
        }

        AttributionIdentifiers identifiers = getAndroidId(context);
        Cursor c = null;
        try {
            String [] projection = {
                    ATTRIBUTION_ID_COLUMN_NAME,
                    ANDROID_ID_COLUMN_NAME,
                    LIMIT_TRACKING_COLUMN_NAME};
            Uri providerUri = null;
            if (context.getPackageManager().resolveContentProvider(
                    ATTRIBUTION_ID_CONTENT_PROVIDER, 0) != null) {
                providerUri = Uri.parse("content://" + ATTRIBUTION_ID_CONTENT_PROVIDER);
            } else if (context.getPackageManager().resolveContentProvider(
                    ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI, 0) != null) {
                providerUri = Uri.parse("content://" + ATTRIBUTION_ID_CONTENT_PROVIDER_WAKIZASHI);
            }
            if (providerUri == null) {
                return identifiers;
            }
            c = context.getContentResolver().query(providerUri, projection, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return identifiers;
            }
            int attributionColumnIndex = c.getColumnIndex(ATTRIBUTION_ID_COLUMN_NAME);
            int androidIdColumnIndex = c.getColumnIndex(ANDROID_ID_COLUMN_NAME);
            int limitTrackingColumnIndex = c.getColumnIndex(LIMIT_TRACKING_COLUMN_NAME);

            identifiers.attributionId = c.getString(attributionColumnIndex);

            // if we failed to call Google's APIs directly (due to improper integration by the
            // client), it may be possible for the local facebook application to relay it to us.
            if (androidIdColumnIndex > 0 && limitTrackingColumnIndex > 0 &&
                    identifiers.getAndroidAdvertiserId() == null) {
                identifiers.androidAdvertiserId = c.getString(androidIdColumnIndex);
                identifiers.limitTracking =
                        Boolean.parseBoolean(c.getString(limitTrackingColumnIndex));
            }
        } catch (Exception e) {
            Log.d(TAG, "Caught unexpected exception in getAttributionId(): " + e.toString());
            return null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        identifiers.fetchTime = System.currentTimeMillis();
        recentlyFetchedIdentifiers = identifiers;
        return identifiers;
    }

    public String getAttributionId() {
        return attributionId;
    }

    public String getAndroidAdvertiserId() {
        return androidAdvertiserId;
    }

    public boolean isTrackingLimited() {
        return limitTracking;
    }
}
