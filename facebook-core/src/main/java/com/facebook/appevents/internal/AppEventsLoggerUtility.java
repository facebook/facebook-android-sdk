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

package com.facebook.appevents.internal;

import android.content.Context;
import com.facebook.LoggingBehavior;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * com.facebook.appevents.internal is solely for the use of other packages within the Facebook SDK
 * for Android. Use of any of the classes in this package is unsupported, and they may be modified
 * or removed without warning at any time.
 */
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class AppEventsLoggerUtility {

  public enum GraphAPIActivityType {
    MOBILE_INSTALL_EVENT,
    CUSTOM_APP_EVENTS,
  }

  private static final Map<GraphAPIActivityType, String> API_ACTIVITY_TYPE_TO_STRING =
      new HashMap<GraphAPIActivityType, String>() {
        {
          put(GraphAPIActivityType.MOBILE_INSTALL_EVENT, "MOBILE_APP_INSTALL");
          put(GraphAPIActivityType.CUSTOM_APP_EVENTS, "CUSTOM_APP_EVENTS");
        }
      };

  public static JSONObject getJSONObjectForGraphAPICall(
      GraphAPIActivityType activityType,
      AttributionIdentifiers attributionIdentifiers,
      String anonymousAppDeviceGUID,
      boolean limitEventUsage,
      Context context)
      throws JSONException {
    JSONObject publishParams = new JSONObject();

    publishParams.put("event", API_ACTIVITY_TYPE_TO_STRING.get(activityType));
    String externalAnalyticsUserId = AppEventsLogger.getUserID();
    if (externalAnalyticsUserId != null) {
      publishParams.put("app_user_id", externalAnalyticsUserId);
    }

    Utility.setAppEventAttributionParameters(
        publishParams, attributionIdentifiers, anonymousAppDeviceGUID, limitEventUsage);

    // The code to get all the Extended info is safe but just in case we can wrap the
    // whole call in its own try/catch block since some of the things it does might
    // cause unexpected exceptions on rooted/funky devices:
    try {
      Utility.setAppEventExtendedDeviceInfoParameters(publishParams, context);
    } catch (Exception e) {
      // Swallow but log
      Logger.log(
          LoggingBehavior.APP_EVENTS,
          "AppEvents",
          "Fetching extended device info parameters failed: '%s'",
          e.toString());
    }

    JSONObject dataProcessingOptions = Utility.getDataProcessingOptions();
    if (dataProcessingOptions != null) {
      for (Iterator<String> it = dataProcessingOptions.keys(); it.hasNext(); ) {
        String key = it.next();
        publishParams.put(key, dataProcessingOptions.get(key));
      }
    }

    publishParams.put("application_package_name", context.getPackageName());

    return publishParams;
  }
}
