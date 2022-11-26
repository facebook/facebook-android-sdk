/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.io.ObjectStreamException
import java.io.Serializable

@AutoHandleExceptions
internal class PersistedEvents : Serializable {
  private val events = hashMapOf<AccessTokenAppIdPair, MutableList<AppEvent>>()

  constructor()
  constructor(appEventMap: HashMap<AccessTokenAppIdPair, MutableList<AppEvent>>) {
    events.putAll(appEventMap)
  }

  fun keySet(): Set<AccessTokenAppIdPair> = events.keys

  fun entrySet(): Set<Map.Entry<AccessTokenAppIdPair, MutableList<AppEvent>>> = events.entries

  operator fun get(accessTokenAppIdPair: AccessTokenAppIdPair): List<AppEvent>? {
    return events[accessTokenAppIdPair]
  }

  fun containsKey(accessTokenAppIdPair: AccessTokenAppIdPair): Boolean {
    return events.containsKey(accessTokenAppIdPair)
  }

  fun addEvents(accessTokenAppIdPair: AccessTokenAppIdPair, appEvents: List<AppEvent>) {
    if (!events.containsKey(accessTokenAppIdPair)) {
      events[accessTokenAppIdPair] = appEvents.toMutableList()
      return
    }
    events[accessTokenAppIdPair]?.addAll(appEvents)
  }

  internal class SerializationProxyV1(
      private val proxyEvents: HashMap<AccessTokenAppIdPair, MutableList<AppEvent>>
  ) : Serializable {
    @Throws(ObjectStreamException::class)
    private fun readResolve(): Any {
      return PersistedEvents(proxyEvents)
    }

    companion object {
      private const val serialVersionUID = 20160629001L
    }
  }

  @Throws(ObjectStreamException::class)
  private fun writeReplace(): Any {
    return SerializationProxyV1(events)
  }

  companion object {
    private const val serialVersionUID = 20160629001L
  }
}
