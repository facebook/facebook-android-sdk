/*
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

package com.facebook.appevents

import android.preference.PreferenceManager
import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger.Companion.getAnalyticsExecutor
import com.facebook.appevents.internal.AppEventUtility.assertIsNotMainThread
import java.util.concurrent.locks.ReentrantReadWriteLock

internal object AnalyticsUserIDStore {
  private val TAG = AnalyticsUserIDStore::class.java.simpleName
  private const val ANALYTICS_USER_ID_KEY = "com.facebook.appevents.AnalyticsUserIDStore.userID"
  private val lock = ReentrantReadWriteLock()
  private var userID: String? = null

  @Volatile private var initialized = false
  @JvmStatic
  fun initStore() {
    if (initialized) {
      return
    }
    getAnalyticsExecutor().execute { initAndWait() }
  }

  @JvmStatic
  fun setUserID(id: String?) {
    assertIsNotMainThread()
    if (!initialized) {
      Log.w(TAG, "initStore should have been called before calling setUserID")
      initAndWait()
    }
    getAnalyticsExecutor().execute {
      lock.writeLock().lock()
      try {
        userID = id
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
        val editor = sharedPreferences.edit()
        editor.putString(ANALYTICS_USER_ID_KEY, userID)
        editor.apply()
      } finally {
        lock.writeLock().unlock()
      }
    }
  }

  @JvmStatic
  fun getUserID(): String? {
    if (!initialized) {
      Log.w(TAG, "initStore should have been called before calling setUserID")
      initAndWait()
    }
    lock.readLock().lock()
    return try {
      userID
    } finally {
      lock.readLock().unlock()
    }
  }

  private fun initAndWait() {
    if (initialized) {
      return
    }
    lock.writeLock().lock()
    try {
      if (initialized) {
        return
      }
      val sharedPreferences =
          PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
      userID = sharedPreferences.getString(ANALYTICS_USER_ID_KEY, null)
      initialized = true
    } finally {
      lock.writeLock().unlock()
    }
  }
}
