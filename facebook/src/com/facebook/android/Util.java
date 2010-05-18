/*
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

package com.facebook.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;


/**
 * @author ssoneff@facebook.com
 *
 */
public final class Util {

    public static String encodeUrl(Bundle parameters) {
        if (parameters == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : parameters.keySet()) {
            if (first) first = false; else sb.append("&");
            sb.append(key + "=" + parameters.getString(key));
        }
        return sb.toString();
    }

    public static Bundle decodeUrl(String s) {
        Bundle params = new Bundle();
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                params.putString(v[0], v[1]);
            }
        }
        return params;
    }

    public static Bundle parseUrl(String url) {
        // hack to prevent MalformedURLException
        url = url.replace("fbconnect", "http"); 
        try {
            URL u = new URL(url);
            Bundle b = decodeUrl(u.getQuery());
            b.putAll(decodeUrl(u.getRef()));
            return b;
        } catch (MalformedURLException e) {
            return new Bundle();
        }
    }

    public static String openUrl(String url, String method, Bundle params) 
          throws MalformedURLException, IOException {
        if (method.equals("GET") || method.equals("DELETE")) {
            url = url + "?" + encodeUrl(params);
        }
        Log.d("Facebook-Util", method + " URL: " + url);
        HttpURLConnection conn = 
            (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod(method);
        if (method.equals("POST") || method.equals("PUT")) {
            conn.setDoOutput(true);
            conn.getOutputStream().write(
                    encodeUrl(params).getBytes("UTF-8"));
        }
        return read(conn.getInputStream());
    }

    private static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    public static String join(String[] strings, String delimiter) {
        return join(Arrays.asList(strings), delimiter);
    }
    
    public static String join(Iterable<String> strings, String delimiter) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String s : strings) {
            if (first) {
            	first = false; 
            } else {
            	sb.append(delimiter);
            }
            sb.append(s);
        }
        return sb.toString();
    }

    public static void clearCookies(Context context) {
        // Edge case: an illegal state exception is thrown if an instance of 
        // CookieSyncManager has not be created.  CookieSyncManager is normally
        // created by a WebKit view, but this might happen if you start the 
        // app, restore saved state, and click logout before running a UI 
        // dialog in a WebView -- in which case the app crashes
        @SuppressWarnings("unused")
        CookieSyncManager cookieSyncMngr = 
            CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
    }

    public static JSONObject parseJson(String response) throws JSONException {
        // Edge case: when sending a POST request to /[post_id]/likes
        // the return value is 'true' or 'false'. Unfortunately
        // these values cause the JSONObject constructor to throw
        // an exception.
        if (response.equals("false")) {
            throw new JSONException("false");
        }
        if (response.equals("true")) {
            response = "{value : true}";
        }
        JSONObject json = new JSONObject(response);
        if (json.has("error")) {
            throw new JSONException(json.getString("error"));
        }
        return json;
    }
}
