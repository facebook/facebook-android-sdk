package com.facebook.android;

import com.facebook.android.Facebook;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class FbUtil {
    
    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-session";

    public static boolean saveSession(Facebook session, Context context) {
        Editor editor =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, session.getAccessToken());
        editor.putLong(EXPIRES, session.getAccessExpires());
        return editor.commit();
    }

    public static boolean restoreSession(Facebook session, Context context) {
        SharedPreferences savedSession =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        session.setAccessToken(savedSession.getString(TOKEN, null));
        session.setAccessExpires(savedSession.getLong(EXPIRES, 0));
        return session.isSessionValid();
    }

    public static void clearSavedSession(Context context) {
        Editor editor = 
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

}
