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

import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

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
public class Stream extends Handler {

	private static final String CACHE_FILE = "cache.txt";

	public void go() {
		dispatcher.getWebView().addJavascriptInterface(new StreamJsHandler(), "app");
		
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
		Facebook fb = SessionStore.restoreSession(getActivity());
		//fb.request("me/home", new StreamRequestListener());
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
            Stream.this.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                	Stream.this.renderResult(html);
                }
            });
        }

      
        @Override
        public void onRequestFail(String error) {
            Log.d("SDK-DEBUG", "Request failed: " + error.toString());
        }
    }
	

	private class StreamJsHandler {
		
		public void like(final String post_id, final boolean val) {
			String method = val ? "POST" : "DELETE";
			Log.d("app", post_id + " " + val + " " + method);
			Facebook fb = SessionStore.restoreSession(getActivity());
			fb.request(post_id + "/likes", method, new Bundle(), new StreamApiRequestListener() {

				@Override
				public void onRequestSucceed(JSONObject response) {
					WebView webView = Stream.this.getWebView();
					webView.loadUrl(
							"javascript:onLike('" + post_id + "'," + val + ")");
				}
			});
		}
		
		public void postComment(final String post_id, String message) {
			Bundle params = new Bundle();
			params.putString("message", message);
			
			final Facebook fb = SessionStore.restoreSession(getActivity());
			fb.request(post_id + "/comments", "POST", params, new StreamApiRequestListener() {
				
				@Override
				public void onRequestSucceed(JSONObject response) {
					final String id = response.optString("id");
					
					// Warning: This is a bit hacky. Ideally, we would cache
					// the viewer's name and id locally and use them to construct
					// the comment object so we don't have to make another request.
					
					fb.request(id, new RequestListener() {
						
						@Override
						public void onRequestSucceed(JSONObject response) {
							String comment = StreamRenderer.renderSingleComment(response);
							Log.d("ASDF", post_id + " " + comment);
							comment.replace("'", "\\'");
							getWebView().loadUrl("javascript:onComment('" + post_id + "','" + comment + "');");
							
						}
						
						@Override
						public void onRequestFail(String error) {
							// TODO Auto-generated method stub
							Log.e("app", "failed to fetch comment data: " + error);
						}
					});
					
				}
				
			});
		}
	}
	
	private abstract static class StreamApiRequestListener extends RequestListener {
						
		@Override
		public void onRequestFail(String error) {
			Log.e("app", "fail");
		}
	}
	
	
}
