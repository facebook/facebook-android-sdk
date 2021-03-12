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

package com.facebook.internal.gatekeeper

import com.facebook.FacebookSdk
import java.util.concurrent.ConcurrentHashMap

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
typealias AppID = String

class GateKeeperRuntimeCache {
  private val gateKeepers = ConcurrentHashMap<AppID, ConcurrentHashMap<String, GateKeeper>>()

  /**
   * Set the cache with a list of name-value pairs
   *
   * @param appId Application ID
   * @param gateKeeperList The list of GateKeeper name-value pairs
   */
  fun setGateKeepers(
      appId: AppID = FacebookSdk.getApplicationId(),
      gateKeeperList: List<GateKeeper>
  ) {
    val gateKeepersMap = ConcurrentHashMap<String, GateKeeper>()
    gateKeeperList.forEach { gateKeepersMap[it.name] = it }
    gateKeepers[appId] = gateKeepersMap
  }

  /**
   * Dump the cache into a list of GateKeeper
   *
   * @param appId Application ID
   * @return the list of GateKeepers where name is the key and GK value is the value
   */
  fun dumpGateKeepers(appId: AppID = FacebookSdk.getApplicationId()): List<GateKeeper>? {
    val gateKeepersMap = gateKeepers[appId]
    return gateKeepersMap?.map { it.value }
  }

  /**
   * Get GateKeeper value with the name
   *
   * @param name the name of the GateKeeper
   * @defaultValue return it if the GateKeeper doesn't exists
   * @return the GateKeeper value
   */
  fun getGateKeeperValue(
      appId: AppID = FacebookSdk.getApplicationId(),
      name: String,
      defaultValue: Boolean
  ): Boolean = getGateKeeper(appId, name)?.value ?: defaultValue

  /**
   * Set GateKeeper value with the name
   *
   * @param appId Application ID
   * @param name the name of the GateKeeper
   * @param value the new value for this GateKeeper
   */
  fun setGateKeeperValue(
      appId: AppID = FacebookSdk.getApplicationId(),
      name: String,
      value: Boolean
  ) {
    setGateKeeper(appId, GateKeeper(name, value))
  }

  /**
   * Get GateKeeper with the name
   *
   * @param appId Application ID
   * @param name the name of the GateKeeper
   * @return the GateKeeper object for this name. null if it doesn't exists
   */
  fun getGateKeeper(appId: AppID = FacebookSdk.getApplicationId(), name: String) =
      gateKeepers[appId]?.get(name)

  /**
   * Set GateKeeper with a GateKeeper object
   * @param appId Application ID
   * @param gateKeeper the GateKeeper object of name-value pair.
   */
  fun setGateKeeper(appId: AppID = FacebookSdk.getApplicationId(), gateKeeper: GateKeeper) {
    if (!gateKeepers.containsKey(appId)) {
      gateKeepers[appId] = ConcurrentHashMap()
    }
    gateKeepers[appId]?.set(gateKeeper.name, gateKeeper)
  }

  /**
   * Reset GateKeeper cache of an application
   *
   * @param appId Application ID
   */
  fun resetCache(appId: AppID = FacebookSdk.getApplicationId()) {
    gateKeepers.remove(appId)
  }
}
