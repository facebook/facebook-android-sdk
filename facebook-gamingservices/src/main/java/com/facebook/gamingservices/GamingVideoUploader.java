/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
   */
  public void uploadToMediaLibrary(
      String caption, Uri videoUri, GraphRequest.OnProgressCallback callback)
      throws FileNotFoundException {

    ShareVideo shareVideo = new ShareVideo.Builder().setLocalUrl(videoUri).build();
    ShareVideoContent videoContent =
        new ShareVideoContent.Builder().setVideo(shareVideo).setContentDescription(caption).build();

    VideoUploader.uploadAsyncWithProgressCallback(videoContent, callback);
  }
}
