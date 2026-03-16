/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.util.Log
import com.facebook.FacebookSdk
import com.facebook.appevents.internal.AppEventUtility.assertIsNotMainThread
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

/** Responsible for reading and writing app events to/from disk */
internal object AppEventDiskStore {
  private val TAG = AppEventDiskStore::class.java.name
  private const val PERSISTED_EVENTS_FILENAME = "AppEventsLogger.persistedevents"

  // Only call from singleThreadExecutor
  @Synchronized
  @JvmStatic
  fun readAndClearStore(): PersistedEvents {
    assertIsNotMainThread()
    var persistedEvents: PersistedEvents? = null
    val context = FacebookSdk.getApplicationContext()
    try {
      val inputStream: InputStream = context.openFileInput(PERSISTED_EVENTS_FILENAME)
      MovedClassObjectInputStream(BufferedInputStream(inputStream)).use { ois ->
        persistedEvents = ois.readObject() as PersistedEvents
      }
    } catch (e: FileNotFoundException) {
      // Expected if we never persisted any events.
    } catch (e: IOException) {
      Log.w(TAG, "Got unexpected exception while reading events: ", e)
    } catch (e: ClassNotFoundException) {
      Log.w(TAG, "Got unexpected exception while reading events: ", e)
    } finally {
      try {
        // Note: We delete the store before we store the events; this means we'd
        // prefer to lose some events in the case of exception rather than
        // potentially log them twice.
        // Always delete this file after the above try catch to recover from read
        // errors.
        context.getFileStreamPath(PERSISTED_EVENTS_FILENAME).delete()
      } catch (ex: Exception) {
        Log.w(TAG, "Got unexpected exception when removing events file: ", ex)
      }
    }
    return persistedEvents ?: PersistedEvents()
  }

  // Only call from singleThreadExecutor
  @JvmStatic
  internal fun saveEventsToDisk(eventsToPersist: PersistedEvents?) {
    val context = FacebookSdk.getApplicationContext()
    try {
      ObjectOutputStream(
          BufferedOutputStream(context.openFileOutput(PERSISTED_EVENTS_FILENAME, 0))).use { oos ->
        oos.writeObject(eventsToPersist)
      }
    } catch (t: IOException) {
      Log.w(TAG, "Got unexpected exception while persisting events: ", t)
      try {
        context.getFileStreamPath(PERSISTED_EVENTS_FILENAME).delete()
      } catch (innerException: Exception) {
        // ignore
      }
    }
  }

  private class MovedClassObjectInputStream(inputStream: InputStream?) :
      ObjectInputStream(inputStream) {
    @Throws(IOException::class, ClassNotFoundException::class)
    override fun readClassDescriptor(): ObjectStreamClass {
      var resultClassDescriptor = super.readClassDescriptor()
      if (resultClassDescriptor.name ==
          ACCESS_TOKEN_APP_ID_PAIR_SERIALIZATION_PROXY_V1_CLASS_NAME) {
        resultClassDescriptor =
            ObjectStreamClass.lookup(AccessTokenAppIdPair.SerializationProxyV1::class.java)
      } else if (resultClassDescriptor.name == APP_EVENT_SERIALIZATION_PROXY_V1_CLASS_NAME) {
        resultClassDescriptor = ObjectStreamClass.lookup(AppEvent.SerializationProxyV2::class.java)
      }
      return resultClassDescriptor
    }

    companion object {
      private const val ACCESS_TOKEN_APP_ID_PAIR_SERIALIZATION_PROXY_V1_CLASS_NAME =
          "com.facebook.appevents.AppEventsLogger\$AccessTokenAppIdPair\$SerializationProxyV1"
      private const val APP_EVENT_SERIALIZATION_PROXY_V1_CLASS_NAME =
          "com.facebook.appevents.AppEventsLogger\$AppEvent\$SerializationProxyV2"
    }
  }
}
