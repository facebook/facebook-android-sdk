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

import com.facebook.FacebookSdk;
import com.facebook.appevents.ml.Model;
import com.facebook.appevents.ml.ModelManager;
import com.facebook.internal.FetchedAppGateKeepersManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AddressFilterManager {

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
        List<String> keys = new ArrayList<>(parameters.keySet());
        JSONObject addressParamsJson = new JSONObject();
        for (String key : keys) {
            String address = parameters.get(key);
            if (shouldFilterKey(address)) {
                parameters.remove(key);
                try {
                    addressParamsJson.put(key, isSampleEnabled ? address : "");
                } catch (JSONException je) {
                    /* swallow */
                }
            }
        }
        if (addressParamsJson.length() != 0) {
            parameters.put("_onDeviceParams", addressParamsJson.toString());
        }
    }

    private static boolean shouldFilterKey(String textFeature) {
        float[] dense = new float[30];
        Arrays.fill(dense, new Float(0));
        String shouldFilter = ModelManager.predict(
                ModelManager.MODEL_ADDRESS_DETECTION, dense, textFeature);
        return shouldFilter != null && shouldFilter.equals(Model.SHOULD_FILTER);
    }
}
