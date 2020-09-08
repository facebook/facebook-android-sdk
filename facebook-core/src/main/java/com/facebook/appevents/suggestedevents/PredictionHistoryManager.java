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

import static com.facebook.appevents.internal.ViewHierarchyConstants.CLASS_NAME_KEY;
import static com.facebook.appevents.internal.ViewHierarchyConstants.TEXT_KEY;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.FacebookSdk;
import com.facebook.appevents.codeless.internal.ViewHierarchy;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
final class PredictionHistoryManager {
  private static final Map<String, String> clickedViewPaths = new HashMap<>();
  private static final String SUGGESTED_EVENTS_HISTORY = "SUGGESTED_EVENTS_HISTORY";
  private static final String CLICKED_PATH_STORE = "com.facebook.internal.SUGGESTED_EVENTS_HISTORY";
  private static SharedPreferences shardPreferences;
  private static final AtomicBoolean initialized = new AtomicBoolean(false);

  private static void initAndWait() {
    if (initialized.get()) {
      return;
    }
    shardPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(CLICKED_PATH_STORE, Context.MODE_PRIVATE);
    clickedViewPaths.putAll(
        Utility.JsonStrToMap(shardPreferences.getString(SUGGESTED_EVENTS_HISTORY, "")));
    initialized.set(true);
  }

  static void addPrediction(String pathID, String predictedEvent) {
    if (!initialized.get()) {
      initAndWait();
    }

    clickedViewPaths.put(pathID, predictedEvent);
    shardPreferences
        .edit()
        .putString(SUGGESTED_EVENTS_HISTORY, Utility.mapToJsonStr(clickedViewPaths))
        .apply();
  }

  @Nullable
  static String getPathID(View view, String text) {
    JSONObject pathRoute = new JSONObject();
    try {
      pathRoute.put(TEXT_KEY, text);
      JSONArray currentPath = new JSONArray();
      while (view != null) {
        currentPath.put(view.getClass().getSimpleName());
        view = ViewHierarchy.getParentOfView(view);
      }
      pathRoute.put(CLASS_NAME_KEY, currentPath);
    } catch (JSONException je) {
      /*no op*/
    }
    return Utility.sha256hash(pathRoute.toString());
  }

  @Nullable
  static String queryEvent(String pathID) {
    return clickedViewPaths.containsKey(pathID) ? clickedViewPaths.get(pathID) : null;
  }
}
