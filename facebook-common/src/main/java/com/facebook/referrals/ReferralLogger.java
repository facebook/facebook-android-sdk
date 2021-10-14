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

package com.facebook.referrals;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.facebook.appevents.InternalAppEventsLogger;
import com.facebook.internal.qualityvalidation.Excuse;
import com.facebook.internal.qualityvalidation.ExcusesForDesignViolations;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/** @deprecated Referral is deprecated. This class will be removed in a future release. */
@Deprecated
@ExcusesForDesignViolations(@Excuse(type = "MISSING_UNIT_TEST", reason = "Legacy"))
public class ReferralLogger {
  static final String EVENT_NAME_REFERRAL_START = "fb_mobile_referral_start";
  static final String EVENT_NAME_REFERRAL_SUCCESS = "fb_mobile_referral_success";
  static final String EVENT_NAME_REFERRAL_CANCEL = "fb_mobile_referral_cancel";
  static final String EVENT_NAME_REFERRAL_ERROR = "fb_mobile_referral_error";

  static final String EVENT_PARAM_AUTH_LOGGER_ID = "0_auth_logger_id";
  static final String EVENT_PARAM_TIMESTAMP = "1_timestamp_ms";
  static final String EVENT_PARAM_ERROR_MESSAGE = "2_error_message";
  static final String EVENT_PARAM_EXTRAS = "3_extras";
  static final String EVENT_EXTRAS_FACEBOOK_VERSION = "facebookVersion";
  static final String EVENT_EXTRAS_REQUEST_CODE = "request_code";

  static final String EVENT_PARAM_VALUE_EMPTY = "";

  static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";

  private final InternalAppEventsLogger logger;
  private String loggerID;
  private String facebookVersion;

  ReferralLogger(Context context, String applicationId) {
    logger = new InternalAppEventsLogger(context, applicationId);
    loggerID = UUID.randomUUID().toString();

    // Store which version of facebook is installed
    try {
      PackageManager packageManager = context.getPackageManager();
      if (packageManager != null) {
        PackageInfo facebookInfo = packageManager.getPackageInfo(FACEBOOK_PACKAGE_NAME, 0);
        if (facebookInfo != null) {
          facebookVersion = facebookInfo.versionName;
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      // Do nothing, just ignore and not log
    }
  }

  private Bundle getReferralLoggingBundle() {
    Bundle bundle = new Bundle();

    // NOTE: We ALWAYS add all params to each event, to ensure predictable mapping on the backend.
    bundle.putString(EVENT_PARAM_AUTH_LOGGER_ID, loggerID);
    bundle.putLong(EVENT_PARAM_TIMESTAMP, System.currentTimeMillis());
    bundle.putString(EVENT_PARAM_ERROR_MESSAGE, EVENT_PARAM_VALUE_EMPTY);
    bundle.putString(EVENT_PARAM_EXTRAS, EVENT_PARAM_VALUE_EMPTY);

    return bundle;
  }

  public void logStartReferral() {
    Bundle bundle = getReferralLoggingBundle();
    try {
      JSONObject extras = new JSONObject();
      extras.put(EVENT_EXTRAS_REQUEST_CODE, ReferralClient.getReferralRequestCode());
      if (facebookVersion != null) {
        extras.put(EVENT_EXTRAS_FACEBOOK_VERSION, facebookVersion);
      }
      bundle.putString(EVENT_PARAM_EXTRAS, extras.toString());
    } catch (JSONException e) {
      // Do nothing, just ignore and not log extras
    }

    logger.logEventImplicitly(EVENT_NAME_REFERRAL_START, bundle);
  }

  public void logSuccess() {
    Bundle bundle = getReferralLoggingBundle();

    logger.logEventImplicitly(EVENT_NAME_REFERRAL_SUCCESS, bundle);
  }

  public void logCancel() {
    Bundle bundle = getReferralLoggingBundle();

    logger.logEventImplicitly(EVENT_NAME_REFERRAL_CANCEL, bundle);
  }

  public void logError(Exception exception) {
    Bundle bundle = getReferralLoggingBundle();
    if (exception != null && exception.getMessage() != null) {
      bundle.putString(EVENT_PARAM_ERROR_MESSAGE, exception.getMessage());
    }

    logger.logEventImplicitly(EVENT_NAME_REFERRAL_ERROR, bundle);
  }
}
