package com.facebook;

import android.graphics.Bitmap;

class ImageResponse {

    private ImageRequest request;
    private Exception error;
    private boolean isCachedRedirect;
    private Bitmap bitmap;

    ImageResponse(ImageRequest request, Exception error, boolean isCachedRedirect, Bitmap bitmap) {
        this.request = request;
        this.error = error;
        this.bitmap = bitmap;
        this.isCachedRedirect = isCachedRedirect;
    }

    ImageRequest getRequest() {
        return request;
    }

    Exception getError() {
        return error;
    }

    Bitmap getBitmap() {
        return bitmap;
    }

    boolean isCachedRedirect() {
        return isCachedRedirect;
    }
}
