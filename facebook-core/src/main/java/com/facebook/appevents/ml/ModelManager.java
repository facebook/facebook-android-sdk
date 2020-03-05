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

package com.facebook.appevents.ml;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.restrictivedatafilter.AddressFilterManager;
import com.facebook.appevents.suggestedevents.SuggestedEventsManager;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ModelManager {

    public enum Task {
        ADDRESS_DETECTION,
        APP_EVENT_PREDICTION,
        MTML_ADDRESS_DETECTION,
        MTML_APP_EVENT_PREDICTION;

        public String toKey() {
            switch (this) {
                case ADDRESS_DETECTION:
                case APP_EVENT_PREDICTION: return "fc3";
                case MTML_ADDRESS_DETECTION: return "address_detect";
                case MTML_APP_EVENT_PREDICTION: return "app_event_pred";
            }
            return  "Unknown";
        }

        @Nullable
        public String toUseCase() {
            switch (this) {
                case ADDRESS_DETECTION: return "DATA_DETECTION_ADDRESS";
                case APP_EVENT_PREDICTION: return "SUGGEST_EVENT";
                case MTML_ADDRESS_DETECTION:
                case MTML_APP_EVENT_PREDICTION: return "MTML";
            }
            return null;
        }
    }

    private static final ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();
    private static final String SDK_MODEL_ASSET = "%s/model_asset";
    private static SharedPreferences shardPreferences;
    private static final String MODEL_ASSERT_STORE = "com.facebook.internal.MODEL_STORE";
    private static final String CACHE_KEY_MODELS = "models";
    private static final String[] APP_SETTING_FIELDS = new String[]{
            "version_id",
            "asset_uri",
            "use_case",
            "thresholds",
            "rules_uri"
    };

    public static void enable() {
        shardPreferences = FacebookSdk.getApplicationContext()
                .getSharedPreferences(MODEL_ASSERT_STORE, Context.MODE_PRIVATE);
        Utility.runOnNonUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject modelJSON = fetchModels();
                    if (modelJSON != null) {
                        shardPreferences.edit().putString(CACHE_KEY_MODELS,
                                modelJSON.toString()).apply();
                    } else {
                        modelJSON = new JSONObject(shardPreferences
                                .getString(CACHE_KEY_MODELS, ""));
                    }
                    addModels(modelJSON);
                    enableSuggestedEvents();
                    enablePIIFiltering();
                } catch (Exception e) {
                    /* no op*/
                }
            }
        });
    }

    private static void addModels(JSONObject modelJSON) {
        Iterator<String> keys = modelJSON.keys();
        try {
            while (keys.hasNext()) {
                String key = keys.next();
                @Nullable Model model = jsonObjectToModel(modelJSON.getJSONObject(key));
                if (model == null) {
                    continue;
                }
                models.put(key, model);
            }
        } catch (JSONException je) {
            /* no op*/
        }
    }

    private static JSONObject parseRawJsonObject(JSONObject jsonObject) {
        JSONObject resultJsonObject = new JSONObject();
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject curJsonObject = jsonArray.getJSONObject(i);
                JSONObject tempJsonObject = new JSONObject();
                tempJsonObject.put("version_id", curJsonObject.getString("version_id"));
                tempJsonObject.put("use_case", curJsonObject.getString("use_case"));
                tempJsonObject.put("thresholds", curJsonObject.getJSONArray("thresholds"));
                tempJsonObject.put("asset_uri", curJsonObject.getString("asset_uri"));
                // rule_uri is optional
                if (curJsonObject.has("rules_uri")) {
                    tempJsonObject.put("rules_uri", curJsonObject.getString("rules_uri"));
                }
                resultJsonObject.put(curJsonObject.getString("use_case"), tempJsonObject);
            }
            return resultJsonObject;
        } catch (JSONException je) {
            return new JSONObject();
        }
    }

    @Nullable
    private static JSONObject fetchModels() {
        ArrayList<String> appSettingFields =
                new ArrayList<>(Arrays.asList(APP_SETTING_FIELDS));
        Bundle appSettingsParams = new Bundle();

        appSettingsParams.putString("fields", TextUtils.join(",", appSettingFields));
        GraphRequest graphRequest = GraphRequest.newGraphPathRequest(null,
                String.format(SDK_MODEL_ASSET, FacebookSdk.getApplicationId()), null);
        graphRequest.setSkipClientToken(true);
        graphRequest.setParameters(appSettingsParams);
        JSONObject rawResponse = graphRequest.executeAndWait().getJSONObject();
        if (rawResponse == null) {
            return null;
        }
        return parseRawJsonObject(rawResponse);
    }

    @Nullable
    private static Model jsonObjectToModel(JSONObject jsonObject) {
        try {
            String useCase = jsonObject.getString("use_case");
            String assetUrl = jsonObject.getString("asset_uri");
            JSONArray threshold = jsonObject.getJSONArray("thresholds");
            int versionId = Integer.parseInt(jsonObject.getString("version_id"));
            String ruleUri = jsonObject.optString("rules_uri", null);
            return new Model(useCase, versionId, assetUrl, ruleUri, parseJsonArray(threshold));
        } catch (JSONException je) {
            return null;
        }
    }

    private static void enableSuggestedEvents() {
        Model model = models.get(Task.APP_EVENT_PREDICTION.toUseCase());
        if (model == null) {
            return;
        }
        Locale locale = Utility.getResourceLocale();
        if (locale != null && !locale.getLanguage().contains("en")) {
            return;
        }

        if (FeatureManager.isEnabled(FeatureManager.Feature.SuggestedEvents)) {
            model.initialize(new Runnable() {
                @Override
                public void run() {
                    SuggestedEventsManager.enable();
                }
            });
        }
    }

    private static void enablePIIFiltering() {
        Model model = models.get(Task.ADDRESS_DETECTION.toUseCase());
        if (model == null) {
            return;
        }

        if (FeatureManager.isEnabled(FeatureManager.Feature.PIIFiltering)) {
            model.initialize(new Runnable() {
                @Override
                public void run() {
                    AddressFilterManager.enable();
                }
            });
        }
    }

    private static float[] parseJsonArray(JSONArray jsonArray) {
        float[] thresholds = new float[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                thresholds[i] = Float.parseFloat(jsonArray.getString(i));
            } catch (JSONException e) {
                /*no op*/
            }
        }
        return thresholds;
    }

    @Nullable
    public static String predict(Task task, float[] dense, String text) {
        Model model = models.get(task.toUseCase());
        if (model == null) {
            return null;
        }
        return model.predict(dense, text, task.toKey());
    }

    @Nullable
    public static File getRuleFile(Task task) {
        Model model = models.get(task.toUseCase());
        if (model == null) {
            return null;
        }
        return model.getRuleFile();
    }
}
