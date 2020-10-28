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

import android.app.Activity;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.appevents.ml.ModelManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONArray;
import org.json.JSONObject;

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class SuggestedEventsManager {
  private static final AtomicBoolean enabled = new AtomicBoolean(false);
  private static final Set<String> productionEvents = new HashSet<>();
  private static final Set<String> eligibleEvents = new HashSet<>();
  private static final String PRODUCTION_EVENTS_KEY = "production_events";
  private static final String ELIGIBLE_EVENTS_KEY = "eligible_for_prediction_events";

  public static synchronized void enable() {
    FacebookSdk.getExecutor()
        .execute(
            new Runnable() {
              @Override
              public void run() {
                if (enabled.get()) {
                  return;
                }
                enabled.set(true);
                initialize();
              }
            });
  }

  private static void initialize() {
    try {
      FetchedAppSettings settings =
          FetchedAppSettingsManager.queryAppSettings(FacebookSdk.getApplicationId(), false);
      if (settings == null) {
        return;
      }
      String rawSuggestedEventSetting = settings.getSuggestedEventsSetting();
      if (rawSuggestedEventSetting == null) {
        return;
      }
      populateEventsFromRawJsonString(rawSuggestedEventSetting);

      if (!productionEvents.isEmpty() || !eligibleEvents.isEmpty()) {
        File ruleFile = ModelManager.getRuleFile(ModelManager.Task.MTML_APP_EVENT_PREDICTION);
        if (ruleFile == null) {
          return;
        }
        FeatureExtractor.initialize(ruleFile);
        Activity currActivity = ActivityLifecycleTracker.getCurrentActivity();
        if (currActivity != null) {
          trackActivity(currActivity);
        }
      }
    } catch (Exception e) {
      /*no op*/
    }
  }

  protected static void populateEventsFromRawJsonString(String rawSuggestedEventSetting) {
    try {
      JSONObject jsonObject = new JSONObject(rawSuggestedEventSetting);

      if (jsonObject.has(PRODUCTION_EVENTS_KEY)) {
        JSONArray jsonArray = jsonObject.getJSONArray(PRODUCTION_EVENTS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
          productionEvents.add(jsonArray.getString(i));
        }
      }
      if (jsonObject.has(ELIGIBLE_EVENTS_KEY)) {
        JSONArray jsonArray = jsonObject.getJSONArray(ELIGIBLE_EVENTS_KEY);
        for (int i = 0; i < jsonArray.length(); i++) {
          eligibleEvents.add(jsonArray.getString(i));
        }
      }
    } catch (Exception e) {
      /*noop*/
    }
  }

  public static void trackActivity(Activity activity) {
    try {
      if (enabled.get()
          && FeatureExtractor.isInitialized()
          && (!productionEvents.isEmpty() || !eligibleEvents.isEmpty())) {
        ViewObserver.startTrackingActivity(activity);
      } else {
        ViewObserver.stopTrackingActivity(activity);
      }
    } catch (Exception e) {
      /*no op*/
    }
  }

  public static boolean isEnabled() {
    return enabled.get();
  }

  static boolean isProductionEvents(String event) {
    return productionEvents.contains(event);
  }

  static boolean isEligibleEvents(String event) {
    return eligibleEvents.contains(event);
  }
}
