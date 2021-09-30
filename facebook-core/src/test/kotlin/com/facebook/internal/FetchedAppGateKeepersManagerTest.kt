package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.gatekeeper.GateKeeper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.ConcurrentHashMap
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FetchedAppGateKeepersManager::class, FacebookSdk::class)
class FetchedAppGateKeepersManagerTest : FacebookPowerMockTestCase() {

  companion object {
    const val APPLICATION_NAME = "aa"
    const val GK1 = "FBSDKFeatureInstrument"
    const val GK2 = "app_events_killswitch"

    const val VALID_JSON =
        "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"gatekeepers\": [\n" +
            "        {\n" +
            "          \"key\": \"" +
            GK1 +
            "\",\n" +
            "          \"value\": true\n" +
            "        },\n" +
            "        {\n" +
            "          \"key\": \"" +
            GK2 +
            "\",\n" +
            "          \"value\": \"false\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"

    const val NON_BOOLEAN_VALUE_RESPONSE =
        "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"gatekeepers\": [\n" +
            "        {\n" +
            "          \"key\": \"" +
            GK1 +
            "\",\n" +
            "          \"value\": swag\n" +
            "        },\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"

    const val EMPTY_GK_LIST_RESPONSE =
        "{\n" +
            "  \"data\": [\n" +
            "    {\n" +
            "      \"gatekeepers\": [\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}"

    const val EMPTY_RESPONSE = "{}"
    const val EMPTY_DATA_RESPONSE = "{\n" + "  \"data\": [\n" + "  ]\n" + "}"
  }
  private var loadAsyncTimes = 0

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_NAME)
    loadAsyncTimes = 0
    mockStatic(FetchedAppGateKeepersManager::class.java)

    whenever(
            FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(
                eq(APPLICATION_NAME), anyOrNull()))
        .thenCallRealMethod()

    whenever(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), anyOrNull(), any()))
        .thenCallRealMethod()

    whenever(FetchedAppGateKeepersManager.getGateKeepersForApplication(anyOrNull()))
        .thenCallRealMethod()

    whenever(FetchedAppGateKeepersManager.setRuntimeGateKeeper(any(), any())).thenCallRealMethod()

    whenever(FetchedAppGateKeepersManager.resetRuntimeGateKeeperCache()).thenCallRealMethod()

    whenever(FetchedAppGateKeepersManager.loadAppGateKeepersAsync()).then { loadAsyncTimes++ }

    // because it is a static variable which holds a lot of state about the GKs, we need to reset it
    // every time
    Whitebox.setInternalState(
        FetchedAppGateKeepersManager::class.java,
        "fetchedAppGateKeepers",
        ConcurrentHashMap<String, JSONObject>())
  }

  @After
  fun clean() {
    FetchedAppGateKeepersManager.resetRuntimeGateKeeperCache()
  }

  @Test
  fun `parse valid json_ok`() {
    val test = JSONObject(VALID_JSON)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertFalse(result.getBoolean(GK2))
    assertTrue(result.getBoolean(GK1))

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertTrue(map[GK1]!!)
    assertFalse(map[GK2]!!)

    val gk1 = FetchedAppGateKeepersManager.getGateKeeperForKey(GK1, APPLICATION_NAME, false)
    val gk2 = FetchedAppGateKeepersManager.getGateKeeperForKey(GK2, APPLICATION_NAME, true)
    assertTrue(gk1)
    assertFalse(gk2)
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertEquals(3, loadAsyncTimes)
  }

  @Test
  fun `parse value isnt boolean_fail`() {
    val test = JSONObject(NON_BOOLEAN_VALUE_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertEquals(0, result.length())

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    // current parser filters out non boolean values, otherwise map will actually return the exact
    // value
    assertEquals(null, map[GK1])

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey(GK1, APPLICATION_NAME, false)
    assertFalse(gk)
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertEquals(2, loadAsyncTimes)
  }

  @Test
  fun `parse empty list of gks_fail`() {
    val test = JSONObject(EMPTY_GK_LIST_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertEquals(0, result.length())

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertEquals(0, map.size)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertFalse(gk)
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertEquals(2, loadAsyncTimes)
  }

  @Test
  fun `parse empty response_fail`() {
    val test = JSONObject(EMPTY_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertEquals(0, result.length())

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertEquals(0, map.size)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertFalse(gk)
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertEquals(2, loadAsyncTimes)
  }

  @Test
  fun `parse empty data response of gks_fail`() {
    val test = JSONObject(EMPTY_DATA_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertEquals(0, result.length())

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertEquals(0, map.size)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertFalse(gk)
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertEquals(2, loadAsyncTimes)
  }

  @Test
  fun `null in gk map is still default value_ok`() {
    val map = mapOf<String, JSONObject?>(APPLICATION_NAME to null)
    Whitebox.setInternalState(
        FetchedAppGateKeepersManager::class.java, "fetchedAppGateKeepers", map)
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    val gk1 = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, true)
    assertFalse(gk)
    assertTrue(gk1)
    assertEquals(2, loadAsyncTimes)
  }

  @Test
  fun `set gate keeper value`() {
    val test = JSONObject(VALID_JSON)
    FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)

    FetchedAppGateKeepersManager.setRuntimeGateKeeper(gateKeeper = GateKeeper(GK1, true))
    val map1 = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertTrue(map1.getValue(GK1))
    FetchedAppGateKeepersManager.setRuntimeGateKeeper(gateKeeper = GateKeeper(GK1, false))
    val map2 = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertFalse(map2.getValue(GK1))
  }
}
