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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.suggestedevents.SuggestedEventsManager;
import com.facebook.internal.FeatureManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModelManager {

    public static final String MODEL_SUGGESTED_EVENTS = "SUGGEST_EVENT";

    private static final ConcurrentMap<String, Model> models = new ConcurrentHashMap<>();

    private static final String SDK_MODEL_ASSET = "%s/model_asset";
    private static final String[] APP_SETTING_FIELDS = new String[]{
            "version_id",
            "asset_uri",
            "use_case",
            "thresholds"
    };

    public static void initialize() {
        // TODO: (jiangyx:T57234811) add cache and improve the function fetchModelFromServer
        InferencerWrapper.initialize();
        if (!InferencerWrapper.hasNeon()) {
            return;
        }
        fetchModelFromServer();
    }

    private static void fetchModelFromServer() {
        FacebookSdk.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> appSettingFields =
                        new ArrayList<>(Arrays.asList(APP_SETTING_FIELDS));
                Bundle appSettingsParams = new Bundle();

                appSettingsParams.putString("fields", TextUtils.join(",", appSettingFields));
                GraphRequest graphRequest = GraphRequest.newGraphPathRequest(null,
                        String.format(SDK_MODEL_ASSET, FacebookSdk.getApplicationId()), null);
                graphRequest.setSkipClientToken(true);
                graphRequest.setParameters(appSettingsParams);
                JSONObject jsonObject = graphRequest.executeAndWait().getJSONObject();

                if (jsonObject != null) {
                    try {
                        JSONArray jsonArray = jsonObject.getJSONArray("data");
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject curJsonObject = jsonArray.getJSONObject(i);

                            String useCase = curJsonObject.getString("use_case");
                            int versionID = curJsonObject.getInt("version_id");
                            String uri = curJsonObject.getString("asset_uri");
                            Model model = new Model(useCase, versionID, uri);
                            models.put(useCase, model);
                        }
                        enableSuggestedEvents();
                    } catch (JSONException e) {
                        /* no op*/
                    }
                }
            }
        });
    }

    // set synchronized because we can enable suggested events through setting from cache as well
    private synchronized static void enableSuggestedEvents() {
        if (SuggestedEventsManager.isEnabled() || !models.containsKey(MODEL_SUGGESTED_EVENTS)) {
            return;
        }
        FeatureManager.checkFeature(FeatureManager.Feature.SuggestedEvents,
                new FeatureManager.Callback() {
                    @Override
                    public void onCompleted(boolean enabled) {
                        if (!enabled) {
                            return;
                        }
                        models.get(MODEL_SUGGESTED_EVENTS).initialize(new Runnable() {
                            @Override
                            public void run() {
                                SuggestedEventsManager.enable();
                            }
                        });
                    }
                });
    }

    @Nullable
    public static String predict(String useCase, float[] dense, String text) {
        // sanity check
        if (!models.containsKey(useCase)) {
            return null;
        }

        return models.get(useCase).predict(dense, text);
    }
}
