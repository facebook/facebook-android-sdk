package com.facebook.appevents;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class RestrictiveDataManager {

    private static final String TAG = RestrictiveDataManager.class.getCanonicalName();
    private static List<RestrictiveParam> restrictiveParams = new ArrayList<>();
    private static Set<String> restrictiveEvents = new HashSet<>();

    public static synchronized void updateFromSetting(String eventFilterResponse) {
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

    static boolean isDeprecatedEvent(String eventName) {
        return restrictiveEvents.contains(eventName);
    }

    @Nullable
    static String getMatchedRuleType(String eventName, String paramKey, String paramVal) {
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
