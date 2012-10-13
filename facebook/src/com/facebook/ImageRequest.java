package com.facebook;

import android.content.Context;
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

    private Context context;
    private URL imageUrl;
    private Callback callback;
    private boolean isCancelled;
    private boolean allowCachedRedirects;

    static ImageRequest createProfilePictureImageRequest(
            Context context,
            String userId,
            int width,
            int height,
            boolean allowCachedImage,
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

        return new ImageRequest(context, new URL(builder.toString()), allowCachedImage, callback);
    }

    ImageRequest(Context context, URL imageUrl, boolean allowCachedRedirects, Callback callback) {
        Validate.notNull(imageUrl, "imageUrl");
        this.context = context;
        this.imageUrl = imageUrl;
        this.callback = callback;
        this.allowCachedRedirects = allowCachedRedirects;
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

    /**
     * Will prevent the registered callback from firing.
     * This method is only reliable when called from the UI thread. If you cancel a request
     * from a non-UI thread, the registered callback may be invoked. For multi-threaded
     * scenarios, it is best to check whether the ImageRequest has been cancelled in the
     * callback.
     */
    void cancel() {
        isCancelled = true;
    }

    boolean isCancelled() {
        return isCancelled;
    }

    boolean isCachedRedirectAllowed() {
        return allowCachedRedirects;
    }
}
