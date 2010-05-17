package com.facebook.facedroid;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

public class Stream extends Handler {

	private static final String CACHE_FILE = "cache.txt";

	public void go() {
		Facebook fb = SessionStore.getSession();
		
		// first load the cached result
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
		
		
		// TODO figure out why the cached result isn't rendered immediately
		// if the following line is executed.
		//fb.request("me/home", new StreamRequestListener());
	}
	public void renderResult(String html) {
		this.dispatcher.loadData(html);
	}
	
	
	
	public void onUrl(String url) {
		Log.d("facedroid fetch", url);
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
	

	private static class StreamJsHandler {
		
		public void comment(String post_id, String comment) {
			Bundle params = new Bundle();
			params.putString("method", "stream.addComment");
			params.putString("post_id", post_id);
			params.putString("comment", comment);
						
			//Stream.this.fb.request(params, listener);
		}
	}
	
	
}
