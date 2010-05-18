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

/**
 * This class implements the application's main Activity.
 * 
 * @author yariv
 */
public class App extends Activity {

	public static final String FB_APP_ID = "124555114230499";
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	
		// Initialize the dispatcher
		Dispatcher dispatcher = new Dispatcher(this);
		dispatcher.addHandler("login", LoginHandler.class);
		dispatcher.addHandler("stream", StreamHandler.class);
		dispatcher.addHandler("logout", LogoutHandler.class);

		// If a session already exists, render the stream page
		// immediately. Otherwise, render the login page.
		Session session = Session.restore(this);
		if (session != null) {
			dispatcher.runHandler("stream");
		} else {
			dispatcher.runHandler("login");
		}
	}
}
