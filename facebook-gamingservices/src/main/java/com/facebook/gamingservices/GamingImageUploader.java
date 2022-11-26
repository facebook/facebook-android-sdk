/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import java.io.File;
import java.io.FileNotFoundException;

public class GamingImageUploader {

  private static final String photoUploadEdge = "me/photos";
  private Context context;

  public GamingImageUploader(Context context) {
    this.context = context;
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageBitmap a bitmap with the image that will be uploaded.
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   */
  public void uploadToMediaLibrary(
      String caption, Bitmap imageBitmap, boolean shouldLaunchMediaDialog) {
    this.uploadToMediaLibrary(caption, imageBitmap, shouldLaunchMediaDialog, null);
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageBitmap a bitmap with the image that will be uploaded.
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   * @param callback a callback that will be called when the request is completed to handle success
   *     or error conditions, can be null.
   */
  public void uploadToMediaLibrary(
      String caption,
      Bitmap imageBitmap,
      boolean shouldLaunchMediaDialog,
      GraphRequest.Callback callback) {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    GraphRequest.Callback openMediaCallback = callback;
    if (shouldLaunchMediaDialog) {
      openMediaCallback = new OpenGamingMediaDialog(this.context, callback);
    }
    GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingImageUploader.photoUploadEdge,
            imageBitmap,
            caption,
            null,
            openMediaCallback)
        .executeAsync();
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageFile the file containing the image to upload
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   * @throws java.io.FileNotFoundException if the file doesn't exist
   */
  public void uploadToMediaLibrary(String caption, File imageFile, boolean shouldLaunchMediaDialog)
      throws FileNotFoundException {
    this.uploadToMediaLibrary(caption, imageFile, shouldLaunchMediaDialog, null);
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageFile the file containing the image to upload
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   * @param callback a callback that will be called when the request is completed to handle success
   *     or error conditions, can be null.
   * @throws java.io.FileNotFoundException if the file doesn't exist
   */
  public void uploadToMediaLibrary(
      String caption,
      File imageFile,
      boolean shouldLaunchMediaDialog,
      GraphRequest.Callback callback)
      throws FileNotFoundException {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();
    GraphRequest.Callback openMediaCallback = callback;
    if (shouldLaunchMediaDialog) {
      openMediaCallback = new OpenGamingMediaDialog(this.context, callback);
    }
    GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingImageUploader.photoUploadEdge,
            imageFile,
            caption,
            null,
            openMediaCallback)
        .executeAsync();
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageUri the file:// or content:// Uri to the image on device
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   * @throws java.io.FileNotFoundException if the file doesn't exist
   */
  public void uploadToMediaLibrary(String caption, Uri imageUri, boolean shouldLaunchMediaDialog)
      throws FileNotFoundException {
    this.uploadToMediaLibrary(caption, imageUri, shouldLaunchMediaDialog, null);
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the image, can be null
   * @param imageUri the file:// or content:// Uri to the image on device
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded image.
   * @param callback a callback that will be called when the request is completed to handle success
   *     or error conditions, can be null.
   * @throws java.io.FileNotFoundException if the file doesn't exist
   */
  public void uploadToMediaLibrary(
      String caption, Uri imageUri, boolean shouldLaunchMediaDialog, GraphRequest.Callback callback)
      throws FileNotFoundException {
    AccessToken accessToken = AccessToken.getCurrentAccessToken();

    GraphRequest.Callback requestCallback = callback;
    if (shouldLaunchMediaDialog) {
      requestCallback = new OpenGamingMediaDialog(this.context, callback);
    }

    GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingImageUploader.photoUploadEdge,
            imageUri,
            caption,
            null,
            requestCallback)
        .executeAsync();
  }
}
