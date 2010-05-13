package com.facebook.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Util.Callback;

// TODO(ssoneff):
// callback handler?
// error codes?
// make Facebook button
// javadocs
// tidy up eclipse config...

// size, title of uiActivity
// support multiple facebook sessions?  provide session manager?
// wrapper for data: FacebookObject?
// lower pri: auto uiInteraction on session failure?
// request queue?  request callbacks for loading, cancelled?

// Questions:
// fix fbconnect://...
// oauth redirect not working
// oauth does not return expires_in
// expires_in is duration or expiration?
// for errors: use string (message), error codes, or exceptions?
// why callback on both receive response and loaded?
// keep track of which permissions this session has?

public class Facebook {

    public static final String SUCCESS_URI = "fbconnect://success";
    public static final String TOKEN = "access_token";
    public static final String EXPIRES = "expires_in";
    
    protected static String OAUTH_ENDPOINT =
        "http://graph.dev.facebook.com/oauth/authorize";
    protected static String UI_SERVER = 
        "http://www.facebook.com/connect/uiserver.php";
    protected static String GRAPH_BASE_URL = 
        "https://graph.facebook.com/";
    protected static String RESTSERVER_URL = 
        "http://api.facebook.com/restserver.php";
    
    private String mAccessToken = null;
    private long mAccessExpires = 0;


    // Initialization
    public void authorize(Context context,
                          String applicationId,
                          String[] permissions,
                          final DialogListener listener) {
        Bundle params = new Bundle();
        //TODO(brent) fix login page post parameters for display=touch
        //params.putString("display", "touch");
        params.putString("type", "user_agent");
        params.putString("client_id", applicationId);
        params.putString("redirect_uri", SUCCESS_URI);
        params.putString("scope", Util.join(permissions, ","));
        dialog(context, "login", params, new DialogListener() {

            @Override
            public void onDialogSucceed(Bundle values) {
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                Log.d("Facebook-authorize", "Login Succeeded! access_token=" +
                        getAccessToken() + " expires=" + getAccessExpires());
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
    
    
    // API requests

    // support old API: method provided as parameter
    public void request(Bundle parameters,
                        RequestListener listener) {
        request(null, "GET", parameters, listener);
    }

    public void request(String graphPath,
                        RequestListener listener) {
        request(graphPath, "GET", new Bundle(), listener);
    }

    public void request(String graphPath,
                        Bundle parameters,
                        RequestListener listener) {
        request(graphPath, "GET", parameters, listener);
    }

    public void request(String graphPath,
                        String httpMethod,
                        Bundle parameters,
                        final RequestListener listener) {
        if (isSessionValid()) {
            parameters.putString(TOKEN, getAccessToken());
        }
        String url = graphPath != null ?
                GRAPH_BASE_URL + graphPath : 
                RESTSERVER_URL;
        Util.asyncOpenUrl(url, httpMethod, parameters, new Callback() {
            public void call(String response) {
                Log.d("Facebook-SDK", "Got response: " + response);
                try {
                    JSONObject o = new JSONObject(response);
                    if (o.has("error")) {
                        listener.onRequestFail(o.getString("error"));
                    } else {
                        listener.onRequestSucceed(o);
                    }
                } catch (JSONException e) {
                    listener.onRequestFail(e.getMessage());
                }
            }
        });
    }


    // UI Server requests

    public void dialog(Context context,
                       String action,
                       DialogListener listener) {
        dialog(context, action, null, listener);
    }

    public void dialog(Context context,
                       String action, 
                       Bundle parameters,
                       final DialogListener listener) {
        // need logic to determine correct endpoint for resource
        // e.g. "login" --> "oauth/authorize"
        String endpoint = action.equals("login") ? OAUTH_ENDPOINT : UI_SERVER;
        String url = endpoint + "?" + Util.encodeUrl(parameters);
        new FbDialog(context, url, listener).show();
    }

    
    // Utilities

    public boolean isSessionValid() {
        Log.d("Facebook SDK", "session valid? token=" + getAccessToken() + 
            " duration: " + (getAccessExpires() - System.currentTimeMillis()));
        return (getAccessToken() != null) && ((getAccessExpires() == 0) || 
            (System.currentTimeMillis() < getAccessExpires()));
    }

    /**
     * Retrieve the OAuth 2.0 access token for API access: treat with care.
     * Returns null if no session exists.
     * 
     * Note that this method accesses global state and is synchronized for
     * thread safety.
     * 
     * @return access token
     */
    public String getAccessToken() {
        return mAccessToken;
    }

    /**
     * Retrieve the current session's expiration time (in milliseconds since
     * Unix epoch), or 0 is no session exists.
     * 
     * Note that this method accesses global state and is synchronized for
     * thread safety.
     * 
     * @return session expiration time
     */
    public long getAccessExpires() {
        return mAccessExpires;
    }
    
    public void setAccessToken(String token) {
        mAccessToken = token;
    }

    public void setAccessExpires(long time) {
        mAccessExpires = time;
    }
    
    public void setAccessExpiresIn(String expires_in) {
        if (expires_in != null) {
            setAccessExpires(System.currentTimeMillis() + 
                    Integer.parseInt(expires_in) * 1000);
        }
    }
    
    
    // Callback Interfaces

    // Questions:
    // problem: callbacks are called in background thread, not UI thread: changes to UI need to be done in UI thread
    // solution 0: make all the interfaces blocking -- but lots of work for developers to get working!
    // solution 1: let the SDK users handle this -- they write code to post action back to UI thread (current)
    // solution 2: add extra callback methods -- one for background thread to call, on for UI thread (perhaps simplest?)
    // solution 3: let developer explicitly provide handler to run the callback
    // solution 4: run everything in the UI thread
    
    public static abstract class RequestListener {

        public abstract void onRequestSucceed(JSONObject response);

        public abstract void onRequestFail(String error);
    }

    public static abstract class DialogListener {

        public abstract void onDialogSucceed(Bundle values);

        public abstract void onDialogFail(String error);

        public void onDialogCancel() { }
    }
}
