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

package com.facebook.appevents;

import android.content.Context;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import com.facebook.LoggingBehavior;
import com.facebook.internal.Logger;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;

@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
@AutoHandleExceptions
class FacebookSDKJSInterface {
  public static final String TAG = FacebookSDKJSInterface.class.getSimpleName();
  private static final String PROTOCOL = "fbmq-0.1";
  private static final String PARAMETER_FBSDK_PIXEL_REFERRAL = "_fb_pixel_referral_id";
  private Context context;

  public FacebookSDKJSInterface(Context context) {
    this.context = context;
  }

  private static Bundle jsonToBundle(JSONObject jsonObject) throws JSONException {
    Bundle bundle = new Bundle();
    Iterator iter = jsonObject.keys();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      String value = jsonObject.getString(key);
      bundle.putString(key, value);
    }
    return bundle;
  }

  private static Bundle jsonStringToBundle(String jsonString) {
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      return jsonToBundle(jsonObject);
    } catch (JSONException ignored) {
      // ignore
    }
    return new Bundle();
  }

  @JavascriptInterface
  public void sendEvent(String pixelId, String event_name, String jsonString) {
    if (pixelId == null) {
      Logger.log(
          LoggingBehavior.DEVELOPER_ERRORS,
          TAG,
          "Can't bridge an event without a referral Pixel ID. "
              + "Check your webview Pixel configuration");
      return;
    }
    final InternalAppEventsLogger logger = new InternalAppEventsLogger(this.context);

    Bundle parameters = jsonStringToBundle(jsonString);
    parameters.putString(PARAMETER_FBSDK_PIXEL_REFERRAL, pixelId);
    logger.logEvent(event_name, parameters);
  }

  @JavascriptInterface
  public String getProtocol() {
    return FacebookSDKJSInterface.PROTOCOL;
  }
}
