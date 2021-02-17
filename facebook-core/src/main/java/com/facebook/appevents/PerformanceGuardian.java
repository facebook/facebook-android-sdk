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

package com.facebook.appevents;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.RestrictTo;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Avoid process activities which process time exceed threshold. Reboot the process if app version
 * changed.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@AutoHandleExceptions
public class PerformanceGuardian {

  public enum UseCase {
    CODELESS,
    SUGGESTED_EVENT;
  }

  private static boolean initialized = false;
  private static SharedPreferences sharedPreferences;

  private static final String BANNED_ACTIVITY_STORE = "com.facebook.internal.BANNED_ACTIVITY";
  private static final String CACHE_APP_VERSION = "app_version";
  private static final Integer ACTIVITY_PROCESS_TIME_THRESHOLD = 40; // ms
  private static final Integer MAX_EXCEED_LIMIT_COUNT = 3;
  private static final Set<String> bannedSuggestedEventActivitySet = new HashSet<>();
  private static final Set<String> bannedCodelessActivitySet = new HashSet<>();
  private static final Map<String, Integer> activityProcessTimeMapCodeless = new HashMap<>();
  private static final Map<String, Integer> activityProcessTimeMapSe = new HashMap<>();

  private static synchronized void initializeIfNotYet() {
    if (initialized) {
      return;
    }
    sharedPreferences =
        FacebookSdk.getApplicationContext()
            .getSharedPreferences(BANNED_ACTIVITY_STORE, Context.MODE_PRIVATE);

    String cachedVersion = sharedPreferences.getString(CACHE_APP_VERSION, "");
    if (!isCacheValid(cachedVersion)) {
      sharedPreferences.edit().clear().apply();
    } else {
      bannedCodelessActivitySet.addAll(
          sharedPreferences.getStringSet(UseCase.CODELESS.toString(), new HashSet<String>()));
      bannedSuggestedEventActivitySet.addAll(
          sharedPreferences.getStringSet(
              UseCase.SUGGESTED_EVENT.toString(), new HashSet<String>()));
    }
    initialized = true;
  }

  /**
   * Return true when current activity is banned. Activities which process time exceed limit
   * multiple times will be added into banned set.
   */
  public static boolean isBannedActivity(String activityName, UseCase useCase) {
    initializeIfNotYet();

    switch (useCase) {
      case CODELESS:
        return bannedCodelessActivitySet.contains(activityName);
      case SUGGESTED_EVENT:
        return bannedSuggestedEventActivitySet.contains(activityName);
    }
    return false;
  }

  /**
   * Calculate current activity process time. Return without action if current process time is
   * normal. Add current activity to banned activity set if exceed activity process time threshold.
   * Update app version.
   */
  public static void limitProcessTime(
      String activityName, UseCase useCase, long startTime, long endTime) {
    initializeIfNotYet();

    long processTime = endTime - startTime;
    if (activityName == null || processTime < ACTIVITY_PROCESS_TIME_THRESHOLD) {
      return;
    }

    switch (useCase) {
      case CODELESS:
        updateActivityMap(
            useCase, activityName, activityProcessTimeMapCodeless, bannedCodelessActivitySet);
        break;
      case SUGGESTED_EVENT:
        updateActivityMap(
            useCase, activityName, activityProcessTimeMapSe, bannedSuggestedEventActivitySet);
        break;
    }
  }

  private static void updateActivityMap(
      UseCase useCase,
      String activityName,
      Map<String, Integer> activityExceedLimitCountMap,
      Set<String> bannedActivitySet) {
    int curExceedLimitCount = 0;
    if (activityExceedLimitCountMap.containsKey(activityName)) {
      curExceedLimitCount = activityExceedLimitCountMap.get(activityName);
    }
    activityExceedLimitCountMap.put(activityName, curExceedLimitCount + 1);
    if (curExceedLimitCount + 1 >= MAX_EXCEED_LIMIT_COUNT) {
      bannedActivitySet.add(activityName);

      sharedPreferences
          .edit()
          .putStringSet(useCase.toString(), bannedActivitySet)
          .putString(CACHE_APP_VERSION, Utility.getAppVersion())
          .apply();
    }
  }

  private static boolean isCacheValid(String previousVersion) {
    String appVersion = Utility.getAppVersion();
    if (appVersion == null || previousVersion.isEmpty()) {
      return false;
    }
    return previousVersion.equals(appVersion);
  }
}
