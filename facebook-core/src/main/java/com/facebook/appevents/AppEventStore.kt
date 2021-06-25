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

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.facebook.FacebookSdk
import com.facebook.appevents.internal.AppEventUtility.assertIsNotMainThread
import com.facebook.internal.Utility.closeQuietly
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass

@AutoHandleExceptions
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object AppEventStore {
  private val TAG = AppEventStore::class.java.name
  private const val PERSISTED_EVENTS_FILENAME = "AppEventsLogger.persistedevents"
  @Synchronized
  @JvmStatic
  fun persistEvents(accessTokenAppIdPair: AccessTokenAppIdPair, appEvents: SessionEventsState) {
    assertIsNotMainThread()
    val persistedEvents = readAndClearStore()
    persistedEvents.addEvents(accessTokenAppIdPair, appEvents.eventsToPersist)
    saveEventsToDisk(persistedEvents)
  }

  @Synchronized
  @JvmStatic
  fun persistEvents(eventsToPersist: AppEventCollection) {
    assertIsNotMainThread()
    val persistedEvents = readAndClearStore()
    for (accessTokenAppIdPair in eventsToPersist.keySet()) {
      val sessionEventsState = checkNotNull(eventsToPersist[accessTokenAppIdPair])
      persistedEvents.addEvents(accessTokenAppIdPair, sessionEventsState.eventsToPersist)
    }
    saveEventsToDisk(persistedEvents)
  }

  // Only call from singleThreadExecutor
  @Synchronized
  @JvmStatic
  fun readAndClearStore(): PersistedEvents {
    assertIsNotMainThread()
    var ois: MovedClassObjectInputStream? = null
    var persistedEvents: PersistedEvents? = null
    val context = FacebookSdk.getApplicationContext()
    try {
      val inputStream: InputStream = context.openFileInput(PERSISTED_EVENTS_FILENAME)
      ois = MovedClassObjectInputStream(BufferedInputStream(inputStream))
      persistedEvents = ois.readObject() as PersistedEvents
    } catch (e: FileNotFoundException) {
      // Expected if we never persisted any events.
    } catch (e: Exception) {
      Log.w(TAG, "Got unexpected exception while reading events: ", e)
    } finally {
      closeQuietly(ois)
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
    if (persistedEvents == null) {
      persistedEvents = PersistedEvents()
    }
    return persistedEvents
  }

  // Only call from singleThreadExecutor
  @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
  @JvmStatic
  fun saveEventsToDisk(eventsToPersist: PersistedEvents?) {
    var oos: ObjectOutputStream? = null
    val context = FacebookSdk.getApplicationContext()
    try {
      oos =
          ObjectOutputStream(
              BufferedOutputStream(context.openFileOutput(PERSISTED_EVENTS_FILENAME, 0)))
      oos.writeObject(eventsToPersist)
    } catch (t: Throwable) {
      Log.w(TAG, "Got unexpected exception while persisting events: ", t)
      try {
        context.getFileStreamPath(PERSISTED_EVENTS_FILENAME).delete()
      } catch (innerException: Exception) {
        // ignore
      }
    } finally {
      closeQuietly(oos)
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
