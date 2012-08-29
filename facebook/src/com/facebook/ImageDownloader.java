package com.facebook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;

class ImageDownloader {

    static void downloadAsync(ImageRequest request) {
        // TODO - Need to integrate/dedupe with LoginSettings & GraphObjectAdapter.PictureDownloader
        ImageDownloadTask downloadTask = new ImageDownloadTask();
        downloadTask.execute(request);
    }

    private static class ImageDownloadTask extends AsyncTask<ImageRequest, Void, ImageResponse> {
        @Override
        protected ImageResponse doInBackground(ImageRequest... requests) {
            Bitmap bitmap = null;
            Exception error = null;
            ImageRequest request = requests[0];
            try {
                URLConnection connection = request.getImageUrl().openConnection();
                InputStream stream = connection.getInputStream();
                bitmap = BitmapFactory.decodeStream(stream);
                // TODO cache
            } catch (IOException e) {
                error = e;
            }
            return new ImageResponse(request, error, bitmap);
        }

        @Override
        protected void onPostExecute(ImageResponse response) {
            super.onPostExecute(response);

            ImageRequest.Callback callback = response.getRequest().getCallback();
            if (callback != null) {
                callback.onCompleted(response);
            }
        }
    }
}
