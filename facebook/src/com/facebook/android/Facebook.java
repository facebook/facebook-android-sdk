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

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.facebook.android.Util.Callback;

/**
 * Main Facebook object for interacting with the Facebook developer API.
 * Provides methods to log in and log out a user, make requests using the REST
 * and Graph APIs, and start user interface interactions with the API (such as
 * pop-ups promoting for credentials, permissions, stream posts, etc.)
 * 
 * @author Steven Soneff (ssoneff@facebook.com)
 */
public class Facebook {

    /* Strings used in the OAuth flow */
    public static final String REDIRECT_URI = "fbconnect://success";
    public static final String TOKEN = "access_token";
    public static final String EXPIRES = "expires_in";
    
    private static final String LOGIN = "login";

    /* Facebook server endpoints: may be modified in a subclass for testing */
    protected static String OAUTH_ENDPOINT = 
        "http://graph.facebook.com/oauth/authorize";
    protected static String UI_SERVER = 
        "http://www.facebook.com/connect/uiserver.php";
    protected static String GRAPH_BASE_URL = 
        "https://graph.facebook.com/";
    protected static String RESTSERVER_URL = 
        "https://api.facebook.com/restserver.php";

    private String mAccessToken = null;
    private long mAccessExpires = 0;
    private LinkedList<LogoutListener> mLogoutListeners = 
        new LinkedList<LogoutListener>();

    /**
     * Starts a dialog which prompts the user to log in to Facebook and grant
     * the requested permissions.
     * 
     * @param context
     *            The Android context in which we want to display the
     *            authorization dialog
     * @param applicationId
     *            The Facebook application identifier e.g. "350685531728"
     * @param permissions
     *            A list of permission required for this application: e.g.
     *            "publish_stream", see
     *            http://developers.facebook.com/docs/authentication/permissions
     * @param listener
     *            Callback interface for notifying the calling application when
     *            the dialog has completed, failed, or been canceled.
     */
    public void authorize(Context context, String applicationId,
            String[] permissions, final DialogListener listener) {
        Bundle params = new Bundle();
        // TODO(brent) fix login page post parameters for display=touch
        // params.putString("display", "touch");
        params.putString("type", "user_agent");
        params.putString("client_id", applicationId);
        params.putString("redirect_uri", REDIRECT_URI);
        params.putString("scope", Util.join(permissions, ","));
        dialog(context, LOGIN, params, new DialogListener() {

            @Override
            public void onDialogSucceed(Bundle values) {
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                Log.d("Facebook-authorize", "Login Succeeded! access_token="
                        + getAccessToken() + " expires=" + getAccessExpires());
                listener.onDialogSucceed(values);
            }

            @Override
            public void onDialogFail(String error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                listener.onDialogFail(error);
            }

            @Override
            public void onDialogCancel() {
                Log.d("Facebook-authorize", "Login cancelled");
                listener.onDialogCancel();
            }
        });
    }

    /**
     * Associate the given listener with this facebook object. The listener's
     * callback interface will be invoked when logout occurs.
     * 
     * @param listener
     *            The callback object for notifying the application when log out
     *            starts and finishes.
     */
    public void addLogoutListener(LogoutListener listener) {
        mLogoutListeners.add(listener);
    }

    /**
     * Remove the given listener from the list of those that will be notified
     * when logout occurs.
     * 
     * @param listener
     *            The callback object for notifying the application when log out
     *            starts and finishes.
     */
    public void removeLogoutListener(LogoutListener listener) {
        mLogoutListeners.remove(listener);
    }

