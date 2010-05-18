package com.facebook.stream;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.android.Facebook;

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
