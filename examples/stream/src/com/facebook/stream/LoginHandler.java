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
import com.facebook.android.Facebook.SessionListener;

/**
 * A handler for the login page.
 * 
 * @author yariv
 */
public class LoginHandler extends Handler {

    private static String[] PERMISSIONS = 
        new String[] { "offline_access", "read_stream", "publish_stream" };
    
	public void go() {
		dispatcher.getWebView().addJavascriptInterface(new JsHandler(), "app");
		dispatcher.loadFile("login.html");
	}
	
	public void onLogin() {
		dispatcher.runHandler("stream");
	}
	
	private class JsHandler {

		public void login() {
			final Activity activity = LoginHandler.this.getActivity();
			activity.runOnUiThread(new Runnable() {
				public void run() {
					// We need to temporarily remove the app's WebView instance
					// because Android apparently doesn't support multiple
					// instances in a single app.
					LoginHandler.this.dispatcher.remove();
					JsHandler.this.authorize();
				}
			});
			
		}
		
		public void authorize() {
			final Facebook fb = new Facebook();
			fb.addSessionListener(new LoginListener(fb));
			fb.authorize(getActivity(), App.FB_APP_ID, PERMISSIONS);
		}
		
		private class LoginListener implements SessionListener {
			
			private Facebook fb;
			
			public LoginListener(Facebook fb) {
				this.fb = fb;
			}
			
    		public void onAuthSucceed() {
    			fb.request("/me", new RequestListener() {

					public void onRequestSucceed(JSONObject response) {
						String uid = response.optString("id");
						String name = response.optString("name");
                    	new Session(fb, uid, name).save(getActivity());

		    			getActivity().runOnUiThread(new Runnable() {
		                    public void run() {
		    					LoginHandler.this.dispatcher.renderWebView();
		                    	LoginHandler.this.onLogin();
		                    }
		                });							
					}

					public void onRequestFail(String error) {
					}
				});
	        }

    		public void onAuthFail(String error) {
    			Log.d("SDK-DEBUG", "Login failed: " + error.toString());
    		}

            public void onLogoutBegin() {
            }

            public void onLogoutFinish() {
            }
		}
	}
	
}
