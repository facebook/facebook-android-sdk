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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.zip.GZIPInputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * Utility class supporting the Facebook Object.
 * 
 * @author ssoneff@facebook.com
 * 
 */
public final class Util {

	// random string as boundary for multi-part http post
	static final String BOUNDARY = "3i2ndDfv2rTHiSisAbouNdArYfORhtTPEefj3q2f";
	static final String END_LINE = "\r\n";
	static final String TWO_HYPHENS = "--";

	static final String ENCODING_GZIP = "gzip";
	static final String METHOD_GET = "GET";

	/**
	 * Set this to true to enable log output. Remember to turn this back off
	 * before releasing. Sending sensitive data to log is a security risk.
	 */
	private static boolean ENABLE_LOG = BuildConfig.DEBUG;

	/**
	 * Generate the multi-part post body providing the parameters and boundary
	 * string
	 * 
	 * @param parameters
	 *            the parameters need to be posted
	 * @param boundary
	 *            the random string as boundary
	 * @return a string of the post body
	 */
	public static String encodePostBody(Bundle parameters, String boundary) {
		if (parameters == null)
			return "";
		StringBuilder sb = new StringBuilder();

		for (String key : parameters.keySet()) {
			Object parameter = parameters.get(key);
			if (!(parameter instanceof String)) {
				continue;
			}

			sb.append("Content-Disposition: form-data; name=\"").append(key)
					.append(END_LINE).append(END_LINE)
					.append(parameter.toString());
			sb.append(END_LINE).append(TWO_HYPHENS).append(boundary)
					.append(END_LINE);
		}

		return sb.toString();
	}

	public static String encodeUrl(String baseUrl, Bundle parameters) {
		Uri uri = Uri.parse(baseUrl);

		if (parameters == null || uri == null) {
			return "";
		}

		Uri.Builder builder = uri.buildUpon();

		for (String key : parameters.keySet()) {
			Object parameter = parameters.get(key);
			if (parameter instanceof String) {
				builder.appendQueryParameter(key, parameter.toString());
			}
		}

		return builder.build().toString();
	}

	public static Bundle decodeUrl(String s) {
		Bundle params = new Bundle();
		if (s != null) {
			String array[] = s.split("&");
			for (String parameter : array) {
				String v[] = parameter.split("=");
				if (v.length == 2) {
					params.putString(URLDecoder.decode(v[0]),
							URLDecoder.decode(v[1]));
				}
			}
		}
		return params;
	}

	/**
	 * Parse a URL query and fragment parameters into a key-value bundle.
	 * 
	 * @param url
	 *            the URL to parse
	 * @return a dictionary bundle of keys and values
	 */
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

	/**
	 * Connect to an HTTP URL and return the response as a string.
	 * 
	 * Note that the HTTP method override is used on non-GET requests. (i.e.
	 * requests are made as "POST" with method specified in the body).
	 * 
	 * @param url
	 *            - the resource to open: must be a welformed URL
	 * @param method
	 *            - the HTTP method to use ("GET", "POST", etc.)
	 * @param params
	 *            - the query parameter for the URL (e.g. access_token=foo)
	 * @return the URL contents as a String
	 * @throws MalformedURLException
	 *             - if the URL format is invalid
	 * @throws IOException
	 *             - if a network problem occurs
	 */
	public static String openUrl(String url, String method, Bundle params)
			throws MalformedURLException, IOException {

		OutputStream os;

		if (METHOD_GET.equalsIgnoreCase(method)) {
			url = encodeUrl(url, params);
		}
		Util.logd("Facebook-Util", method + " URL: " + url);
		HttpURLConnection conn = (HttpURLConnection) new URL(url)
				.openConnection();
		conn.setRequestProperty("User-Agent", System.getProperties()
				.getProperty("http.agent") + " FacebookAndroidSDK");
		conn.setRequestProperty("Accept-Encoding", ENCODING_GZIP);

		if (!METHOD_GET.equalsIgnoreCase(method)) {
			Bundle dataparams = new Bundle();
			Bundle strparams = new Bundle();

			if (null != params) {
				for (final String key : params.keySet()) {
					Object parameter = params.get(key);

					if (parameter instanceof byte[]) {
						dataparams.putByteArray(key, (byte[]) parameter);
					} else {
						strparams.putString(key, parameter.toString());
					}
				}
			}
			params = strparams;

			// use method override
			if (!params.containsKey("method")) {
				params.putString("method", method);
			}

			if (params.containsKey("access_token")) {
				params.putString("access_token",
						URLDecoder.decode(params.getString("access_token")));
			}

			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"multipart/form-data;boundary=" + BOUNDARY);
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setChunkedStreamingMode(0);
			conn.setRequestProperty("Connection", "Keep-Alive");
			conn.connect();
			os = new BufferedOutputStream(conn.getOutputStream());

			StringBuilder sb = new StringBuilder();
			sb.append(TWO_HYPHENS);
			sb.append(BOUNDARY);
			sb.append(END_LINE);

			sb.append(encodePostBody(params, BOUNDARY));
			sb.append(END_LINE);

			sb.append(TWO_HYPHENS);
			sb.append(BOUNDARY);
			sb.append(END_LINE);

			os.write(sb.toString().getBytes());

			if (!dataparams.isEmpty()) {

				for (String key : dataparams.keySet()) {
					os.write(("Content-Disposition: form-data; filename=\""
							+ key + "\"" + END_LINE).getBytes());
					os.write(("Content-Type: content/unknown" + END_LINE + END_LINE)
							.getBytes());
					os.write(dataparams.getByteArray(key));
					os.write((END_LINE + TWO_HYPHENS + BOUNDARY + END_LINE)
							.getBytes());

				}
			}
			os.flush();
			os.close();
		}

