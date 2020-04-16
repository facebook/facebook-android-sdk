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

package com.facebook.appevents.integrity;

import android.support.annotation.Nullable;

import com.facebook.FacebookSdk;
import com.facebook.appevents.ml.ModelManager;
import com.facebook.internal.FetchedAppGateKeepersManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class IntegrityManager {

    public static final String INTEGRITY_TYPE_NONE = "none";
    public static final String INTEGRITY_TYPE_ADDRESS = "address";
    public static final String INTEGRITY_TYPE_HEALTH = "health";

    private static final String RESTRICTIVE_ON_DEVICE_PARAMS_KEY = "_onDeviceParams";
    private static boolean enabled = false;
    private static boolean isSampleEnabled = false;

    public static void enable() {
        enabled = true;
        isSampleEnabled = FetchedAppGateKeepersManager.getGateKeeperForKey(
                "FBSDKFeatureAddressDetectionSample",
                FacebookSdk.getApplicationId(), false);
    }

    public static void processParameters(Map<String, String> parameters) {
        if (!enabled || parameters.size() == 0) {
            return;
        }
        try {
            List<String> keys = new ArrayList<>(parameters.keySet());

            JSONObject restrictiveParamJson = new JSONObject();
            for (String key : keys) {
                String value = parameters.get(key);

                if (shouldFilter(key) || shouldFilter(value)) {
                    parameters.remove(key);
                    restrictiveParamJson.put(key, isSampleEnabled ? value : "");
                }
            }
            if (restrictiveParamJson.length() != 0) {
                parameters.put(RESTRICTIVE_ON_DEVICE_PARAMS_KEY, restrictiveParamJson.toString());
            }
        } catch (Exception e) {
            /* swallow */
        }
    }

    private static boolean shouldFilter(String input) {
        String predictResult = getIntegrityPredictionResult(input);
        return !INTEGRITY_TYPE_NONE.equals(predictResult);
    }

    private static String getIntegrityPredictionResult(String textFeature) {
        float[] dense = new float[30];
        Arrays.fill(dense, 0);
        @Nullable String[] res = ModelManager.predict(
                ModelManager.Task.MTML_INTEGRITY_DETECT,
                new float[][]{dense},
                new String[]{textFeature});
        return res[0];
    }
}
