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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieSyncManager;

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
    public static final String CANCEL_URI = "fbconnect:cancel";
    public static final String TOKEN = "access_token";
    public static final String EXPIRES = "expires_in";
    
    private static final String LOGIN = "login";

    /* Facebook server endpoints: may be modified in a subclass for testing */
    protected static String OAUTH_ENDPOINT = 
        "https://graph.facebook.com/oauth/authorize";
    protected static String UI_SERVER = 
        "https://www.facebook.com/connect/uiserver.php";
    protected static String GRAPH_BASE_URL = 
        "https://graph.facebook.com/";
    protected static String RESTSERVER_URL = 
        "https://api.facebook.com/restserver.php";

    private String mAccessToken = null;
    private long mAccessExpires = 0;
    
    /**
     * Starts a dialog which prompts the user to log in to Facebook and grant
     * the requested permissions to the given application.
     * 
     * This method implements the OAuth 2.0 User-Agent flow to retrieve an 
     * access token for use in API requests.  In this flow, the user 
     * credentials are handled by Facebook in an embedded WebView, not by the 
     * client application.  As such, the dialog makes a network request and 
     * renders HTML content rather than a native UI.  The access token is 
     * retrieved from a redirect to a special URL that the WebView handles.
     * 
     * Note that User credentials could be handled natively using the 
     * OAuth 2.0 Username and Password Flow, but this is not supported by this
     * SDK.
     * 
     * See http://developers.facebook.com/docs/authentication/ and 
     * http://wiki.oauth.net/OAuth-2 for more details.
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in the original calling thread (not in a background thread).
     * 
     * Also note that requests may be made to the API without calling 
     * authorize first, in which case only public information is returned.
     * 
     * @param context
     *            The Android context in which we want to display the
     *            authorization dialog
     * @param applicationId
     *            The Facebook application identifier e.g. "350685531728"
     * @param permissions
     *            A list of permission required for this application: e.g.
     *            "read_stream", "publish_stream", "offline_access", etc. see
     *            http://developers.facebook.com/docs/authentication/permissions
     *            This parameter should not be null -- if you do not require any
     *            permissions, then pass in an empty String array.
     * @param listener
     *            Callback interface for notifying the calling application when
     *            the dialog has completed, failed, or been canceled.
     */
    public void authorize(Context context,
    					  String applicationId,
                          String[] permissions,
                          final DialogListener listener) {
        Bundle params = new Bundle();
        params.putString("client_id", applicationId);
        if (permissions.length > 0) {
            params.putString("scope", TextUtils.join(",", permissions));
        }
        CookieSyncManager.createInstance(context);
        dialog(context, LOGIN, params, new DialogListener() {

            public void onComplete(Bundle values) {
                // ensure any cookies set by the dialog are saved
                CookieSyncManager.getInstance().sync(); 
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                if (isSessionValid()) {
                    Log.d("Facebook-authorize", "Login Success! access_token=" 
                        + getAccessToken() + " expires=" + getAccessExpires());
                    listener.onComplete(values);
                } else {
                    onFacebookError(new FacebookError(
                            "failed to receive access_token"));
                }                
            }

            public void onError(DialogError error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                listener.onError(error);
            }

            public void onFacebookError(FacebookError error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                listener.onFacebookError(error);
            }

            public void onCancel() {
                Log.d("Facebook-authorize", "Login cancelled");
                listener.onCancel();
            }
        });
    }
    
    /**
     * Invalidate the current user session by removing the access token in
     * memory, clearing the browser cookie, and calling auth.expireSession
     * through the API.  
     * 
     * Note that this method blocks waiting for a network response, so do not
     * call it in a UI thread.
     * 
     * @param context
     *            The Android context in which the logout should be called: it
     *            should be the same context in which the login occurred in
     *            order to clear any stored cookies
     * @throws IOException 
     * @throws MalformedURLException 
     * @return JSON string representation of the auth.expireSession response 
     *            ("true" if successful)
     */
    public String logout(Context context) 
          throws MalformedURLException, IOException {
        Util.clearCookies(context);
        Bundle b = new Bundle();
        b.putString("method", "auth.expireSession");
        String response = request(b);
        setAccessToken(null);
        setAccessExpires(0);
        return response;
    }

    /**
     * Make a request to Facebook's old (pre-graph) API with the given 
     * parameters. One of the parameter keys must be "method" and its value 
     * should be a valid REST server API method.  
     * 
     * See http://developers.facebook.com/docs/reference/rest/
     *  
     * Note that this method blocks waiting for a network response, so do not 
     * call it in a UI thread.
     * 
     * Example: 
     * <code>
     *  Bundle parameters = new Bundle();
     *  parameters.putString("method", "auth.expireSession");
     *  String response = request(parameters);
     * </code>
     * 
     * @param parameters
     *            Key-value pairs of parameters to the request. Refer to the
     *            documentation: one of the parameters must be "method".
     * @throws IOException 
     *            if a network error occurs
     * @throws MalformedURLException 
     *            if accessing an invalid endpoint
     * @throws IllegalArgumentException
     *            if one of the parameters is not "method"
     * @return JSON string representation of the response
     */
    public String request(Bundle parameters) 
          throws MalformedURLException, IOException {
        if (!parameters.containsKey("method")) {
            throw new IllegalArgumentException("API method must be specified. "
                    + "(parameters must contain key \"method\" and value). See"
                    + " http://developers.facebook.com/docs/reference/rest/");
        }
        return request(null, parameters, "GET");
    }
    
    /**
     * Make a request to the Facebook Graph API without any parameters.
     * 
     * See http://developers.facebook.com/docs/api
     *  
     * Note that this method blocks waiting for a network response, so do not 
     * call it in a UI thread.
     * 
     * @param graphPath
     *            Path to resource in the Facebook graph, e.g., to fetch data
     *            about the currently logged authenticated user, provide "me",
     *            which will fetch http://graph.facebook.com/me
     * @throws IOException 
     * @throws MalformedURLException
     * @return JSON string representation of the response
     */
    public String request(String graphPath) 
          throws MalformedURLException, IOException {
        return request(graphPath, new Bundle(), "GET");
    }
    
    /**
     * Make a request to the Facebook Graph API with the given string 
     * parameters using an HTTP GET (default method).
     * 
     * See http://developers.facebook.com/docs/api
     *  
     * Note that this method blocks waiting for a network response, so do not 
     * call it in a UI thread.
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
     * @return JSON string representation of the response
     */
    public String request(String graphPath, Bundle parameters) 
          throws MalformedURLException, IOException {
        return request(graphPath, parameters, "GET");
    }
    
    /**
     * Synchronously make a request to the Facebook Graph API with the given
     * HTTP method and string parameters. Note that binary data parameters 
     * (e.g. pictures) are not yet supported by this helper function.
     * 
     * See http://developers.facebook.com/docs/api
     *  
     * Note that this method blocks waiting for a network response, so do not 
     * call it in a UI thread.
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
     *            http verb, e.g. "GET", "POST", "DELETE"
     * @throws IOException 
     * @throws MalformedURLException 
     * @return JSON string representation of the response
     */
    public String request(String graphPath,
                          Bundle parameters, 
                          String httpMethod) 
          throws FileNotFoundException, MalformedURLException, IOException {
        parameters.putString("format", "json");
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
        String url = graphPath != null ?
            GRAPH_BASE_URL + graphPath : 
            RESTSERVER_URL;
        return Util.openUrl(url, httpMethod, parameters);
    }
    
    /**
     * Generate a UI dialog for the request action in the given Android 
     * context.
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in the original calling thread (not in a background thread).
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
     * Generate a UI dialog for the request action in the given Android 
     * context with the provided parameters.
     * 
     * Note that this method is asynchronous and the callback will be invoked 
     * in the original calling thread (not in a background thread).
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
        String endpoint;
        if (action.equals(LOGIN)) {
            endpoint = OAUTH_ENDPOINT;
            parameters.putString("type", "user_agent");
            parameters.putString("redirect_uri", REDIRECT_URI);
        } else {
            endpoint = UI_SERVER;
            parameters.putString("method", action);
            parameters.putString("next", REDIRECT_URI);
        }
        parameters.putString("display", "touch");
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
        String url = endpoint + "?" + Util.encodeUrl(parameters);
        if (context.checkCallingOrSelfPermission(Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            Util.showAlert(context, "Error", 
                    "Application requires permission to access the Internet");
        } else {
            new FbDialog(context, url, listener).show();
        }
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
        if (expiresIn != null && !expiresIn.equals("0")) {
            setAccessExpires(System.currentTimeMillis()
                    + Integer.parseInt(expiresIn) * 1000);
        }
    }

    
    /**
     * Callback interface for dialog requests.
     *
     */
    public static interface DialogListener {

        /**
         * Called when a dialog completes.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         * @param values
         *            Key-value string pairs extracted from the response.
         */
        public void onComplete(Bundle values);

        /**
         * Called when a Facebook responds to a dialog with an error.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         */        
        public void onFacebookError(FacebookError e);
        
        /**
         * Called when a dialog has an error.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         */        
        public void onError(DialogError e);

        /**
         * Called when a dialog is canceled by the user.
         * 
         * Executed by the thread that initiated the dialog.
         * 
         */
        public void onCancel();
        
    }
    
}
