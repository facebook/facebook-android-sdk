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
 * A utility class for storing Facebook session data.
 * 
 * @author yariv
 */
public class SessionStore {
    
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-session";
    
    private static Facebook global;
    
    public static boolean saveSession(Facebook fb, Context context) {
        Editor editor =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, fb.getAccessToken());
        editor.putLong(EXPIRES, fb.getAccessExpires());
        if (editor.commit()) {
            global = fb;
            return true;
        }
        return false;
    }

    public static Facebook restoreSession(Context context) {
    	if (global != null) {
    		if (global.isSessionValid()) {
    			return global;
    		} else {
    			return null;
    		}
    	}
    	
        SharedPreferences savedSession =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        global = new Facebook();
        global.setAccessToken(savedSession.getString(TOKEN, null));
        global.setAccessExpires(savedSession.getLong(EXPIRES, 0));
        if (global.isSessionValid()) {
        	return global;
        }
        return null;
    }

    public static void clearSavedSession(Context context) {
        Editor editor = 
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        global = null;
    }

}
