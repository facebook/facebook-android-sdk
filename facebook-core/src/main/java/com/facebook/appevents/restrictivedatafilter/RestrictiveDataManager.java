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

package com.facebook.appevents.restrictivedatafilter;

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RestrictiveDataManager {

    private static boolean enabled = false;
    private static final String TAG = RestrictiveDataManager.class.getCanonicalName();
    private static final List<RestrictiveParamFilter> restrictiveParamFilters = new ArrayList<>();

    public static void enable() {
        enabled = true;
        initialize();
    }

    private static void initialize() {
        try {
            FetchedAppSettings settings = FetchedAppSettingsManager.queryAppSettings(
                    FacebookSdk.getApplicationId(), false);
            if (settings == null) {
                return;
            }
            String eventFilterResponse = settings.getRestrictiveDataSetting();
            if (eventFilterResponse == null) {
                return;
            }

            if (!eventFilterResponse.isEmpty()) {
                JSONObject jsonObject = new JSONObject(eventFilterResponse);

                restrictiveParamFilters.clear();

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject json = jsonObject.getJSONObject(key);
                    if (json != null) {
                        JSONObject restrictiveParamJson = json.optJSONObject("restrictive_param");
                        RestrictiveParamFilter restrictiveParamFilter
                                = new RestrictiveParamFilter(key, new HashMap<String, String>());
                        if (restrictiveParamJson != null) {
                            restrictiveParamFilter.restrictiveParams = Utility
                                    .convertJSONObjectToStringMap(restrictiveParamJson);
                            restrictiveParamFilters.add(restrictiveParamFilter);
                        }
                    }
                }
            }
        } catch (Exception e) {
            /* swallow */
        }
    }

    public static void processParameters(Map<String, String> parameters, String eventName) {
        if (!enabled) {
            return;
        }

        Map<String, String> restrictedParams = new HashMap<>();
        List<String> keys = new ArrayList<>(parameters.keySet());

        for (String key : keys) {
            String type = RestrictiveDataManager.getMatchedRuleType(eventName, key);
            if (type != null) {
                restrictedParams.put(key, type);
                parameters.remove(key);
            }
        }

        if (restrictedParams.size() > 0) {
            try {
                JSONObject restrictedJSON = new JSONObject();
                for (Map.Entry<String, String> entry : restrictedParams.entrySet()) {
                    restrictedJSON.put(entry.getKey(), entry.getValue());
                }

                parameters.put("_restrictedParams", restrictedJSON.toString());
            } catch (JSONException e) {
                /* swallow */
            }
        }
    }

    @Nullable
    private static String getMatchedRuleType(String eventName, String paramKey) {
        try {
            List<RestrictiveParamFilter> restrictiveParamFiltersCopy = new
                    ArrayList<>(restrictiveParamFilters);
            for (RestrictiveParamFilter filter : restrictiveParamFiltersCopy) {
                if (filter == null) { // sanity check
                    continue;
                }

                if (eventName.equals(filter.eventName)) {
                    for (String param : filter.restrictiveParams.keySet()) {
                        if (paramKey.equals(param)) {
                            return filter.restrictiveParams.get(param);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getMatchedRuleType failed", e);
        }
        return null;
    }

    static class RestrictiveParamFilter {
        String eventName;
        Map<String, String> restrictiveParams;

        RestrictiveParamFilter(String eventName, Map<String, String> restrictiveParams) {
            this.eventName = eventName;
            this.restrictiveParams = restrictiveParams;
        }
    }
}
