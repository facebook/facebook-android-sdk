/*
 * Copyright 2012 Facebook, Inc.
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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

/**
 * Extends the {@link Facebook} object so that it remembers the last access
 * token obtained from Facebook's servers.
 *
 * @author Lorenzo Villani
 */
public class FacebookClient extends Facebook {
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_ACCESS_EXPIRES = "access_expires";

    private final SharedPreferences mSharedPreferences;
    private final String mAccessToken;
    private final long mExpires;

    /**
     * Wraps a {@link DialogListener} saving the access token and its expiration
     * time when users complete the authorization process. Then forward the call
     * to the original {@link DialogListener}.
     *
     * @author Lorenzo Villani
     */
    private final class SaveTokenListener implements DialogListener {
    private final DialogListener mDelegate;

    /**
     * Constructor.
     *
     * @param delegate
     *            The delegate {@link DialogListener}.
     */
    public SaveTokenListener(final DialogListener delegate) {
        this.mDelegate = delegate;
    }

    @Override
    public void onComplete(final Bundle values) {
        final Editor editor = FacebookClient.this.mSharedPreferences.edit();

        editor.putString(KEY_ACCESS_TOKEN, getAccessToken());
        editor.putLong(KEY_ACCESS_EXPIRES, getAccessExpires());
        editor.commit();

        this.mDelegate.onComplete(values);
    }

    @Override
    public void onFacebookError(final FacebookError e) {
        this.mDelegate.onFacebookError(e);
    }

    @Override
    public void onError(final DialogError e) {
        this.mDelegate.onError(e);
    }

    @Override
    public void onCancel() {
        this.mDelegate.onCancel();
    }
    }

    /**
     * Constructor.
     *
     * @param context
     *            The application context.
     * @param appId
     *            This application's Facebook Application ID.
     * @since 1.0.0
     */
    public FacebookClient(final Context context, final String appId) {
        super(appId);

        this.mSharedPreferences = context.getSharedPreferences("facebook",
                                                               Context.MODE_PRIVATE);

        this.mAccessToken = this.mSharedPreferences.getString(KEY_ACCESS_TOKEN,
                                                              null);

        this.mExpires = this.mSharedPreferences.getLong(KEY_ACCESS_EXPIRES, 0);

        if (this.mAccessToken != null) {
            setAccessToken(this.mAccessToken);
        }

        if (this.mExpires != 0) {
            setAccessExpires(this.mExpires);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(final Activity activity, final DialogListener listener) {
        if (!isSessionValid()) {
            super.authorize(activity, new SaveTokenListener(listener));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(final Activity activity, final String[] permissions,
        final DialogListener listener) {
        if (!isSessionValid()) {
            super.authorize(activity, permissions,
                            new SaveTokenListener(listener));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void authorize(final Activity activity, final String[] permissions,
        final int activityCode, final DialogListener listener) {
        if (!isSessionValid()) {
            super.authorize(activity, permissions, activityCode,
                            new SaveTokenListener(listener));
        }
    }
}
