/**
 * Copyright 2010-present Facebook
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

package com.facebook.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

@SuppressWarnings("deprecation")
public class SessionStore {

    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String LAST_UPDATE = "last_update";
    private static final String KEY = "facebook-session";

    /*
     * Save the access token and expiry date so you don't have to fetch it each
     * time
     */
    public static boolean save(Facebook session, Context context) {
        Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, session.getAccessToken());
        editor.putLong(EXPIRES, session.getAccessExpires());
        editor.putLong(LAST_UPDATE, session.getLastAccessUpdate());
        return editor.commit();
    }

    /*
     * Restore the access token and the expiry date from the shared preferences.
     */
    public static boolean restore(Facebook session, Context context) {
        SharedPreferences savedSession = context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        session.setTokenFromCache(
                savedSession.getString(TOKEN, null),
                savedSession.getLong(EXPIRES, 0),
                savedSession.getLong(LAST_UPDATE, 0));
        return session.isSessionValid();
    }

    public static void clear(Context context) {
        Editor editor = context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

}
