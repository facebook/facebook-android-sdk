/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import com.facebook.appevents.AppEventDiskStore.readAndClearStore
import com.facebook.appevents.AppEventDiskStore.saveEventsToDisk
import com.facebook.appevents.internal.AppEventUtility.assertIsNotMainThread
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

@AutoHandleExceptions
internal object AppEventStore {
  private val TAG = AppEventStore::class.java.name

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
}
