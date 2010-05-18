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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.LinkedList;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

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
    private LinkedList<AuthListener> mAuthListeners = 
        new LinkedList<AuthListener>();
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
    public void authorize(Context context,
                          String applicationId,
                          String[] permissions) {
        Bundle params = new Bundle();
        params.putString("type", "user_agent");
        params.putString("client_id", applicationId);
        params.putString("redirect_uri", REDIRECT_URI);
        params.putString("scope", Util.join(permissions, ","));
        dialog(context, LOGIN, params, new DialogListener() {

            @Override
            public void onSuccess(Bundle values) {
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                if (isSessionValid()) {
                    Log.d("Facebook-authorize", "Login Success! access_token=" 
                        + getAccessToken() + " expires=" + getAccessExpires());
                    for (AuthListener listener : mAuthListeners) {
                        listener.onAuthSucceed();
                    }
                } else {
                    onError("did not receive access_token");
                }                
            }

            @Override
            public void onError(String error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                for (AuthListener listener : mAuthListeners) {
                    listener.onAuthFail(error);
                }
            }

            @Override
            public void onCancel() {
                Log.d("Facebook-authorize", "Login cancelled");
                onError("User Cancelled");
            }
        });
    }

    /**
     * Associate the given listener with this Facebook object. The listener's
     * callback interface will be invoked when authentication events occur.
     * 
     * @param listener
     *            The callback object for notifying the application when auth
     *            events happen.
     */
    public void addAuthListener(AuthListener listener) {
        mAuthListeners.add(listener);
    }

    /**
     * Remove the given listener from the list of those that will be notified
     * when authentication events occur.
     * 
     * @param listener
     *            The callback object for notifying the application when auth
     *            events happen.
     */
    public void removeAuthListener(AuthListener listener) {
        mAuthListeners.remove(listener);
    }

    /**
     * Associate the given listener with this Facebook object. The listener's
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
        Util.clearCookies(context);
        Bundle b = new Bundle();
        b.putString("method", "auth.expireSession");
        final Handler handler = new Handler();
        asyncRequest(b, new RequestListener() {

            @Override
            public void onSuccess(String response) {
                setAccessToken(null);
                setAccessExpires(0);
                if (response.length() == 0 || response.equals("false")){
                    Log.w("Facebook-SDK", "Server request failed, " +
                          "but local session state cleared");
                }
                handler.post(new Runnable() {
                    
                    @Override
                    public void run() {
                        for (LogoutListener l : mLogoutListeners) {
                            l.onLogoutFinish();
                        }                   
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w("Facebook-SDK", 
                      "auth.expireSession request failed: " + error);
                onSuccess("false");
            }
        });
    }

    /**
     * Make a request to Facebook's old (pre-graph) API with the given 
     * parameters. One of the parameter keys must be "method" and its value 
     * should be a valid REST server API method.  Note that this method blocks 
     * waiting for a network response, so do not call it in a UI thread.
     * 
     * See http://developers.facebook.com/docs/reference/rest/
     * 
     * @param parameters
     *            Key-value pairs of parameters to the request. Refer to the
     *            documentation.
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public String request(Bundle parameters) 
          throws MalformedURLException, IOException {
        return request(null, parameters, "GET");
    }
    
    /**
     * Make a request to the Facebook Graph API without any parameters.  Note
     * that this method blocks waiting for a network response, so do not call 
     * it in a UI thread
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public String request(String graphPath) 
          throws MalformedURLException, IOException {
        return request(graphPath, new Bundle(), "GET");
    }
    
    /**
     * Make a request to the Facebook Graph API with the given string 
     * parameters using an HTTP GET (default method).  Note that this method 
     * blocks waiting for a network response, so do not call it in a UI thread.
     * 
     * See http://developers.facebook.com/docs/api
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
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public void request(String graphPath, Bundle parameters) 
          throws MalformedURLException, IOException {
        request(graphPath, parameters, "GET");
    }
    
    /**
     * Synchronously make a request to the Facebook Graph API with the given
     * HTTP method and string parameters. Note that binary data parameters 
     * (e.g. pictures) are not yet supported by this helper function.
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param parameters
     *            key-value string parameters, e.g. the path "search" with
     *            parameters {"q" : "facebook"} would produce a query for the
     *            following graph resource:
     *            https://graph.facebook.com/search?q=facebook
     * @param httpMethod
     *            http verb, e.g. "POST", "DELETE"
     * @throws IOException 
     * @throws MalformedURLException 
     */
    public String request(String graphPath,
                          Bundle parameters, 
                          String httpMethod) 
          throws MalformedURLException, IOException {
        parameters.putString("format", "json");
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
        String url = graphPath != null ? GRAPH_BASE_URL + graphPath : 
            RESTSERVER_URL;
        return Util.openUrl(url, httpMethod, parameters);
    }
    
    /**
     * Make a request to Facebook's old (pre-graph) API with the given 
     * parameters. One of the parameter keys must be "method" and its value 
     * should be a valid REST server API method.
     * 
     * See http://developers.facebook.com/docs/reference/rest/
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in a background thread; operations that affect the UI will need to be 
     * posted to the UI thread or an appropriate handler.
     * 
     * @param parameters
     *            Key-value pairs of parameters to the request. Refer to the
     *            documentation.
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void asyncRequest(Bundle parameters,
                             RequestListener listener) {
        asyncRequest(null, parameters, "GET", listener);
    }

    /**
     * Make a request to the Facebook Graph API without any parameters.
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in a background thread; operations that affect the UI will need to be 
     * posted to the UI thread or an appropriate handler.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void asyncRequest(String graphPath,
                             RequestListener listener) {
        asyncRequest(graphPath, new Bundle(), "GET", listener);
    }

    /**
     * Make a request to the Facebook Graph API with the given string parameters
     * using an HTTP GET (default method).
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in a background thread; operations that affect the UI will need to be 
     * posted to the UI thread or an appropriate handler.
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
    public void asyncRequest(String graphPath,
                             Bundle parameters,
                             RequestListener listener) {
        asyncRequest(graphPath, parameters, "GET", listener);
    }

    /**
     * Make a request to the Facebook Graph API with the given HTTP method and
     * string parameters. Note that binary data parameters (e.g. pictures) are
     * not yet supported by this helper function.
     * 
     * See http://developers.facebook.com/docs/api
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in a background thread; operations that affect the UI will need to be 
     * posted to the UI thread or an appropriate handler.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @param parameters
     *            key-value string parameters, e.g. the path "search" with
     *            parameters {"q" : "facebook"} would produce a query for the
     *            following graph resource:
     *            https://graph.facebook.com/search?q=facebook
     * @param httpMethod
     *            http verb, e.g. "POST", "DELETE"
     * @param listener
     *            Callback interface to notify the application when the request
     *            has completed.
     */
    public void asyncRequest(final String graphPath,
                             final Bundle parameters, 
                             final String httpMethod,
                             final RequestListener listener) {
        new Thread() {
            @Override public void run() {
                try {
                    String resp = request(graphPath, parameters, httpMethod);
                    listener.onSuccess(resp);
                } catch (MalformedURLException e) {
                    listener.onError(e.getMessage());
                } catch (IOException e) {
                    listener.onError(e.getMessage());
                }
            }
        }.start();
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
        dialog(context, action, new Bundle(), listener);
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
        String endpoint = action.equals(LOGIN) ? OAUTH_ENDPOINT : UI_SERVER;
        parameters.putString("method", action);
        parameters.putString("next", REDIRECT_URI);
        // TODO(luke) auth_token bug needs fix asap so we can take this out
        if (!action.equals(LOGIN)) {
            parameters.putString("display", "touch");
        }
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
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

    /**
     * Callback interface for authorization events.
     *
     */
    public static interface AuthListener {

        /**
         * Called when a auth flow completes successfully and a valid OAuth 
         * Token was received.
         * 
         * Executed by the thread that initiated the authentication.
         * 
         * API requests can now be made.
         */
        public void onAuthSucceed();

        /**
         * Called when a login completes unsuccessfully with an error. 
         *  
         * Executed by the thread that initiated the authentication.
         */
        public void onAuthFail(String error);
    }
    
    /**
     * Callback interface for logout events.
     *
     */ 
    public static interface LogoutListener {
        /**
         * Called when logout begins, before session is invalidated.  
         * Last chance to make an API call.  
         * 
         * Executed by the thread that initiated the logout.
         */
        public void onLogoutBegin();

        /**
         * Called when the session information has been cleared.
         * UI should be updated to reflect logged-out state.
         * 
         * Executed by the thread that initiated the logout.
         */
        public void onLogoutFinish();
    }

    /**
     * Callback interface for API requests.
     *
     */
    public static interface RequestListener {

        /**
         * Called when a request succeeds and response has been parsed to 
         * a JSONObject.
         * 
         * Executed by a background thread: do not update the UI in this method.
         */
        public void onSuccess(String response);

        /**
         * Called when a request completes unsuccessfully with an error.
         * 
         * Executed by a background thread: do not update the UI in this method.
         */
        public void onError(String error);
    }

    /**
     * Callback interface for dialog requests.
     *
     */
    public static interface DialogListener {

        /**
         * Called when a dialog completes successful.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         * @param values
         *            Key-value string pairs extracted from the response.
         */
        public void onSuccess(Bundle values);

        /**
         * Called when a dialog completes unsuccessfully with an error.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         */        
        public void onError(String error);

        /**
         * Called when a dialog is canceled by the user.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         */
        public void onCancel();
    }

}
