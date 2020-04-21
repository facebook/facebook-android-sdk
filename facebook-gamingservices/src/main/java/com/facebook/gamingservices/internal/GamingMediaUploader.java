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
package com.facebook.gamingservices.internal;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.HttpMethod;
import com.facebook.GraphRequest;
import com.facebook.internal.Utility;
import java.io.File;
import java.io.FileNotFoundException;

/**
 * com.facebook.gamingservices.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is unsupported, and they may
 * be modified or removed without warning at any time.
 */
public abstract class GamingMediaUploader {

    private static final String photoUploadEdge = "me/photos";

    public static void uploadToGamingServices(
            String caption,
            Bitmap imageBitmap,
            Bundle params,
            GraphRequest.Callback callback) {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingMediaUploader.photoUploadEdge,
            imageBitmap,
            caption,
            params,
            callback).executeAsync();
    }

    public static void uploadToGamingServices(
            String caption,
            File imageFile,
            Bundle params,
            GraphRequest.Callback callback
    ) throws FileNotFoundException {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        GraphRequest.newUploadPhotoRequest(
            accessToken,
            GamingMediaUploader.photoUploadEdge,
            imageFile,
            caption,
            params,
            callback).executeAsync();
    }

    public static void uploadToGamingServices(
            String caption,
            Uri imageUri,
            Bundle params,
            GraphRequest.Callback callback
    ) throws FileNotFoundException {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (Utility.isFileUri(imageUri) || Utility.isContentUri(imageUri)) {
            GraphRequest.newUploadPhotoRequest(
                accessToken,
                GamingMediaUploader.photoUploadEdge,
                imageUri,
                caption,
                params,
                callback).executeAsync();
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
                callback)).executeAsync();
        }
    }
}
