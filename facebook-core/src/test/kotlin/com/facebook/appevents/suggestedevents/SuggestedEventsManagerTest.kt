package com.facebook.appevents.suggestedevents

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertFalse
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
    assertThat(SuggestedEventsManager.isProductionEvents("a")).isTrue
    assertThat(SuggestedEventsManager.isProductionEvents("b")).isTrue
    assertThat(SuggestedEventsManager.isProductionEvents("c")).isTrue
    assertFalse(SuggestedEventsManager.isProductionEvents("d"))
    assertThat(SuggestedEventsManager.isEligibleEvents("x")).isTrue
    assertThat(SuggestedEventsManager.isEligibleEvents("y")).isTrue
    assertThat(SuggestedEventsManager.isEligibleEvents("z")).isTrue
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
