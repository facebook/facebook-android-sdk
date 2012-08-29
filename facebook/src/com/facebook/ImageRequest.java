package com.facebook;

import android.graphics.Bitmap;
import android.net.Uri;

import java.net.MalformedURLException;
import java.net.URL;

class ImageRequest {

    interface Callback {
        void onCompleted(ImageResponse response);
    }

    static final int UNSPECIFIED_DIMENSION = 0;

    private static final String PROFILEPIC_URL_FORMAT =
            "https://graph.facebook.com/%s/picture";
    private static final String HEIGHT_PARAM = "height";
    private static final String WIDTH_PARAM = "width";
    private static final String MIGRATION_PARAM = "migration_overrides";
    private static final String MIGRATION_VALUE = "{october_2012:true}";

    private URL imageUrl;
    private Callback callback;

    static ImageRequest createProfilePictureImageRequest(
            String userId,
            int width,
            int height,
            Callback callback)
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

        return new ImageRequest(new URL(builder.toString()), callback);
    }

    ImageRequest(URL imageUrl, Callback callback) {
        Validate.notNull(imageUrl, "imageUrl");
        this.imageUrl = imageUrl;
        this.callback = callback;
    }

    URL getImageUrl() {
        return imageUrl;
    }

    Callback getCallback() {
        return callback;
    }
}
