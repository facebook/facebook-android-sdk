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
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
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
 * @author  Jim Brusstar (jimbru@facebook.com),
 *          Yariv Sadan (yariv@facebook.com),
 *          Luke Shepard (lshepard@facebook.com)
 */
public class Facebook {

    // Strings used in the authorization flow
    public static final String REDIRECT_URI = "fbconnect://success";
    public static final String CANCEL_URI = "fbconnect://cancel";
    public static final String TOKEN = "access_token";
    public static final String EXPIRES = "expires_in";
    public static final String SINGLE_SIGN_ON_DISABLED = "service_disabled";

    public static final int FORCE_DIALOG_AUTH = -1;

    private static final String LOGIN = "oauth";

    // Used as default activityCode by authorize(). See authorize() below.
    private static final int DEFAULT_AUTH_ACTIVITY_CODE = 32665;

    // Facebook server endpoints: may be modified in a subclass for testing
    protected static String DIALOG_BASE_URL =
        "https://m.facebook.com/dialog/";
    protected static String GRAPH_BASE_URL =
        "https://graph.facebook.com/";
    protected static String RESTSERVER_URL =
        "https://api.facebook.com/restserver.php";

    private String mAccessToken = null;
    private long mAccessExpires = 0;
    private String mAppId;

    private Activity mAuthActivity;
    private String[] mAuthPermissions;
    private int mAuthActivityCode;
    private DialogListener mAuthDialogListener;

    /**
     * Constructor for Facebook object.
     *
     * @param appId
     *          Your Facebook application ID. Found at
     *          www.facebook.com/developers/apps.php.
     */
    public Facebook(String appId) {
        if (appId == null) {
            throw new IllegalArgumentException(
                    "You must specify your application ID when instantiating " +
                    "a Facebook object. See README for details.");
        }
        mAppId = appId;
    }

    /**
     * Default authorize method. Grants only basic permissions.
     *
     * See authorize() below for @params.
     */
    public void authorize(Activity activity, final DialogListener listener) {
        authorize(activity, new String[] {}, DEFAULT_AUTH_ACTIVITY_CODE,
                listener);
    }

    /**
     * Authorize method that grants custom permissions.
     *
     * See authorize() below for @params.
     */
    public void authorize(Activity activity, String[] permissions,
            final DialogListener listener) {
        authorize(activity, permissions, DEFAULT_AUTH_ACTIVITY_CODE, listener);
    }

    /**
     * Full authorize method.
     *
     * Starts either an Activity or a dialog which prompts the user to log in to
     * Facebook and grant the requested permissions to the given application.
     *
     * This method will, when possible, use Facebook's single sign-on for
     * Android to obtain an access token. This involves proxying a call through
     * the Facebook for Android stand-alone application, which will handle the
     * authentication flow, and return an OAuth access token for making API
     * calls.
     *
     * Because this process will not be available for all users, if single
     * sign-on is not possible, this method will automatically fall back to the
     * OAuth 2.0 User-Agent flow. In this flow, the user credentials are handled
     * by Facebook in an embedded WebView, not by the client application. As
     * such, the dialog makes a network request and renders HTML content rather
     * than a native UI. The access token is retrieved from a redirect to a
     * special URL that the WebView handles.
     *
     * Note that User credentials could be handled natively using the OAuth 2.0
     * Username and Password Flow, but this is not supported by this SDK.
     *
     * See http://developers.facebook.com/docs/authentication/ and
     * http://wiki.oauth.net/OAuth-2 for more details.
     *
     * Note that this method is asynchronous and the callback will be invoked in
     * the original calling thread (not in a background thread).
     *
     * Also note that requests may be made to the API without calling authorize
     * first, in which case only public information is returned.
     *
     * IMPORTANT: Note that single sign-on authentication will not function
     * correctly if you do not include a call to the authorizeCallback() method
     * in your onActivityResult() function! Please see below for more
     * information. single sign-on may be disabled by passing FORCE_DIALOG_AUTH
     * as the activityCode parameter in your call to authorize().
     *
     * @param activity
     *            The Android activity in which we want to display the
     *            authorization dialog.
     * @param applicationId
     *            The Facebook application identifier e.g. "350685531728"
     * @param permissions
     *            A list of permissions required for this application: e.g.
     *            "read_stream", "publish_stream", "offline_access", etc. see
     *            http://developers.facebook.com/docs/authentication/permissions
     *            This parameter should not be null -- if you do not require any
     *            permissions, then pass in an empty String array.
     * @param activityCode
     *            Single sign-on requires an activity result to be called back
     *            to the client application -- if you are waiting on other
     *            activities to return data, pass a custom activity code here to
     *            avoid collisions. If you would like to force the use of legacy
     *            dialog-based authorization, pass FORCE_DIALOG_AUTH for this
     *            parameter. Otherwise just omit this parameter and Facebook
     *            will use a suitable default. See
     *            http://developer.android.com/reference/android/
     *              app/Activity.html for more information.
     * @param listener
     *            Callback interface for notifying the calling application when
     *            the authentication dialog has completed, failed, or been
     *            canceled.
     */
    public void authorize(Activity activity, String[] permissions,
            int activityCode, final DialogListener listener) {

        boolean singleSignOnStarted = false;

        mAuthDialogListener = listener;

        // Prefer single sign-on, where available.
        if (activityCode >= 0) {
            singleSignOnStarted = startSingleSignOn(activity, mAppId,
                    permissions, activityCode);
        }
        // Otherwise fall back to traditional dialog.
        if (!singleSignOnStarted) {
            startDialogAuth(activity, permissions);
        }
    }

