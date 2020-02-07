/**
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
     * After uploading the player will receive a notification that a new item on their media
     * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger
     * the Media Dialog to open and allow immediate sharing.
     *
     * @param caption          the user generated caption for the image, can be null
     * @param imageBitmap      a bitmap with the image that will be uploaded.
     * @param shouldLaunchMediaDialog  if set to True will open the Media Dialog in the FB App
     *                                 to allow the user to share the uploaded image.
     */
    public void uploadToMediaLibrary(
            String caption,
            Bitmap imageBitmap,
            boolean shouldLaunchMediaDialog) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest.newUploadPhotoRequest(
                accessToken,
                GamingImageUploader.photoUploadEdge,
                imageBitmap,
                caption,
                null,
                shouldLaunchMediaDialog ? new OpenGamingMediaDialog(this.context) : null).executeAsync();
    }

    /**
     * Uploads an image to a player's Gaming Media Library.
     *
     * After uploading the player will receive a notification that a new item on their media
     * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger
     * the Media Dialog to open and allow immediate sharing.
     *
     * @param caption          the user generated caption for the image, can be null
     * @param imageFile        the file containing the image to upload
     * @param shouldLaunchMediaDialog  if set to True will open the Media Dialog in the FB App
     *                                 to allow the user to share the uploaded image.
     * @throws java.io.FileNotFoundException if the file doesn't exist
     */
    public void uploadToMediaLibrary(
            String caption,
            File imageFile,
            boolean shouldLaunchMediaDialog
    ) throws FileNotFoundException {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest.newUploadPhotoRequest(
                accessToken,
                GamingImageUploader.photoUploadEdge,
                imageFile,
                caption,
                null,
                shouldLaunchMediaDialog ? new OpenGamingMediaDialog(this.context) : null).executeAsync();
    }

    /**
     * Uploads an image to a player's Gaming Media Library.
     *
     * After uploading the player will receive a notification that a new item on their media
     * library is ready to share. If shouldLaunchMediaDialog is set to true this will also trigger
     * the Media Dialog to open and allow immediate sharing.
     *
     * @param caption          the user generated caption for the image, can be null
     * @param imageUri         the file:// or content:// Uri to the image on device
     * @param shouldLaunchMediaDialog  if set to True will open the Media Dialog in the FB App
     *                                 to allow the user to share the uploaded image.
     * @throws java.io.FileNotFoundException if the file doesn't exist
     */
    public void uploadToMediaLibrary(
            String caption,
            Uri imageUri,
            boolean shouldLaunchMediaDialog
    ) throws FileNotFoundException {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest.newUploadPhotoRequest(
                accessToken,
                GamingImageUploader.photoUploadEdge,
                imageUri,
                caption,
                null,
                shouldLaunchMediaDialog ? new OpenGamingMediaDialog(this.context) : null).executeAsync();
    }
}
