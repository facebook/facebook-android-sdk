package com.facebook.appevents;

import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public final class RestrictiveParameterManager {

    private static final String TAG = RestrictiveParameterManager.class.getCanonicalName();
    private static List<RestrictiveRule> restrictiveRules = new ArrayList<>();

    public static void updateRulesFromSetting(String response) {
        try {
            restrictiveRules.clear();
            JSONObject jsonObject = new JSONObject(response);
            String rawStr = jsonObject.optString("restrictive_data_filter_rules");
            JSONArray jsonArray = new JSONArray(rawStr);
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
        } catch (JSONException _je) {/*no op*/}
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
}
