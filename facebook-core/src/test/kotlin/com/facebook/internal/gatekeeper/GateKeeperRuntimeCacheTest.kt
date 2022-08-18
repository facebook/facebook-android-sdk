package com.facebook.internal.gatekeeper

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.whenever
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
    assertThat(dumpMap.getValue(GK2)).isFalse
  }

  @Test
  fun `test empty gate keeper`() {
    val cache = GateKeeperRuntimeCache()

    assertThat(cache.getGateKeeperValue(APPLICATION_ID, GK1, true)).isTrue
    assertThat(cache.getGateKeeperValue(APPLICATION_ID, GK1, false)).isFalse
  }

  @Test
  fun `test multiple applications`() {
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(APPLICATION_ID, GK1, true)
    cache.setGateKeeperValue(APPLICATION_ID2, GK1, false)
    assertThat(cache.getGateKeeperValue(APPLICATION_ID, GK1, false)).isTrue
    assertThat(cache.getGateKeeperValue(APPLICATION_ID2, GK1, true)).isFalse
  }

  @Test
  fun `test write to default app Id`() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_ID2)
    val cache = GateKeeperRuntimeCache()
    cache.setGateKeeperValue(name = GK1, value = true)
    assertThat(cache.getGateKeeperValue(name = GK1, defaultValue = false)).isTrue
    assertThat(cache.getGateKeeperValue(appId = APPLICATION_ID, name = GK1, defaultValue = false))
        .isFalse
  }
}
