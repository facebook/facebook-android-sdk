package com.facebook.android;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Util.Callback;

// TODO(ssoneff):
// refine callback interface...
// keep track of permissions
// clean up login / logout interface
// make Facebook button
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
    private static final String PERMISSIONS = "scope";
    private static final String KEY = "facebook-session";

    private final Context mContext;
    private final String mAppId;
    private static String mAccessToken = null;
    private static long mAccessExpires = 0;
    private static Set<String> mPermissions = new HashSet<String>();


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

    public void authorize(final String[] permissions,
                          final DialogListener listener) {
        Bundle params = new Bundle();
        //TODO(brent) fix login page post parameters for display=touch
        //params.putString("display", "touch");
        params.putString("type", "user_agent");
        params.putString("client_id", mAppId);
        params.putString("redirect_uri", SUCCESS_URI);
        if (permissions != null) {
            params.putString(PERMISSIONS, Util.join(permissions, ","));
        }
        dialog("login", params, new DialogListener() {

            @Override
            public void onDialogSucceed(Bundle values) {
                addPermissions(permissions);
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

    public void dialog(String action,
                       DialogListener listener) {
        dialog(action, null, listener);
    }

    public void dialog(String action, 
                       Bundle parameters,
                       final DialogListener listener) {
        if (mContext == null) {
            Log.w("Facebook-SDK", "Cannot create a dialog without context");
        }
        // need logic to determine correct endpoint for resource
        // e.g. "login" --> "oauth/authorize"
        String endpoint = action.equals("login") ? OAUTH_ENDPOINT : UI_SERVER;
        String url = endpoint + "?" + Util.encodeUrl(parameters);
        new FbDialog(mContext, url, "", listener).show();
    }

    // utilities

    public boolean isSessionValid() {
        Log.d("Facebook SDK", "session valid? token=" + getAccessToken() + 
            " duration: " + (getAccessExpires() - System.currentTimeMillis()));
        return (getAccessToken() != null) && ((getAccessExpires() == 0) || 
            (System.currentTimeMillis() < getAccessExpires()));
    }

    private void storeSession() {
        Editor editor =
            mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, getAccessToken());
        editor.putLong(EXPIRES, getAccessExpires());
        editor.putString(PERMISSIONS, Util.join(getPermissions(), ","));
        if (editor.commit()) {
            Log.d("Facebook-WebView", "changes committed");
        } else {
            Log.d("Facebook-WebView", "changes NOT committed");
        }
        // hmm ... commit does not work on emulator ... file system problem?
        SharedPreferences s =
            mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        Log.d("FB-DBG", "Stored: access_token=" + s.getString(TOKEN, "NONE"));
    }

    private void restoreSavedSession() {
        SharedPreferences savedSession = 
            mContext.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        setAccessToken(savedSession.getString(TOKEN, null));
        setAccessExpires(savedSession.getLong(EXPIRES, 0));
        addPermissions(savedSession.getString(PERMISSIONS, "").split(","));
    }
    
    private void clearStoredSession() {
        Editor editor = mContext.getSharedPreferences(
                KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
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
    public static synchronized String getAccessToken() {
        return Facebook.mAccessToken;
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
    public static synchronized long getAccessExpires() {
        return Facebook.mAccessExpires;
    }

    /**
     * Retrieve cached set of requested permissions. Note that if the user does
     * not allow permissions, revokes some or signs out and signs in as another
     * user, this list will be inconsistent. To obtain an authoritative list of
     * permissions, query the FQL permissions table for the current UID.
     * 
     * Note that this method accesses global state and is synchronized for
     * thread safety.
     * 
     * @see http://developers.facebook.com/docs/reference/fql/permissions
     * @return array of permission strings (non-authoritative, cached locally)
     */
    public static synchronized String[] getPermissions() {
        return Facebook.mPermissions.toArray(new String[0]);
    }
    
    private static synchronized void setAccessToken(String token) {
        Facebook.mAccessToken = token;
    }

    private static synchronized void setAccessExpires(long time) {
        Facebook.mAccessExpires = time;
    }
    
    private static void setAccessExpiresIn(String expires_in) {
        if (expires_in != null) {
            setAccessExpires(System.currentTimeMillis() + 
                    Integer.parseInt(expires_in) * 1000);
        }
    }

    private static synchronized void addPermissions(String[] permissions) {
        Facebook.mPermissions.addAll(Arrays.asList(permissions));
    }
    
    
    // callback interfaces

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
