/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import java.util.HashSet
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
internal object JsonUtil {
  fun jsonObjectClear(jsonObject: JSONObject) {
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      keys.next()
      keys.remove()
    }
  }

  fun jsonObjectContainsValue(jsonObject: JSONObject, value: Any): Boolean {
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      val thisValue = jsonObject.opt(keys.next())
      if (thisValue != null && thisValue == value) {
        return true
      }
    }
    return false
  }

  fun jsonObjectEntrySet(jsonObject: JSONObject): Set<Map.Entry<String, Any>> {
    val result = HashSet<Map.Entry<String, Any>>()
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      val key = keys.next()
      val value = jsonObject.get(key)
      result.add(JSONObjectEntry(key, value))
    }
    return result
  }

  fun jsonObjectKeySet(jsonObject: JSONObject): Set<String> {
    val result = HashSet<String>()
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      result.add(keys.next())
    }
    return result
  }

  fun jsonObjectPutAll(jsonObject: JSONObject, map: Map<String, Any>) {
    val entrySet = map.entries
    for ((key, value) in entrySet) {
      try {
        jsonObject.putOpt(key, value)
      } catch (e: JSONException) {
        throw IllegalArgumentException(e)
      }
    }
  }

  fun jsonObjectValues(jsonObject: JSONObject): Collection<Any> {
    val result = mutableListOf<Any>()
    val keys = jsonObject.keys()
    while (keys.hasNext()) {
      result.add(jsonObject.get(keys.next()))
    }
    return result
  }

  private class JSONObjectEntry constructor(override val key: String, override val value: Any) :
      MutableMap.MutableEntry<String, Any> {
    override fun setValue(newValue: Any): Any {
      throw UnsupportedOperationException("JSONObjectEntry is immutable")
    }
  }
}
