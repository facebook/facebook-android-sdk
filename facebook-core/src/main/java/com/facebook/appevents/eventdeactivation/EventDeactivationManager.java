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

package com.facebook.appevents.eventdeactivation;

import android.support.annotation.RestrictTo;

import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.appevents.AppEvent;
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class EventDeactivationManager {

    private static boolean enabled = false;
    private static final List<DeprecatedParamFilter> deprecatedParamFilters = new ArrayList<>();
    private static final Set<String> deprecatedEvents = new HashSet<>();

    public static void enable() {
        enabled = true;
        initialize();
    }

    private static synchronized void initialize() {
        try {
            FetchedAppSettings settings = FetchedAppSettingsManager.queryAppSettings(
                    FacebookSdk.getApplicationId(), false);
            if (settings == null) {
                return;
            }
            String eventFilterResponse = settings.getRestrictiveDataSetting();
            if (!eventFilterResponse.isEmpty()) {
                JSONObject jsonObject = new JSONObject(eventFilterResponse);

                deprecatedParamFilters.clear();

                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject json = jsonObject.getJSONObject(key);
                    if (json != null) {
                        if (json.optBoolean("is_deprecated_event")) {
                            deprecatedEvents.add(key);
                        } else {
                            JSONArray deprecatedParamJsonArray = json
                                    .optJSONArray("deprecated_param");
                            DeprecatedParamFilter deprecatedParamFilter = new DeprecatedParamFilter(key,
                                    new ArrayList<String>());
                            if (deprecatedParamJsonArray != null) {
                                deprecatedParamFilter.deprecateParams = Utility
                                        .convertJSONArrayToList(deprecatedParamJsonArray);
                            }
                            deprecatedParamFilters.add(deprecatedParamFilter);
                        }
                    }
                }
            }
        } catch (Exception e) {
            /* swallow */
        }
    }

    public static void processEvents(List<AppEvent> events) {
        if (!enabled) {
            return;
        }

        Iterator<AppEvent> iterator = events.iterator();
        while (iterator.hasNext()) {
            AppEvent event = iterator.next();
            if (deprecatedEvents.contains(event.getName())) {
                iterator.remove();
            }
        }
    }

    public static void processDeprecatedParameters(Map<String, String> parameters,
                                                   String eventName) {
        if (!enabled) {
            return;
        }
        List<String> keys = new ArrayList<>(parameters.keySet());
        List<DeprecatedParamFilter> deprecatedParamFiltersCopy = new ArrayList<>(deprecatedParamFilters);
        for (DeprecatedParamFilter filter : deprecatedParamFiltersCopy) {
            if (!filter.eventName.equals(eventName)) {
                continue;
            }
            for (String key : keys) {
                if (filter.deprecateParams.contains(key)) {
                    parameters.remove(key);
                }
            }
        }
    }

    static class DeprecatedParamFilter {
        String eventName;
        List<String> deprecateParams;

        DeprecatedParamFilter(String eventName, List<String> deprecateParams) {
            this.eventName = eventName;
            this.deprecateParams = deprecateParams;
        }
    }
}
