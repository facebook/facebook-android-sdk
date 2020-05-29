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

package com.facebook.internal;

import static com.facebook.FacebookSdk.APP_EVENT_PREFERENCES;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.facebook.FacebookSdk;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

@AutoHandleExceptions
public class InstallReferrerUtil {

  private static final String IS_REFERRER_UPDATED = "is_referrer_updated";

  private InstallReferrerUtil() {}

  public static void tryUpdateReferrerInfo(Callback callback) {
    if (!isUpdated()) {
      tryConnectReferrerInfo(callback);
    }
  }

  private static void tryConnectReferrerInfo(final Callback callback) {
    final InstallReferrerClient referrerClient =
        InstallReferrerClient.newBuilder(FacebookSdk.getApplicationContext()).build();
    InstallReferrerStateListener installReferrerStateListener =
        new InstallReferrerStateListener() {
          @Override
          public void onInstallReferrerSetupFinished(int responseCode) {
            switch (responseCode) {
              case InstallReferrerClient.InstallReferrerResponse.OK:
                ReferrerDetails response;
                try {
                  response = referrerClient.getInstallReferrer();
                } catch (RemoteException e) {
                  return;
                }

                String referrerUrl = response.getInstallReferrer();
                if (referrerUrl != null
                    && (referrerUrl.contains("fb") || referrerUrl.contains("facebook"))) {
                  callback.onReceiveReferrerUrl(referrerUrl);
                }
                updateReferrer(); // even if we are not interested in the url, there is no reason to
                // ask for it again
                break;

              case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                updateReferrer(); // No point retrying if feature not supported
                break;
              case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                break;
            }
          }

          @Override
          public void onInstallReferrerServiceDisconnected() {}
        };

    try {
      referrerClient.startConnection(installReferrerStateListener);
    } catch (Exception e) {
      return;
    }
  }

  private static void updateReferrer() {
    SharedPreferences preferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
    preferences.edit().putBoolean(IS_REFERRER_UPDATED, true).apply();
  }

  private static boolean isUpdated() {
    SharedPreferences preferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(APP_EVENT_PREFERENCES, Context.MODE_PRIVATE);
    return preferences.getBoolean(IS_REFERRER_UPDATED, false);
  }

  public interface Callback {
    void onReceiveReferrerUrl(String s);
  }
}
