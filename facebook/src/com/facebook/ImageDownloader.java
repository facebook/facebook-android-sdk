package com.facebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.EnumSet;

class ImageDownloader {

    /**
     * Downloads the image specified in the passed in request.
     * If a callback is specified, it is guaranteed to be invoked on the calling thread.
     * @param request Request to process
     */
    static void downloadAsync(ImageRequest request) {
        ImageDownloadTask downloadTask = new ImageDownloadTask();
        downloadTask.execute(request);
    }

    private static class ImageDownloadTask extends AsyncTask<ImageRequest, Void, ImageResponse> {
        @Override
        protected ImageResponse doInBackground(ImageRequest... requests) {
            Bitmap bitmap = null;
            Exception error = null;
            ImageRequest request = requests[0];
            boolean isCachedRedirect = false;

            if (!request.isCancelled()) {
                URL url = request.getImageUrl();
                InputStream stream = null;
                try {
                    if (request.isCachedRedirectAllowed()) {
                        stream = ImageResponseCache.getCachedImageStream(
                                url,
                                request.getContext(),
                                EnumSet.of(ImageResponseCache.Options.FOLLOW_REDIRECTS));
                        isCachedRedirect = stream != null;
                    }

                    if (!isCachedRedirect) {
                        stream = ImageResponseCache.getImageStream(
                                url,
                                request.getContext(),
                                ImageResponseCache.Options.NONE);
                    }

                    if (stream != null) {
                        bitmap = BitmapFactory.decodeStream(stream);
                    }
                } catch (IOException e) {
                    error = e;
                } finally {
                    Utility.closeQuietly(stream);
                }
            }

            return new ImageResponse(request, error, isCachedRedirect, bitmap);
        }

        @Override
        protected void onPostExecute(ImageResponse response) {
            super.onPostExecute(response);

            ImageRequest request = response.getRequest();
            ImageRequest.Callback callback = request.getCallback();
            if (!request.isCancelled() && callback != null) {
                callback.onCompleted(response);
            }
        }
    }
}
