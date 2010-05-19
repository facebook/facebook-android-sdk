package com.facebook.android;

import java.io.IOException;
import java.net.MalformedURLException;

import android.content.Context;
import android.os.Bundle;

public class AsyncFacebook {

	Facebook fb;
	
    public AsyncFacebook(Facebook fb) {
    	this.fb = fb;
    	
        // for testing: TODO(ssoneff) remove
    	Facebook.OAUTH_ENDPOINT = "https://graph.dev.facebook.com/oauth/authorize";
    	Facebook.UI_SERVER = "http://www.dev.facebook.com/connect/uiserver.php";
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
    public void logout(final Context context, final RequestListener listener) {
        new Thread() {
            @Override public void run() {
                try {
                    String response = fb.logout(context);
                    if (response.length() == 0 || response.equals("false")){
                        listener.onError("auth.expireSession failed");
                        return;
                    }
                    listener.onComplete(response);
                } catch (MalformedURLException e) {
                    listener.onError(e.getMessage());
                } catch (IOException e) {
                    listener.onError(e.getMessage());
                }
            }
        }.start();
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
    public void request(Bundle parameters,
                        RequestListener listener) {
        request(null, parameters, "GET", listener);
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
    public void request(String graphPath,
                        RequestListener listener) {
        request(graphPath, new Bundle(), "GET", listener);
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
    public void request(String graphPath,
                        Bundle parameters,
                        RequestListener listener) {
        request(graphPath, parameters, "GET", listener);
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
    public void request(final String graphPath,
                        final Bundle parameters, 
                        final String httpMethod,
                        final RequestListener listener) {
        new Thread() {
            @Override public void run() {
                try {
                    String resp = fb.request(graphPath, parameters, httpMethod);
                    listener.onComplete(resp);
                } catch (MalformedURLException e) {
                    listener.onError(e.getMessage());
                } catch (IOException e) {
                    listener.onError(e.getMessage());
                }
            }
        }.start();
    }

    
    /**
     * Callback interface for API requests.
     *
     */
    public static interface RequestListener {

        /**
         * Called when a request completes with the given response.
         * 
         * Executed by a background thread: do not update the UI in this method.
         */
        public void onComplete(String response);

        /**
         * Called when a request has an error.
         * 
         * Executed by a background thread: do not update the UI in this method.
         */
        public void onError(String error);
    }
    
}
