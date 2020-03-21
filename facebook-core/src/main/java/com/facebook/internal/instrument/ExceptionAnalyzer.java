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

package com.facebook.internal.instrument;

import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.GraphResponse;
import com.facebook.core.BuildConfig;
import com.facebook.internal.FeatureManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ExceptionAnalyzer {

    private static boolean enabled = false;

    public static void enable() {
        enabled = true;

        if (FacebookSdk.getAutoLogAppEventsEnabled()) {
            sendExceptionAnalysisReports();
        }
    }

    public static void execute(Throwable e) {
        if (!enabled || BuildConfig.DEBUG) {
            return;
        }

        Set<String> disabledFeatures = new HashSet<>();
        for (final StackTraceElement element : e.getStackTrace()) {
            FeatureManager.Feature feature = FeatureManager.getFeature(element.getClassName());
            if (feature != FeatureManager.Feature.Unknown) {
                FeatureManager.disableFeature(feature);
                disabledFeatures.add(feature.toString());
            }
        }

        if (FacebookSdk.getAutoLogAppEventsEnabled() && !disabledFeatures.isEmpty()) {
            InstrumentData.Builder.build(new JSONArray(disabledFeatures)).save();
        }
    }

    private static void sendExceptionAnalysisReports() {
        File[] reports = InstrumentUtility.listExceptionAnalysisReportFiles();
        List<GraphRequest> requests = new ArrayList<>();
        for (File report : reports) {
            final InstrumentData instrumentData = InstrumentData.Builder.load(report);
            if (instrumentData.isValid()) {
                JSONObject params = new JSONObject();
                try {
                    params.put("crash_shield", instrumentData.toString());
                    final GraphRequest request = GraphRequest.newPostRequest(null, String.format("%s" +
                            "/instruments", FacebookSdk.getApplicationId()), params, new GraphRequest.Callback() {
                        @Override
                        public void onCompleted(GraphResponse response) {
                            try {
                                if (response.getError() == null
                                        && response.getJSONObject().getBoolean("success")) {
                                    instrumentData.clear();
                                }
                            } catch (JSONException e) {  /* no op */ }
                        }
                    });
                    requests.add(request);
                } catch (JSONException e) {  /* no op */ }
            }
        }

        if (requests.isEmpty()) {
            return;
        }
        GraphRequestBatch requestBatch = new GraphRequestBatch(requests);
        requestBatch.executeAsync();
    }
}
