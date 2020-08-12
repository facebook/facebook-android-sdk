// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

// @lint-ignore LICENSELINT
/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * <p>You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * <p>As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * <p>THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.gamingservices.cloudgaming.internal;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.FacebookRequestError;
import com.facebook.appevents.InternalAppEventsLogger;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONObject;

/**
 * com.facebook.gamingservices.cloudgaming.internal is solely for the use of other packages within
 * the Facebook SDK for Android. Use of any of the classes in this package is unsupported, and they
 * may be modified or removed without warning at any time.
 */
public class SDKLogger {
  private static SDKLogger instance;
  private final InternalAppEventsLogger logger;
  private String appID = null;
  private String userID = null;
  private ConcurrentHashMap<String, String> requestIDToFunctionTypeMap;

  private SDKLogger(Context context) {
    logger = new InternalAppEventsLogger(context);
    requestIDToFunctionTypeMap = new ConcurrentHashMap<>();
  }

  public static synchronized SDKLogger getInstance(Context context) {
    if (instance == null) {
      instance = new SDKLogger(context);
    }
    return instance;
  }

  public void logPreparingRequest(String functionType, String requestID, JSONObject payloads) {
    Bundle parameters = this.getParametersWithRequestIDAndFunctionType(requestID, functionType);
    parameters.putString(SDKAnalyticsEvents.PARAMETER_PAYLOAD, payloads.toString());
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_PREPARING_REQUEST, parameters);
  }

  public void logSentRequest(String functionType, String requestID, JSONObject payloads) {
    Bundle parameters = this.getParametersWithRequestIDAndFunctionType(requestID, functionType);
    requestIDToFunctionTypeMap.put(requestID, functionType);
    parameters.putString(SDKAnalyticsEvents.PARAMETER_PAYLOAD, payloads.toString());
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_SENT_REQUEST, parameters);
  }

  public void logSendingSuccessResponse(String requestID) {
    Bundle parameters = this.getParametersWithRequestIDAndFunctionType(requestID);
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_SENDING_SUCCESS_RESPONSE, parameters);
  }

  public void logSendingErrorResponse(FacebookRequestError error, @Nullable String requestID) {
    Bundle parameters = this.getParametersWithRequestIDAndFunctionType(requestID);
    parameters.putString(
        SDKAnalyticsEvents.PARAMETER_ERROR_CODE, Integer.toString(error.getErrorCode()));
    parameters.putString(SDKAnalyticsEvents.PARAMETER_ERROR_TYPE, error.getErrorType());
    parameters.putString(SDKAnalyticsEvents.PARAMETER_ERROR_MESSAGE, error.getErrorMessage());
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_SENDING_ERROR_RESPONSE, parameters);
  }

  public void logLoginSuccess() {
    Bundle parameters = this.getInitParameters();
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_LOGIN_SUCCESS, parameters);
  }

  public void setAppID(String appID) {
    this.appID = appID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  private Bundle getParametersWithRequestIDAndFunctionType(@Nullable String requestID) {
    Bundle parameters = getInitParameters();
    if (requestID != null) {
      String functionType = requestIDToFunctionTypeMap.getOrDefault(requestID, null);
      parameters.putString(SDKAnalyticsEvents.PARAMETER_REQUEST_ID, requestID);
      if (functionType != null) {
        parameters.putString(SDKAnalyticsEvents.PARAMETER_FUNCTION_TYPE, functionType);
        requestIDToFunctionTypeMap.remove(requestID);
      }
    }
    return parameters;
  }

  private Bundle getParametersWithRequestIDAndFunctionType(String requestID, String functionType) {
    Bundle parameters = getInitParameters();
    parameters.putString(SDKAnalyticsEvents.PARAMETER_REQUEST_ID, requestID);
    parameters.putString(SDKAnalyticsEvents.PARAMETER_FUNCTION_TYPE, functionType);
    return parameters;
  }

  private Bundle getInitParameters() {
    Bundle parameters = new Bundle();
    if (this.appID != null) {
      parameters.putString(SDKAnalyticsEvents.PARAMETER_APP_ID, this.appID);
    }
    if (this.userID != null) {
      parameters.putString(SDKAnalyticsEvents.PARAMETER_USER_ID, this.userID);
    }
    return parameters;
  }
}