    /**
     * Internal method to handle single sign-on backend for authorize().
     *
     * @param activity
     *            The Android Activity that will parent the ProxyAuth Activity.
     * @param applicationId
     *            The Facebook application identifier.
     * @param permissions
     *            A list of permissions required for this application. If you do
     *            not require any permissions, pass an empty String array.
     * @param activityCode
     *            Activity code to uniquely identify the result Intent in the
     *            callback.
     */
    private boolean startSingleSignOn(Activity activity, String applicationId,
            String[] permissions, int activityCode) {
        boolean didSucceed = true;
        Intent intent = new Intent();

        intent.setClassName("com.facebook.katana",
                "com.facebook.katana.ProxyAuth");
        intent.putExtra("client_id", applicationId);
        if (permissions.length > 0) {
            intent.putExtra("scope", TextUtils.join(",", permissions));
        }

        // Verify that the application whose package name is
        // com.facebook.katana.ProxyAuth
        // has the expected FB app signature.
        if (!validateAppSignatureForIntent(activity, intent)) {
            return false;
        }

        mAuthActivity = activity;
        mAuthPermissions = permissions;
        mAuthActivityCode = activityCode;
        try {
            activity.startActivityForResult(intent, activityCode);
        } catch (ActivityNotFoundException e) {
            didSucceed = false;
        }

        return didSucceed;
    }

