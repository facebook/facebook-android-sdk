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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class FetchedAppGateKeepersManager {
    private static final String TAG = FetchedAppGateKeepersManager.class.getCanonicalName();
    private static final String APP_GATEKEEPERS_PREFS_STORE =
            "com.facebook.internal.preferences.APP_GATEKEEPERS";
    private static final String APP_GATEKEEPERS_PREFS_KEY_FORMAT =
            "com.facebook.internal.APP_GATEKEEPERS.%s";
    private static final String APP_PLATFORM = "android";
    private static final String APPLICATION_GATEKEEPER_EDGE = "mobile_sdk_gk";
    private static final String APPLICATION_GATEKEEPER_FIELD = "gatekeepers";
    private static final String APPLICATION_GRAPH_DATA = "data";
    private static final String APPLICATION_FIELDS = "fields";
    private static final String APPLICATION_PLATFORM = "platform";
    private static final String APPLICATION_SDK_VERSION = "sdk_version";

    // GK names
    public static final String APP_EVENTS_IF_AUTO_LOG_SUBS = "app_events_if_auto_log_subs";
    public static final String APP_EVENTS_KILLSWITCH = "app_events_killswitch";

    private static final Map<String, JSONObject> fetchedAppGateKeepers =
            new ConcurrentHashMap<>();

    private static final long APPLICATION_GATEKEEPER_CACHE_TIMEOUT = 60 * 60 * 1000;
    private static @Nullable Long timestamp = null;

    public synchronized static void loadAppGateKeepersAsync() {
        if (isTimestampValid(timestamp)) {
            return;
        }

        final Context context = FacebookSdk.getApplicationContext();
        final String applicationId = FacebookSdk.getApplicationId();
        final String gateKeepersKey = String.format(APP_GATEKEEPERS_PREFS_KEY_FORMAT, applicationId);

        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                // See if we had a cached copy of gatekeepers and use that immediately
                SharedPreferences gateKeepersSharedPrefs = context.getSharedPreferences(
                        APP_GATEKEEPERS_PREFS_STORE,
                        Context.MODE_PRIVATE);
                String gateKeepersJSONString = gateKeepersSharedPrefs.getString(
                        gateKeepersKey,
                        null);

                if (!Utility.isNullOrEmpty(gateKeepersJSONString)) {
                    JSONObject gateKeepersJSON = null;
                    try {
                        gateKeepersJSON = new JSONObject(gateKeepersJSONString);
                    } catch (JSONException je) {
                        Utility.logd(Utility.LOG_TAG, je);
                    }
                    if (gateKeepersJSON != null) {
                        parseAppGateKeepersFromJSON(applicationId, gateKeepersJSON);
                    }
                }

                JSONObject gateKeepersResultJSON = getAppGateKeepersQueryResponse(applicationId);
                if (gateKeepersResultJSON != null) {
                    // Update timestamp only when the GateKeepers are successfully fetched
                    timestamp = System.currentTimeMillis();
                    parseAppGateKeepersFromJSON(applicationId, gateKeepersResultJSON);

                    gateKeepersSharedPrefs.edit()
                            .putString(gateKeepersKey, gateKeepersResultJSON.toString())
                            .apply();
                }
            }
        });
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread. This call can block for long time if network is not available and network
    // timeout is long.
    public static @Nullable JSONObject queryAppGateKeepers(
            final String applicationId,
            final boolean forceRequery) {
        // Cache the last app checked results.
        if (!forceRequery && fetchedAppGateKeepers.containsKey(applicationId)) {
            return fetchedAppGateKeepers.get(applicationId);
        }

        JSONObject response = getAppGateKeepersQueryResponse(applicationId);
        if (response == null) {
            return null;
        }

        final Context context = FacebookSdk.getApplicationContext();
        final String gateKeepersKey = String.format(APP_GATEKEEPERS_PREFS_KEY_FORMAT, applicationId);

        SharedPreferences gateKeepersSharedPrefs = context.getSharedPreferences(
                APP_GATEKEEPERS_PREFS_STORE,
                Context.MODE_PRIVATE);
        gateKeepersSharedPrefs.edit()
                .putString(gateKeepersKey, response.toString())
                .apply();

        return parseAppGateKeepersFromJSON(applicationId, response);
    }

    public static boolean getGateKeeperForKey(
            final String name,
            final String applicationId,
            final boolean defaultValue) {
        loadAppGateKeepersAsync();
        if (applicationId == null || !fetchedAppGateKeepers.containsKey(applicationId)) {
            return defaultValue;
        }
        return fetchedAppGateKeepers.get(applicationId).optBoolean(name, defaultValue);
    }

    // Note that this method makes a synchronous Graph API call, so should not be called from the
    // main thread.
    private static @Nullable JSONObject getAppGateKeepersQueryResponse(final String applicationId) {
        Bundle appGateKeepersParams = new Bundle();
        appGateKeepersParams.putString(APPLICATION_PLATFORM, APP_PLATFORM);
        appGateKeepersParams.putString(APPLICATION_SDK_VERSION, FacebookSdk.getSdkVersion());
        appGateKeepersParams.putString(APPLICATION_FIELDS, APPLICATION_GATEKEEPER_FIELD);

        GraphRequest request = GraphRequest.newGraphPathRequest(null,
                String.format("%s/%s", applicationId, APPLICATION_GATEKEEPER_EDGE),
                null);
        request.setSkipClientToken(true);
        request.setParameters(appGateKeepersParams);

        return request.executeAndWait().getJSONObject();
    }

    private synchronized static JSONObject parseAppGateKeepersFromJSON(
            final String applicationId,
            JSONObject gateKeepersJSON) {
        JSONObject result;
        if (fetchedAppGateKeepers.containsKey(applicationId)) {
            result = fetchedAppGateKeepers.get(applicationId);
        } else {
            result = new JSONObject();
        }
        JSONArray arr = gateKeepersJSON.optJSONArray(APPLICATION_GRAPH_DATA);
        JSONObject gateKeepers = null;
        if (arr != null) {
            gateKeepers = arr.optJSONObject(0);
        }
        // If there does exist a valid JSON object in arr, initialize result with this JSON object
        if (gateKeepers != null && gateKeepers.optJSONArray(APPLICATION_GATEKEEPER_FIELD) != null) {
            JSONArray data = gateKeepers.optJSONArray(APPLICATION_GATEKEEPER_FIELD);
            for (int i = 0; i < data.length(); i++) {
                try {
                    JSONObject gk = data.getJSONObject(i);
                    result.put(gk.getString("key"), gk.getBoolean("value"));
                } catch (JSONException je) {
                    Utility.logd(Utility.LOG_TAG, je);
                }
            }
        }

        fetchedAppGateKeepers.put(applicationId, result);
        return result;
    }

    private static boolean isTimestampValid(Long timestamp) {
        if (timestamp == null) {
            return false;
        }
        return System.currentTimeMillis() - timestamp < APPLICATION_GATEKEEPER_CACHE_TIMEOUT;
    }
}
