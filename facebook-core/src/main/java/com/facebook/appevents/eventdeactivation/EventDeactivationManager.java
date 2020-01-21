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
import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class EventDeactivationManager {

    private static boolean enabled = false;
    private static List<DeprecatedParam> deprecatedParams = new ArrayList<>();
    private static Set<String> deprecatedEvents = new HashSet<>();

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

                deprecatedParams.clear();

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
                            DeprecatedParam deprecatedParam = new DeprecatedParam(key,
                                    new ArrayList<String>());
                            if (deprecatedParamJsonArray != null) {
                                deprecatedParam.deprecateParams = Utility
                                        .convertJSONArrayToList(deprecatedParamJsonArray);
                            }
                            deprecatedParams.add(deprecatedParam);
                        }
                    }
                }
            }
        } catch (Exception e) {
            /* swallow */
        }
    }

    static class DeprecatedParam {
        String eventName;
        List<String> deprecateParams;

        DeprecatedParam(String eventName, List<String> deprecateParams) {
            this.eventName = eventName;
            this.deprecateParams = deprecateParams;
        }
    }
}
