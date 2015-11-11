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
import android.util.Log;
import com.facebook.LoggingBehavior;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
class ImageResponseCache {
    static final String TAG = ImageResponseCache.class.getSimpleName();

    private volatile static FileLruCache imageCache;

    synchronized static FileLruCache getCache(Context context) throws IOException{
        if (imageCache == null) {
            imageCache = new FileLruCache(TAG, new FileLruCache.Limits());
        }
        return imageCache;
    }

    // Get stream from cache, or return null if the image is not cached.
    // Does not throw if there was an error.
    static InputStream getCachedImageStream(Uri uri, Context context) {
        InputStream imageStream = null;
        if (uri != null) {
            if (isCDNURL(uri)) {
                try {
                    FileLruCache cache = getCache(context);
                    imageStream = cache.get(uri.toString());
                } catch (IOException e) {
                    Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, e.toString());
                }
            }
        }

        return imageStream;
    }

    static InputStream interceptAndCacheImageStream(
            Context context,
            HttpURLConnection connection
    ) throws IOException {
        InputStream stream = null;
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            Uri uri = Uri.parse(connection.getURL().toString());
            stream = connection.getInputStream(); // Default stream in case caching fails
            try {
                if (isCDNURL(uri)) {
                    FileLruCache cache = getCache(context);

                    // Wrap stream with a caching stream
                    stream = cache.interceptAndPut(
                            uri.toString(),
                            new BufferedHttpInputStream(stream, connection));
                }
            } catch (IOException e) {
                // Caching is best effort
            }
        }
        return stream;
    }

   private static boolean isCDNURL(Uri uri) {
        if (uri != null) {
            String uriHost = uri.getHost();

            if (uriHost.endsWith("fbcdn.net")) {
                return true;
            }

            if (uriHost.startsWith("fbcdn") && uriHost.endsWith("akamaihd.net")) {
                return true;
            }
        }

        return false;
    }

    static void clearCache(Context context) {
        try {
            getCache(context).clearCache();
        } catch (IOException e) {
            Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "clearCache failed " + e.getMessage());
        }
    }

    private static class BufferedHttpInputStream extends BufferedInputStream {
        HttpURLConnection connection;
        BufferedHttpInputStream(InputStream stream, HttpURLConnection connection) {
            super(stream, Utility.DEFAULT_STREAM_BUFFER_SIZE);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            super.close();
            Utility.disconnectQuietly(connection);
        }
    }
}

