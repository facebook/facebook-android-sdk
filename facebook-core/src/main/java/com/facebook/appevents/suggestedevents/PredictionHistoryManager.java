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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import android.view.View;

import com.facebook.FacebookSdk;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.internal.Utility;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class PredictionHistoryManager {
    private static final Map<String, String> clickedViewPaths = new HashMap<>();
    private static final String SUGGESTED_EVENTS_HISTORY = "SUGGESTED_EVENTS_HISTORY";
    private static final String CLICKED_PATH_STORE
            = "com.facebook.internal.SUGGESTED_EVENTS_HISTORY";
    private static SharedPreferences shardPreferences;
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private static void initAndWait() {
        if (initialized.get()) {
            return;
        }
        shardPreferences = FacebookSdk.getApplicationContext()
                .getSharedPreferences(CLICKED_PATH_STORE, Context.MODE_PRIVATE);
        clickedViewPaths.putAll(Utility.JsonStrToMap(shardPreferences
                .getString(SUGGESTED_EVENTS_HISTORY, "")));
        initialized.set(true);
    }

    static void addPrediction(String pathID, String predictedEvent) {
        if (!initialized.get()) {
            initAndWait();
        }

        clickedViewPaths.put(pathID, predictedEvent);
        shardPreferences.edit()
                .putString(SUGGESTED_EVENTS_HISTORY, Utility.mapToJsonStr(clickedViewPaths))
                .apply();
    }

    @Nullable
    static String getPathID(View view) {
        View currentView = view;
        JSONObject jsonObject = new JSONObject();
        while (currentView != null) {
            SuggestedEventViewHierarchy.updateBasicInfo(currentView, jsonObject);
            currentView = (View) ViewHierarchy.getParentOfView(currentView);
        }
        return Utility.sha256hash(jsonObject.toString());
    }

    @Nullable
    static String queryEvent(String pathID) {
        return clickedViewPaths.containsKey(pathID) ?
                clickedViewPaths.get(pathID) : null;
    }
}
