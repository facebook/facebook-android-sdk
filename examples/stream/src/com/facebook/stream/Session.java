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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.android.Facebook;

/**
 * A utility class for storing and retrieving Facebook session data.
 * 
 * @author yariv
 */
public class Session {

    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-session";
    private static final String UID = "uid";
    private static final String NAME = "name";

    private static Session singleton;

    // The Facebook object
    private Facebook fb;

    // The user id of the logged in user
    private String uid;

    // The user name of the logged in user
    private String name;

    /**
     * Constructor
     * 
     * @param fb
     * @param uid
     * @param name
     */
    public Session(Facebook fb, String uid, String name) {
        this.fb = fb;
        this.uid = uid;
        this.name = name;
    }

    /**
     * Returns the Facebook object
     */
    public Facebook getFb() {
        return fb;
    }

    /**
     * Returns the session user's id
     */
    public String getUid() {
        return uid;
    }

    /**
     * Returns the session user's name 
     */
    public String getName() {
        return name;
    }

    /**
     * Stores the session data on disk.
     * 
     * @param context
     * @return
     */
    public boolean save(Context context) {

        Editor editor =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, fb.getAccessToken());
        editor.putLong(EXPIRES, fb.getAccessExpires());
        editor.putString(UID, uid);
        editor.putString(NAME, name);
        if (editor.commit()) {
            singleton = this;
            return true;
        }
        return false;
    }

    /**
     * Loads the session data from disk. If the session
     * has been loaded it's 
     * 
     * @param context
     * @return
     */
    public static Session restore(Context context) {
        if (singleton != null) {
            if (singleton.getFb().isSessionValid()) {
                return singleton;
            } else {
                return null;
            }
        }

        SharedPreferences prefs =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        Facebook fb = new Facebook();
        fb.setAccessToken(prefs.getString(TOKEN, null));
        fb.setAccessExpires(prefs.getLong(EXPIRES, 0));
        String uid = prefs.getString(UID, null);
        String name = prefs.getString(NAME, null);
        if (!fb.isSessionValid() || uid == null || name == null) {
            return null;
        }

        Session session = new Session(fb, uid, name);
        singleton = session;
        return session;
    }

    /**
     * Clears the saved session data.
     * 
     * @param context
     */
    public static void clearSavedSession(Context context) {
        Editor editor = 
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        singleton = null;
    }

}
