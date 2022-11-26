/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.cloudgaming;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.Nullable;
import com.facebook.FacebookRequestError;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This Singleton class handles receiving messages from the daemon. This is a singleton to
 * centralize receiving all messages onto a single thread.
 */
public class DaemonReceiver {
  private static @Nullable DaemonReceiver single_instance = null;

  private static ConcurrentHashMap<String, CompletableFuture<GraphResponse>> requestStore;
  private static SDKLogger mLogger;

  // private constructor restricted to this class itself
  private DaemonReceiver(Context context) {
    final IntentFilter filter = new IntentFilter(SDKConstants.RECEIVER_INTENT);
    HandlerThread thread = new HandlerThread(SDKConstants.RECEIVER_HANDLER);
    thread.start();
    context.registerReceiver(
        new DaemonBroadcastReceiver(), filter, null, new Handler(thread.getLooper()));

    requestStore = new ConcurrentHashMap();
    mLogger = SDKLogger.getInstance(context);
  }

  // package-private getter
  synchronized ConcurrentHashMap<String, CompletableFuture<GraphResponse>> getRequestStore() {
    return requestStore;
  }

  // package-private static method to create instance of Singleton class
  static synchronized DaemonReceiver getInstance(Context context) {
    if (single_instance == null) {
      single_instance = new DaemonReceiver(context);
    }

    return single_instance;
  }

  private static GraphResponse processResponse(JSONObject payload, String requestID) {
    if (!payload.isNull("success")) {
      return createSuccessResponse(payload, requestID);
    } else if (!payload.isNull("error")) {
      return createErrorResponse(payload, requestID);
    }

    // response is malformed
    return createDefaultErrorResponse(requestID);
  }

  private static GraphResponse createSuccessResponse(JSONObject response, String requestID) {
    if (response.optJSONObject("success") != null) {
      mLogger.logSendingSuccessResponse(requestID);
      // if the response is a JSONObject
      // passing dummy parameters; only the payload is critical
      return (new GraphResponse(new GraphRequest(), null, "", response.optJSONObject("success")));
    } else if (response.optJSONArray("success") != null) {
      mLogger.logSendingSuccessResponse(requestID);
      // if the response is a JSONArray
      // passing dummy parameters; only the payload is critical
      return (new GraphResponse(new GraphRequest(), null, "", response.optJSONArray("success")));
    }

    // response is malformed
    return createDefaultErrorResponse(requestID);
  }

  static GraphResponse createErrorResponse(FacebookRequestError error, @Nullable String requestID) {
    // passing dummy parameters; only the payload is critical
    mLogger.logSendingErrorResponse(error, requestID);
    return (new GraphResponse(new GraphRequest(), null, error));
  }

  private static GraphResponse createErrorResponse(JSONObject response, String requestID) {
    JSONObject error = response.optJSONObject("error");
    if (error != null) {
      return createErrorResponse(
          new FacebookRequestError(
              error.optInt("code"), error.optString("type"), error.optString("message")),
          requestID);
    }

    // response is malformed
    return createDefaultErrorResponse(requestID);
  }

  private static GraphResponse createDefaultErrorResponse(String requestID) {
    return createErrorResponse(
        new FacebookRequestError(20, "UNSUPPORTED_FORMAT", "The response format is invalid."),
        requestID);
  }

  private static class DaemonBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      try {
        JSONObject payload = new JSONObject(intent.getStringExtra(SDKConstants.RECEIVER_PAYLOAD));
        String requestID = payload.getString(SDKConstants.REQUEST_ID);

        if (requestStore.containsKey(requestID)) {
          CompletableFuture<GraphResponse> future = requestStore.remove(requestID);
          if (future != null) {
            GraphResponse processedPayload = DaemonReceiver.processResponse(payload, requestID);
            future.complete(processedPayload);
          }
        }
      } catch (JSONException e) {
        // Received response from Daemon with no corresponding request
      }
    }
  }
}
