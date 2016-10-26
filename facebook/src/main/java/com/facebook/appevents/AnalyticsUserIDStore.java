/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.AppEventUtility;

import java.util.concurrent.locks.ReentrantReadWriteLock;

class AnalyticsUserIDStore {
    private static final String TAG = AnalyticsUserIDStore.class.getSimpleName();
    private static final String ANALYTICS_USER_ID_KEY =
            "com.facebook.appevents.AnalyticsUserIDStore.userID";

    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static String userID;
    private static volatile boolean initialized = false;

    public static void initStore() {
        if (initialized) {
            return;
        }

        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                initAndWait();
            }
        });
    }

    public static void setUserID(final String id) {
        AppEventUtility.assertIsNotMainThread();
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserID");
            initAndWait();
        }

        AppEventsLogger.getAnalyticsExecutor().execute(new Runnable() {
            @Override
            public void run() {
                lock.writeLock().lock();
                try {
                    userID = id;
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(
                                    FacebookSdk.getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(ANALYTICS_USER_ID_KEY, userID);
                    editor.apply();
                } finally {
                    lock.writeLock().unlock();
                }
            }
        });
    }

    public static String getUserID() {
        if (!initialized) {
            Log.w(TAG, "initStore should have been called before calling setUserID");
            initAndWait();
        }

        lock.readLock().lock();
        try {
            return userID;
        } finally {
            lock.readLock().unlock();
        }
    }

    private static void initAndWait() {
        if (initialized) {
            return;
        }

        lock.writeLock().lock();
        try {
            if (initialized) {
                return;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                    FacebookSdk.getApplicationContext());
            userID = sharedPreferences.getString(ANALYTICS_USER_ID_KEY, null);
            initialized = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
