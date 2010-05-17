package com.facebook.facedroid;

import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.android.Facebook;

public class App extends Activity {

	public static final String FB_APP_ID = "19151524762";
	
	public static String accessToken;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Dispatcher dispatcher = new Dispatcher(this);
		dispatcher.addHandler("login", Login.class);
		dispatcher.addHandler("stream", Stream.class);
		dispatcher.addHandler("logout", Logout.class);

		Facebook fb = SessionStore.restoreSession(this);
		if (fb != null) {
			dispatcher.runHandler("stream");
		} else {
			dispatcher.runHandler("login");
		}
	}
}
