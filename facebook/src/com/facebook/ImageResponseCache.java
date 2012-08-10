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
    public static final String TAG = ImageResponseCache.class.getSimpleName();
    
    private volatile static List<String> cdnHosts;
    private volatile static FileLruCache imageCache;
    
    // Get stream from cache is the url is CDN url and the content is in the cache
    // Otherwise open the connection with this url, and if response code is HTTP_OK, 
    // we will get the InputStream from the connection.  And if the url is CDN url, cache
    // the content in the cache.
    // If response code is not HTTP_OK, return ErrorStream from the connection.
    public static InputStream getImageStream(String uri, Context context) throws IOException{
        Validate.notNull(uri, "uri");
        Validate.notNull(context, "context");
        InputStream is = null;
        
        FileLruCache cache = null;
        URL url = new URL(uri);
        if (isCDNURL(url)) {
            // This is CDN connection that we might have the image in cache
            cache = getCache(context);
            try {
                is = cache.get(uri);
            } catch (IOException e) {
                Logger.log(LoggingBehaviors.CACHE, Log.WARN, TAG, "Error to get input stream from cache: " + e);
                // We ignore the error here and leave the is as null,
                // so we can still get the connect directly from the HttoURLConnection
            }
        }
        
        if (is == null) {
            // The image is not in the cache, or failed to get the image from cache.
            // Get the input stream from connection.
            // If the response is OK, and cache is not null, that means cache the response.
            is = getStreamFromConnection(url, cache);
        }
        
        return is;
    }
    
    synchronized static FileLruCache getCache(Context context) throws IOException{
        if (imageCache == null) {
            imageCache = new FileLruCache(context.getApplicationContext(), TAG, new FileLruCache.Limits());
        }
        return imageCache;
    }
    
    private static InputStream getStreamFromConnection(URL url, FileLruCache cache) throws IOException {
        InputStream istream = null;
        HttpURLConnection connection = null;
        connection = (HttpURLConnection)url.openConnection();
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            istream = new BufferedInputStream(connection.getInputStream());
            if (cache != null) {
                // If the cache is not null, we cache the result
                try {
                    InputStream wrapped = cache.interceptAndPut(url.toString(), istream);
                    // If cache succeed, return the wrapped stream
                    istream = wrapped;
                } catch (IOException e) {
                    // Failed to cache the data, return the original InputStream get from connection.
                }
            }
        } else {
            // If response is not HTTP_OK, return error stream
            istream = new BufferedInputStream(connection.getErrorStream());
        }
        return istream;
    }
    
    private static List<String> getCDNHosts() {
        if (cdnHosts == null) {
            cdnHosts = new ArrayList<String>();
            cdnHosts.add("akamaihd.net");
            cdnHosts.add("fbcdn.net");
        }
        return cdnHosts;
    }
    
    private static boolean isCDNURL(URL url) {
        boolean result = false;
        String uriHost = url.getHost();
        List<String> cdnHosts = getCDNHosts();
        for (String host : cdnHosts) {
            if (uriHost.endsWith(host)) {
                result = true;
                break;
            }
        }
        return result;
    }
}

