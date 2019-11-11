package com.facebook.appevents.suggestedevents;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
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
