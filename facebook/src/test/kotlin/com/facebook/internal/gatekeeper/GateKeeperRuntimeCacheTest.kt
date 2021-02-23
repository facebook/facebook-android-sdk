package com.facebook.internal.gatekeeper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GateKeeperRuntimeCacheTest {

  companion object {
    const val GK1 = "FBSDKFeatureInstrument"
    const val GK2 = "app_events_killswitch"
  }

  @Test
  fun `test map dump`() {
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(GK1, true)
    cache.setGateKeeperValue(GK2, false)

    val dumpMap = HashMap<String, Boolean>()
    cache.dumpGateKeepers().forEach { dumpMap[it.name] = it.value }
    assertTrue(dumpMap.getValue(GK1))
    assertFalse(dumpMap.getValue(GK2))
  }
  @Test
  fun `test empty gate keeper`() {
    val cache = GateKeeperRuntimeCache()

    assertTrue(cache.getGateKeeperValue(GK1, true))
    assertFalse(cache.getGateKeeperValue(GK1, false))
  }
}
