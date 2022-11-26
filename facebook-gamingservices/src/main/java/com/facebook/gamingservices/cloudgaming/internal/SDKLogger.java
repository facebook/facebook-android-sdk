/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
  private String sessionID = null;
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

  // Helper to log an internal error
  public static void logInternalError(Context context, SDKMessageEnum functionType, Exception e) {
    SDKLogger.getInstance(context).logInternalError(functionType, e);
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

  public void logGameLoadComplete() {
    Bundle parameters = this.getInitParameters();
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_GAME_LOAD_COMPLETE, parameters);
  }

  public void logInternalError(SDKMessageEnum functionType, Exception e) {
    Bundle parameters = this.getInitParameters();
    parameters.putString(SDKAnalyticsEvents.PARAMETER_FUNCTION_TYPE, functionType.toString());
    parameters.putString(SDKAnalyticsEvents.PARAMETER_ERROR_TYPE, e.getClass().getName());
    parameters.putString(SDKAnalyticsEvents.PARAMETER_ERROR_MESSAGE, e.getMessage());
    logger.logEventImplicitly(SDKAnalyticsEvents.EVENT_INTERNAL_ERROR, parameters);
  }

  public void setAppID(String appID) {
    this.appID = appID;
  }

  public void setUserID(String userID) {
    this.userID = userID;
  }

  public void setSessionID(String sessionID) {
    this.sessionID = sessionID;
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
    if (this.sessionID != null) {
      parameters.putString(SDKAnalyticsEvents.PARAMETER_SESSION_ID, this.sessionID);
    }
    return parameters;
  }
}
