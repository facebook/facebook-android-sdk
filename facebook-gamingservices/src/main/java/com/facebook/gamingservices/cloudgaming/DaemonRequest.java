/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.cloudgaming;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.facebook.FacebookRequestError;
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKLogger;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.json.JSONException;
import org.json.JSONObject;

public class DaemonRequest {
  private Context mContext;
  private JSONObject mParameters;
  private Callback mCallback;
  private ConcurrentHashMap<String, CompletableFuture<GraphResponse>> mRequestStore;
  private SDKLogger mLogger;

  DaemonRequest(Context context, JSONObject parameters, Callback callback) {
    this.mContext = context;
    this.mParameters = parameters;
    this.mCallback = callback;

    this.mRequestStore = DaemonReceiver.getInstance(context).getRequestStore();
    this.mLogger = SDKLogger.getInstance(context);
  }

  private void executeAsync() throws ExecutionException, InterruptedException {
    createRequest()
        .thenAccept(
            new Consumer<GraphResponse>() {
              @Override
              public void accept(GraphResponse response) {
                if (mCallback != null) {
                  mCallback.onCompleted(response);
                }
              }
            });
  }

  private GraphResponse executeAndWait() throws ExecutionException, InterruptedException {
    return createRequest().get();
  }

  private GraphResponse executeAndWait(int timeout)
      throws ExecutionException, InterruptedException, TimeoutException {
    return createRequest().get(timeout, TimeUnit.SECONDS);
  }

  private CompletableFuture<GraphResponse> createRequest() {
    CompletableFuture<GraphResponse> response =
        CompletableFuture.supplyAsync(
            new Supplier<GraphResponse>() {
              @Override
              public GraphResponse get() {
                String uniqueID = UUID.randomUUID().toString();
                try {
                  mParameters.put(SDKConstants.REQUEST_ID, uniqueID);

                  Intent intent = new Intent();
                  // allow initialization (auto-login) request to be a publicly broadcasted
                  String functionType = mParameters.getString(SDKConstants.PARAM_TYPE);
                  mLogger.logPreparingRequest(functionType, uniqueID, mParameters);
                  if (!functionType.equals(SDKMessageEnum.GET_ACCESS_TOKEN.toString())
                      && !functionType.equals(SDKMessageEnum.IS_ENV_READY.toString())) {
                    SharedPreferences sharedPreferences =
                        mContext.getSharedPreferences(
                            SDKConstants.PREF_DAEMON_PACKAGE_NAME, Activity.MODE_PRIVATE);
                    String daemonPackageName =
                        sharedPreferences.getString(SDKConstants.PARAM_DAEMON_PACKAGE_NAME, null);
                    // does not exist
                    if (daemonPackageName == null) {
                      FacebookRequestError error =
                          new FacebookRequestError(
                              FacebookRequestError.INVALID_ERROR_CODE,
                              "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
                              "Unable to correctly create the request with a secure connection");
                      return DaemonReceiver.createErrorResponse(error, uniqueID);
                    } else {
                      intent.setPackage(daemonPackageName);
                    }
                  }
                  intent.setAction(SDKConstants.REQUEST_ACTION);
                  Iterator iter = mParameters.keys();
                  while (iter.hasNext()) {
                    String key = (String) iter.next();
                    String value = mParameters.getString(key);
                    intent.putExtra(key, value);
                  }

                  CompletableFuture<GraphResponse> future = new CompletableFuture<GraphResponse>();
                  mRequestStore.put(uniqueID, future);
                  mContext.sendBroadcast(intent);
                  mLogger.logSentRequest(functionType, uniqueID, mParameters);

                  return future.get();
                } catch (JSONException | InterruptedException | ExecutionException e) {
                  FacebookRequestError error =
                      new FacebookRequestError(
                          FacebookRequestError.INVALID_ERROR_CODE,
                          "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
                          "Unable to correctly create the request or obtain response");
                  return DaemonReceiver.createErrorResponse(error, uniqueID);
                }
              }
            });
    return response;
  }

  public interface Callback {
    /**
     * The method that will be called when a request completes.
     *
     * @param response the Response of this request, which may include error information if the
     *     request was unsuccessful
     */
    void onCompleted(GraphResponse response);
  }

  public static void executeAsync(
      Context context,
      @Nullable JSONObject parameters,
      DaemonRequest.Callback callback,
      SDKMessageEnum type) {
    try {
      JSONObject updatedParameters =
          parameters == null
              ? (new JSONObject().put(SDKConstants.PARAM_TYPE, type.toString()))
              : parameters.put(SDKConstants.PARAM_TYPE, type.toString());
      DaemonRequest request = new DaemonRequest(context, updatedParameters, callback);
      request.executeAsync();
    } catch (JSONException | ExecutionException | InterruptedException e) {
      if (callback != null) {
        FacebookRequestError error =
            new FacebookRequestError(
                FacebookRequestError.INVALID_ERROR_CODE,
                "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
                "Unable to correctly create the request or obtain response");
        callback.onCompleted(DaemonReceiver.createErrorResponse(error, null));
      }
    }
  }

  public static GraphResponse executeAndWait(
      Context context, @Nullable JSONObject parameters, SDKMessageEnum type) {
    try {
      JSONObject updatedParameters =
          parameters == null
              ? (new JSONObject().put(SDKConstants.PARAM_TYPE, type.toString()))
              : parameters.put(SDKConstants.PARAM_TYPE, type.toString());
      DaemonRequest request = new DaemonRequest(context, updatedParameters, null);
      return request.executeAndWait();
    } catch (JSONException | ExecutionException | InterruptedException e) {
      FacebookRequestError error =
          new FacebookRequestError(
              FacebookRequestError.INVALID_ERROR_CODE,
              "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
              "Unable to correctly create the request or obtain response");
      return DaemonReceiver.createErrorResponse(error, null);
    }
  }

  public static GraphResponse executeAndWait(
      Context context, @Nullable JSONObject parameters, SDKMessageEnum type, int timeout) {
    try {
      JSONObject updatedParameters =
          parameters == null
              ? (new JSONObject().put(SDKConstants.PARAM_TYPE, type.toString()))
              : parameters.put(SDKConstants.PARAM_TYPE, type.toString());
      DaemonRequest request = new DaemonRequest(context, updatedParameters, null);
      return request.executeAndWait(timeout);
    } catch (JSONException | ExecutionException | InterruptedException | TimeoutException e) {
      FacebookRequestError error =
          new FacebookRequestError(
              FacebookRequestError.INVALID_ERROR_CODE,
              "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
              "Unable to correctly create the request or obtain response");
      return DaemonReceiver.createErrorResponse(error, null);
    }
  }

  public static void executeAsync(
      Context context,
      @Nullable JSONObject parameters,
      DaemonRequest.Callback callback,
      String type) {
    try {
      JSONObject updatedParameters =
          parameters == null
              ? (new JSONObject().put(SDKConstants.PARAM_TYPE, type))
              : parameters.put(SDKConstants.PARAM_TYPE, type);
      DaemonRequest request = new DaemonRequest(context, updatedParameters, callback);
      request.executeAsync();
    } catch (JSONException | ExecutionException | InterruptedException e) {
      if (callback != null) {
        FacebookRequestError error =
            new FacebookRequestError(
                FacebookRequestError.INVALID_ERROR_CODE,
                "DAEMON_REQUEST_EXECUTE_ASYNC_FAILED",
                "Unable to correctly create the request or obtain response");
        callback.onCompleted(DaemonReceiver.createErrorResponse(error, null));
      }
    }
  }
}
