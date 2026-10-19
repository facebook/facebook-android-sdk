/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookTestCase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JSONObjectExtensionsTest : FacebookTestCase() {
  private val nested = JSONObject().put("a", 1)
  private val json =
      JSONObject()
          .put("string", "value")
          .put("empty", "")
          .put("int", 42)
          .put("double", 1.5)
          .put("bool", true)
          .put("null", JSONObject.NULL)
          .put("object", nested)
          .put("array", JSONArray().put("x"))

  @Test
  fun `test optStringOrNull returns null for a missing key`() {
    assertNull(json.optStringOrNull("missing"))
  }

  @Test
  fun `test optStringOrNull stringifies present values`() {
    assertEquals("value", json.optStringOrNull("string"))
    assertEquals("", json.optStringOrNull("empty"))
    assertEquals("42", json.optStringOrNull("int"))
    assertEquals("1.5", json.optStringOrNull("double"))
    assertEquals("true", json.optStringOrNull("bool"))
    assertEquals("null", json.optStringOrNull("null"))
    assertEquals(nested.toString(), json.optStringOrNull("object"))
    assertEquals("[\"x\"]", json.optStringOrNull("array"))
  }

  @Test
  fun `test optStringOrNull matches optString for present keys`() {
    for (key in json.keys()) {
      assertEquals(json.optString(key, "fallback"), json.optStringOrNull(key))
    }
  }
}
