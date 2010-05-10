package com.facebook.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.facebook.android.Util.Callback;

// TODO(ssoneff):
// make Facebook button
// logout function?

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
	public static final String PREFS_KEY = "facebook-session";
	
	private static final String OAUTH_ENDPOINT = "http://graph.dev.facebook.com/oauth/authorize";
	private static final String UI_SERVER = "http://www.facebook.com/connect/uiserver.php";
	private static final String GRAPH_BASE_URL = "https://graph.facebook.com/";
	private static final String RESTSERVER_URL = "http://api.facebook.com/restserver.php";
	private static final String TOKEN = "oauth_token";
	
	private Context mContext;
	private String mAppId;
	private String mAccessToken = null;
	private long mAccessExpires = 0;
	
	
	// Initialization
	
	public Facebook(Context c, String clientId) {
		this.mContext = c;
		this.mAppId = clientId;
	}

	public void login(DialogListener listener) {
		authorize(null, listener);
	}
	
	public void logout() {
		// TODO(ssoneff) how does logout work? de-auth api method??
		// support multiple logout listeners?
	}
	
	public void authorize(String[] permissions, final DialogListener listener) {
		Bundle params = new Bundle();
		params.putString("display", "touch");
		params.putString("type", "user_agent");
		params.putString("client_id", mAppId);
		params.putString("redirect_uri", SUCCESS_URI);
		if (permissions != null) params.putString("scope", Util.join(permissions, ','));
		dialog("login", params, new DialogListener() {
			
			@Override
			public void onDialogSucceed(Bundle values) {
	    		String token = values.getString("access_token");
	    		String expires = values.getString("expires_in");
	    		Log.d("Facebook", "Success! access_token=" + token + " expires=" + expires);
	    		
				SharedPreferences s = mContext.getSharedPreferences(Facebook.PREFS_KEY, Context.MODE_PRIVATE);
				s.edit().putString("access_token", token);
				s.edit().putString("expires_in", expires);
				
				// WTF? commit does not work on emulator ... file system problem?
				if (s.edit().commit()) {
					Log.d("Facebook-WebView", "changes committed");
				} else {
					Log.d("Facebook-WebView", "changes NOT committed");
				}
				s = mContext.getSharedPreferences(Facebook.PREFS_KEY, Context.MODE_PRIVATE);
				Log.d("Facebook-Callback", "Stored: access_token=" + s.getString("access_token", "NONE"));
				
				setAccessToken(token);
				setAccessExpiresIn(expires);
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
	
	public void request(String graphPath, String httpMethod, Bundle parameters, final RequestListener listener) {
		if (isSessionValid()) { parameters.putString(TOKEN, mAccessToken); }
		String url = graphPath != null ? GRAPH_BASE_URL + graphPath : RESTSERVER_URL;
		Util.asyncOpenUrl(url, httpMethod, Util.encodeUrl(parameters), new Callback() {
			public void call(String response) {
				try {
					JSONObject o = new JSONObject(response);
					if (o.has("error")) {
						listener.onRequestFail(o.getString("error"));
					} else {
						listener.onRequestSucceed(o);
					}
				} catch (JSONException e) {
					e.printStackTrace();
					listener.onRequestFail(e.getMessage());
				}
			}
		});
	}
	
	
	// UI Server requests
	
	public void dialog(String action, DialogListener listener) {
		dialog(action, null, listener);
	}
	
	public void dialog(String action, Bundle parameters, final DialogListener listener) {
		// need logic to determine correct endpoint for resource, e.g. "login" --> "oauth/authorize"
		String endpoint = action.equals("login") ? OAUTH_ENDPOINT : UI_SERVER;
		final String url = parameters == null ? endpoint + "?" + Util.encodeUrl(parameters) : endpoint;
		
		// This is buggy: webview dies with null pointer exception (but not in my code)...
		/*
		final ProgressDialog spinner = ProgressDialog.show(mContext, "Facebook", "Loading...");
		final Handler h = new Handler();
		
		// start async data fetch
		Util.asyncOpenUrl(url, "GET", Util.encodeUrl(parameters), new Callback() {
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
		new FbDialog(mContext, url + "?" + Util.encodeUrl(parameters), "", listener).show();
	}
	
	
	// utilities
	
	public boolean isSessionValid() {
		if (mAccessToken == null) {
			SharedPreferences store = mContext.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
			mAccessToken = store.getString("access_token", null);
			mAccessExpires = store.getLong("expires_in", 0);
			// throw not logged-in exception if no access token? need to call login() first
		}
		Log.d("Facebook SDK", "session valid? token=" + mAccessToken + " duration: " + -(System.currentTimeMillis() - mAccessExpires));
		return mAccessToken != null && (mAccessExpires == 0 || System.currentTimeMillis() < mAccessExpires);
	}

	// get/set
	
	public String getAccessToken() {
		return mAccessToken;
	}

	public long getAccessExpires() {
		return mAccessExpires;
	}
	
	public void setAccessToken(String token) {
		mAccessToken = token;
	}
	
	public void setAccessExpiresIn(String expires_in) {
		if (expires_in != null) mAccessExpires = System.currentTimeMillis() + Integer.parseInt(expires_in)*1000;
	}
	
	
	// callback interfaces
	
	public static abstract class SessionLogoutListener {
		
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
