package com.facebook.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.facebook.FacebookSdk;

import static com.facebook.FacebookSdk.APP_EVENT_PREFERENCES;

public class InstallReferrerUtil {

    private static final String TAG = InstallReferrerUtil.class.getSimpleName();

    private static final String IS_REFERRER_UPDATED = "is_referrer_updated";

    private InstallReferrerUtil() {}

    public static void tryUpdateReferrerInfo(Callback callback) {
        if (!isUpdated()) {
            tryConnectReferrerInfo(callback);
        }
    }

    private static void tryConnectReferrerInfo(final Callback callback) {

        try
        {
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(FacebookSdk.getApplicationContext()).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            ReferrerDetails response;
                            try {
                                response = referrerClient.getInstallReferrer();
                            } catch (Exception e) {
                                return;
                            }

                            String referrerUrl = response.getInstallReferrer();
                            if (referrerUrl != null && (referrerUrl.contains("fb") || referrerUrl.contains("facebook"))) {
                                callback.onReceiveReferrerUrl(referrerUrl);
                            }
                            updateReferrer(); //even if we are not interested in the url, there is no reason to ask for it again
                            break;

                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            updateReferrer(); //No point retrying if feature not supported
                            break;
                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                }
            });
        }
        catch(Throwable ex) {
            Log.e(TAG,"InstallReferrerClient thrown exception" + ex.getMessage());
        }
    }

    private static void updateReferrer() {
        SharedPreferences preferences = FacebookSdk.getApplicationContext().getSharedPreferences(
                APP_EVENT_PREFERENCES,
                Context.MODE_PRIVATE);
        preferences.edit().putBoolean(IS_REFERRER_UPDATED, true).apply();
    }

    private static boolean isUpdated() {
        SharedPreferences preferences = FacebookSdk.getApplicationContext().getSharedPreferences(
                APP_EVENT_PREFERENCES,
                Context.MODE_PRIVATE);
        return preferences.getBoolean(IS_REFERRER_UPDATED, false);
    }

    public interface Callback {
        void onReceiveReferrerUrl(String s);
    }
}
