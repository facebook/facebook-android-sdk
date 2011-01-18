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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import android.util.Log;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.AsyncFacebookRunner.RequestListener;

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
        new AsyncFacebookRunner(fb).logout(getActivity(), 
                new RequestListener() {

            public void onComplete(String response, final Object state) {
                dispatcher.runHandler("login");
            }

            public void onFileNotFoundException(FileNotFoundException error,
                                                final Object state) {
                Log.e("app", error.toString());
                dispatcher.runHandler("login");
            }

            public void onIOException(IOException error, final Object state) {
                Log.e("app", error.toString());
                dispatcher.runHandler("login");            
            }

            public void onMalformedURLException(MalformedURLException error,
                                                final Object state) {
                Log.e("app", error.toString());
                dispatcher.runHandler("login");
            }

            public void onFacebookError(FacebookError error,
                                        final Object state) {
                Log.e("app", error.toString());
                dispatcher.runHandler("login");
            }
        });
    }
}
