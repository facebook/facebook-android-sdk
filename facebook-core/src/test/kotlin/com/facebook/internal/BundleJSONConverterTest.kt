/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.os.Bundle
import com.facebook.FacebookTestCase
import com.facebook.FacebookTestUtility
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

class BundleJSONConverterTest : FacebookTestCase() {
  @Test
  fun testSimpleValues() {
    val arrayList = arrayListOf("1st", "2nd", "third")
    val innerBundle1 = Bundle()
    innerBundle1.putInt("inner", 1)
    val innerBundle2 = Bundle()
    innerBundle2.putString("inner", "2")
    innerBundle2.putStringArray("deep list", arrayOf("7", "8"))
    innerBundle1.putBundle("nested bundle", innerBundle2)
    val b = Bundle()
    b.putBoolean("boolValue", true)
    b.putInt("intValue", 7)
    b.putLong("longValue", 5_000_000_000L)
    b.putDouble("doubleValue", 3.14)
    b.putString("stringValue", "hello world")
    b.putStringArray("stringArrayValue", arrayOf("first", "second"))
    b.putStringArrayList("stringArrayListValue", arrayList)
    b.putBundle("nested", innerBundle1)
    val json = BundleJSONConverter.convertToJSON(b)
    Assert.assertNotNull(json)
    Assert.assertEquals(true, json.getBoolean("boolValue"))
    Assert.assertEquals(7, json.getInt("intValue"))
    Assert.assertEquals(5_000_000_000L, json.getLong("longValue"))
    Assert.assertEquals(
        3.14, json.getDouble("doubleValue"), FacebookTestUtility.DOUBLE_EQUALS_DELTA)
    Assert.assertEquals("hello world", json.getString("stringValue"))
    var jsonArray = json.getJSONArray("stringArrayValue")
    Assert.assertEquals(2, jsonArray.length())
    Assert.assertEquals("first", jsonArray.getString(0))
    Assert.assertEquals("second", jsonArray.getString(1))
    jsonArray = json.getJSONArray("stringArrayListValue")
    Assert.assertEquals(3, jsonArray.length())
    Assert.assertEquals("1st", jsonArray.getString(0))
    Assert.assertEquals("2nd", jsonArray.getString(1))
    Assert.assertEquals("third", jsonArray.getString(2))
    var innerJson = json.getJSONObject("nested")
    Assert.assertEquals(1, innerJson.getInt("inner"))
    innerJson = innerJson.getJSONObject("nested bundle")
    Assert.assertEquals("2", innerJson.getString("inner"))
    jsonArray = innerJson.getJSONArray("deep list")
    Assert.assertEquals(2, jsonArray.length())
    Assert.assertEquals("7", jsonArray.getString(0))
    Assert.assertEquals("8", jsonArray.getString(1))
    var finalBundle: Bundle? = BundleJSONConverter.convertToBundle(json)
    Assert.assertNotNull(finalBundle)
    checkNotNull(finalBundle)
    Assert.assertEquals(true, finalBundle.getBoolean("boolValue"))
    Assert.assertEquals(7, finalBundle.getInt("intValue"))
    Assert.assertEquals(5_000_000_000L, finalBundle.getLong("longValue"))
    Assert.assertEquals(
        3.14, finalBundle.getDouble("doubleValue"), FacebookTestUtility.DOUBLE_EQUALS_DELTA)
    Assert.assertEquals("hello world", finalBundle.getString("stringValue"))
    var stringList = finalBundle.getStringArrayList("stringArrayValue")
    checkNotNull(stringList)
    Assert.assertEquals(2, stringList.size)
    Assert.assertEquals("first", stringList[0])
    Assert.assertEquals("second", stringList[1])
    stringList = finalBundle.getStringArrayList("stringArrayListValue")
    checkNotNull(stringList)
    Assert.assertEquals(3, stringList.size)
    Assert.assertEquals("1st", stringList[0])
    Assert.assertEquals("2nd", stringList[1])
    Assert.assertEquals("third", stringList[2])
    val finalInnerBundle = finalBundle.getBundle("nested")
    checkNotNull(finalInnerBundle)
    Assert.assertEquals(1, finalInnerBundle.getInt("inner"))
    finalBundle = finalInnerBundle.getBundle("nested bundle")
    checkNotNull(finalBundle)
    Assert.assertEquals("2", finalBundle.getString("inner"))
    stringList = finalBundle.getStringArrayList("deep list")
    checkNotNull(stringList)
    Assert.assertEquals(2, stringList.size)
    Assert.assertEquals("7", stringList[0])
    Assert.assertEquals("8", stringList[1])
  }

  @Test
  fun testUnsupportedValues() {
    val b = Bundle()
    b.putShort("shortValue", 7.toShort())
    var exceptionCaught = false
    try {
      BundleJSONConverter.convertToJSON(b)
    } catch (a: IllegalArgumentException) {
      exceptionCaught = true
    }
    assertThat(exceptionCaught).isTrue
    val jsonArray = JSONArray()
    jsonArray.put(10)
    val json = JSONObject()
    json.put("arrayValue", jsonArray)
    exceptionCaught = false
    try {
      BundleJSONConverter.convertToBundle(json)
    } catch (a: IllegalArgumentException) {
      exceptionCaught = true
    }
    assertThat(exceptionCaught).isTrue
  }
}
