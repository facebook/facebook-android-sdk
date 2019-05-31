package com.facebook.appevents;

import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public final class RestrictiveParameterManager {

    private static final String TAG = RestrictiveParameterManager.class.getCanonicalName();
    private static List<RestrictiveRule> restrictiveRules = new ArrayList<>();
    private static List<RestrictiveEventFilter> restrictiveEventFilters = new ArrayList<>();

    public static void updateFromSetting(String ruleResponse, String eventFilterResponse) {
        try {
            // update restrictive rules
            if (ruleResponse != null) {
                JSONArray jsonArray = new JSONArray(ruleResponse);
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
            if (eventFilterResponse != null) {
                restrictiveEventFilters.clear();
                JSONObject jsonObject = new JSONObject(eventFilterResponse);
                Iterator<String> keys = jsonObject.keys();
                while(keys.hasNext()) {
                    String key = keys.next();
                    JSONObject paramJson = jsonObject.getJSONObject(key).optJSONObject("restrictive_param");
                    if (paramJson != null) {
                        restrictiveEventFilters.add(
                                new RestrictiveEventFilter(key, Utility.convertJSONObjectToStringMap(paramJson)));
                    }
                }
            }
        } catch (JSONException je) {
            Log.e(TAG, "updateRulesFromSetting failed", je);
        }
    }

    @Nullable
    static String getMatchedRuleType(String eventName, String paramKey, String paramVal) {
        for (RestrictiveEventFilter filter : restrictiveEventFilters) {
            if (eventName.equals(filter.eventName)) {
                for (String param : filter.params.keySet()) {
                    if (paramKey.equals(param)) {
                        return filter.params.get(param);
                    }
                }
            }
        }

        for (RestrictiveRule rule : restrictiveRules) {
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

    static class RestrictiveEventFilter {
        String eventName;
        Map<String, String> params;

        RestrictiveEventFilter(String eventName, Map<String, String> params) {
            this.eventName = eventName;
            this.params = params;
        }
    }
}
