/**
 * Copyright 2010 Facebook, Inc.
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

package com.facebook;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.Log;

class ImageResponseCache {
    static final String TAG = ImageResponseCache.class.getSimpleName();
    
    private volatile static List<String> cdnHosts;
    private volatile static FileLruCache imageCache;

    // Get stream from cache if present, otherwise get from web.
    // If not cached and the uri points to a CDN, store the result in cache.
    static InputStream getImageStream(URL url, Context context) throws IOException {
        Validate.notNull(url, "url");
        Validate.notNull(context, "context");

        InputStream cacheStream = getCachedImageStream(url, context);
        if (cacheStream != null) {
            return cacheStream;
        }

        return getStreamFromConnection(url, context);
    }

    // Get stream from cache, or return null if the image is not cached.
    // Does not throw if there was an error.
    static InputStream getCachedImageStream(URL url, Context context) {
        if ((url != null) && isCDNURL(url)) {
            try {
                FileLruCache cache = getCache(context);
                return cache.get(url.toString());
            } catch (IOException e) {
                Logger.log(LoggingBehaviors.CACHE, Log.WARN, TAG, e.toString());
            }
        }

        return null;
    }

    synchronized static FileLruCache getCache(Context context) throws IOException{
        if (imageCache == null) {
            imageCache = new FileLruCache(context.getApplicationContext(), TAG, new FileLruCache.Limits());
        }
        return imageCache;
    }
    
    private static InputStream getStreamFromConnection(URL url, Context context) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        InputStream stream;

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            stream = new BufferedInputStream(connection.getInputStream(), Utility.DEFAULT_STREAM_BUFFER_SIZE);

            if (isCDNURL(url)) {
                try {
                    FileLruCache cache = getCache(context);

                    // Wrap stream with a caching stream
                    stream = cache.interceptAndPut(url.toString(), stream);
                } catch (IOException e) {
                    // On cache failure, stream is unchanged and we return the original http stream
                }
            }
        } else {
            // If response is not HTTP_OK, return error stream
            stream = new BufferedInputStream(connection.getErrorStream(), Utility.DEFAULT_STREAM_BUFFER_SIZE);
        }

        return stream;
    }

    private static boolean isCDNURL(URL url) {
        String uriHost = url.getHost();

        if (uriHost.endsWith("fbcdn.net")) {
            return true;
        }

        if (uriHost.startsWith("fbcdn") && uriHost.endsWith("akamaihd.net")) {
            return true;
        }

        return false;
    }
}