    /**
     * Invalidate the current user session by removing the access token in
     * memory, clearing the browser cookie, and calling auth.expireSession
     * through the API. If the application needs to be notified before log out
     * starts (in order to make last API calls, for instance) or after log out
     * has finished (to update UI elements), then be sure to provide an
     * appropriate logout listener.
     * 
     * @see addLogoutListener()
     * 
     * @param context
     *            The Android context in which the logout should be called: it
     *            should be the same context in which the login occurred in
     *            order to clear any stored cookies
     */
    public void logout(Context context) {
        for (LogoutListener l : mLogoutListeners) {
            l.onLogoutBegin();
        }
        @SuppressWarnings("unused")  // Prevent illegal state exception
        CookieSyncManager cookieSyncMngr = 
            CookieSyncManager.createInstance(context);
        
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        
        Bundle b = new Bundle();
        b.putString("method", "auth.expireSession");
        request(b, new RequestListener() {

            @Override
            public void onRequestSucceed(JSONObject response) {
                setAccessToken(null);
                setAccessExpires(0);
                for (LogoutListener l : mLogoutListeners) {
                    l.onLogoutFinish();
                }
            }

            @Override
            public void onRequestFail(String error) {
                Log.w("Facebook-SDK", "auth.expireSession request failed, "
                        + "but local session state cleared");
                onRequestSucceed(null);
            }
        });
    }

    /**
     * Make a request to the Facebook REST API with the given parameters. One of
     * the parameter keys must be "method" and its value should be a valid REST
     * server API method.
     * 
     * See http://developers.facebook.com/docs/reference/rest/
     * 
     * Note that the callback will be invoked in a background thread; operations
     * that affect the UI will need to be posted to the UI thread or an
     * appropriate handler.
     * 
     * @param parameters
     *            Key-value pairs of parameters to the request. Refer to the
     *            documentation.
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void request(Bundle parameters,
                        RequestListener listener) {
        request(null, "GET", parameters, listener);
    }

    /**
     * Make a request to the Facebook Graph API without any parameters.
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that the callback will be invoked in a background thread; operations
     * that affect the UI will need to be posted to the UI thread or an
     * appropriate handler.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void request(String graphPath,
                        RequestListener listener) {
        request(graphPath, "GET", new Bundle(), listener);
    }

    /**
     * Make a request to the Facebook Graph API with the given string parameters
     * using an HTTP GET (default method).
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that the callback will be invoked in a background thread; operations
     * that affect the UI will need to be posted to the UI thread or an
     * appropriate handler.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param parameters
     *            key-value string parameters, e.g. the path "search" with
     *            parameters "q" : "facebook" would produce a query for the
     *            following graph resource:
     *            https://graph.facebook.com/search?q=facebook
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void request(String graphPath,
                        Bundle parameters,
                        RequestListener listener) {
        request(graphPath, "GET", parameters, listener);
    }

    /**
     * Make a request to the Facebook Graph API with the given HTTP method and
     * string parameters. Note that binary data parameters (e.g. pictures) are
     * not yet supported by this helper function.
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that the callback will be invoked in a background thread; operations
     * that affect the UI will need to be posted to the UI thread or an
     * appropriate handler.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param httpMethod
     *            http verb, e.g. "POST", "DELETE"
     * @param parameters
     *            key-value string parameters, e.g. the path "search" with
     *            parameters {"q" : "facebook"} would produce a query for the
     *            following graph resource:
     *            https://graph.facebook.com/search?q=facebook
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void request(String graphPath,
                        String httpMethod, 
                        Bundle parameters,
                        final RequestListener listener) {
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
        parameters.putString("format", "json");
        String url = graphPath != null ? 
                GRAPH_BASE_URL + graphPath : 
                RESTSERVER_URL;
        Util.asyncOpenUrl(url, httpMethod, parameters, new Callback() {
            public void call(String response) {
                Log.d("Facebook-SDK", "Got response: " + response);

                // Edge case: when sending a POST request to /[post_id]/likes
                // the return value is 'true' or 'false'. Unfortunately
                // these values cause the JSONObject constructor to throw
                // an exception.
                if (response.equals("true")) {
                    listener.onRequestSucceed(null);
                    return;
                }
                if (response.equals("false")) {
                    listener.onRequestFail(null);
                    return;
                }

                try {
                    JSONObject json = new JSONObject(response);
                    if (json.has("error")) {
                        listener.onRequestFail(json.getString("error"));
                    } else {
                        listener.onRequestSucceed(json);
                    }
                } catch (JSONException e) {
                    listener.onRequestFail(e.getMessage());
                }
            }
        });
    }

    /**
     * Generate a UI dialog in the given Android context.
     * 
     * @param context
     *            The Android context in which we will generate this dialog.
     * @param action
     *            String representation of the desired method: e.g. "login",
     *            "stream.publish", ...
     * @param listener
     *            Callback interface to notify the application when the dialog
     *            has completed.
     */
    public void dialog(Context context, 
                       String action, 
                       DialogListener listener) {
        dialog(context, action, null, listener);
    }

