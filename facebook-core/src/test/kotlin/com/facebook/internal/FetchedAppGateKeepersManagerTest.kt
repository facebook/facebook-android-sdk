/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.gatekeeper.GateKeeper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FacebookSdk::class)
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
  private var asyncTaskExecutedTimes = 0

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn(APPLICATION_NAME)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    val mockExecutor = mock<Executor>()
    whenever(mockExecutor.execute(any())).thenAnswer {
      asyncTaskExecutedTimes++
      // reset isLoading
      Whitebox.setInternalState(
          FetchedAppGateKeepersManager::class.java, "isLoading", AtomicBoolean(false))
    }
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)

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
    assertThat(result.getBoolean(GK2)).isFalse
    assertThat(result.getBoolean(GK1)).isTrue

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map[GK1]).isTrue
    assertThat(map[GK2]).isFalse

    val gk1 = FetchedAppGateKeepersManager.getGateKeeperForKey(GK1, APPLICATION_NAME, false)
    val gk2 = FetchedAppGateKeepersManager.getGateKeeperForKey(GK2, APPLICATION_NAME, true)
    assertThat(gk1).isTrue
    assertThat(gk2).isFalse
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertThat(asyncTaskExecutedTimes).isEqualTo(3)
  }

  @Test
  fun `parse value isnt boolean_fail`() {
    val test = JSONObject(NON_BOOLEAN_VALUE_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertThat(result.length()).isEqualTo(0)

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    // current parser filters out non boolean values, otherwise map will actually return the exact
    // value
    assertThat(map[GK1]).isNull()

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey(GK1, APPLICATION_NAME, false)
    assertThat(gk).isFalse
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertThat(asyncTaskExecutedTimes).isEqualTo(2)
  }

  @Test
  fun `parse empty list of gks_fail`() {
    val test = JSONObject(EMPTY_GK_LIST_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertThat(result.length()).isEqualTo(0)

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map.size).isEqualTo(0)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertThat(gk).isFalse()
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertThat(asyncTaskExecutedTimes).isEqualTo(2)
  }

  @Test
  fun `parse empty response_fail`() {
    val test = JSONObject(EMPTY_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertThat(result.length()).isEqualTo(0)

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map.size).isEqualTo(0)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertThat(gk).isFalse
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertThat(asyncTaskExecutedTimes).isEqualTo(2)
  }

  @Test
  fun `parse empty data response of gks_fail`() {
    val test = JSONObject(EMPTY_DATA_RESPONSE)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    assertThat(result.length()).isEqualTo(0)

    val map = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map.size).isEqualTo(0)

    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    assertThat(gk).isFalse
    // "Both getGateKeepersForApplication and getGateKeeperForKey call async"
    assertThat(asyncTaskExecutedTimes).isEqualTo(2)
  }

  @Test
  fun `null in gk map is still default value_ok`() {
    val map = mapOf<String, JSONObject?>(APPLICATION_NAME to null)
    Whitebox.setInternalState(
        FetchedAppGateKeepersManager::class.java, "fetchedAppGateKeepers", map)
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, false)
    val gk1 = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", APPLICATION_NAME, true)
    assertThat(gk).isFalse
    assertThat(gk1).isTrue
    assertThat(asyncTaskExecutedTimes).isEqualTo(2)
  }

  @Test
  fun `set gate keeper value`() {
    val test = JSONObject(VALID_JSON)
    FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(APPLICATION_NAME, test)
    FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)

    FetchedAppGateKeepersManager.setRuntimeGateKeeper(gateKeeper = GateKeeper(GK1, true))
    val map1 = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map1.getValue(GK1)).isTrue
    FetchedAppGateKeepersManager.setRuntimeGateKeeper(gateKeeper = GateKeeper(GK1, false))
    val map2 = FetchedAppGateKeepersManager.getGateKeepersForApplication(APPLICATION_NAME)
    assertThat(map2.getValue(GK1)).isFalse
  }
}