		String response = "";
		try {
			InputStream is = conn.getInputStream();

			// If the returned content type is gzip, lets de-compress it
			if (ENCODING_GZIP.equalsIgnoreCase(conn.getContentEncoding())
					&& !(is instanceof GZIPInputStream)) {
				is = new GZIPInputStream(is);
			}
			response = read(is);
		} catch (FileNotFoundException e) {
			// Error Stream contains JSON that we can parse to a FB error
			response = read(conn.getErrorStream());
		}
		return response;
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

	public static void clearCookies(Context context) {
		// Edge case: an illegal state exception is thrown if an instance of
		// CookieSyncManager has not be created. CookieSyncManager is normally
		// created by a WebKit view, but this might happen if you start the
		// app, restore saved state, and click logout before running a UI
		// dialog in a WebView -- in which case the app crashes
		@SuppressWarnings("unused")
		CookieSyncManager cookieSyncMngr = CookieSyncManager
				.createInstance(context);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.removeAllCookie();
	}

	/**
	 * Parse a server response into a JSON Object. This is a basic
	 * implementation using org.json.JSONObject representation. More
	 * sophisticated applications may wish to do their own parsing.
	 * 
	 * The parsed JSON is checked for a variety of error fields and a
	 * FacebookException is thrown if an error condition is set, populated with
	 * the error message and error type or code if available.
	 * 
	 * @param response
	 *            - string representation of the response
	 * @return the response as a JSON Object
	 * @throws JSONException
	 *             - if the response is not valid JSON
	 * @throws FacebookError
	 *             - if an error condition is set
	 */
	public static JSONObject parseJson(String response) throws JSONException,
			FacebookError {
		// Edge case: when sending a POST request to /[post_id]/likes
		// the return value is 'true' or 'false'. Unfortunately
		// these values cause the JSONObject constructor to throw
		// an exception.
		if (response.equals("false")) {
			throw new FacebookError("request failed");
		}
		if (response.equals("true")) {
			response = "{value : true}";
		}
		JSONObject json = new JSONObject(response);

		// errors set by the server are not consistent
		// they depend on the method and endpoint
		if (json.has("error")) {
			JSONObject error = json.getJSONObject("error");
			throw new FacebookError(error.getString("message"),
					error.getString("type"), 0);
		}
		if (json.has("error_code") && json.has("error_msg")) {
			throw new FacebookError(json.getString("error_msg"), "",
					Integer.parseInt(json.getString("error_code")));
		}
		if (json.has("error_code")) {
			throw new FacebookError("request failed", "", Integer.parseInt(json
					.getString("error_code")));
		}
		if (json.has("error_msg")) {
			throw new FacebookError(json.getString("error_msg"));
		}
		if (json.has("error_reason")) {
			throw new FacebookError(json.getString("error_reason"));
		}
		return json;
	}

	/**
	 * Display a simple alert dialog with the given text and title.
	 * 
	 * @param context
	 *            Android context in which the dialog should be displayed
	 * @param title
	 *            Alert dialog title
	 * @param text
	 *            Alert dialog message
	 */
	public static void showAlert(Context context, String title, String text) {
		Builder alertBuilder = new Builder(context);
		alertBuilder.setTitle(title);
		alertBuilder.setMessage(text);
		alertBuilder.create().show();
	}

	/**
	 * A proxy for Log.d api that kills log messages in release build. It not
	 * recommended to send sensitive information to log output in shipping apps.
	 * 
	 * @param tag
	 * @param msg
	 */
	public static void logd(String tag, String msg) {
		if (ENABLE_LOG) {
			Log.d(tag, msg);
		}
	}
}
