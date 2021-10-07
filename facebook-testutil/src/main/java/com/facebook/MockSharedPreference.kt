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
import android.content.SharedPreferences
import android.os.Build

@TargetApi(Build.VERSION_CODES.N)
class MockSharedPreference : SharedPreferences {
  private val preferenceMap: HashMap<String, Any?> = HashMap()
  private val editor: MockEditor = MockEditor(preferenceMap)
  override fun getAll(): Map<String, *> {
    return preferenceMap
  }

  override fun getString(key: String, defValue: String?): String? {
    return preferenceMap.getOrDefault(key, defValue) as String?
  }

  override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
    return preferenceMap.getOrDefault(key, defValues) as Set<String>?
  }

  override fun getInt(key: String, defValue: Int): Int {
    return preferenceMap.getOrDefault(key, defValue) as Int
  }

  override fun getLong(key: String, defValue: Long): Long {
    return preferenceMap.getOrDefault(key, defValue) as Long
  }

  override fun getFloat(key: String, defValue: Float): Float {
    return preferenceMap.getOrDefault(key, defValue) as Float
  }

  override fun getBoolean(key: String, defValue: Boolean): Boolean {
    return preferenceMap.getOrDefault(key, defValue) as Boolean
  }

  override fun contains(key: String): Boolean {
    return preferenceMap.containsKey(key)
  }

  override fun edit(): SharedPreferences.Editor {
    return editor
  }

  override fun registerOnSharedPreferenceChangeListener(
      listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) = Unit
  override fun unregisterOnSharedPreferenceChangeListener(
      listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) = Unit

  class MockEditor internal constructor(private val preferenceMap: MutableMap<String, Any?>) :
      SharedPreferences.Editor {
    override fun putString(key: String, value: String?): SharedPreferences.Editor {
      preferenceMap[key] = value
      return this
    }

    override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
      preferenceMap[key] = values
      return this
    }

    override fun putInt(key: String, value: Int): SharedPreferences.Editor {
      preferenceMap[key] = value
      return this
    }

    override fun putLong(key: String, value: Long): SharedPreferences.Editor {
      preferenceMap[key] = value
      return this
    }

    override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
      preferenceMap[key] = value
      return this
    }

    override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
      preferenceMap[key] = value
      return this
    }

    override fun remove(key: String): SharedPreferences.Editor {
      preferenceMap.remove(key)
      return this
    }

    override fun clear(): SharedPreferences.Editor {
      preferenceMap.clear()
      return this
    }

    override fun commit(): Boolean {
      return true
    }

    override fun apply() = Unit
  }
}
