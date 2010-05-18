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
package com.facebook.stream;

import org.json.JSONObject;

import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

/**
 * A handler for the logout link. This handler doesn't render
 * its own page. After logging out it redirects the user
 * to the login handler.
 * 
 * @author yariv
 */
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