    /**
     * Query the signature for the application that would be invoked by the
     * given intent and verify that it matches the FB application's signature.
     *
     * @param activity
     * @param intent
     * @param validSignature
     * @return true if the app's signature matches the expected signature.
     */
    private boolean validateAppSignatureForIntent(Activity activity,
            Intent intent) {

        ResolveInfo resolveInfo =
            activity.getPackageManager().resolveActivity(intent, 0);
        if (resolveInfo == null) {
            return false;
        }

        String packageName = resolveInfo.activityInfo.packageName;
        PackageInfo packageInfo;
        try {
            packageInfo = activity.getPackageManager().getPackageInfo(
                    packageName, PackageManager.GET_SIGNATURES);
        } catch (NameNotFoundException e) {
            return false;
        }

        for (Signature signature : packageInfo.signatures) {
            if (signature.toCharsString().equals(FB_APP_SIGNATURE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Internal method to handle dialog-based authentication backend for
     * authorize().
     *
     * @param activity
     *            The Android Activity that will parent the auth dialog.
     * @param applicationId
     *            The Facebook application identifier.
     * @param permissions
     *            A list of permissions required for this application. If you do
     *            not require any permissions, pass an empty String array.
     */
	private void startDialogAuth(Activity activity, String[] permissions) {
        Bundle params = new Bundle();
        if (permissions.length > 0) {
            params.putString("scope", TextUtils.join(",", permissions));
        }
        CookieSyncManager.createInstance(activity);
        dialog(activity, LOGIN, params, new DialogListener() {

            public void onComplete(Bundle values) {
                // ensure any cookies set by the dialog are saved
                CookieSyncManager.getInstance().sync();
                setAccessToken(values.getString(TOKEN));
                setAccessExpiresIn(values.getString(EXPIRES));
                if (isSessionValid()) {
                    Log.d("Facebook-authorize", "Login Success! access_token="
                            + getAccessToken() + " expires="
                            + getAccessExpires());
                    mAuthDialogListener.onComplete(values);
                } else {
                    mAuthDialogListener.onFacebookError(new FacebookError(
                                    "Failed to receive access token."));
                }
            }

            public void onError(DialogError error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                mAuthDialogListener.onError(error);
            }

            public void onFacebookError(FacebookError error) {
                Log.d("Facebook-authorize", "Login failed: " + error);
                mAuthDialogListener.onFacebookError(error);
            }

            public void onCancel() {
                Log.d("Facebook-authorize", "Login canceled");
                mAuthDialogListener.onCancel();
            }
        });
    }

    /**
     * IMPORTANT: This method must be invoked at the top of the calling
     * activity's onActivityResult() function or Facebook authentication will
     * not function properly!
     *
     * If your calling activity does not currently implement onActivityResult(),
     * you must implement it and include a call to this method if you intend to
     * use the authorize() method in this SDK.
     *
     * For more information, see
     * http://developer.android.com/reference/android/app/
     *   Activity.html#onActivityResult(int, int, android.content.Intent)
     */
    public void authorizeCallback(int requestCode, int resultCode, Intent data) {
        if (requestCode == mAuthActivityCode) {

            // Successfully redirected.
            if (resultCode == Activity.RESULT_OK) {

                // Check OAuth 2.0/2.10 error code.
                String error = data.getStringExtra("error");
                if (error == null) {
                    error = data.getStringExtra("error_type");
                }

                // A Facebook error occurred.
                if (error != null) {
                    if (error.equals(SINGLE_SIGN_ON_DISABLED)
                            || error.equals("AndroidAuthKillSwitchException")) {
                        Log.d("Facebook-authorize", "Hosted auth currently "
                            + "disabled. Retrying dialog auth...");
                        startDialogAuth(mAuthActivity, mAuthPermissions);
                    } else if (error.equals("access_denied")
                            || error.equals("OAuthAccessDeniedException")) {
                        Log.d("Facebook-authorize", "Login canceled by user.");
                        mAuthDialogListener.onCancel();
                    } else {
                        Log.d("Facebook-authorize", "Login failed: " + error);
                        mAuthDialogListener.onFacebookError(
                                new FacebookError(error));
                    }

                // No errors.
                } else {
                    setAccessToken(data.getStringExtra(TOKEN));
                    setAccessExpiresIn(data.getStringExtra(EXPIRES));
                    if (isSessionValid()) {
                        Log.d("Facebook-authorize",
                                "Login Success! access_token="
                                        + getAccessToken() + " expires="
                                        + getAccessExpires());
                        mAuthDialogListener.onComplete(data.getExtras());
                    } else {
                        mAuthDialogListener.onFacebookError(new FacebookError(
                                        "Failed to receive access token."));
                    }
                }

            // An error occurred before we could be redirected.
            } else if (resultCode == Activity.RESULT_CANCELED) {

                // An Android error occured.
                if (data != null) {
                    Log.d("Facebook-authorize",
                            "Login failed: " + data.getStringExtra("error"));
                    mAuthDialogListener.onError(
                            new DialogError(
                                    data.getStringExtra("error"),
                                    data.getIntExtra("error_code", -1),
                                    data.getStringExtra("failing_url")));

                // User pressed the 'back' button.
                } else {
                    Log.d("Facebook-authorize", "Login canceled by user.");
                    mAuthDialogListener.onCancel();
                }
            }
        }
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
     * Make a request to the Facebook Graph API with the given string parameters
     * using an HTTP GET (default method).
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
     * @param params
     *            Key-value string parameters, e.g. the path "search" with
     *            parameters {"q" : "facebook"} would produce a query for the
     *            following graph resource:
     *            https://graph.facebook.com/search?q=facebook
     * @param httpMethod
     *            http verb, e.g. "GET", "POST", "DELETE"
     * @throws IOException
     * @throws MalformedURLException
     * @return JSON string representation of the response
     */
    public String request(String graphPath, Bundle params, String httpMethod)
            throws FileNotFoundException, MalformedURLException, IOException {
        params.putString("format", "json");
        if (isSessionValid()) {
            params.putString(TOKEN, getAccessToken());
        }
        String url = (graphPath != null) ? GRAPH_BASE_URL + graphPath
                                         : RESTSERVER_URL;
        return Util.openUrl(url, httpMethod, params);
    }

    /**
     * Generate a UI dialog for the request action in the given Android context.
     *
     * Note that this method is asynchronous and the callback will be invoked in
     * the original calling thread (not in a background thread).
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
    public void dialog(Context context, String action,
            DialogListener listener) {
        dialog(context, action, new Bundle(), listener);
    }

    /**
     * Generate a UI dialog for the request action in the given Android context
     * with the provided parameters.
     *
     * Note that this method is asynchronous and the callback will be invoked in
     * the original calling thread (not in a background thread).
     *
     * @param context
     *            The Android context in which we will generate this dialog.
     * @param action
     *            String representation of the desired method: e.g. "feed" ...
     * @param parameters
     *            String key-value pairs to be passed as URL parameters.
     * @param listener
     *            Callback interface to notify the application when the dialog
     *            has completed.
     */
    public void dialog(Context context, String action, Bundle parameters,
            final DialogListener listener) {

        String endpoint = DIALOG_BASE_URL + action;
        parameters.putString("display", "touch");
        parameters.putString("redirect_uri", REDIRECT_URI);

        if (action.equals(LOGIN)) {
            parameters.putString("type", "user_agent");
            parameters.putString("client_id", mAppId);
        } else {
            parameters.putString("app_id", mAppId);
        }

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
        return (getAccessToken() != null) &&
                ((getAccessExpires() == 0) ||
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
     * @param token
     *            - access token
     */
    public void setAccessToken(String token) {
        mAccessToken = token;
    }

    /**
     * Set the current session's expiration time (in milliseconds since Unix
     * epoch), or 0 if the session doesn't expire.
     *
     * @param time
     *            - timestamp in milliseconds
     */
    public void setAccessExpires(long time) {
        mAccessExpires = time;
    }

    /**
     * Set the current session's duration (in seconds since Unix epoch).
     *
     * @param expiresIn
     *            - duration in seconds
     */
    public void setAccessExpiresIn(String expiresIn) {
        if (expiresIn != null && !expiresIn.equals("0")) {
            setAccessExpires(System.currentTimeMillis()
                    + Integer.parseInt(expiresIn) * 1000);
        }
    }

    public String getAppId() {
        return mAppId;
    }

    public void setAppId(String appId) {
        mAppId = appId;
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

    public static final String FB_APP_SIGNATURE =
        "30820268308201d102044a9c4610300d06092a864886f70d0101040500307a310"
        + "b3009060355040613025553310b30090603550408130243413112301006035504"
        + "07130950616c6f20416c746f31183016060355040a130f46616365626f6f6b204"
        + "d6f62696c653111300f060355040b130846616365626f6f6b311d301b06035504"
        + "03131446616365626f6f6b20436f72706f726174696f6e3020170d30393038333"
        + "13231353231365a180f32303530303932353231353231365a307a310b30090603"
        + "55040613025553310b30090603550408130243413112301006035504071309506"
        + "16c6f20416c746f31183016060355040a130f46616365626f6f6b204d6f62696c"
        + "653111300f060355040b130846616365626f6f6b311d301b06035504031314466"
        + "16365626f6f6b20436f72706f726174696f6e30819f300d06092a864886f70d01"
        + "0101050003818d0030818902818100c207d51df8eb8c97d93ba0c8c1002c928fa"
        + "b00dc1b42fca5e66e99cc3023ed2d214d822bc59e8e35ddcf5f44c7ae8ade50d7"
        + "e0c434f500e6c131f4a2834f987fc46406115de2018ebbb0d5a3c261bd97581cc"
        + "fef76afc7135a6d59e8855ecd7eacc8f8737e794c60a761c536b72b11fac8e603"
        + "f5da1a2d54aa103b8a13c0dbc10203010001300d06092a864886f70d010104050"
        + "0038181005ee9be8bcbb250648d3b741290a82a1c9dc2e76a0af2f2228f1d9f9c"
        + "4007529c446a70175c5a900d5141812866db46be6559e2141616483998211f4a6"
        + "73149fb2232a10d247663b26a9031e15f84bc1c74d141ff98a02d76f85b2c8ab2"
        + "571b6469b232d8e768a7f7ca04f7abe4a775615916c07940656b58717457b42bd"
        + "928a2";

}
