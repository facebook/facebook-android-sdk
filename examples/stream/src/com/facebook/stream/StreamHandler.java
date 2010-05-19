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
package com.facebook.stream;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.facebook.android.AsyncFacebook;
import com.facebook.android.Facebook;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebook.RequestListener;

/**
 * A handler for the stream page. It's responsible for
 * fetching the stream data from the API and storing it
 * in a local file based cache. It uses the helper class
 * StreamRenderer to render the stream.
 *  
 * @author yariv
 */
public class StreamHandler extends Handler {

	private static final String CACHE_FILE = "cache.txt";

	/**
	 * Called by the dispatcher to render the stream page.
	 */
	public void go() {
		dispatcher.getWebView().addJavascriptInterface(
				new StreamJsHandler(this), "app");
		
		// first try to load the cached data
		try {
			String cached = FileIO.read(getActivity(), CACHE_FILE);
			if (cached != null) {
				JSONObject obj = new JSONObject(cached);
				dispatcher.loadData(StreamRenderer.render(obj));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Facebook fb = Session.restore(getActivity()).getFb();
		new AsyncFacebook(fb).request("me/home", new StreamRequestListener());
	}

	public class StreamRequestListener implements RequestListener {

		public void onComplete(String response) {
			try {
				JSONObject obj = Util.parseJson(response);
		     	// try to cache the result
	        	try {
					FileIO.write(getActivity(), response, CACHE_FILE);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Convert the result into an HTML string and then load it
				// into the WebView in the UI thread.
	        	final String html = StreamRenderer.render(obj);
	            getActivity().runOnUiThread(new Runnable() {
	                public void run() {
	                	dispatcher.loadData(html);
	                }
	            });
	            
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public void onError(String error) {
			Log.d("app", error);
		}
    }
}
