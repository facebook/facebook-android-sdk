/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices.internal

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import com.facebook.internal.Utility
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.File
import java.io.FileNotFoundException

/**
 * com.facebook.gamingservices.internal is solely for the use of other packages within the Facebook
 * SDK for Android. Use of any of the classes in this package is unsupported, and they may be
 * modified or removed without warning at any time.
 */
@AutoHandleExceptions
object GamingMediaUploader {

  private const val photoUploadEdge = "me/photos"

  @JvmStatic
  fun uploadToGamingServices(
      caption: String?,
      imageBitmap: Bitmap,
      params: Bundle?,
      callback: GraphRequest.Callback?
  ) =
      GraphRequest.newUploadPhotoRequest(
              AccessToken.getCurrentAccessToken(),
              photoUploadEdge,
              imageBitmap,
              caption,
              params,
              callback)
          .executeAsync()

  @JvmStatic
  @Throws(FileNotFoundException::class)
  fun uploadToGamingServices(
      caption: String?,
      imageFile: File,
      params: Bundle?,
      callback: GraphRequest.Callback?
  ) =
      GraphRequest.newUploadPhotoRequest(
              AccessToken.getCurrentAccessToken(),
              photoUploadEdge,
              imageFile,
              caption,
              params,
              callback)
          .executeAsync()

  @JvmStatic
  @Throws(FileNotFoundException::class)
  fun uploadToGamingServices(
      caption: String?,
      imageUri: Uri,
      params: Bundle?,
      callback: GraphRequest.Callback?
  ) =
      if (Utility.isFileUri(imageUri) || Utility.isContentUri(imageUri)) {
        GraphRequest.newUploadPhotoRequest(
                AccessToken.getCurrentAccessToken(),
                photoUploadEdge,
                imageUri,
                caption,
                params,
                callback)
            .executeAsync()
      } else {
        val parameters = Bundle()
        params?.let { parameters.putAll(it) }
        parameters.putString("url", imageUri.toString())
        if (!caption.isNullOrEmpty()) {
           parameters.putString("caption", caption)
        }
        GraphRequest(
                AccessToken.getCurrentAccessToken(),
                photoUploadEdge,
                parameters,
                HttpMethod.POST,
                callback)
            .executeAsync()
      }
}
