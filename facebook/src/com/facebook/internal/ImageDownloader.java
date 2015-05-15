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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.facebook.FacebookException;
import com.facebook.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class ImageDownloader {
    private static final int DOWNLOAD_QUEUE_MAX_CONCURRENT = WorkQueue.DEFAULT_MAX_CONCURRENT;
    private static final int CACHE_READ_QUEUE_MAX_CONCURRENT = 2;
    private static Handler handler;
    private static WorkQueue downloadQueue = new WorkQueue(DOWNLOAD_QUEUE_MAX_CONCURRENT);
    private static WorkQueue cacheReadQueue = new WorkQueue(CACHE_READ_QUEUE_MAX_CONCURRENT);

    private static final Map<RequestKey, DownloaderContext> pendingRequests = new HashMap<RequestKey, DownloaderContext>();

    /**
     * Downloads the image specified in the passed in request.
     * If a callback is specified, it is guaranteed to be invoked on the calling thread.
     * @param request Request to process
     */
    public static void downloadAsync(ImageRequest request) {
        if (request == null) {
            return;
        }

        // NOTE: This is the ONLY place where the original request's Url is read. From here on,
        // we will keep track of the Url separately. This is because we might be dealing with a
        // redirect response and the Url might change. We can't create our own new ImageRequests
        // for these changed Urls since the caller might be doing some book-keeping with the
        // requests object reference. So we keep the old references and just map them to new urls in
        // the downloader.
        RequestKey key = new RequestKey(request.getImageUri(), request.getCallerTag());
        synchronized (pendingRequests) {
            DownloaderContext downloaderContext = pendingRequests.get(key);
            if (downloaderContext != null) {
                downloaderContext.request = request;
                downloaderContext.isCancelled = false;
                downloaderContext.workItem.moveToFront();
            } else {
                enqueueCacheRead(request, key, request.isCachedRedirectAllowed());
            }
        }
    }

    public static boolean cancelRequest(ImageRequest request) {
        boolean cancelled = false;
        RequestKey key = new RequestKey(request.getImageUri(), request.getCallerTag());
        synchronized (pendingRequests) {
            DownloaderContext downloaderContext = pendingRequests.get(key);
            if (downloaderContext != null) {
                // If we were able to find the request in our list of pending requests, then we will
                // definitely be able to prevent an ImageResponse from being issued. This is
                // regardless of whether a cache-read or network-download is underway for this
                // request.
                cancelled = true;

                if (downloaderContext.workItem.cancel()) {
                    pendingRequests.remove(key);
                } else {
                    // May be attempting a cache-read right now. So keep track of the cancellation
                    // to prevent network calls etc
                    downloaderContext.isCancelled = true;
                }
            }
        }

        return cancelled;
    }

    public static void prioritizeRequest(ImageRequest request) {
        RequestKey key = new RequestKey(request.getImageUri(), request.getCallerTag());
        synchronized (pendingRequests) {
            DownloaderContext downloaderContext = pendingRequests.get(key);
            if (downloaderContext != null) {
                downloaderContext.workItem.moveToFront();
            }
        }
    }

    public static void clearCache(Context context) {
        ImageResponseCache.clearCache(context);
        UrlRedirectCache.clearCache();
    }

    private static void enqueueCacheRead(
            ImageRequest request,
            RequestKey key,
            boolean allowCachedRedirects) {
        enqueueRequest(
                request,
                key,
                cacheReadQueue,
                new CacheReadWorkItem(request.getContext(), key, allowCachedRedirects));
    }

    private static void enqueueDownload(ImageRequest request, RequestKey key) {
        enqueueRequest(
                request,
                key,
                downloadQueue,
                new DownloadImageWorkItem(request.getContext(), key));
    }

    private static void enqueueRequest(
            ImageRequest request,
            RequestKey key,
            WorkQueue workQueue,
            Runnable workItem) {
        synchronized (pendingRequests) {
            DownloaderContext downloaderContext = new DownloaderContext();
            downloaderContext.request = request;
            pendingRequests.put(key, downloaderContext);

            // The creation of the WorkItem should be done after the pending request has been
            // registered. This is necessary since the WorkItem might kick off right away and
            // attempt to retrieve the request's DownloaderContext prior to it being ready for
            // access.
            //
            // It is also necessary to hold on to the lock until after the workItem is created,
            // since calls to cancelRequest or prioritizeRequest might come in and expect a
            // registered request to have a workItem available as well.
            downloaderContext.workItem = workQueue.addActiveWorkItem(workItem);
        }
    }

    private static void issueResponse(
            RequestKey key,
            final Exception error,
            final Bitmap bitmap,
            final boolean isCachedRedirect) {
        // Once the old downloader context is removed, we are thread-safe since this is the
        // only reference to it
        DownloaderContext completedRequestContext = removePendingRequest(key);
        if (completedRequestContext != null && !completedRequestContext.isCancelled) {
            final ImageRequest request = completedRequestContext.request;
            final ImageRequest.Callback callback = request.getCallback();
            if (callback != null) {
                getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        ImageResponse response = new ImageResponse(
                                request,
                                error,
                                isCachedRedirect,
                                bitmap);
                        callback.onCompleted(response);
                    }
                });
            }
        }
    }

    private static void readFromCache(
            RequestKey key,
            Context context,
            boolean allowCachedRedirects) {
        InputStream cachedStream = null;
        boolean isCachedRedirect = false;
        if (allowCachedRedirects) {
            Uri redirectUri = UrlRedirectCache.getRedirectedUri(key.uri);
            if (redirectUri != null) {
                cachedStream = ImageResponseCache.getCachedImageStream(redirectUri, context);
                isCachedRedirect = cachedStream != null;
            }
        }

        if (!isCachedRedirect) {
            cachedStream = ImageResponseCache.getCachedImageStream(key.uri, context);
        }

        if (cachedStream != null) {
            // We were able to find a cached image.
            Bitmap bitmap = BitmapFactory.decodeStream(cachedStream);
            Utility.closeQuietly(cachedStream);
            issueResponse(key, null, bitmap, isCachedRedirect);
        } else {
            // Once the old downloader context is removed, we are thread-safe since this is the
            // only reference to it
            DownloaderContext downloaderContext = removePendingRequest(key);
            if (downloaderContext != null && !downloaderContext.isCancelled) {
                enqueueDownload(downloaderContext.request, key);
            }
        }
    }

    private static void download(RequestKey key, Context context) {
        HttpURLConnection connection = null;
        InputStream stream = null;
        Exception error = null;
        Bitmap bitmap = null;
        boolean issueResponse = true;

        try {
            URL url = new URL(key.uri.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    // redirect. So we need to perform further requests
                    issueResponse = false;

                    String redirectLocation = connection.getHeaderField("location");
                    if (!Utility.isNullOrEmpty(redirectLocation)) {
                        Uri redirectUri = Uri.parse(redirectLocation);
                        UrlRedirectCache.cacheUriRedirect(key.uri, redirectUri);

                        // Once the old downloader context is removed, we are thread-safe since this
                        // is the only reference to it
                        DownloaderContext downloaderContext = removePendingRequest(key);
                        if (downloaderContext != null && !downloaderContext.isCancelled) {
                            enqueueCacheRead(
                                    downloaderContext.request,
                                    new RequestKey(redirectUri, key.tag),
                                    false);
                        }
                    }
                    break;

                case HttpURLConnection.HTTP_OK:
                    // image should be available
                    stream = ImageResponseCache.interceptAndCacheImageStream(context, connection);
                    bitmap = BitmapFactory.decodeStream(stream);
                    break;

                default:
                    stream = connection.getErrorStream();
                    StringBuilder errorMessageBuilder = new StringBuilder();
                    if (stream != null) {
                        InputStreamReader reader = new InputStreamReader(stream);
                        char[] buffer = new char[128];
                        int bufferLength;
                        while ((bufferLength = reader.read(buffer, 0, buffer.length)) > 0) {
                            errorMessageBuilder.append(buffer, 0, bufferLength);
                        }
                        Utility.closeQuietly(reader);
                    } else {
                        errorMessageBuilder.append(
                            context.getString(R.string.com_facebook_image_download_unknown_error));
                    }
                    error = new FacebookException(errorMessageBuilder.toString());
                    break;
            }
        } catch (IOException e) {
            error = e;
        } finally {
            Utility.closeQuietly(stream);
            Utility.disconnectQuietly(connection);
        }

        if (issueResponse) {
            issueResponse(key, error, bitmap, false);
        }
    }

    private static synchronized Handler getHandler() {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
        return handler;
    }

    private static DownloaderContext removePendingRequest(RequestKey key) {
        synchronized (pendingRequests) {
            return pendingRequests.remove(key);
        }
    }

    private static class RequestKey {
        private static final int HASH_SEED = 29; // Some random prime number
        private static final int HASH_MULTIPLIER = 37; // Some random prime number

        Uri uri;
        Object tag;

        RequestKey(Uri url, Object tag) {
            this.uri = url;
            this.tag = tag;
        }

        @Override
        public int hashCode() {
            int result = HASH_SEED;

            result = (result * HASH_MULTIPLIER) + uri.hashCode();
            result = (result * HASH_MULTIPLIER) + tag.hashCode();

            return result;
        }

        @Override
        public boolean equals(Object o) {
            boolean isEqual = false;

            if (o != null && o instanceof RequestKey) {
                RequestKey compareTo = (RequestKey)o;
                isEqual = compareTo.uri == uri && compareTo.tag == tag;
            }

            return isEqual;
        }
    }

    private static class DownloaderContext {
        WorkQueue.WorkItem workItem;
        ImageRequest request;
        boolean isCancelled;
    }

    private static class CacheReadWorkItem implements Runnable {
        private Context context;
        private RequestKey key;
        private boolean allowCachedRedirects;

        CacheReadWorkItem(Context context, RequestKey key, boolean allowCachedRedirects) {
            this.context = context;
            this.key = key;
            this.allowCachedRedirects = allowCachedRedirects;
        }

        @Override
        public void run() {
            readFromCache(key, context, allowCachedRedirects);
        }
    }

    private static class DownloadImageWorkItem implements Runnable {
        private Context context;
        private RequestKey key;

        DownloadImageWorkItem(Context context, RequestKey key) {
            this.context = context;
            this.key = key;
        }

        @Override
        public void run() {
            download(key, context);
        }

    }
}
