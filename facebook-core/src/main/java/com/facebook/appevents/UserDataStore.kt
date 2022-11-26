/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.util.Patterns
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger.Companion.getAnalyticsExecutor
import com.facebook.appevents.aam.MetadataRule.Companion.getEnabledRuleNames
import com.facebook.internal.Utility.jsonStrToMap
import com.facebook.internal.Utility.mapToJsonStr
import com.facebook.internal.Utility.sha256hash
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import java.lang.StringBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

@AutoHandleExceptions
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object UserDataStore {
  private val TAG = UserDataStore::class.java.simpleName
  private const val USER_DATA_KEY = "com.facebook.appevents.UserDataStore.userData"
  private const val INTERNAL_USER_DATA_KEY = "com.facebook.appevents.UserDataStore.internalUserData"
  private lateinit var sharedPreferences: SharedPreferences
  private val initialized = AtomicBoolean(false)
  private const val MAX_NUM = 5
  private const val DATA_SEPARATOR = ","
  private val externalHashedUserData = ConcurrentHashMap<String?, String?>()
  private val internalHashedUserData = ConcurrentHashMap<String?, String?>()

  /** User data types */
  const val EMAIL = "em"
  const val FIRST_NAME = "fn"
  const val LAST_NAME = "ln"
  const val PHONE = "ph"
  const val DATE_OF_BIRTH = "db"
  const val GENDER = "ge"
  const val CITY = "ct"
  const val STATE = "st"
  const val ZIP = "zp"
  const val COUNTRY = "country"
  @JvmStatic
  fun initStore() {
    if (initialized.get()) {
      return
    }
    initAndWait()
  }

  private fun writeDataIntoCache(key: String, value: String) {
    FacebookSdk.getExecutor().execute {
      if (!initialized.get()) {
        initAndWait()
      }
      sharedPreferences.edit().putString(key, value).apply()
    }
  }

  @JvmStatic
  fun setUserDataAndHash(ud: Bundle?) {
    getAnalyticsExecutor().execute {
      if (!initialized.get()) {
        Log.w(TAG, "initStore should have been called before calling setUserData")
        initAndWait()
      }
      updateHashUserData(ud)
      writeDataIntoCache(USER_DATA_KEY, mapToJsonStr(externalHashedUserData))
      writeDataIntoCache(INTERNAL_USER_DATA_KEY, mapToJsonStr(internalHashedUserData))
    }
  }

  @JvmStatic
  fun setUserDataAndHash(
      email: String?,
      firstName: String?,
      lastName: String?,
      phone: String?,
      dateOfBirth: String?,
      gender: String?,
      city: String?,
      state: String?,
      zip: String?,
      country: String?
  ) {
    val ud = Bundle()
    if (email != null) {
      ud.putString(EMAIL, email)
    }
    if (firstName != null) {
      ud.putString(FIRST_NAME, firstName)
    }
    if (lastName != null) {
      ud.putString(LAST_NAME, lastName)
    }
    if (phone != null) {
      ud.putString(PHONE, phone)
    }
    if (dateOfBirth != null) {
      ud.putString(DATE_OF_BIRTH, dateOfBirth)
    }
    if (gender != null) {
      ud.putString(GENDER, gender)
    }
    if (city != null) {
      ud.putString(CITY, city)
    }
    if (state != null) {
      ud.putString(STATE, state)
    }
    if (zip != null) {
      ud.putString(ZIP, zip)
    }
    if (country != null) {
      ud.putString(COUNTRY, country)
    }
    setUserDataAndHash(ud)
  }

  @JvmStatic
  fun clear() {
    getAnalyticsExecutor().execute {
      if (!initialized.get()) {
        Log.w(TAG, "initStore should have been called before calling setUserData")
        initAndWait()
      }
      externalHashedUserData.clear()
      sharedPreferences.edit().putString(USER_DATA_KEY, null).apply()
    }
  }

  @JvmStatic
  internal fun getHashedUserData(): String {
    if (!initialized.get()) {
      Log.w(TAG, "initStore should have been called before calling setUserID")
      initAndWait()
    }
    return mapToJsonStr(externalHashedUserData)
  }

  @JvmStatic
  fun getAllHashedUserData(): String {
    if (!initialized.get()) {
      initAndWait()
    }
    val allHashedUserData: MutableMap<String?, String?> = HashMap()
    allHashedUserData.putAll(externalHashedUserData)
    allHashedUserData.putAll(enabledInternalUserData)
    return mapToJsonStr(allHashedUserData)
  }

  private val enabledInternalUserData: Map<String?, String?>
    private get() {
      val enabledInternalUD: MutableMap<String?, String?> = HashMap()
      val ruleNames: Set<String?> = getEnabledRuleNames()
      for (ruleKey in internalHashedUserData.keys) {
        if (ruleNames.contains(ruleKey)) {
          enabledInternalUD[ruleKey] = internalHashedUserData[ruleKey]
        }
      }
      return enabledInternalUD
    }

  @Synchronized
  private fun initAndWait() {
    if (initialized.get()) {
      return
    }
    sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(FacebookSdk.getApplicationContext())
    val externalUdRaw = sharedPreferences.getString(USER_DATA_KEY, "") ?: ""
    val internalUdRaw = sharedPreferences.getString(INTERNAL_USER_DATA_KEY, "") ?: ""
    externalHashedUserData.putAll(jsonStrToMap(externalUdRaw))
    internalHashedUserData.putAll(jsonStrToMap(internalUdRaw))
    initialized.set(true)
  }

  private fun updateHashUserData(ud: Bundle?) {
    if (ud == null) {
      return
    }
    for (key in ud.keySet()) {
      val rawVal = ud[key] ?: continue
      val value = rawVal.toString()
      if (maybeSHA256Hashed(value)) {
        externalHashedUserData[key] = value.toLowerCase()
      } else {
        val encryptedValue = sha256hash(normalizeData(key, value))
        if (encryptedValue != null) {
          externalHashedUserData[key] = encryptedValue
        }
      }
    }
  }

  @JvmStatic
  fun setInternalUd(ud: Map<String, String>) {
    if (!initialized.get()) {
      initAndWait()
    }
    for ((key, rawVal) in ud) {
      val value = sha256hash(normalizeData(key, rawVal.trim { it <= ' ' }))
      if (internalHashedUserData.containsKey(key)) {
        val originalVal = internalHashedUserData[key]
        val previousData = originalVal?.split(DATA_SEPARATOR.toRegex())?.toTypedArray() ?: arrayOf()
        val set: MutableSet<String?> = mutableSetOf(*previousData)
        if (set.contains(value)) {
          return
        }
        val sb = StringBuilder()
        if (previousData.isEmpty()) {
          sb.append(value)
        } else if (previousData.size < MAX_NUM) {
          sb.append(originalVal).append(DATA_SEPARATOR).append(value)
        } else {
          for (i in 1 until MAX_NUM) {
            sb.append(previousData[i]).append(DATA_SEPARATOR)
          }
          sb.append(value)
          set.remove(previousData[0])
        }
        // Update new added value into hashed User Data and save to cache
        internalHashedUserData[key] = sb.toString()
      } else {
        internalHashedUserData[key] = value
      }
    }
    writeDataIntoCache(INTERNAL_USER_DATA_KEY, mapToJsonStr(internalHashedUserData))
  }

  private fun normalizeData(type: String, data: String): String {
    var data = data
    data = data.trim { it <= ' ' }.toLowerCase()
    if (EMAIL == type) {
      return if (Patterns.EMAIL_ADDRESS.matcher(data).matches()) {
        data
      } else {
        Log.e(TAG, "Setting email failure: this is not a valid email address")
        ""
      }
    }
    if (PHONE == type) {
      return data.replace("[^0-9]".toRegex(), "")
    }
    if (GENDER == type) {
      data = if (data.isNotEmpty()) data.substring(0, 1) else ""
      return if ("f" == data || "m" == data) {
        data
      } else {
        Log.e(TAG, "Setting gender failure: the supported value for gender is f or m")
        ""
      }
    }
    return data
  }

  private fun maybeSHA256Hashed(data: String): Boolean {
    return data.matches(Regex("[A-Fa-f0-9]{64}"))
  }
}
