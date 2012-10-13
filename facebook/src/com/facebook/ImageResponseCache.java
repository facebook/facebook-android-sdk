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

import android.content.Context;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.EnumSet;

class ImageResponseCache {
    static final String TAG = ImageResponseCache.class.getSimpleName();
    private static final String REDIRECT_CONTENT_TAG = TAG + "_Redirect";

    private volatile static FileLruCache imageCache;

    static InputStream getCachedImageStream(URL url, Context context) {
        return getCachedImageStream(url, context, Options.NONE);
    }

    // Get stream from cache, or return null if the image is not cached.
    // Does not throw if there was an error.
    static InputStream getCachedImageStream(URL url, Context context, EnumSet<Options> options) {
        InputStream imageStream = null;
        if (url != null) {
            if (options.contains(Options.FOLLOW_REDIRECTS)) {
                url = getRedirectedURL(context, url.toString());
            }

            if (isCDNURL(url)) {
                try {
                    FileLruCache cache = getCache(context);
                    imageStream = cache.get(url.toString());
                } catch (IOException e) {
                    Logger.log(LoggingBehaviors.CACHE, Log.WARN, TAG, e.toString());
                }
            }
        }

        return imageStream;
    }

    synchronized static FileLruCache getCache(Context context) throws IOException{
        if (imageCache == null) {
            imageCache = new FileLruCache(context.getApplicationContext(), TAG, new FileLruCache.Limits());
        }
        return imageCache;
    }

    static InputStream getImageStream(URL url, Context context) throws IOException {
        return getImageStream(
                url,
                context,
                EnumSet.of(Options.FOLLOW_REDIRECTS, Options.RETURN_STREAM_ON_HTTP_ERROR));
    }

    // Get stream from cache if present, otherwise get from web.
    // If not cached and the uri points to a CDN, store the result in cache.
    static InputStream getImageStream(URL url, Context context, EnumSet<Options> options) throws IOException {
        Validate.notNull(url, "url");
        Validate.notNull(context, "context");

        InputStream stream = null;
        boolean performRequest = true;
        while (performRequest) {
            performRequest = false;
            // See if the url has been cached
            stream = getCachedImageStream(url, context);
            if (stream != null) {
                break;
            }

            // Since it isn't cached, make the network call
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(options.contains(Options.FOLLOW_REDIRECTS));

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                    // redirect. So we need to perform further requests
                    String redirectLocation = connection.getHeaderField("location");
                    if (!Utility.isNullOrEmpty(redirectLocation)) {
                        cacheImageRedirect(context, url, redirectLocation);
                        url = new URL(redirectLocation);
                        performRequest = true;
                    }
                    break;

                case HttpURLConnection.HTTP_OK:
                    // image should be available
                    stream = cacheImageFromStream(
                            context,
                            url,
                            new BufferedHttpInputStream(connection.getInputStream(), connection));
                    break;

                default:
                    if (options.contains(Options.RETURN_STREAM_ON_HTTP_ERROR)) {
                        // If response is not HTTP_OK, return error stream
                        stream = new BufferedHttpInputStream(connection.getErrorStream(), connection);
                    }
                    break;
            }
        }

        return stream;
    }

    private static InputStream cacheImageFromStream(Context context, URL url, InputStream stream) {
        if (isCDNURL(url)) {
            try {
                FileLruCache cache = getCache(context);

                // Wrap stream with a caching stream
                stream = cache.interceptAndPut(url.toString(), stream);
            } catch (IOException e) {
                // Caching is best effort
            }
        }
        return stream;
    }

    private static void cacheImageRedirect(Context context, URL fromUrl, String toUrl) {
        OutputStream redirectStream = null;
        try {
            FileLruCache cache = getCache(context);
            redirectStream = cache.openPutStream(fromUrl.toString(), REDIRECT_CONTENT_TAG);
            redirectStream.write(toUrl.getBytes());
        } catch (IOException e) {
            // Caching is best effort
        } finally {
            Utility.closeQuietly(redirectStream);
        }
    }

    private static URL getRedirectedURL(Context context, String url) {
        URL finalUrl = null;
        InputStreamReader reader = null;
        try {
            InputStream stream;
            FileLruCache cache = getCache(context);
            boolean redirectExists = false;
            while ((stream = cache.get(url, REDIRECT_CONTENT_TAG)) != null) {
                redirectExists = true;

                // Get the redirected url
                reader = new InputStreamReader(stream);
                char[] buffer = new char[128];
                int bufferLength;
                StringBuilder urlBuilder = new StringBuilder();
                while ((bufferLength = reader.read(buffer, 0, buffer.length)) > 0) {
                    urlBuilder.append(buffer, 0, bufferLength);
                }
                Utility.closeQuietly(reader);

                // Iterate to the next url in the redirection
                url = urlBuilder.toString();
            }

            if (redirectExists) {
                finalUrl = new URL(url);
            }
        } catch (MalformedURLException e) {
            // caching is best effort, so ignore the exception
        } catch (IOException ioe) {
        } finally {
            Utility.closeQuietly(reader);
        }

        return finalUrl;
    }

    private static boolean isCDNURL(URL url) {
        if (url != null) {
            String uriHost = url.getHost();

            if (uriHost.endsWith("fbcdn.net")) {
                return true;
            }

            if (uriHost.startsWith("fbcdn") && uriHost.endsWith("akamaihd.net")) {
                return true;
            }
        }

        return false;
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

    enum Options {
        FOLLOW_REDIRECTS,
        RETURN_STREAM_ON_HTTP_ERROR;
        public static final EnumSet<Options> NONE = EnumSet.noneOf(Options.class);
    }
}

