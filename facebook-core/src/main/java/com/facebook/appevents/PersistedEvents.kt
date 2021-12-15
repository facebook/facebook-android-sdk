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

  fun keySet(): Set<AccessTokenAppIdPair> {
    return events.keys
  }

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
