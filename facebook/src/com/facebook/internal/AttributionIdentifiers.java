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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.FacebookException;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private String androidInstallerPackage;
    private boolean limitTracking;
    private long fetchTime;

    private static AttributionIdentifiers recentlyFetchedIdentifiers;

    private static AttributionIdentifiers getAndroidId(Context context) {
        AttributionIdentifiers identifiers = getAndroidIdViaReflection(context);
        if (identifiers == null) {
            identifiers = getAndroidIdViaService(context);
            if (identifiers == null) {
                identifiers = new AttributionIdentifiers();
            }
        }
        return identifiers;
    }

    private static AttributionIdentifiers getAndroidIdViaReflection(Context context) {
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
                return null;
            }

            Object connectionResult = Utility.invokeMethodQuietly(
                    null, isGooglePlayServicesAvailable, context);
            if (!(connectionResult instanceof Integer)
                    || (Integer) connectionResult != CONNECTION_RESULT_SUCCESS) {
                return null;
            }

            Method getAdvertisingIdInfo = Utility.getMethodQuietly(
                    "com.google.android.gms.ads.identifier.AdvertisingIdClient",
                    "getAdvertisingIdInfo",
                    Context.class
            );
            if (getAdvertisingIdInfo == null) {
                return null;
            }
            Object advertisingInfo = Utility.invokeMethodQuietly(
                    null, getAdvertisingIdInfo, context);
            if (advertisingInfo == null) {
                return null;
            }

            Method getId = Utility.getMethodQuietly(advertisingInfo.getClass(), "getId");
            Method isLimitAdTrackingEnabled = Utility.getMethodQuietly(
                    advertisingInfo.getClass(),
                    "isLimitAdTrackingEnabled");
            if (getId == null || isLimitAdTrackingEnabled == null) {
                return null;
            }

            AttributionIdentifiers identifiers = new AttributionIdentifiers();
            identifiers.androidAdvertiserId =
                    (String) Utility.invokeMethodQuietly(advertisingInfo, getId);
            identifiers.limitTracking = (Boolean) Utility.invokeMethodQuietly(
                    advertisingInfo,
                    isLimitAdTrackingEnabled);
        } catch (Exception e) {
            Utility.logd("android_id", e);
        }
        return null;
    }

    private static AttributionIdentifiers getAndroidIdViaService(Context context) {
        GoogleAdServiceConnection connection = new GoogleAdServiceConnection();
        Intent intent = new Intent("com.google.android.gms.ads.identifier.service.START");
        intent.setPackage("com.google.android.gms");
        if(context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            try {
                GoogleAdInfo adInfo = new GoogleAdInfo(connection.getBinder());
                AttributionIdentifiers identifiers = new AttributionIdentifiers();
                identifiers.androidAdvertiserId = adInfo.getAdvertiserId();
                identifiers.limitTracking = adInfo.isTrackingLimited();
                return identifiers;
            } catch (Exception exception) {
                Utility.logd("android_id", exception);
            } finally {
                context.unbindService(connection);
            }
        }
        return null;
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
            String installerPackageName = getInstallerPackageName(context);
            if (installerPackageName != null) {
                identifiers.androidInstallerPackage = installerPackageName;
            }
            if (providerUri == null) {
                return cacheAndReturnIdentifiers(identifiers);
            }
            c = context.getContentResolver().query(providerUri, projection, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return cacheAndReturnIdentifiers(identifiers);
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
        return cacheAndReturnIdentifiers(identifiers);
    }

    private static AttributionIdentifiers cacheAndReturnIdentifiers(
            AttributionIdentifiers identifiers) {
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

    public String getAndroidInstallerPackage() {
        return androidInstallerPackage;
    }

    public boolean isTrackingLimited() {
        return limitTracking;
    }

    @Nullable
    private static String getInstallerPackageName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            return packageManager.getInstallerPackageName(context.getPackageName());
        }
        return null;
    }

    private static final class GoogleAdServiceConnection implements ServiceConnection {
        private AtomicBoolean consumed = new AtomicBoolean(false);
        private final BlockingQueue<IBinder> queue = new LinkedBlockingDeque<>();

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                queue.put(service);
            } catch (InterruptedException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        public IBinder getBinder() throws InterruptedException {
            if (consumed.compareAndSet(true, true)) {
                throw new IllegalStateException("Binder already consumed");
            }
            return queue.take();
        }
    }

    private static final class GoogleAdInfo implements IInterface {
        private static final int FIRST_TRANSACTION_CODE = Binder.FIRST_CALL_TRANSACTION;
        private static final int SECOND_TRANSACTION_CODE = FIRST_TRANSACTION_CODE + 1;

        private IBinder binder;

        GoogleAdInfo(IBinder binder) {
            this.binder = binder;
        }

        @Override
        public IBinder asBinder() {
            return binder;
        }

        public String getAdvertiserId() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            String id;
            try {
                data.writeInterfaceToken(
                        "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                binder.transact(FIRST_TRANSACTION_CODE, data, reply, 0);
                reply.readException();
                id = reply.readString();
            } finally {
                reply.recycle();
                data.recycle();
            }
            return id;
        }

        public boolean isTrackingLimited() throws RemoteException {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            boolean limitAdTracking;
            try {
                data.writeInterfaceToken(
                        "com.google.android.gms.ads.identifier.internal.IAdvertisingIdService");
                data.writeInt(1);
                binder.transact(SECOND_TRANSACTION_CODE, data, reply, 0);
                reply.readException();
                limitAdTracking = 0 != reply.readInt();
            } finally {
                reply.recycle();
                data.recycle();
            }
            return limitAdTracking;
        }
    }
}
