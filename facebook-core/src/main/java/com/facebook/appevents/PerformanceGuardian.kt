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

package com.facebook.appevents

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo
import com.facebook.FacebookSdk
import com.facebook.internal.Utility.getAppVersion
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions

/**
 * Avoid process activities which process time exceed threshold. Reboot the process if app version
 * changed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
object PerformanceGuardian {
  private var initialized = false
  private lateinit var sharedPreferences: SharedPreferences
  private const val BANNED_ACTIVITY_STORE = "com.facebook.internal.BANNED_ACTIVITY"
  private const val CACHE_APP_VERSION = "app_version"
  private const val ACTIVITY_PROCESS_TIME_THRESHOLD = 40 // ms
  private const val MAX_EXCEED_LIMIT_COUNT = 3
  private val bannedSuggestedEventActivitySet: MutableSet<String> = hashSetOf()
  private val bannedCodelessActivitySet: MutableSet<String> = hashSetOf()
  private val activityProcessTimeMapCodeless: MutableMap<String, Int> = hashMapOf()
  private val activityProcessTimeMapSe: MutableMap<String, Int> = hashMapOf()
  @Synchronized
  @JvmStatic
  private fun initializeIfNotYet() {
    if (initialized) {
      return
    }
    sharedPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(BANNED_ACTIVITY_STORE, Context.MODE_PRIVATE)
    val cachedVersion = sharedPreferences.getString(CACHE_APP_VERSION, "")
    if (!isCacheValid(cachedVersion)) {
      sharedPreferences.edit().clear().apply()
    } else {
      bannedCodelessActivitySet.addAll(
          sharedPreferences.getStringSet(UseCase.CODELESS.toString(), mutableSetOf())
              ?: mutableSetOf())
      bannedSuggestedEventActivitySet.addAll(
          sharedPreferences.getStringSet(UseCase.SUGGESTED_EVENT.toString(), mutableSetOf())
              ?: mutableSetOf())
    }
    initialized = true
  }

  /**
   * Return true when current activity is banned. Activities which process time exceed limit
   * multiple times will be added into banned set.
   */
  @JvmStatic
  fun isBannedActivity(activityName: String, useCase: UseCase?): Boolean {
    initializeIfNotYet()
    when (useCase) {
      UseCase.CODELESS -> return bannedCodelessActivitySet.contains(activityName)
      UseCase.SUGGESTED_EVENT -> return bannedSuggestedEventActivitySet.contains(activityName)
      else -> {}
    }
    return false
  }

  /**
   * Calculate current activity process time. Return without action if current process time is
   * normal. Add current activity to banned activity set if exceed activity process time threshold.
   * Update app version.
   */
  @JvmStatic
  fun limitProcessTime(activityName: String?, useCase: UseCase?, startTime: Long, endTime: Long) {
    initializeIfNotYet()
    val processTime = endTime - startTime
    if (activityName == null || processTime < ACTIVITY_PROCESS_TIME_THRESHOLD) {
      return
    }
    when (useCase) {
      UseCase.CODELESS ->
          updateActivityMap(
              useCase, activityName, activityProcessTimeMapCodeless, bannedCodelessActivitySet)
      UseCase.SUGGESTED_EVENT ->
          updateActivityMap(
              useCase, activityName, activityProcessTimeMapSe, bannedSuggestedEventActivitySet)
      else -> {}
    }
  }

  private fun updateActivityMap(
      useCase: UseCase,
      activityName: String,
      activityExceedLimitCountMap: MutableMap<String, Int>,
      bannedActivitySet: MutableSet<String>
  ) {
    var curExceedLimitCount = 0
    if (activityExceedLimitCountMap.containsKey(activityName)) {
      curExceedLimitCount = activityExceedLimitCountMap[activityName] ?: 0
    }
    activityExceedLimitCountMap[activityName] = curExceedLimitCount + 1
    if (curExceedLimitCount + 1 >= MAX_EXCEED_LIMIT_COUNT) {
      bannedActivitySet.add(activityName)
      sharedPreferences
          .edit()
          .putStringSet(useCase.toString(), bannedActivitySet)
          .putString(CACHE_APP_VERSION, getAppVersion())
          .apply()
    }
  }

  private fun isCacheValid(previousVersion: String?): Boolean {
    val appVersion = getAppVersion()
    return if (appVersion == null || previousVersion == null || previousVersion.isEmpty()) {
      false
    } else previousVersion == appVersion
  }

  enum class UseCase {
    CODELESS,
    SUGGESTED_EVENT
  }
}
