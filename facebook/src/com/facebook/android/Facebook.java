package com.facebook.android;

import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.facebook.android.Util.Callback;

// TODO(ssoneff):
// clean up login / logout interface
// keep track of permissions
// make Facebook button
// refine callback interface...
// error codes?
// javadocs
// make endpoint URLs set-able
// fix null pointer exception in webview
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

    private static final String OAUTH_ENDPOINT = "http://graph.dev.facebook.com/oauth/authorize";
    private static final String UI_SERVER = "http://www.facebook.com/connect/uiserver.php";
    private static final String GRAPH_BASE_URL = "https://graph.facebook.com/";
    private static final String RESTSERVER_URL = "http://api.facebook.com/restserver.php";
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-session";

    final private Context mContext;
    final private String mAppId;
    private static String mAccessToken = null;
    private static long mAccessExpires = 0;


    // Initialization

    // for Facebook requests without a context
    public Facebook() {
        this.mContext = null;
        this.mAppId = null;
    }
    
    public Facebook(Context c, String clientId) {
        this.mContext = c;
        this.mAppId = clientId;
        if (getAccessToken() == null) {
            restoreSavedSession();
        }
    }

    public void authorize(String[] permissions,
                          final DialogListener listener) {
        // check if we have a context to run in...
        // check logged in, permissions: abort if login unnecessary
        Bundle params = new Bundle();
        params.putString("display", "touch");
        params.putString("type", "user_agent");
        params.putString("client_id", mAppId);
        params.putString("redirect_uri", SUCCESS_URI);
        if (permissions != null) {
            params.putString("scope", Util.join(permissions, ','));
        }
        dialog("login", params, new DialogListener() {

            @Override
            public void onDialogSucceed(Bundle values) {
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                storeSession();
                Log.d("Facebook", "Success! access_token=" + getAccessToken() +
                        " expires=" + getAccessExpires());
                listener.onDialogSucceed(values);
            }

            @Override
            public void onDialogFail(String error) {
                Log.d("Facebook-Callback", "Dialog failed: " + error);
                listener.onDialogFail(error);
            }

            @Override
            public void onDialogCancel() {
                Log.d("Facebook-Callback", "Dialog cancelled");
                listener.onDialogCancel();
            }
        });
    }
    
    public void logout() {
        setAccessToken("");
        setAccessExpires(0);
        clearStoredSession();
    }
    
    
    // API requests

    // support old API: method provided as parameter
    public void request(Bundle parameters, RequestListener listener) {
        request(null, "GET", parameters, listener);
    }

    public void request(String graphPath, RequestListener listener) {
        request(graphPath, "GET", new Bundle(), listener);
    }

    public void request(String graphPath, Bundle parameters, RequestListener listener) {
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

    public void dialog(String action, DialogListener listener) {
        dialog(action, null, listener);
    }

    public void dialog(String action, 
                       Bundle parameters,
                       final DialogListener listener) {
        if (mContext == null) {
            Log.w("Facebook-SDK", "Cannot create a dialog without context");
        }
        // need logic to determine correct endpoint for resource, e.g. "login" --> "oauth/authorize"
        String endpoint = action.equals("login") ? OAUTH_ENDPOINT : UI_SERVER;
        final String url = endpoint + "?" + Util.encodeUrl(parameters);

        // This is buggy: webview dies with null pointer exception (but not in my code)...
/*
        final ProgressDialog spinner = ProgressDialog.show(mContext, "Facebook", "Loading...");
        final Handler h = new Handler();

        // start async data fetch
        Util.asyncOpenUrl(url, "GET", null, new Callback() {
            @Override public void call(final String response) {
                Log.d("Facebook", "got response: " + response);
                if (response.length() == 0) listener.onDialogFail("Empty response");
                h.post(new Runnable() {
                    @Override public void run() {
                        //callback: close progress dialog
                        spinner.dismiss();
                        new FbDialog(mContext, url, response, listener).show();
                    }
                });
            }
        });
*/
        new FbDialog(mContext, url, "", listener).show();
    }


    // utilities

    public boolean isSessionValid() {
        Log.d("Facebook SDK", "session valid? token=" + getAccessToken() + 
            " duration: " + (getAccessExpires() - System.currentTimeMillis()));
        return getAccessToken() != null && (getAccessExpires() == 0 || 
            System.currentTimeMillis() < getAccessExpires());
    }

    private void storeSession() {
        Editor editor = mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, getAccessToken());
        editor.putLong(EXPIRES, getAccessExpires());
        if (editor.commit()) {
            Log.d("Facebook-WebView", "changes committed");
        } else {
            Log.d("Facebook-WebView", "changes NOT committed");
        }
        // WTF? commit does not work on emulator ... file system problem?
        SharedPreferences s = mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        Log.d("Facebook-Callback", "Stored: access_token=" + s.getString(TOKEN, "NONE"));
    }

    private void restoreSavedSession() {
        SharedPreferences savedSession = 
            mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        setAccessToken(savedSession.getString(TOKEN, null));
        setAccessExpires(savedSession.getLong(EXPIRES, 0));
    }
    
    private void clearStoredSession() {
        Editor editor = mContext.getSharedPreferences(
                KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    } 
    
    
    // get/set

    public static synchronized String getAccessToken() {
        return mAccessToken;
    }

    public static synchronized long getAccessExpires() {
        return mAccessExpires;
    }

    public static synchronized void setAccessToken(String token) {
        mAccessToken = token;
    }

    public static synchronized void setAccessExpires(long time) {
        mAccessExpires = time;
    }
    
    public static void setAccessExpiresIn(String expires_in) {
        if (expires_in != null) {
            setAccessExpires(System.currentTimeMillis() + 
                    Integer.parseInt(expires_in) * 1000);
        }
    }


    // callback interfaces

    // Questions:
    // problem: callbacks are called in background thread, not UI thread: changes to UI need to be done in UI thread
    // solution 0: make all the interfaces blocking -- but lots of work for developers to get working!
    // solution 1: let the SDK users handle this -- they write code to post action back to UI thread (current)
    // solution 2: add extra callback methods -- one for background thread to call, on for UI thread (perhaps simplest?)
    // solution 3: let developer explicitly provide handler to run the callback
    // solution 4: run everything in the UI thread

    public static abstract class LogoutListener {

        public void onSessionLogoutStart() { }

        public void onSessionLogoutFinish() { }
    }

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
