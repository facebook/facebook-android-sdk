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

package com.facebook

import android.annotation.TargetApi
import android.os.Parcel
import android.os.Parcelable
import org.assertj.core.api.Assertions.fail
import org.json.JSONArray
import org.json.JSONObject

object TestUtils {
  const val DOUBLE_EQUALS_DELTA = 0.00001

  @JvmStatic
  fun <E : Parcelable> parcelAndUnparcel(obj: E): E? {
    val writeParcel = Parcel.obtain()
    val readParcel = Parcel.obtain()
    return try {
      writeParcel.writeParcelable(obj, 0)
      val bytes = writeParcel.marshall()
      readParcel.unmarshall(bytes, 0, bytes.size)
      readParcel.setDataPosition(0)
      readParcel.readParcelable(obj.javaClass.classLoader)
    } finally {
      writeParcel.recycle()
      readParcel.recycle()
    }
  }

  @TargetApi(16)
  @JvmStatic
  fun assertEquals(expected: JSONObject?, actual: JSONObject?) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return
    }
    return fail("expected: <$expected>, but was: <$actual>")
  }

  @TargetApi(16)
  @JvmStatic
  fun assertEquals(expected: JSONArray?, actual: JSONArray?) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return
    }
    return fail("expected: <$expected>, but was: <$actual>")
  }

  private fun areEqual(expected: JSONObject?, actual: JSONObject?): Boolean {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    val expectedKeys = expected.keys().asSequence().toHashSet()
    for (key in actual.keys()) {
      if (!areEqual(expected.opt(key), actual.opt(key))) {
        return false
      }
      expectedKeys.remove(key)
    }
    return expectedKeys.isEmpty()
  }

  private fun areEqual(expected: JSONArray?, actual: JSONArray?): Boolean {
    // JSONObject.equals does not do an order-independent comparison, so we need to check values
    // that are JSONObject
    // manually
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    if (expected.length() != actual.length()) {
      return false
    }
    val length = expected.length()
    for (i in 0 until length) {
      if (!areEqual(expected.opt(i), actual.opt(i))) {
        return false
      }
    }
    return true
  }

  private fun areEqual(expected: Any?, actual: Any?): Boolean {
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    if (expected is JSONObject && actual is JSONObject) {
      return areEqual(expected as JSONObject?, actual as JSONObject?)
    }
    return if (expected is JSONArray && actual is JSONArray) {
      areEqual(expected as JSONArray?, actual as JSONArray?)
    } else expected == actual
  }
}
