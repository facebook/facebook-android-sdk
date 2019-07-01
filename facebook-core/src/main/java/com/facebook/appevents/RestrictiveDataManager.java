package com.facebook.appevents;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public final class RestrictiveDataManager {

    private static final String TAG = RestrictiveDataManager.class.getCanonicalName();
    private static List<RestrictiveRule> restrictiveRules = new ArrayList<>();
    private static List<RestrictiveParam> restrictiveParams = new ArrayList<>();
    private static Set<String> restrictiveEvents = new HashSet<>();

    public static synchronized void updateFromSetting(
            @NonNull String ruleResponse, @NonNull String eventFilterResponse) {
        try {
            // update restrictive rules
            if (!ruleResponse.isEmpty()) {
                JSONArray jsonArray = new JSONArray(ruleResponse);

                restrictiveRules.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                String keyRegex = object.optString("key_regex");
                String valRagex = object.optString("value_regex");
                String valNegRagex = object.optString("value_negative_regex");
                String type = object.optString("type");

                if (Utility.isNullOrEmpty(keyRegex)
                        && Utility.isNullOrEmpty(valRagex)
                        && Utility.isNullOrEmpty(valNegRagex)) {
                    continue;
                }

                restrictiveRules.add(new RestrictiveRule(
                        keyRegex, valRagex, valNegRagex, type));
                }
            }

            // update restrictive event filters
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
    static String getMatchedRuleType(@NonNull String eventName, @NonNull String paramKey, @NonNull String paramVal) {
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

            ArrayList<RestrictiveRule> restrictiveRulesCopy = new ArrayList<>(restrictiveRules);
            for (RestrictiveRule rule : restrictiveRulesCopy) {
                if (rule == null) { // sanity check
                    continue;
                }

                // not matched to key
                if (!Utility.isNullOrEmpty(rule.keyRegex) && !paramKey.matches(rule.keyRegex)) {
                    continue;
                }
                // matched to neg val
                if (!Utility.isNullOrEmpty(rule.valNegRegex) && paramVal.matches(rule.valNegRegex)) {
                    continue;
                }
                // not matched to val
                if (!Utility.isNullOrEmpty(rule.valRegex) && !paramVal.matches(rule.valRegex)) {
                    continue;
                }
                return rule.type;
            }
        } catch (Exception e) {
            Log.w(TAG, "getMatchedRuleType failed", e);
        }

        return null;
    }

    static class RestrictiveRule {
        String keyRegex;
        String valRegex;
        String valNegRegex;
        String type;

        RestrictiveRule(String keyRegex, String valRegex, String valNegRegex, String type) {
            this.keyRegex = keyRegex;
            this.valRegex = valRegex;
            this.valNegRegex = valNegRegex;
            this.type = type;
        }
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
