/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.widget;

import android.content.Context;
import android.net.Uri;
import com.facebook.internal.Validate;

import java.net.MalformedURLException;
import java.net.URL;

class ImageRequest {

    interface Callback {
        /**
         * This method should always be called on the UI thread. ImageDownloader makes
         * sure to do this when it is responsible for issuing the ImageResponse
         * @param response
         */
        void onCompleted(ImageResponse response);
    }

    static final int UNSPECIFIED_DIMENSION = 0;

    private static final String PROFILEPIC_URL_FORMAT =
            "https://graph.facebook.com/%s/picture";
    private static final String HEIGHT_PARAM = "height";
    private static final String WIDTH_PARAM = "width";
    private static final String MIGRATION_PARAM = "migration_overrides";
    private static final String MIGRATION_VALUE = "{october_2012:true}";

    private Context context;
    private URL imageUrl;
    private Callback callback;
    private boolean allowCachedRedirects;
    private Object callerTag;

    static URL getProfilePictureUrl(
            String userId,
            int width,
            int height)
        throws MalformedURLException {

        Validate.notNullOrEmpty(userId, "userId");

        width = Math.max(width, UNSPECIFIED_DIMENSION);
        height = Math.max(height, UNSPECIFIED_DIMENSION);

        if (width == UNSPECIFIED_DIMENSION && height == UNSPECIFIED_DIMENSION) {
            throw new IllegalArgumentException("Either width or height must be greater than 0");
        }

        Uri.Builder builder = new Uri.Builder().encodedPath(String.format(PROFILEPIC_URL_FORMAT, userId));

        if (height != UNSPECIFIED_DIMENSION) {
            builder.appendQueryParameter(HEIGHT_PARAM, String.valueOf(height));
        }

        if (width != UNSPECIFIED_DIMENSION) {
            builder.appendQueryParameter(WIDTH_PARAM, String.valueOf(width));
        }

        builder.appendQueryParameter(MIGRATION_PARAM, MIGRATION_VALUE);

        return new URL(builder.toString());
    }

    private ImageRequest(Builder builder) {
        this.context = builder.context;
        this.imageUrl = builder.imageUrl;
        this.callback = builder.callback;
        this.allowCachedRedirects = builder.allowCachedRedirects;
        this.callerTag = builder.callerTag == null ? new Object() : builder.callerTag;
    }

    Context getContext() {
        return context;
    }

    URL getImageUrl() {
        return imageUrl;
    }

    Callback getCallback() {
        return callback;
    }

    boolean isCachedRedirectAllowed() {
        return allowCachedRedirects;
    }

    Object getCallerTag() {
        return callerTag;
    }

    static class Builder {
        // Required
        private Context context;
        private URL imageUrl;

        // Optional
        private Callback callback;
        private boolean allowCachedRedirects;
        private Object callerTag;

        Builder(Context context, URL imageUrl) {
            Validate.notNull(imageUrl, "imageUrl");
            this.context = context;
            this.imageUrl = imageUrl;
        }

        Builder setCallback(Callback callback) {
            this.callback = callback;
            return this;
        }

        Builder setCallerTag(Object callerTag) {
            this.callerTag = callerTag;
            return this;
        }

        Builder setAllowCachedRedirects(boolean allowCachedRedirects) {
            this.allowCachedRedirects = allowCachedRedirects;
            return this;
        }

        ImageRequest build() {
            return new ImageRequest(this);
        }
    }
}
