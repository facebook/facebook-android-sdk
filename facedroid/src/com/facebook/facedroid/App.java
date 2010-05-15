package com.facebook.facedroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.facebook.android.Facebook;

public class App extends Activity {

	public static final String FB_APP_ID = "19151524762";
	
	public static String accessToken;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WebUI webui = new WebUI(this);
		Controller controller;
		//SessionStore.clearSavedSession(this);
		Facebook fb = SessionStore.restoreSession(this);
		if (fb != null) {
			controller = new Stream(webui, fb);
		} else {
			controller = new Login(webui);
		}
		controller.render();
	}
}
