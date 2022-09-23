/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.cloudgaming;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.internal.GamingMediaUploader;
import java.io.File;
import java.io.FileNotFoundException;

public abstract class AppToUserNotificationSender {

  public static void scheduleAppToUserNotification(
      String title,
      String body,
      Bitmap media,
      int timeInterval,
      @Nullable String payload,
      GraphRequest.Callback callback)
      throws FileNotFoundException {
    MediaUploadCallback mediaUploadCallback =
        new MediaUploadCallback(title, body, timeInterval, payload, callback);
    Bundle parameters = getParameters();
    GamingMediaUploader.uploadToGamingServices(
        SDKConstants.PARAM_A2U_CAPTION, media, parameters, mediaUploadCallback);
  }

  public static void scheduleAppToUserNotification(
      String title,
      String body,
      File media,
      int timeInterval,
      @Nullable String payload,
      GraphRequest.Callback callback)
      throws FileNotFoundException {
    MediaUploadCallback mediaUploadCallback =
        new MediaUploadCallback(title, body, timeInterval, payload, callback);
    Bundle parameters = getParameters();
    GamingMediaUploader.uploadToGamingServices(
        SDKConstants.PARAM_A2U_CAPTION, media, parameters, mediaUploadCallback);
  }

  public static void scheduleAppToUserNotification(
      String title,
      String body,
      Uri media,
      int timeInterval,
      @Nullable String payload,
      GraphRequest.Callback callback)
      throws FileNotFoundException {
    MediaUploadCallback mediaUploadCallback =
        new MediaUploadCallback(title, body, timeInterval, payload, callback);
    Bundle parameters = getParameters();
    GamingMediaUploader.uploadToGamingServices(
        SDKConstants.PARAM_A2U_CAPTION, media, parameters, mediaUploadCallback);
  }

  private static Bundle getParameters() {
    Bundle parameters = new Bundle();
    parameters.putString("upload_source", "A2U");
    return parameters;
  }
}

class MediaUploadCallback implements GraphRequest.Callback {
  private String title;
  private String body;
  private int timeInterval;
  @Nullable private String payload;
  GraphRequest.Callback callback;

  public MediaUploadCallback(
      String title,
      String body,
      int timeInterval,
      @Nullable String payload,
      GraphRequest.Callback callback) {
    this.title = title;
    this.body = body;
    this.timeInterval = timeInterval;
    this.payload = payload;
    this.callback = callback;
  }

  @Override
  public void onCompleted(GraphResponse response) {
    if (response.getError() != null) {
      throw new FacebookException(response.getError().getErrorMessage());
    } else {
      String mediaID = response.getJSONObject().optString(SDKConstants.PARAM_A2U_RESPONSE_ID);
      AccessToken accessToken = AccessToken.getCurrentAccessToken();
      Bundle parameters = new Bundle();
      parameters.putString(SDKConstants.PARAM_A2U_TITLE, this.title);
      parameters.putString(SDKConstants.PARAM_A2U_BODY, this.body);
      parameters.putInt(SDKConstants.PARAM_A2U_TIME_INTERVAL, this.timeInterval);
      if (this.payload != null) {
        parameters.putString(SDKConstants.PARAM_A2U_PAYLOAD, this.payload);
      }
      parameters.putString(SDKConstants.PARAM_A2U_MEDIA_ID, mediaID);
      GraphRequest request =
          new GraphRequest(
              accessToken,
              SDKConstants.PARAM_A2U_GRAPH_PATH,
              parameters,
              HttpMethod.POST,
              this.callback);
      request.executeAsync();
    }
  }
}
