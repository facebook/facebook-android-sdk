/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import java.util.Date
import kotlin.collections.HashSet
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject

object FacebookTestUtility {
  const val DOUBLE_EQUALS_DELTA = 0.00001

  fun nowPlusSeconds(offset: Long): Date {
    return Date(Date().time + offset * 1000L)
  }

  fun <T> assertSameCollectionContents(expected: Collection<T?>?, actual: Collection<T?>?) {
    if (expected == null || expected.isEmpty()) {
      assertThat(actual).isNullOrEmpty()
    } else {
      checkNotNull(actual)
      assertThat(actual).isNotEmpty
      for (item in expected) {
        assertThat(actual).contains(item)
      }
      for (item in actual) {
        assertThat(expected).contains(item)
      }
    }
  }

  private fun assertEqualContents(a: Bundle, b: Bundle, collectionOrderMatters: Boolean) {
    for (key in a.keySet()) {
      assertThat(b.containsKey(key)).isTrue.withFailMessage("bundle does not include key $key")
      val aValue = a[key]
      val bValue = b[key]
      if (!collectionOrderMatters && aValue is Collection<*> && bValue is Collection<*>) {
        this.assertSameCollectionContents(aValue as Collection<*>?, bValue as Collection<*>?)
      } else {
        assertThat(a[key]).isEqualTo(b[key])
      }
    }
    for (key in b.keySet()) {
      assertThat(a.containsKey(key)).isTrue.withFailMessage("bundle does not include key $key")
    }
  }

  @JvmStatic
  fun assertEqualContentsWithoutOrder(a: Bundle, b: Bundle) {
    assertEqualContents(a, b, false)
  }

  fun assertEqualContents(a: Bundle, b: Bundle) {
    assertEqualContents(a, b, true)
  }

  @JvmStatic
  fun assertEquals(expected: JSONObject?, actual: JSONObject?) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return
    }

    assertThat(true).isFalse.withFailMessage("JSONObject is not equal")
  }

  @JvmStatic
  fun assertEquals(expected: JSONArray?, actual: JSONArray?) {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (areEqual(expected, actual)) {
      return
    }
    assertThat(true).isFalse.withFailMessage("JSONArray is not equal")
  }

  fun <T : Any> assertNotNull(actual: T?): T {
    assertThat(actual).isNotNull
    return actual as T
  }

  private fun areEqual(expected: JSONObject?, actual: JSONObject?): Boolean {
    // JSONObject.equals does not do an order-independent comparison, so let's roll our own  :(
    if (expected === actual) {
      return true
    }
    if (expected == null || actual == null) {
      return false
    }
    val expectedKeysIterator = expected.keys()
    val expectedKeys = HashSet<String>()
    while (expectedKeysIterator.hasNext()) {
      expectedKeys.add(expectedKeysIterator.next())
    }
    val actualKeysIterator = actual.keys()
    while (actualKeysIterator.hasNext()) {
      val key = actualKeysIterator.next()
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

  inline fun <reified E : Parcelable?> parcelAndUnparcel(obj: E): E? {
    val writeParcel = Parcel.obtain()
    val readParcel = Parcel.obtain()
    return try {
      writeParcel.writeParcelable(obj, 0)
      val bytes = writeParcel.marshall()
      readParcel.unmarshall(bytes, 0, bytes.size)
      readParcel.setDataPosition(0)
      val classLoader = E::class.java.classLoader
      readParcel.readParcelable(classLoader)
    } finally {
      writeParcel.recycle()
      readParcel.recycle()
    }
  }
}
