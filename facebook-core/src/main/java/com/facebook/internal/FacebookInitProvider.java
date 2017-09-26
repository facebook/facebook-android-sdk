// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.internal;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.facebook.FacebookSdk;

public final class FacebookInitProvider extends ContentProvider {
    private static final String TAG = FacebookInitProvider.class.getSimpleName();

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCreate() {
        try {
            FacebookSdk.sdkInitialize(getContext());
        } catch (Exception ex) {
            Log.i(TAG, "Failed to auto initialize the Facebook SDK", ex);
        }
        return false;
    }

    @Override
    public Cursor query(
            final Uri uri,
            final String[] projection,
            final String selection,
            final String[] selectionArgs,
            final String sortOrder) {
        return null;
    }

    @Override
    public String getType(final Uri uri) {
        return null;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        return null;
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(
            final Uri uri,
            final ContentValues values,
            final String selection,
            final String[] selectionArgs) {
        return 0;
    }
}
