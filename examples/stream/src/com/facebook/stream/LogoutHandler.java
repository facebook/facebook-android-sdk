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

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.SessionListener;

/**
 * A handler for the logout link. This handler doesn't render
 * its own page. After logging out it redirects the user
 * to the login handler.
 * 
 * @author yariv
 */
public class LogoutHandler extends Handler {

	/**
	 * Called by the dispatcher when the user clicks 'logout'.
	 */
	public void go() {
		Facebook fb = Session.restore(getActivity()).getFb();
		
		// clear the local session data
		Session.clearSavedSession(getActivity());
		
		fb.addSessionListener(new SessionListener() {
			public void onLogoutFinish() {
				dispatcher.runHandler("login");
			}

            public void onAuthFail(String error) {  }

            public void onAuthSucceed() { }

            public void onLogoutBegin() { }
		});
		
		fb.logout(getActivity());
	}
}
