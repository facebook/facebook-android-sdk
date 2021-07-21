/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.internal

import android.os.Bundle
import android.os.Parcel
import com.facebook.FacebookTestCase
import kotlin.collections.HashMap
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilityTest : FacebookTestCase() {
  @Test
  fun testStringMapToParcel() {
    // Test null
    assertNull(roundtrip(null))
    val map = HashMap<String?, String?>()

    // Test empty
    assertEquals(0, roundtrip(map)?.size)

    // Test regular
    map["a"] = "100"
    map["b"] = null
    map["c"] = "hello"
    val result = roundtrip(map)
    assertEquals(3, result?.size)
    assertEquals(map, result)
    assertEquals("100", result?.get("a"))
    assertNull(result?.get("b"))
    assertEquals("hello", result?.get("c"))
  }

  private fun roundtrip(input: Map<String?, String?>?): Map<String?, String?>? {
    val parcel = Parcel.obtain()
    return try {
      Utility.writeStringMapToParcel(parcel, input)
      parcel.setDataPosition(0)
      Utility.readStringMapFromParcel(parcel)
    } finally {
      parcel.recycle()
    }
  }

  @Test
  fun testUriBuild() {
    val path = "v10.0/dialog/access"

    val parameters = Bundle()
    parameters.putString("f1", "v1")
    parameters.putString("f2", "v2")
    parameters.putString("f3", "www.facebook.com")

    var uri = Utility.buildUri(ServerProtocol.getDialogAuthority(), path, null)
    assertEquals("https://m.facebook.com/v10.0/dialog/access", uri.toString())

    uri = Utility.buildUri(ServerProtocol.getDialogAuthority(), path, parameters)
    assertEquals(
        "https://m.facebook.com/v10.0/dialog/access?f1=v1&f2=v2&f3=www.facebook.com",
        uri.toString())

    // Test parseUrlQueryString
    val query = Utility.parseUrlQueryString(uri.query)
    assertEquals(parameters.toString(), query.toString())
  }

  @Test
  fun testBuildBundle() {
    val parameters = Bundle()
    Utility.putCommaSeparatedStringList(parameters, "k1", listOf("12", "34", "56", "78"))

    assertEquals("12,34,56,78", parameters.get("k1"))

    val barray = arrayOf(true, false).toBooleanArray()
    Utility.putJSONValueInBundle(parameters, "k2", barray)
    assertArrayEquals(barray, parameters.get("k2") as BooleanArray)
  }

  @Test
  fun testUnmodifiableCollection() {
    val result = Utility.unmodifiableCollection("t1", "t2")
    assertArrayEquals(arrayOf("t1", "t2"), result.toTypedArray())
  }

  @Test
  fun testConvertJSONObjectToStringMap() {
    val validJson = "{\"k1\": true, \"k2\": \"value\"}"
    val result = Utility.convertJSONObjectToStringMap(JSONObject(validJson))
    assertEquals(mapOf("k1" to "true", "k2" to "value"), result)
  }
}
