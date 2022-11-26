/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookTestCase
import java.util.HashMap
import junit.framework.Assert
import org.json.JSONObject
import org.junit.Test

class JsonUtilTest : FacebookTestCase() {

  @Test
  fun testJsonObjectClear() {
    val jsonObject = JSONObject()
    jsonObject.put("hello", "world")
    jsonObject.put("hocus", "pocus")
    JsonUtil.jsonObjectClear(jsonObject)
    Assert.assertEquals(0, jsonObject.length())
  }

  @Test
  fun testJsonObjectContainsValue() {
    val jsonObject = JSONObject()
    jsonObject.put("hello", "world")
    jsonObject.put("hocus", "pocus")
    Assert.assertTrue(JsonUtil.jsonObjectContainsValue(jsonObject, "pocus"))
    Assert.assertFalse(JsonUtil.jsonObjectContainsValue(jsonObject, "Fred"))
  }

  @Test
  fun testJsonObjectEntrySet() {
    val jsonObject = JSONObject()
    jsonObject.put("hello", "world")
    jsonObject.put("hocus", "pocus")
    val entrySet = JsonUtil.jsonObjectEntrySet(jsonObject)
    Assert.assertEquals(2, entrySet.size)
  }

  @Test
  fun testJsonObjectKeySet() {
    val jsonObject = JSONObject()
    jsonObject.put("hello", "world")
    jsonObject.put("hocus", "pocus")
    val keySet = JsonUtil.jsonObjectKeySet(jsonObject)
    Assert.assertEquals(2, keySet.size)
    Assert.assertTrue(keySet.contains("hello"))
    Assert.assertFalse(keySet.contains("world"))
  }

  @Test
  fun testJsonObjectPutAll() {
    val map = HashMap<String, Any>()
    map["hello"] = "world"
    map["hocus"] = "pocus"
    val jsonObject = JSONObject()
    JsonUtil.jsonObjectPutAll(jsonObject, map)
    Assert.assertEquals("pocus", jsonObject["hocus"])
    Assert.assertEquals(2, jsonObject.length())
  }

  @Test
  fun testJsonObjectValues() {
    val jsonObject = JSONObject()
    jsonObject.put("hello", "world")
    jsonObject.put("hocus", "pocus")
    val values = JsonUtil.jsonObjectValues(jsonObject)
    Assert.assertEquals(2, values.size)
    Assert.assertTrue(values.contains("world"))
  }
}
