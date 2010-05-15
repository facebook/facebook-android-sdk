package com.facebook.facedroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;

public class Login extends Controller {

	public Login(WebUI webui) {
		super(webui);
	}
	
	public void render() {
		webui.getWebView().addJavascriptInterface(new JsHandler(), "app");
		webui.loadFile("login.html");
	}
	
	public void onLogin(Bundle vals, Facebook fb) {
		webui.render();
		Stream stream = new Stream(webui, fb);
		stream.render();
	}
	
	private class JsHandler {
		
		public void login() {
			final Activity activity = Login.this.getActivity();
			activity.runOnUiThread(new Runnable() {
				public void run() {
					// we need to do this because android apparantly doesn't like
					// having multiple WebView instances
					Login.this.webui.remove();
					JsHandler.this.authorize();
				}
			});
			
		}
		
		public void authorize() {
			final Activity activity = Login.this.getActivity();
			final Facebook fb = new Facebook();
			fb.authorize(activity, App.FB_APP_ID,
							new String[] { "offline_access", "read_stream", "publish_stream" },
							new DialogListener() {

	    		@Override
	    		public void onDialogSucceed(final Bundle values) {
	    			activity.runOnUiThread(new Runnable() {
	                    public void run() {
	                    	App.accessToken = fb.getAccessToken();
	                    	SessionStore.saveSession(fb, activity);
	                    	Login.this.onLogin(values, fb);
	                    }
	                });
		        }

	    		@Override
	    		public void onDialogFail(String error) {
	    			Log.d("SDK-DEBUG", "Login failed: " + error.toString());
	    		}
	    	});
		}
	}
	
}
