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

package com.facebook.appevents.ondeviceprocessing;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.appevents.AppEvent;
import com.facebook.appevents.eventdeactivation.EventDeactivationManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;

@AutoHandleExceptions
class RemoteServiceParametersHelper {

  private static final String TAG = RemoteServiceWrapper.class.getSimpleName();

  @Nullable
  static Bundle buildEventsBundle(
      RemoteServiceWrapper.EventType eventType, String applicationId, List<AppEvent> appEvents) {
    appEvents = new ArrayList<>(appEvents);
    Bundle eventBundle = new Bundle();

    eventBundle.putString("event", eventType.toString());
    eventBundle.putString("application_id", applicationId);

    if (RemoteServiceWrapper.EventType.CUSTOM_APP_EVENTS == eventType) {
      JSONArray filteredEventsJson = buildEventsJson(appEvents, applicationId);
      if (filteredEventsJson.length() == 0) {
        return null;
      }
      eventBundle.putString("custom_events", filteredEventsJson.toString());
    }

    return eventBundle;
  }

  private static JSONArray buildEventsJson(List<AppEvent> appEvents, String applicationId) {
    JSONArray filteredEventsJsonArray = new JSONArray();

    // Drop deprecated events
    EventDeactivationManager.processEvents(appEvents);

    boolean includeImplicitEvents = includeImplicitEvents(applicationId);
    for (AppEvent event : appEvents) {
      if (event.isChecksumValid()) {
        boolean isExplicitEvent = !event.getIsImplicit();
        if (isExplicitEvent || (event.getIsImplicit() && includeImplicitEvents)) {
          filteredEventsJsonArray.put(event.getJSONObject());
        }
      } else {
        Utility.logd(TAG, "Event with invalid checksum: " + event.toString());
      }
    }

    return filteredEventsJsonArray;
  }

  private static boolean includeImplicitEvents(String applicationId) {
    boolean supportsImplicitLogging = false;
    FetchedAppSettings appSettings =
        FetchedAppSettingsManager.queryAppSettings(applicationId, false);
    if (appSettings != null) {
      supportsImplicitLogging = appSettings.supportsImplicitLogging();
    }
    return supportsImplicitLogging;
  }
}
