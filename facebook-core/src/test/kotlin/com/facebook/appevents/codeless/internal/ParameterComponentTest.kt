/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved. <p> You are hereby granted a
 * non-exclusive, worldwide, royalty-free license to use, copy, modify, and distribute this software
 * in source code or binary form for use in connection with the web services and APIs provided by
 * Facebook. <p> As with any software that integrates with the Facebook platform, your use of this
 * software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY
 * OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.facebook.appevents.codeless.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.util.common.assertThrows
import java.util.Arrays
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ParameterComponentTest : FacebookPowerMockTestCase() {

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
      "{'name': 'sample_name'," +
          "'value': 'sample_value'," +
          "'path': [ $validPathJsonString ]" +
          "}"

  private val invalidSample1 =
      "{'name': 'sample_name'," +
          "'value': 'sample_value'," +
          "'path': [ $invalidPathJsonString ]" +
          "}"

  private val invalidSample2 =
      "{'value': 'sample_value'," + "'path': [ $validPathJsonString ]" + "}"

  private val validSample2 = "{'name': 'sample_name'," + "'path': [ $validPathJsonString ]" + "}"

  private val validSample3 =
      "{'name': 'sample_name'," + "'value': 'sample_value'," + "'path': []" + "}"

  @Test
  fun `getInstanceFromJson with valid input 1`() {
    val validSampleJson = JSONObject(validSample)
    val sampleComponent = ParameterComponent(validSampleJson)
    val pathComponent1 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent2 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent3 = PathComponent(JSONObject("{'class_name': '$className2', 'text': '$text'}"))
    val pathComponents = Arrays.asList(pathComponent1, pathComponent2, pathComponent3)

    assertEquals("sample_name", sampleComponent.name)
    assertEquals("sample_value", sampleComponent.value)

    assertEquals(pathComponents.size, sampleComponent.path.size)
    for (i in 0 until sampleComponent.path.size) {
      val expectedPath = pathComponents[i]
      val samplePath = sampleComponent.path[i]
      assertEquals(expectedPath.className, samplePath.className)
      assertEquals(expectedPath.index, samplePath.index)
      assertEquals(expectedPath.id, samplePath.id)
      assertEquals(expectedPath.text, samplePath.text)
      assertEquals(expectedPath.tag, samplePath.tag)
      assertEquals(expectedPath.description, samplePath.description)
      assertEquals(expectedPath.hint, samplePath.hint)
      assertEquals(expectedPath.matchBitmask, samplePath.matchBitmask)
    }
  }

  @Test
  fun `getInstanceFromJson with missing value`() {
    val validSampleJson = JSONObject(validSample2)
    val sampleComponent = ParameterComponent(validSampleJson)
    val pathComponent1 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent2 = PathComponent(JSONObject("{'class_name': '$className1'}"))
    val pathComponent3 = PathComponent(JSONObject("{'class_name': '$className2', 'text': '$text'}"))
    val pathComponents = Arrays.asList(pathComponent1, pathComponent2, pathComponent3)

    assertEquals("sample_name", sampleComponent.name)
    assertEquals("", sampleComponent.value)

    assertEquals(pathComponents.size, sampleComponent.path.size)
    for (i in 0 until sampleComponent.path.size) {
      val expectedPath = pathComponents[i]
      val samplePath = sampleComponent.path[i]
      assertEquals(expectedPath.className, samplePath.className)
      assertEquals(expectedPath.index, samplePath.index)
      assertEquals(expectedPath.id, samplePath.id)
      assertEquals(expectedPath.text, samplePath.text)
      assertEquals(expectedPath.tag, samplePath.tag)
      assertEquals(expectedPath.description, samplePath.description)
      assertEquals(expectedPath.hint, samplePath.hint)
      assertEquals(expectedPath.matchBitmask, samplePath.matchBitmask)
    }
  }

  @Test
  fun `getInstanceFromJson with empty path`() {
    val validSampleJson = JSONObject(validSample3)
    val sampleComponent = ParameterComponent(validSampleJson)

    assertEquals("sample_name", sampleComponent.name)
    assertEquals("sample_value", sampleComponent.value)

    assertEquals(0, sampleComponent.path.size)
  }

  @Test
  fun `getInstanceFromJson with invalid path`() {
    val invalidSampleJson = JSONObject(invalidSample1)
    assertThrows<JSONException> { ParameterComponent(invalidSampleJson) }
  }

  @Test
  fun `getInstanceFromJson with missing name`() {
    val invalidSampleJson = JSONObject(invalidSample2)
    assertThrows<JSONException> { ParameterComponent(invalidSampleJson) }
  }
}
