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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

/**
 * Implements functions that can be called from Javascript in the
 * stream page.
 * 
 * @author yariv
 */
class StreamJsHandler {
	
	/**
	 * 
	 */
	private final StreamHandler streamHandler;

	/**
	 * @param streamHandler
	 */
	StreamJsHandler(StreamHandler streamHandler) {
		this.streamHandler = streamHandler;
	}

	private Facebook getFb() {
		return Session.restore(streamHandler.getActivity()).getFb();
	}
	
	public void updateStatus(final String message) {
		Facebook fb = getFb();
		Bundle params = new Bundle();
		params.putString("message", message);
		fb.request("me/feed", "POST", params, new ApiRequestListener() {

			@Override
			public void onRequestSucceed(JSONObject response) {
				try {
					String postId = response.getString("id");
					JSONObject post = new JSONObject();
					post.put("id", postId);
					post.put("message", message);
					
					JSONObject from = getFromObj();
					post.put("from", from);
					
					JSONArray actions = new JSONArray();
					JSONObject like = new JSONObject();
					like.put("name", "Like");
					actions.put(like);
					
					JSONObject comment = new JSONObject();
					comment.put("name", "Comment");
					actions.put(comment);
					
					post.put("actions", actions);
					
					SimpleDateFormat format = StreamRenderer.getDateFormat();
					String timestamp = format.format(new Date());
					post.put("created_time", timestamp);
					
					String html = StreamRenderer.renderSinglePost(post);
					html = html.replace("'", "\\\'");
					callJs("onStatusUpdated('" + html + "');");
				} catch (JSONException e) {		
					e.printStackTrace();
				}
				
			}
			
		});
	}
	
	public void like(final String post_id, final boolean val) {
		String method = val ? "POST" : "DELETE";
		getFb().request(post_id + "/likes", method, new Bundle(),
				new ApiRequestListener() {

			@Override
			public void onRequestSucceed(JSONObject response) {
				callJs("javascript:onLike('" + post_id + "'," + val + ")");
			}
		});
	}


	public void postComment(final String post_id, final String message) {
		Bundle params = new Bundle();
		params.putString("message", message);
		
		final Facebook fb = getFb();
		fb.request(post_id + "/comments", "POST", params,
				new ApiRequestListener() {
			
			@Override
			public void onRequestSucceed(JSONObject response) {
				
				try {
					String commentId = response.getString("id");
					
					JSONObject comment = new JSONObject();
					comment.put("id", commentId);
					comment.put("from", getFromObj());
					comment.put("message", message);

					String html = StreamRenderer.renderSingleComment(comment);
					html = html.replace("'", "\\'");
					callJs("onComment('" + post_id + "','" + html + "');");
				} catch (JSONException e) {
					e.printStackTrace();
				}
						
			}
		});
	}
	
	private void callJs(String js) {
		streamHandler.getWebView().loadUrl("javascript:" + js);
	}
	
	private JSONObject getFromObj() throws JSONException {
		Session session = Session.restore(streamHandler.getActivity());				
		JSONObject from = new JSONObject();
		from.put("id", session.getUid());
		from.put("name", session.getName());
		return from;
	}
	
	abstract static class ApiRequestListener extends RequestListener {
		
		@Override
		public void onRequestFail(String error) {
			Log.e("app", "fail");
		}
	}
}