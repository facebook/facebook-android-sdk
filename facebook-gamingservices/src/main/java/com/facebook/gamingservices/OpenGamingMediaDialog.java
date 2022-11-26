/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.gamingservices.cloudgaming.CloudGameLoginHandler;
import com.facebook.gamingservices.cloudgaming.DaemonRequest;
import com.facebook.gamingservices.cloudgaming.internal.SDKConstants;
import com.facebook.gamingservices.cloudgaming.internal.SDKMessageEnum;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Callback Handler to show the Gaming Media Dialog after media is uploaded to the Gaming Media
 * Library.
 */
public class OpenGamingMediaDialog implements GraphRequest.OnProgressCallback {

  private Context context;
  private GraphRequest.Callback nestedCallback;

  public OpenGamingMediaDialog(Context context) {
    this(context, null);
  }

  public OpenGamingMediaDialog(Context context, GraphRequest.Callback callback) {
    this.context = context;
    this.nestedCallback = callback;
  }

  @Override
  public void onCompleted(GraphResponse response) {
    if (this.nestedCallback != null) {
      this.nestedCallback.onCompleted(response);
    }

    if (response == null || response.getError() != null) {
      return;
    }

    String id = response.getJSONObject().optString("id", null);
    String video_id = response.getJSONObject().optString("video_id", null);
    if (id == null && video_id == null) {
      return;
    }

    id = id != null ? id : video_id;

    boolean isRunningInCloud = CloudGameLoginHandler.isRunningInCloud();

    if (isRunningInCloud) {
      JSONObject parameters = new JSONObject();
      try {
        parameters.put(SDKConstants.PARAM_DEEP_LINK_ID, id);
        parameters.put(SDKConstants.PARAM_DEEP_LINK, "MEDIA_ASSET");
        DaemonRequest.executeAsync(
            this.context, parameters, null, SDKMessageEnum.OPEN_GAMING_SERVICES_DEEP_LINK);
      } catch (JSONException e) {
        // we would get a JSONException if we try to put something that can't be JSON encoded
        // into the parameters object. However we know this is not going to happen since both our
        // parameters are strings. This empty catch block is just here to make the compiler happy.
      }
    } else {
      String dialog_uri = "https://fb.gg/me/media_asset/" + id;
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dialog_uri));
      this.context.startActivity(intent);
    }
  }

  @Override
  public void onProgress(long current, long max) {
    if (this.nestedCallback != null
        && this.nestedCallback instanceof GraphRequest.OnProgressCallback) {
      ((GraphRequest.OnProgressCallback) this.nestedCallback).onProgress(current, max);
    }
  }
}
