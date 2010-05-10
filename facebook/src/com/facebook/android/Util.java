package com.facebook.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.Bundle;
import android.util.Log;

/**
 * @author ssoneff@facebook.com
 *
 */
public final class Util {

	public static interface Callback {
		public void call(String result);
	}

	private static final String STRING_BOUNDARY = "$$BOUNDARYajsfljas;l-#019823092";

	public static String encodeUrl(Bundle b) {
		if (b == null) return "";
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String key : b.keySet()) {
			if (first) first = false; else sb.append("&");
			sb.append(key + "=" + b.getString(key));
		}
		Log.d("Facebook-Util", "encode: " + sb.toString());
		return sb.toString();
	}

	public static Bundle decodeUrl(String s) {
		Log.d("Facebook-Util", "decode: " + s);
		Bundle b = new Bundle();
		if (s == null) return b;
		String array[] = s.split("&");
		for (String p : array) {
			String v[] = p.split("=");
			b.putString(v[0], v[1]);
		}
		return b;
	}
	
	public static Bundle parseUrl(String url) {
		url = url.replace("fbconnect", "http"); // HACK to prevent MalformedURLException
		URL u = null;
		try {
			u = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new Bundle();
		}
		Bundle b = decodeUrl(u.getQuery());
		b.putAll(decodeUrl(u.getRef()));
		return b;
	}
	
	public static String openUrl(String url, String method,	String parameters) {
		Log.d("Facebook-Util", "Opening URL: " + url + " Query: " + parameters);
		try {
			if (method.equals("GET")) { url = url + "?" + parameters; }
			HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod(method);
			if (method.equals("POST")) {
				conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + STRING_BOUNDARY);
				conn.getOutputStream().write(parameters.getBytes("UTF-8"));
			}
			return read(conn.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private static String read(InputStream in) throws IOException {
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(in));
		for (String inputLine; (inputLine = r.readLine()) != null; ) sb.append(inputLine);
		in.close();
		return sb.toString();
	}

	public static String join(String[] strings, char delimiter) {
	     StringBuilder sb = new StringBuilder();
	     boolean first = true;
	     for (String s : strings) {
	    	 if (first) first = false; else sb.append(delimiter);
	         sb.append(s);
	     }
	     return sb.toString();
	}

	public static void asyncOpenUrl(final String url, final String httpMethod, final String parameters, final Callback callback) {
		new Thread() {
			@Override public void run() {
				callback.call(openUrl(url, httpMethod, parameters));
			}
		}.run();
	}
    
}
