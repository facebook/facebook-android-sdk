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

package com.facebook.internal;

import android.content.Context;
import android.net.Uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class ImageRequest {

    public interface Callback {
        /**
         * This method should always be called on the UI thread. ImageDownloader makes
         * sure to do this when it is responsible for issuing the ImageResponse
         * @param response
         */
        void onCompleted(ImageResponse response);
    }

    public static final int UNSPECIFIED_DIMENSION = 0;

    private static final String SCHEME = "https";
    private static final String AUTHORITY = "graph.facebook.com";
    private static final String PATH = "%s/picture";
    private static final String HEIGHT_PARAM = "height";
    private static final String WIDTH_PARAM = "width";
    private static final String MIGRATION_PARAM = "migration_overrides";
    private static final String MIGRATION_VALUE = "{october_2012:true}";

    private Context context;
    private Uri imageUri;
    private Callback callback;
    private boolean allowCachedRedirects;
    private Object callerTag;

    public static Uri getProfilePictureUri(
            String userId,
            int width,
            int height) {

        Validate.notNullOrEmpty(userId, "userId");

        width = Math.max(width, UNSPECIFIED_DIMENSION);
        height = Math.max(height, UNSPECIFIED_DIMENSION);

        if (width == UNSPECIFIED_DIMENSION && height == UNSPECIFIED_DIMENSION) {
            throw new IllegalArgumentException("Either width or height must be greater than 0");
        }

        Uri.Builder builder =
                new Uri.Builder()
                        .scheme(SCHEME)
                        .authority(AUTHORITY)
                        .path(String.format(Locale.US, PATH, userId));

        if (height != UNSPECIFIED_DIMENSION) {
            builder.appendQueryParameter(HEIGHT_PARAM, String.valueOf(height));
        }

        if (width != UNSPECIFIED_DIMENSION) {
            builder.appendQueryParameter(WIDTH_PARAM, String.valueOf(width));
        }

        builder.appendQueryParameter(MIGRATION_PARAM, MIGRATION_VALUE);

        return builder.build();
    }

    private ImageRequest(Builder builder) {
        this.context = builder.context;
        this.imageUri = builder.imageUrl;
        this.callback = builder.callback;
        this.allowCachedRedirects = builder.allowCachedRedirects;
        this.callerTag = builder.callerTag == null ? new Object() : builder.callerTag;
    }

    public Context getContext() {
        return context;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public Callback getCallback() {
        return callback;
    }

    public boolean isCachedRedirectAllowed() {
        return allowCachedRedirects;
    }

    public Object getCallerTag() {
        return callerTag;
    }

    public static class Builder {
        // Required
        private Context context;
        private Uri imageUrl;

        // Optional
        private Callback callback;
        private boolean allowCachedRedirects;
        private Object callerTag;

        public Builder(Context context, Uri imageUri) {
            Validate.notNull(imageUri, "imageUri");
            this.context = context;
            this.imageUrl = imageUri;
        }

        public Builder setCallback(Callback callback) {
            this.callback = callback;
            return this;
        }

        public Builder setCallerTag(Object callerTag) {
            this.callerTag = callerTag;
            return this;
        }

        public Builder setAllowCachedRedirects(boolean allowCachedRedirects) {
            this.allowCachedRedirects = allowCachedRedirects;
            return this;
        }

        public ImageRequest build() {
            return new ImageRequest(this);
        }
    }
}
