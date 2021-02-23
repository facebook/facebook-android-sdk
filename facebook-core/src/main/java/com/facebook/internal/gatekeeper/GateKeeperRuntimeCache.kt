package com.facebook.internal.gatekeeper

import java.util.concurrent.ConcurrentHashMap

class GateKeeperRuntimeCache {
  private val gateKeepers = ConcurrentHashMap<String, GateKeeper>()

  /**
   * Set the cache with a list of name-value pairs
   *
   * @param gateKeeperList The list of GateKeeper name-value pairs
   */
  fun setGateKeepers(gateKeeperList: List<GateKeeper>) {
    gateKeeperList.forEach { gateKeepers[it.name] = it }
  }

  /**
   * Dump the cache into a list of GateKeeper
   *
   * @return the list of GateKeepers where name is the key and GK value is the value
   */
  fun dumpGateKeepers(): List<GateKeeper> {
    return gateKeepers.map { it.value }
  }

  /**
   * Get GateKeeper value with the name
   *
   * @param name the name of the GateKeeper
   * @defaultValue return it if the GateKeeper doesn't exists
   * @return the GateKeeper value
   */
  fun getGateKeeperValue(name: String, defaultValue: Boolean): Boolean =
      getGateKeeper(name)?.value ?: defaultValue

  /**
   * Set GateKeeper value with the name
   *
   * @param name the name of the GateKeeper
   * @param value the new value for this GateKeeper
   */
  fun setGateKeeperValue(name: String, value: Boolean) {
    setGateKeeper(GateKeeper(name, value))
  }

  /**
   * Get GateKeeper with the name
   *
   * @param name the name of the GateKeeper
   * @return the GateKeeper object for this name. null if it doesn't exists
   */
  fun getGateKeeper(name: String) = gateKeepers[name]

  /**
   * Set GateKeeper with a GateKeeper object
   * @param gateKeeper the GateKeeper object of name-value pair.
   */
  fun setGateKeeper(gateKeeper: GateKeeper) {
    gateKeepers[gateKeeper.name] = gateKeeper
  }
}
