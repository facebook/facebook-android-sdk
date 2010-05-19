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

import android.app.Activity;
import android.util.Log;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.RequestListener;

/**
 * A handler for the login page.
 * 
 * @author yariv
 */
public class LoginHandler extends Handler {

	// The permissions that the app should request from the user
	// when the user authorizes the app.
    private static String[] PERMISSIONS = 
        new String[] { "offline_access", "read_stream", "publish_stream" };
    
    /**
     * Render the Login page.
     */
	public void go() {
		dispatcher.getWebView().addJavascriptInterface(
				new JsHandler(), "app");
		dispatcher.loadFile("login.html");
	}
	
	/**
	 * Contains functions that are exported to the Javascript context
	 * in Login.html
	 * 
	 * @author yariv
	 */
	private class JsHandler {

		/**
		 * Opens the Facebook login dialog.
		 */
		public void login() {
			final Activity activity = LoginHandler.this.getActivity();
			activity.runOnUiThread(new Runnable() {
				public void run() {
					// We need to temporarily remove the app's WebView
					// instance because Android apparently doesn't support
					// multiple WebView instances in the same app.
					dispatcher.hideWebView();
					final Facebook fb = new Facebook();
					fb.addLoginListener(new LoginListener(fb));
					fb.authorize(getActivity(), App.FB_APP_ID, PERMISSIONS);
				}
			});
			
		}

		private class AppLoginListener implements AuthListener {
			
			private Facebook fb;
			
			public AuthListener(Facebook fb) {
				this.fb = fb;
			}

			/**
			 * Called after the user authorizes the app.
			 */
    		public void onAuthSucceed() {
    			/**
    			 * We request the user's info so we can cache it locally and
    			 * use it to render the new html snippets
    			 * when the user updates her status or comments on a post. 
    			 */
    			fb.request("/me", new RequestListener() {

					public void onSuccess(String response) {
						
						// save the session data
						String uid = response.optString("id");
						String name = response.optString("name");
                    	new Session(fb, uid, name).save(getActivity());

                    	// render the Stream page in the UI thread
		    			getActivity().runOnUiThread(new Runnable() {
		                    public void run() {
		    					dispatcher.showWebView();
		    					dispatcher.runHandler("stream");
		                    }
		                });							
					}

					public void onFailure(String error) {
						Log.e("app", "login failed: " + error);
					}
				});
	        }

    		public void onAuthFail(String error) {
    			Log.d("app", "login failed: " + error.toString());
    		}

            public void onLogoutBegin() {
            }

            public void onLogoutFinish() {
            }
		}
	}
	
}
