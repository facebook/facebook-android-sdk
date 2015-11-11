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

import android.net.Uri;
import android.util.Log;
import com.facebook.LoggingBehavior;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
class UrlRedirectCache {
    static final String TAG = UrlRedirectCache.class.getSimpleName();
    private static final String REDIRECT_CONTENT_TAG = TAG + "_Redirect";

    private volatile static FileLruCache urlRedirectCache;

    synchronized static FileLruCache getCache() throws IOException{
        if (urlRedirectCache == null) {
            urlRedirectCache = new FileLruCache(TAG, new FileLruCache.Limits());
        }
        return urlRedirectCache;
    }

    static Uri getRedirectedUri(Uri uri) {
        if (uri == null) {
            return null;
        }

        String uriString = uri.toString();
        InputStreamReader reader = null;
        try {
            InputStream stream;
            FileLruCache cache = getCache();
            boolean redirectExists = false;
            while ((stream = cache.get(uriString, REDIRECT_CONTENT_TAG)) != null) {
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
                uriString = urlBuilder.toString();
            }

            if (redirectExists) {
                return Uri.parse(uriString);
            }
        } catch (IOException ioe) {
        } finally {
            Utility.closeQuietly(reader);
        }

        return null;
    }

    static void cacheUriRedirect(Uri fromUri, Uri toUri) {
        if (fromUri == null || toUri == null) {
            return;
        }

        OutputStream redirectStream = null;
        try {
            FileLruCache cache = getCache();
            redirectStream = cache.openPutStream(fromUri.toString(), REDIRECT_CONTENT_TAG);
            redirectStream.write(toUri.toString().getBytes());
        } catch (IOException e) {
            // Caching is best effort
        } finally {
            Utility.closeQuietly(redirectStream);
        }
    }

    static void clearCache() {
        try {
            getCache().clearCache();
        } catch (IOException e) {
            Logger.log(LoggingBehavior.CACHE, Log.WARN, TAG, "clearCache failed " + e.getMessage());
        }
    }
}
