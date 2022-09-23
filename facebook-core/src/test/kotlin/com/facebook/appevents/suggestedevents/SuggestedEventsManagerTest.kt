/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
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
    assertThat(SuggestedEventsManager.isProductionEvents("a")).isTrue
    assertThat(SuggestedEventsManager.isProductionEvents("b")).isTrue
    assertThat(SuggestedEventsManager.isProductionEvents("c")).isTrue
    assertThat(SuggestedEventsManager.isProductionEvents("d")).isFalse
    assertThat(SuggestedEventsManager.isEligibleEvents("x")).isTrue
    assertThat(SuggestedEventsManager.isEligibleEvents("y")).isTrue
    assertThat(SuggestedEventsManager.isEligibleEvents("z")).isTrue
    assertThat(SuggestedEventsManager.isEligibleEvents("a")).isFalse
  }

  @Test
  fun `parse zero production events`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(emptyProdEventOnlyJson)
    assertThat(SuggestedEventsManager.isProductionEvents("a")).isFalse
  }

  @Test
  fun `parse zero eligible events`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(emptyEligibleEventOnlyJson)
    assertThat(SuggestedEventsManager.isProductionEvents("a")).isFalse
  }

  @Test
  fun `parse invalid json`() {
    SuggestedEventsManager.populateEventsFromRawJsonString(invalidJson)
    assertThat(SuggestedEventsManager.isProductionEvents("a")).isFalse
    assertThat(SuggestedEventsManager.isEligibleEvents("b")).isFalse
  }
}
