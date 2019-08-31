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

import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

import com.facebook.appevents.AppEvent;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RestrictiveDataManager {

    private static boolean enabled = false;
    private static final String TAG = RestrictiveDataManager.class.getCanonicalName();
    private static List<RestrictiveParam> restrictiveParams = new ArrayList<>();
    private static Set<String> restrictiveEvents = new HashSet<>();

    public static void enable() {
        enabled = true;
    }

    public static synchronized void updateFromSetting(String eventFilterResponse) {
        if (!enabled) {
            return;
        }

        try {
            if (!eventFilterResponse.isEmpty()) {
                JSONObject jsonObject = new JSONObject(eventFilterResponse);

                restrictiveParams.clear();
                restrictiveEvents.clear();

                Iterator<String> keys = jsonObject.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    JSONObject json = jsonObject.getJSONObject(key);
                    if (json != null) {
                        if (json.optBoolean("is_deprecated_event")) {
                            restrictiveEvents.add(key);
                        } else {
                            JSONObject paramJson = jsonObject.getJSONObject(key).optJSONObject("restrictive_param");
                            if (paramJson != null) {
                                restrictiveParams.add(
                                        new RestrictiveParam(key, Utility.convertJSONObjectToStringMap(paramJson)));
                            }
                        }
                    }
                }
            }
        } catch (JSONException je) {
            Log.w(TAG, "updateRulesFromSetting failed", je);
        } catch (Exception e) {
            Log.w(TAG, "updateFromSetting failed", e);
        }
    }

    public static void processEvents(List<AppEvent> events) {
        if (!enabled) {
            return;
        }

        Iterator<AppEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            AppEvent event = iterator.next();
            if (RestrictiveDataManager.isDeprecatedEvent(event.getName())) {
                iterator.remove();
            }
        }
    }

    public static void processParameters(Map<String, String> parameters, String eventName) {
        if (!enabled) {
            return;
        }

        Map<String, String> restrictedParams = new HashMap<>();
        ArrayList<String> keys = new ArrayList<>(parameters.keySet());

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
                for (Map.Entry<String, String> entry: restrictedParams.entrySet()) {
                    restrictedJSON.put(entry.getKey(), entry.getValue());
                }

                parameters.put("_restrictedParams", restrictedJSON.toString());
            } catch (JSONException e) {
                Log.w(TAG, "processParameters failed", e);
            }
        }
    }

    private static boolean isDeprecatedEvent(String eventName) {
        return restrictiveEvents.contains(eventName);
    }

    @Nullable
    private static String getMatchedRuleType(String eventName, String paramKey) {
        try {
            ArrayList<RestrictiveParam> restrictiveParamsCopy = new ArrayList<>(restrictiveParams);
            for (RestrictiveParam filter : restrictiveParamsCopy) {
                if (filter == null) { // sanity check
                    continue;
                }

                if (eventName.equals(filter.eventName)) {
                    for (String param : filter.params.keySet()) {
                        if (paramKey.equals(param)) {
                            return filter.params.get(param);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getMatchedRuleType failed", e);
        }

        return null;
    }

    static class RestrictiveParam {
        String eventName;
        Map<String, String> params;

        RestrictiveParam(String eventName, Map<String, String> params) {
            this.eventName = eventName;
            this.params = params;
        }
    }
}
