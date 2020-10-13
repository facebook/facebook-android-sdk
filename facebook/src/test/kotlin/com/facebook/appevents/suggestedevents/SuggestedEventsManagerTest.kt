package com.facebook.appevents.suggestedevents

import com.facebook.FacebookPowerMockTestCase
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.reflect.Whitebox

class SuggestedEventsManagerTest : FacebookPowerMockTestCase() {
  private val validJson =
      "{\n" +
          "  \"production_events\": [a,b,c\n" +
          "  ],\n" +
          "  \"eligible_for_prediction_events\": [x,y,z\n" +
          "  ]" +
          "}"

  private val emptyProdEventOnlyJson = "{\n" + "  \"production_events\": [\n" + "  ],\n" + "}"

  private val emptyEligibleEventOnlyJson =
      "{\n" + "  \"eligible_for_prediction_events\": [\n" + "  ]" + "}"

  private val invalidJson =
      "{\n" +
          "  \"production_events\": \"a\"\n" +
          "  ],\n" +
          "  \"eligible_for_prediction_events\":  \"b\"\n" +
          "  ]" +
          "}"
  @Before
  fun init() {
    Whitebox.setInternalState(
        SuggestedEventsManager::class.java, "productionEvents", HashSet<String>())
    Whitebox.setInternalState(
        SuggestedEventsManager::class.java, "eligibleEvents", HashSet<String>())
  }
  @Test
  fun `parse valid json_ok`() {
    val test = JSONObject(validJson) // to verify its actually valid, will throw exception
    SuggestedEventsManager.populateEventsFromRawJsonString(validJson)
    assertTrue(SuggestedEventsManager.isProductionEvents("a"))
    assertTrue(SuggestedEventsManager.isProductionEvents("b"))
    assertTrue(SuggestedEventsManager.isProductionEvents("c"))
    assertFalse(SuggestedEventsManager.isProductionEvents("d"))
    assertTrue(SuggestedEventsManager.isEligibleEvents("x"))
    assertTrue(SuggestedEventsManager.isEligibleEvents("y"))
    assertTrue(SuggestedEventsManager.isEligibleEvents("z"))
    assertFalse(SuggestedEventsManager.isEligibleEvents("a"))
  }

  @Test
  fun `parse zero production events`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(emptyProdEventOnlyJson)
    assertFalse(SuggestedEventsManager.isProductionEvents("a"))
  }

  @Test
  fun `parse zero eligible events`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(emptyEligibleEventOnlyJson)
    assertFalse(SuggestedEventsManager.isProductionEvents("a"))
  }

  @Test
  fun `parse invalid json`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(invalidJson)
    assertFalse(SuggestedEventsManager.isProductionEvents("a"))
    assertFalse(SuggestedEventsManager.isEligibleEvents("b"))
  }
}
