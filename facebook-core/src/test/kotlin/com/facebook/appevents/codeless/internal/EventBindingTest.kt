/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.codeless.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.util.common.assertThrows
import java.util.Arrays
import kotlin.collections.ArrayList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class EventBindingTest : FacebookPowerMockTestCase() {
  private val className1 = "android.widget.LinearLayout"
  private val className2 = "android.widget.TextView"
  private val text = "Inner Label"
  private val validPathJsonString =
      "{'class_name': '$className1'}," +
          "{'class_name': '$className1'}," +
          "{" +
          "'class_name': '$className2'," +
          "'text': '$text'" +
          "}"

  private val invalidPathJsonString =
      "{'class_name': '$className1'}," +
          "{'class_name': '$className1'}," +
          "{" +
          "'text': '$text'" +
          "}"

  private val validSample =
      "{'event_name': 'sample_event'," +
          "'method': 'MANUAL', 'event_type': 'CLICK'," +
          "'app_version': '1.0', 'path_type': 'absolute'," +
          "'path': [ $validPathJsonString ]" +
          "}"

  private val invalidSample1 =
      "{'method': 'MANUAL'," +
          "'event_type': 'CLICK'," +
          "'app_version': '1.0', 'path_type': 'absolute'," +
          "'path': [ $validPathJsonString ]" +
          "}"

  private val invalidSample2 =
      "{'event_name': 'sample_event'," +
          "'method': 'MANUAL_TEST', 'event_type': 'CLICK'," +
          "'app_version': '1.0', 'path_type': 'absolute'," +
          "'path': [ $validPathJsonString ]" +
          "}"

  private val invalidSample3 =
      "{'event_name': 'sample_event'," +
          "'method': 'MANUAL', 'event_type': 'CLICK'," +
          "'app_version': '1.0', 'path_type': 'absolute'," +
          "'path': [ $invalidPathJsonString ]" +
          "}"

  private val validSampleArray = "[ $validSample ]"
  private val invalidSampleArray1 = "[ $invalidSample1 ]"
  private val invalidSampleArray2 = "[ $invalidSample2 ]"
  private val invalidSampleArray3 = "[ $invalidSample3 ]"
  private val emptyEventBindingList = ArrayList<EventBinding>()

  @Test
  fun `getInstanceFromJson with valid json input`() {
    val validSampleJson = JSONObject(validSample)
    val sampleBinding = EventBinding.getInstanceFromJson(validSampleJson)
    val pathComponent1 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent2 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent3 = PathComponent(JSONObject("{'class_name': '$className2', 'text': '$text'}"))
    val pathComponents = Arrays.asList(pathComponent1, pathComponent2, pathComponent3)
    val expectedResult =
        EventBinding(
            "sample_event",
            EventBinding.MappingMethod.MANUAL,
            EventBinding.ActionType.CLICK,
            "1.0",
            pathComponents,
            ArrayList<ParameterComponent>(),
            "",
            "absolute",
            "")

    assertEquals(expectedResult.eventName, sampleBinding.eventName)
    assertEquals(expectedResult.method, sampleBinding.method)
    assertEquals(expectedResult.type, sampleBinding.type)
    assertEquals(expectedResult.appVersion, sampleBinding.appVersion)
    assertEquals(expectedResult.viewPath.size, sampleBinding.viewPath.size)
    for (i in sampleBinding.viewPath.indices) {
      val eventBinding = sampleBinding.viewPath[i]
      val expectedEventBinding = expectedResult.viewPath[i]
      assertEquals(expectedEventBinding.className, eventBinding.className)
      assertEquals(expectedEventBinding.index, eventBinding.index)
      assertEquals(expectedEventBinding.id, eventBinding.id)
      assertEquals(expectedEventBinding.text, eventBinding.text)
      assertEquals(expectedEventBinding.tag, eventBinding.tag)
      assertEquals(expectedEventBinding.description, eventBinding.description)
      assertEquals(expectedEventBinding.hint, eventBinding.hint)
      assertEquals(expectedEventBinding.matchBitmask, eventBinding.matchBitmask)
    }
    assertEquals(expectedResult.viewParameters, sampleBinding.viewParameters)
    assertEquals(expectedResult.componentId, sampleBinding.componentId)
    assertEquals(expectedResult.pathType, sampleBinding.pathType)
    assertEquals(expectedResult.activityName, sampleBinding.activityName)
  }

  @Test
  fun `getInstanceFromJson with input of missing required field`() {
    val invalidSampleJson = JSONObject(invalidSample1)
    assertThrows<JSONException> { EventBinding.getInstanceFromJson(invalidSampleJson) }
  }

  @Test
  fun `getInstanceFromJson with input of invalid enum value`() {
    val invalidSampleJson = JSONObject(invalidSample2)
    assertThrows<IllegalArgumentException> { EventBinding.getInstanceFromJson(invalidSampleJson) }
  }

  @Test
  fun `getInstanceFromJson with input of invalid path`() {
    val invalidSampleJson = JSONObject(invalidSample3)
    assertThrows<JSONException> { EventBinding.getInstanceFromJson(invalidSampleJson) }
  }

  @Test
  fun `parseArray with valid input`() {
    val validSampleJson = JSONArray(validSampleArray)
    val eventBindingList = EventBinding.parseArray(validSampleJson)
    assertEquals(1, eventBindingList.size)
  }

  @Test
  fun `parseArray with input null`() {
    val eventBindingList = EventBinding.parseArray(null)
    assertEquals(emptyEventBindingList, eventBindingList)
  }

  @Test
  fun `parseArray with input empty json array`() {
    val eventBindingList = EventBinding.parseArray(JSONArray())
    assertEquals(emptyEventBindingList, eventBindingList)
  }

  @Test
  fun `parseArray with input of missing required field`() {
    val invalidSampleJson = JSONArray(invalidSampleArray1)
    val eventBindingList = EventBinding.parseArray(invalidSampleJson)
    assertEquals(emptyEventBindingList, eventBindingList)
  }

  @Test
  fun `parseArray with input of invalid enum value`() {
    val invalidSampleJson = JSONArray(invalidSampleArray2)
    val eventBindingList = EventBinding.parseArray(invalidSampleJson)
    assertEquals(emptyEventBindingList, eventBindingList)
  }

  @Test
  fun `parseArray with input of invalid path`() {
    val invalidSampleJson = JSONArray(invalidSampleArray3)
    val eventBindingList = EventBinding.parseArray(invalidSampleJson)
    assertEquals(emptyEventBindingList, eventBindingList)
  }
}
