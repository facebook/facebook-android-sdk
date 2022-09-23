/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.internal;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * com.facebook.gamingservices.internal is solely for the use of other packages within the Facebook
 * SDK for Android. Use of any of the classes in this package is unsupported, and they may be
 * modified or removed without warning at any time.
 */
@AutoHandleExceptions
public abstract class GamingMediaUploader {

  private static final String photoUploadEdge = "me/photos";

  public static void uploadToGamingServices(
      String caption, Bitmap imageBitmap, Bundle params, GraphRequest.Callback callback) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingMediaUploader.photoUploadEdge,
            imageBitmap,
            caption,
            params,
            callback)
        .executeAsync();
  }

  public static void uploadToGamingServices(
      String caption, File imageFile, Bundle params, GraphRequest.Callback callback)
      throws FileNotFoundException {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    GraphRequest.newUploadPhotoRequest(
            accessToken, GamingMediaUploader.photoUploadEdge, imageFile, caption, params, callback)
        .executeAsync();
  }

  public static void uploadToGamingServices(
      String caption, Uri imageUri, Bundle params, GraphRequest.Callback callback)
      throws FileNotFoundException {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    if (Utility.isFileUri(imageUri) || Utility.isContentUri(imageUri)) {
      GraphRequest.newUploadPhotoRequest(
              accessToken, GamingMediaUploader.photoUploadEdge, imageUri, caption, params, callback)
          .executeAsync();
    } else {
      Bundle parameters = new Bundle();
      if (params != null) {
        parameters.putAll(params);
      }
      parameters.putString("url", imageUri.toString());
      if (caption != null && !caption.isEmpty()) {
        parameters.putString("caption", caption);
      }
      (new GraphRequest(
              accessToken,
              GamingMediaUploader.photoUploadEdge,
              parameters,
              HttpMethod.POST,
              callback))
          .executeAsync();
    }
  }
}
