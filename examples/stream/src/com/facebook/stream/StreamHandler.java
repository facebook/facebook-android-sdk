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

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

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

	public void go() {
		dispatcher.getWebView().addJavascriptInterface(
				new StreamJsHandler(this), "app");
		
		// first try to load the cached data
		try {
			String cached = FileIO.read(getActivity(), CACHE_FILE);
			if (cached != null) {
				JSONObject obj = new JSONObject(cached);
				renderResult(StreamRenderer.render(obj));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		
		// TODO figure out why the cached result isn't rendered
		// if we send the request.
		Facebook fb = Session.restore(getActivity()).getFb();
		fb.request("me/home", new StreamRequestListener());
	}
	
	public void renderResult(String html) {
		dispatcher.loadData(html);
	}
	
	public class StreamRequestListener extends RequestListener {

        @Override
        public void onRequestSucceed(final JSONObject response) {
        	try {
				FileIO.write(getActivity(), response.toString(), CACHE_FILE);
			} catch (IOException e) {
				e.printStackTrace();
			}
        	final String html = StreamRenderer.render(response);
            StreamHandler.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                	StreamHandler.this.renderResult(html);
                }
            });
        }

      
        @Override
        public void onRequestFail(String error) {
            Log.d("SDK-DEBUG", "Request failed: " + error.toString());
        }
    }
}
