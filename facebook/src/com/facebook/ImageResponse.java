package com.facebook;

import android.graphics.Bitmap;

class ImageResponse {

    private ImageRequest request;
    private Exception error;
    private Bitmap bitmap;

    ImageResponse(ImageRequest request, Exception error, Bitmap bitmap) {
        this.request = request;
        this.error = error;
        this.bitmap = bitmap;
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
}
