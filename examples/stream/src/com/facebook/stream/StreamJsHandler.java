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
import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;

/**
 * Implements functions that can be called from Javascript in the
 * stream page.
 * 
 * @author yariv
 */
class StreamJsHandler {

    // The handler for the Stream page
    private final StreamHandler streamHandler;

    /**
     * @param streamHandler
     */
    StreamJsHandler(StreamHandler streamHandler) {
        this.streamHandler = streamHandler;
    }

    /**
     * Returns the Facebook object.
     */
    private AsyncFacebookRunner getFb() {
        Facebook fb = Session.restore(streamHandler.getActivity()).getFb();
        return new AsyncFacebookRunner(fb);
    }

    /**
     * Update the status and render the resulting status at the
     * top of the stream.
     *  
     * @param message
     */
    public void updateStatus(final String message) {
        AsyncFacebookRunner fb = getFb();
        Bundle params = new Bundle();
        params.putString("message", message);
        fb.request("me/feed", params, "POST", new AsyncRequestListener() {

            public void onComplete(JSONObject obj, final Object state) {
                String html;
                try {
                    html = renderStatus(obj, message);
                    html = html.replace("'", "\\\'");
                    callJs("onStatusUpdated('" + html + "');");
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }, null);
    }

    /**
     * Renders the html for the new status.
     * 
     * @param response
     * @param message
     * @return
     * @throws JSONException
     */
    private String renderStatus(JSONObject response, String message)
            throws JSONException {

        String postId = response.getString("id");
        JSONObject post = new JSONObject();
        post.put("id", postId);
        post.put("message", message);

        JSONObject from = createAuthorObj();
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
        return html;
    }

    /**
     * Like or unlike a post
     * 
     * @param post_id
     * @param val if the action should be a like (true) or an unlike (false)
     */
    public void like(final String post_id, final boolean val) {
        Bundle params = new Bundle();
        if (!val) {
            params.putString("method", "delete");
        }
        getFb().request(post_id + "/likes", new Bundle(), "POST",
                new AsyncRequestListener() {

            public void onComplete(JSONObject response, final Object state) {
                callJs("javascript:onLike('" + post_id + "'," + val + ")");
            }
        }, null);
    }


    public void postComment(final String post_id, final String message) {
        Bundle params = new Bundle();
        params.putString("message", message);

        getFb().request(post_id + "/comments", params, "POST",
                new AsyncRequestListener() {

            public void onComplete(JSONObject response, final Object state) {

                try {
                    String html = renderComment(response, message);
                    html = html.replace("'", "\\'");
                    callJs("onComment('" + post_id + "','" + html + "');");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, null);
    }

    /**
     * Renders the html string for a new comment.
     * 
     * @param response
     * @param message
     * @return
     * @throws JSONException
     */
    private String renderComment(JSONObject response, String message)
            throws JSONException {

        JSONObject comment = new JSONObject();
        String commentId = response.getString("id");
        comment.put("id", commentId);
        comment.put("from", createAuthorObj());
        comment.put("message", message);

        String html = StreamRenderer.renderSingleComment(comment);
        return html;
    }

    /**
     * Executes javascript code inside WebKit.
     * 
     * @param js
     */
    private void callJs(String js) {
        streamHandler.getWebView().loadUrl("javascript:" + js);
    }

    /**
     * Creates a JSONObject for the post or comment author.
     *  
     * @return
     * @throws JSONException
     */
    private JSONObject createAuthorObj() throws JSONException {
        Session session = Session.restore(streamHandler.getActivity());
        JSONObject from = new JSONObject();
        from.put("id", session.getUid());
        from.put("name", session.getName());
        return from;
    }
}