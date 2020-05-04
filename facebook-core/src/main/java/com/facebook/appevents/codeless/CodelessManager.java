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

package com.facebook.appevents.codeless;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.core.BuildConfig;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class CodelessManager {
    private static final ViewIndexingTrigger viewIndexingTrigger = new ViewIndexingTrigger();

    @Nullable private static SensorManager sensorManager;
    @Nullable private static ViewIndexer viewIndexer;
    @Nullable private static String deviceSessionID;
    private static final AtomicBoolean isCodelessEnabled = new AtomicBoolean(true);
    private static Boolean isAppIndexingEnabled = false;
    private static volatile Boolean isCheckingSession = false;

    public static void onActivityResumed(final Activity activity) {
        if (!isCodelessEnabled.get()) {
            return;
        }

        CodelessMatcher.getInstance().add(activity);

        final Context applicationContext = activity.getApplicationContext();
        final String appId = FacebookSdk.getApplicationId();
        final FetchedAppSettings appSettings =
                FetchedAppSettingsManager.getAppSettingsWithoutQuery(appId);
        if ((appSettings != null && appSettings.getCodelessEventsEnabled()) ||
                (BuildConfig.DEBUG && AppEventUtility.isEmulator())) {
            sensorManager = (SensorManager) applicationContext.
                    getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) {
                return;
            }

            Sensor accelerometer = sensorManager
                    .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            viewIndexer = new ViewIndexer(activity);
            viewIndexingTrigger.setOnShakeListener(
                    new ViewIndexingTrigger.OnShakeListener() {
                        @Override
                        public void onShake() {
                            boolean codelessEventsEnabled = appSettings != null &&
                                    appSettings.getCodelessEventsEnabled();
                            boolean codelessSetupEnabled = FacebookSdk.getCodelessSetupEnabled() ||
                                    (BuildConfig.DEBUG && AppEventUtility.isEmulator());
                            if (codelessEventsEnabled && codelessSetupEnabled) {
                                checkCodelessSession(appId);
                            }
                        }
                    });
            sensorManager.registerListener(
                    viewIndexingTrigger, accelerometer, SensorManager.SENSOR_DELAY_UI);

            if (appSettings != null && appSettings.getCodelessEventsEnabled()) {
                viewIndexer.schedule();
            }
        }

        if (BuildConfig.DEBUG
                && AppEventUtility.isEmulator()
                && !isAppIndexingEnabled) {
            // Check session on start when app launched
            // on emulator and built in DEBUG mode
            checkCodelessSession(appId);
        }
    }

    public static void onActivityPaused(final Activity activity) {
        if (!isCodelessEnabled.get()) {
            return;
        }

        CodelessMatcher.getInstance().remove(activity);

        if (null != viewIndexer) {
            viewIndexer.unschedule();
        }
        if (null != sensorManager) {
            sensorManager.unregisterListener(viewIndexingTrigger);
        }
    }

    public static void onActivityDestroyed(Activity activity) {
        CodelessMatcher.getInstance().destroy(activity);
    }

    public static void enable() {
        isCodelessEnabled.set(true);
    }

    public static void disable() {
        isCodelessEnabled.set(false);
    }

    private static void checkCodelessSession(final String applicationId) {
        if (isCheckingSession) {
            return;
        }
        isCheckingSession = true;

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final GraphRequest request = GraphRequest.newPostRequest(
                        null,
                        String.format(Locale.US, "%s/app_indexing_session", applicationId),
                        null,
                        null);

                Bundle requestParameters = request.getParameters();
                if (requestParameters == null) {
                    requestParameters = new Bundle();
                }

                final Context context = FacebookSdk.getApplicationContext();
                AttributionIdentifiers identifiers =
                        AttributionIdentifiers.getAttributionIdentifiers(context);

                JSONArray extInfoArray = new JSONArray();
                extInfoArray.put(Build.MODEL != null ? Build.MODEL : "");
                if (identifiers != null && identifiers.getAndroidAdvertiserId() != null) {
                    extInfoArray.put(identifiers.getAndroidAdvertiserId());
                } else {
                    extInfoArray.put("");
                }
                extInfoArray.put(BuildConfig.DEBUG ? "1" : "0");
                extInfoArray.put(AppEventUtility.isEmulator() ? "1" : "0");
                // Locale
                Locale locale = Utility.getCurrentLocale();
                extInfoArray.put(locale.getLanguage() + "_" + locale.getCountry());
                String extInfo = extInfoArray.toString();
                requestParameters.putString(com.facebook.appevents.codeless.internal.Constants.DEVICE_SESSION_ID,
                        getCurrentDeviceSessionID());
                requestParameters.putString(com.facebook.appevents.codeless.internal.Constants.EXTINFO, extInfo);
                request.setParameters(requestParameters);

                GraphResponse res = request.executeAndWait();
                JSONObject jsonRes = res.getJSONObject();
                isAppIndexingEnabled = jsonRes != null
                        && jsonRes.optBoolean(com.facebook.appevents.codeless.internal.Constants.APP_INDEXING_ENABLED, false);
                if (!isAppIndexingEnabled) {
                    deviceSessionID = null;
                } else {
                    if (null != viewIndexer) {
                        viewIndexer.schedule();
                    }
                }

                isCheckingSession = false;
            }
        });
    }

    static String getCurrentDeviceSessionID() {
        if (null == deviceSessionID) {
            deviceSessionID = UUID.randomUUID().toString();
        }

        return deviceSessionID;
    }

    static boolean getIsAppIndexingEnabled() {
        return isAppIndexingEnabled;
    }

    static void updateAppIndexing(Boolean appIndexingEnalbed) {
        isAppIndexingEnabled = appIndexingEnalbed;
    }
}
