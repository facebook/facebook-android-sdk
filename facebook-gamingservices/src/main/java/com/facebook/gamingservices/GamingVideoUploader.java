/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.gamingservices;

import android.content.Context;
import android.net.Uri;
import com.facebook.GraphRequest;
import com.facebook.share.internal.VideoUploader;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import java.io.FileNotFoundException;

public class GamingVideoUploader {

  private Context context;

  /**
   * Utility class to upload Videos to a Player's Gaming Media Library.
   *
   * @param context Android Context for the activity launching initiating an upload.
   */
  public GamingVideoUploader(Context context) {
    // we don't use the Context yet but may need it to provide a callback
    // similar to the Image Uploader.
    this.context = context;
  }

  /**
   * Uploads a video to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share.
   *
   * <p>Note: After the upload is complete the video still needs to be encoded by Facebook and won't
   * be immediately available to be shared.
   *
   * @param caption the user generated caption for the video, can be null
   * @param videoUri the file:// or content:// Uri to the video on device
   * @throws java.io.FileNotFoundException if the videoUri doesn't exist.
   */
  public void uploadToMediaLibrary(String caption, Uri videoUri) throws FileNotFoundException {
    this.uploadToMediaLibrary(caption, videoUri, null);
  }

  /**
   * Uploads a video to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share.
   *
   * <p>Note: After the upload is complete the video still needs to be encoded by Facebook and won't
   * be immediately available to be shared.
   *
   * @param caption the user generated caption for the video, can be null
   * @param videoUri the file:// or content:// Uri to the video on device
   * @param callback an optional OnProgressCallback to track the upload process.
   * @throws java.io.FileNotFoundException if the videoUri doesn't exist.
   */
  public void uploadToMediaLibrary(
      String caption, Uri videoUri, GraphRequest.OnProgressCallback callback)
      throws FileNotFoundException {
    this.uploadToMediaLibrary(caption, videoUri, false, callback);
  }

  /**
   * Uploads an image to a player's Gaming Media Library.
   *
   * <p>After uploading the player will receive a notification that a new item on their media
   * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger the
   * Media Dialog to open and allow immediate sharing.
   *
   * @param caption the user generated caption for the video, can be null
   * @param videoUri the file:// or content:// Uri to the video on device
   * @param shouldLaunchMediaDialog if set to True will open the Media Dialog in the FB App to allow
   *     the user to share the uploaded video.
   * @param callback an optional OnProgressCallback to track the upload process.
   * @throws java.io.FileNotFoundException if the videoUri doesn't exist.
   */
  public void uploadToMediaLibrary(
      String caption,
      Uri videoUri,
      boolean shouldLaunchMediaDialog,
      GraphRequest.OnProgressCallback callback)
      throws FileNotFoundException {

    ShareVideo shareVideo = new ShareVideo.Builder().setLocalUrl(videoUri).build();
    ShareVideoContent videoContent =
        new ShareVideoContent.Builder().setVideo(shareVideo).setContentDescription(caption).build();

    GraphRequest.OnProgressCallback uploadCallback = callback;
    if (shouldLaunchMediaDialog) {
      uploadCallback = new OpenGamingMediaDialog(this.context, callback);
    }

    VideoUploader.uploadAsyncWithProgressCallback(videoContent, uploadCallback);
  }
}
