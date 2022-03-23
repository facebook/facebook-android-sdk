package com.facebook.internal.gatekeeper

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class GateKeeperRuntimeCacheTest : FacebookPowerMockTestCase() {

  companion object {
    const val APPLICATION_ID = "123456789"
    const val APPLICATION_ID2 = "987654321"
    const val GK1 = "FBSDKFeatureInstrument"
    const val GK2 = "app_events_killswitch"
  }

  @Test
  fun `test map dump`() {
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(APPLICATION_ID, GK1, true)
    cache.setGateKeeperValue(APPLICATION_ID, GK2, false)

    val dumpMap = HashMap<String, Boolean>()
    cache.dumpGateKeepers(APPLICATION_ID)?.forEach { dumpMap[it.name] = it.value }
    assertThat(dumpMap.getValue(GK1)).isTrue
    assertFalse(dumpMap.getValue(GK2))
  }

  @Test
  fun `test empty gate keeper`() {
    val cache = GateKeeperRuntimeCache()

    assertThat(cache.getGateKeeperValue(APPLICATION_ID, GK1, true)).isTrue
    assertFalse(cache.getGateKeeperValue(APPLICATION_ID, GK1, false))
  }

  @Test
  fun `test multiple applications`() {
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(APPLICATION_ID, GK1, true)
    cache.setGateKeeperValue(APPLICATION_ID2, GK1, false)
    assertThat(cache.getGateKeeperValue(APPLICATION_ID, GK1, false)).isTrue
    assertFalse(cache.getGateKeeperValue(APPLICATION_ID2, GK1, true))
  }

  @Test
  fun `test write to default app Id`() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_ID2)
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(name = GK1, value = true)
    assertThat(cache.getGateKeeperValue(name = GK1, defaultValue = false)).isTrue
    assertFalse(cache.getGateKeeperValue(appId = APPLICATION_ID, name = GK1, defaultValue = false))
  }
}
