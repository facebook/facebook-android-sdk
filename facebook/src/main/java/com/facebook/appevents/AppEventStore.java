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

import android.content.Context;
import android.util.Log;

import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.internal.Utility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;

class AppEventStore {
    private static final String TAG = AppEventStore.class.getName();
    private static final String PERSISTED_EVENTS_FILENAME = "AppEventsLogger.persistedevents";

    public static synchronized void persistEvents(
            final AccessTokenAppIdPair accessTokenAppIdPair,
            final SessionEventsState appEvents) {
        AppEventUtility.assertIsNotMainThread();
        PersistedEvents persistedEvents = readAndClearStore();

        if (persistedEvents.containsKey(accessTokenAppIdPair)) {
            persistedEvents
                    .get(accessTokenAppIdPair)
                    .addAll(appEvents.getEventsToPersist());
        } else {
            persistedEvents.addEvents(accessTokenAppIdPair, appEvents.getEventsToPersist());
        }

        saveEventsToDisk(persistedEvents);
    }

    public static synchronized void persistEvents(
            final AppEventCollection eventsToPersist) {
        AppEventUtility.assertIsNotMainThread();
        PersistedEvents persistedEvents = readAndClearStore();
        for (AccessTokenAppIdPair accessTokenAppIdPair : eventsToPersist.keySet()) {
            SessionEventsState sessionEventsState = eventsToPersist.get(
                    accessTokenAppIdPair);
            persistedEvents.addEvents(
                    accessTokenAppIdPair,
                    sessionEventsState.getEventsToPersist());
        }

        saveEventsToDisk(persistedEvents);
    }

    // Only call from singleThreadExecutor
    public static synchronized PersistedEvents readAndClearStore() {
        AppEventUtility.assertIsNotMainThread();

        MovedClassObjectInputStream ois = null;
        PersistedEvents persistedEvents = null;
        Context context = FacebookSdk.getApplicationContext();
        try {
            InputStream is = context.openFileInput(PERSISTED_EVENTS_FILENAME);
            ois = new MovedClassObjectInputStream(new BufferedInputStream(is));

            persistedEvents = (PersistedEvents) ois.readObject();
        } catch (FileNotFoundException e) {
            // Expected if we never persisted any events.
        } catch (Exception e) {
            Log.w(TAG, "Got unexpected exception while reading events: ", e);
        } finally {
            Utility.closeQuietly(ois);


            try {
                // Note: We delete the store before we store the events; this means we'd
                // prefer to lose some events in the case of exception rather than
                // potentially log them twice.
                // Always delete this file after the above try catch to recover from read
                // errors.
                context.getFileStreamPath(PERSISTED_EVENTS_FILENAME).delete();
            } catch (Exception ex) {
                Log.w(TAG, "Got unexpected exception when removing events file: ", ex);
            }
        }

        if (persistedEvents == null) {
            persistedEvents = new PersistedEvents();
        }

        return persistedEvents;
    }

    // Only call from singleThreadExecutor
    private static void saveEventsToDisk(
            PersistedEvents eventsToPersist) {
        ObjectOutputStream oos = null;
        Context context = FacebookSdk.getApplicationContext();
        try {
            oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            context.openFileOutput(PERSISTED_EVENTS_FILENAME, 0)));
            oos.writeObject(eventsToPersist);
        } catch (Exception e) {
            Log.w(TAG, "Got unexpected exception while persisting events: ", e);
            try {
                context.getFileStreamPath(PERSISTED_EVENTS_FILENAME).delete();
            } catch (Exception innerException) {
                // ignore
            }
        } finally {
            Utility.closeQuietly(oos);
        }
    }

    private static class MovedClassObjectInputStream extends ObjectInputStream {
        private static final String ACCESS_TOKEN_APP_ID_PAIR_SERIALIZATION_PROXY_V1_CLASS_NAME =
                "com.facebook.appevents.AppEventsLogger$AccessTokenAppIdPair$SerializationProxyV1";
        private static final String APP_EVENT_SERIALIZATION_PROXY_V1_CLASS_NAME =
                "com.facebook.appevents.AppEventsLogger$AppEvent$SerializationProxyV1";

        public MovedClassObjectInputStream(InputStream in) throws IOException {
            super(in);
        }

        @Override
        protected ObjectStreamClass readClassDescriptor()
                throws IOException, ClassNotFoundException {
            ObjectStreamClass resultClassDescriptor = super.readClassDescriptor();

            if (resultClassDescriptor.getName().equals(
                    ACCESS_TOKEN_APP_ID_PAIR_SERIALIZATION_PROXY_V1_CLASS_NAME)) {
                resultClassDescriptor = ObjectStreamClass.lookup(
                        com.facebook.appevents.AccessTokenAppIdPair.SerializationProxyV1.class);
            } else if (resultClassDescriptor.getName().equals(
                    APP_EVENT_SERIALIZATION_PROXY_V1_CLASS_NAME)) {
                resultClassDescriptor = ObjectStreamClass.lookup(
                        com.facebook.appevents.AppEvent.SerializationProxyV1.class);
            }

            return resultClassDescriptor;
        }
    }
}
