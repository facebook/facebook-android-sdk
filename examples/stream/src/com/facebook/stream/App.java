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
