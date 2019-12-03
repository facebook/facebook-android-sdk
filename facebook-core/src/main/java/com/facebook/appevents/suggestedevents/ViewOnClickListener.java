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

package com.facebook.appevents.suggestedevents;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.view.View;

import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.appevents.ml.ModelManager;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static com.facebook.appevents.internal.ViewHierarchyConstants.*;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ViewOnClickListener implements View.OnClickListener {
    private static final String TAG = ViewOnClickListener.class.getCanonicalName();
    private static final String API_ENDPOINT = "%s/suggested_events";
    public static final String OTHER_EVENT = "other";

    private @Nullable View.OnClickListener baseListener;

    private static final Set<Integer> viewsAttachedListener = new HashSet<>();

    private WeakReference<View> rootViewWeakReference;
    private WeakReference<View> hostViewWeakReference;
    private String activityName;

    static void attachListener(View hostView, View rootView, String activityName) {
        int key = hostView.hashCode();
        if (!viewsAttachedListener.contains(key)) {
            hostView.setOnClickListener(
                    new ViewOnClickListener(hostView, rootView, activityName));
            viewsAttachedListener.add(key);
        }
    }

    private ViewOnClickListener(View hostView, View rootView, String activityName) {
        baseListener = ViewHierarchy.getExistingOnClickListener(hostView);
        hostViewWeakReference = new WeakReference<>(hostView);
        rootViewWeakReference = new WeakReference<>(rootView);
        this.activityName = activityName.toLowerCase().replace("activity", "");
    }

    @Override
    public void onClick(View view) {
        if (baseListener != null) {
            baseListener.onClick(view);
        }
        process();
    }

    private void process() {
        View rootView = rootViewWeakReference.get();
        View hostView = hostViewWeakReference.get();
        if (rootView == null || hostView == null) {
            return;
        }

        try {
            // query history
            @Nullable String pathID = PredictionHistoryManager.getPathID(hostView);
            if (pathID == null) {
                return;
            }
            String buttonText = ViewHierarchy.getTextOfView(hostView);
            if (queryHistoryAndProcess(pathID, buttonText)) {
                return;
            }

            // run prediction
            final JSONObject data = new JSONObject();
            data.put(VIEW_KEY, SuggestedEventViewHierarchy.getDictionaryOfView(rootView, hostView));
            data.put(SCREEN_NAME_KEY, activityName);
            predictAndProcess(pathID, buttonText, data);
        } catch (Exception e) {
            /*no op*/
        }
    }

    // return True if successfully found history prediction
    private static boolean queryHistoryAndProcess(final String pathID, final String buttonText) {
        // not found
        final String queriedEvent = PredictionHistoryManager.queryEvent(pathID);
        if (queriedEvent == null) {
            return false;
        }

        if (!queriedEvent.equals(OTHER_EVENT)) {
            Utility.runOnNonUiThread(new Runnable() {
                @Override
                public void run() {
                    processPredictedResult(queriedEvent, buttonText, new float[]{});
                }
            });
        }

        return true;
    }

    private void predictAndProcess(final String pathID,
                                   final String buttonText, final JSONObject viewData) {
        Utility.runOnNonUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    String appName = Utility
                            .getAppName(FacebookSdk.getApplicationContext())
                            .toLowerCase();
                    float[] dense = FeatureExtractor.getDenseFeatures(
                            viewData, appName);
                    String textFeature = FeatureExtractor.getTextFeature(
                            buttonText, activityName, appName);
                    if (dense == null) {
                        return;
                    }
                    String predictedEvent = ModelManager.predict(
                            ModelManager.MODEL_SUGGESTED_EVENTS, dense, textFeature);
                    if (predictedEvent == null) {
                        return;
                    }

                    PredictionHistoryManager.addPrediction(pathID, predictedEvent);
                    if (!predictedEvent.equals(OTHER_EVENT)) {
                        processPredictedResult(predictedEvent, buttonText, dense);
                    }
                } catch (Exception e) {
                    /*no op*/
                }
            }
        });
    }

    private static void processPredictedResult(String predictedEvent, String buttonText, float[] dense) {
        if (SuggestedEventsManager.isProductionEvents(predictedEvent)) {
            InternalAppEventsLogger logger = new InternalAppEventsLogger(
                    FacebookSdk.getApplicationContext());
            logger.logEventFromSE(predictedEvent, buttonText);
        } else if (SuggestedEventsManager.isEligibleEvents(predictedEvent)) {
            sendPredictedResult(predictedEvent, buttonText, dense);
        }
    }

    private static void sendPredictedResult(
            final String eventToPost, final String buttonText, float[] dense) {
        Bundle publishParams = new Bundle();
        try {
            publishParams.putString("event_name", eventToPost);

            JSONObject metadata = new JSONObject();
            StringBuilder denseSB = new StringBuilder();
            for (float f : dense) {
                denseSB.append(f).append(",");
            }
            metadata.put("dense", denseSB.toString());
            metadata.put("button_text", buttonText);
            publishParams.putString("metadata", metadata.toString());
            final GraphRequest postRequest = GraphRequest.newPostRequest(
                    null,
                    String.format(Locale.US, API_ENDPOINT,
                            FacebookSdk.getApplicationId()),
                    null,
                    null);
            postRequest.setParameters(publishParams);
            postRequest.executeAndWait();
        } catch (JSONException e) {
            /*no op*/
        }
    }
}