    /**
     * Generate a UI dialog in the given Android context with the provided
     * parameters.
     * 
     * @param context
     *            The Android context in which we will generate this dialog.
     * @param action
     *            String representation of the desired method: e.g. "login",
     *            "stream.publish", ...
     * @param parameters
     *            key-value string parameters
     * @param listener
     *            Callback interface to notify the application when the dialog
     *            has completed.
     */
    public void dialog(Context context, 
                       String action, 
                       Bundle parameters,
                       final DialogListener listener) {
        // need logic to determine correct endpoint for resource
        // e.g. "login" --> "oauth/authorize"
        String endpoint = action.equals(LOGIN) ? OAUTH_ENDPOINT : UI_SERVER;
        String url = endpoint + "?" + Util.encodeUrl(parameters);
        new FbDialog(context, url, listener).show();
    }

    /**
     * @return boolean - whether this object has an non-expired session token
     */
    public boolean isSessionValid() {
        return (getAccessToken() != null) && ((getAccessExpires() == 0) || 
            (System.currentTimeMillis() < getAccessExpires()));
    }

    /**
     * Retrieve the OAuth 2.0 access token for API access: treat with care.
     * Returns null if no session exists.
     * 
     * @return String - access token
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Retrieve the current session's expiration time (in milliseconds since
     * Unix epoch), or 0 if the session doesn't expire or doesn't exist.
     * 
     * @return long - session expiration time
     */
    public long getAccessExpires() {
        return mAccessExpires;
    }

    /**
     * Set the OAuth 2.0 access token for API access.
     * 
     * @param token - access token
     */
    public void setAccessToken(String token) {
        mAccessToken = token;
    }

    /**
     * Set the current session's expiration time (in milliseconds since Unix
     * epoch), or 0 if the session doesn't expire.
     * 
     * @param time - timestamp in milliseconds
     */
    public void setAccessExpires(long time) {
        mAccessExpires = time;
    }

    /**
     * Set the current session's duration (in seconds since Unix epoch).
     * 
     * @param expiresIn - duration in seconds
     */
    public void setAccessExpiresIn(String expiresIn) {
        if (expiresIn != null) {
            setAccessExpires(System.currentTimeMillis()
                    + Integer.parseInt(expiresIn) * 1000);
        }
    }

    // Callback Interfaces

    // Questions:
    // problem: callbacks are called in background thread, not UI thread:
    // changes to UI need to be done in UI thread
    // solution 0: make all the interfaces blocking -- but lots of work for
    // developers to get working!
    // solution 1: let the SDK users handle this -- they write code to post
    // action back to UI thread (current)
    // solution 2: add extra callback methods -- one for background thread to
    // call, on for UI thread (perhaps simplest?)
    // solution 3: let developer explicitly provide handler to run the callback
    // solution 4: run everything in the UI thread

    public static abstract class LogoutListener {

        public void onLogoutBegin() {
        }

        public void onLogoutFinish() {
        }
    }

    public static abstract class RequestListener {

        public abstract void onRequestSucceed(JSONObject response);

        public abstract void onRequestFail(String error);
    }

    public static abstract class DialogListener {

        public abstract void onDialogSucceed(Bundle values);

        public abstract void onDialogFail(String error);

        public void onDialogCancel() {
        }
    }

}
