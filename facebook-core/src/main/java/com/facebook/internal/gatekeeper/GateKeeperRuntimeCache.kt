/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
   * @param defaultValue return it if the GateKeeper doesn't exists
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
