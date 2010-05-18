package com.facebook.stream;

import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

public class Logout extends Handler {

	public void go() {
		Facebook fb = SessionStore.restoreSession(getActivity());
		
		// clear the local session data
		SessionStore.clearSavedSession(getActivity());

		// Asynchronously, ask the server to expire the access token.
		// This is important to do in case the access token gets compromised somehow.
		Bundle apiParams = new Bundle();
		apiParams.putString("method", "auth.expireSession");
		fb.request(apiParams, new RequestListener() {

			@Override
			public void onRequestSucceed(JSONObject response) {
				Log.d("app", "auth.expireSession succeeded");
			}

			@Override
			public void onRequestFail(String error) {
				Log.d("app", "auth.expireSession failed");
			}
		});
		dispatcher.runHandler("login");
	}
}
