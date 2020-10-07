package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.isA
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(FetchedAppGateKeepersManager::class)
class FetchedAppGateKeepersManagerTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"data\": [\n" +
          "    {\n" +
          "      \"gatekeepers\": [\n" +
          "        {\n" +
          "          \"key\": \"FBSDKFeatureInstrument\",\n" +
          "          \"value\": true\n" +
          "        },\n" +
          "        {\n" +
          "          \"key\": \"app_events_killswitch\",\n" +
          "          \"value\": \"false\"\n" +
          "        }\n" +
          "      ]\n" +
          "    }\n" +
          "  ]\n" +
          "}"

  private val nonBoolValueValidJson =
      "{\n" +
          "  \"data\": [\n" +
          "    {\n" +
          "      \"gatekeepers\": [\n" +
          "        {\n" +
          "          \"key\": \"FBSDKFeatureInstrument\",\n" +
          "          \"value\": swag\n" +
          "        },\n" +
          "      ]\n" +
          "    }\n" +
          "  ]\n" +
          "}"

  private val emptyGKsResponse =
      "{\n" +
          "  \"data\": [\n" +
          "    {\n" +
          "      \"gatekeepers\": [\n" +
          "      ]\n" +
          "    }\n" +
          "  ]\n" +
          "}"

  private val emptyResponse = "{}"
  private val emptyDataResponse = "{\n" + "  \"data\": [\n" + "  ]\n" + "}"

  @Before
  fun init() {
    mockStatic(FetchedAppGateKeepersManager::class.java)
    whenCalled(
            FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON(
                isA(String::class.java), isA(JSONObject::class.java)))
        .thenCallRealMethod()
    whenCalled(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                isA(String::class.java), isA(String::class.java), isA(Boolean::class.java)))
        .thenCallRealMethod()
    whenCalled(FetchedAppGateKeepersManager.loadAppGateKeepersAsync()).then {}
    // because it is a static variable which holds a lot of state about the GKs, we need to reset it
    // every time
    Whitebox.setInternalState(
        FetchedAppGateKeepersManager::class.java,
        "fetchedAppGateKeepers",
        ConcurrentHashMap<String, JSONObject>())
  }

  @Test
  fun `parse valid json_ok`() {
    val test = JSONObject(validJson)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON("aa", test)
    assertFalse(result.getBoolean("app_events_killswitch"))
    assertTrue(result.getBoolean("FBSDKFeatureInstrument"))
    val gk1 =
        FetchedAppGateKeepersManager.getGateKeeperForKey("FBSDKFeatureInstrument", "aa", false)
    val gk2 = FetchedAppGateKeepersManager.getGateKeeperForKey("app_events_killswitch", "aa", true)
    assertTrue(gk1)
    assertFalse(gk2)
  }

  @Test
  fun `parse value isnt boolean_fail`() {
    val test = JSONObject(nonBoolValueValidJson)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON("aa", test)
    assertEquals(0, result.length())
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("FBSDKFeatureInstrument", "aa", false)
    assertFalse(gk)
  }

  @Test
  fun `parse empty list of gks_fail`() {
    val test = JSONObject(emptyGKsResponse)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON("aa", test)
    assertEquals(0, result.length())
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", "aa", false)
    assertFalse(gk)
  }

  @Test
  fun `parse empty response_fail`() {
    val test = JSONObject(emptyResponse)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON("aa", test)
    assertEquals(0, result.length())
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", "aa", false)
    assertFalse(gk)
  }

  @Test
  fun `parse empty data response of gks_fail`() {
    val test = JSONObject(emptyDataResponse)
    val result = FetchedAppGateKeepersManager.parseAppGateKeepersFromJSON("aa", test)
    assertEquals(0, result.length())
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", "aa", false)
    assertFalse(gk)
  }

  @Test
  fun `null in gk map is still default value_ok`() {
    val map = mapOf<String, JSONObject?>("aa" to null)
    Whitebox.setInternalState(
        FetchedAppGateKeepersManager::class.java, "fetchedAppGateKeepers", map)
    val gk = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", "aa", false)
    val gk1 = FetchedAppGateKeepersManager.getGateKeeperForKey("anything", "aa", true)
    assertFalse(gk)
    assertTrue(gk1)
  }
}
