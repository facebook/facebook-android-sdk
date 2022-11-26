/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import com.facebook.FacebookSdk
import com.facebook.LoggingBehavior
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.internal.HashUtils.computeChecksum
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.internal.security.CertificateUtil.getCertificateHash
import java.util.Locale

@AutoHandleExceptions
internal object SessionLogger {
  private const val PACKAGE_CHECKSUM = "PCKGCHKSUM"
  private val TAG = SessionLogger::class.java.canonicalName
  private val INACTIVE_SECONDS_QUANTA =
      longArrayOf(
          5 * DateUtils.MINUTE_IN_MILLIS,
          15 * DateUtils.MINUTE_IN_MILLIS,
          30 * DateUtils.MINUTE_IN_MILLIS,
          1 * DateUtils.HOUR_IN_MILLIS,
          6 * DateUtils.HOUR_IN_MILLIS,
          12 * DateUtils.HOUR_IN_MILLIS,
          1 * DateUtils.DAY_IN_MILLIS,
          2 * DateUtils.DAY_IN_MILLIS,
          3 * DateUtils.DAY_IN_MILLIS,
          7 * DateUtils.DAY_IN_MILLIS,
          14 * DateUtils.DAY_IN_MILLIS,
          21 * DateUtils.DAY_IN_MILLIS,
          28 * DateUtils.DAY_IN_MILLIS,
          60 * DateUtils.DAY_IN_MILLIS,
          90 * DateUtils.DAY_IN_MILLIS,
          120 * DateUtils.DAY_IN_MILLIS,
          150 * DateUtils.DAY_IN_MILLIS,
          180 * DateUtils.DAY_IN_MILLIS,
          365 * DateUtils.DAY_IN_MILLIS)

  @JvmStatic
  fun logActivateApp(
      activityName: String,
      sourceApplicationInfo: SourceApplicationInfo?,
      appId: String?,
      context: Context
  ) {
    val sourAppInfoStr = sourceApplicationInfo?.toString() ?: "Unclassified"
    val eventParams = Bundle()
    eventParams.putString(AppEventsConstants.EVENT_PARAM_SOURCE_APPLICATION, sourAppInfoStr)
    eventParams.putString(
        AppEventsConstants.EVENT_PARAM_PACKAGE_FP, computePackageChecksum(context))
    eventParams.putString(AppEventsConstants.EVENT_PARAM_APP_CERT_HASH, getCertificateHash(context))
    val logger = InternalAppEventsLogger.createInstance(activityName, appId, null)
    logger.logEvent(AppEventsConstants.EVENT_NAME_ACTIVATED_APP, eventParams)
    if (InternalAppEventsLogger.getFlushBehavior() != AppEventsLogger.FlushBehavior.EXPLICIT_ONLY) {
      logger.flush()
    }
  }

  @JvmStatic
  fun logDeactivateApp(activityName: String, sessionInfo: SessionInfo?, appId: String?) {
    if (sessionInfo == null) {
      return
    }
    var interruptionDurationMillis =
        sessionInfo.diskRestoreTime ?: 0 - (sessionInfo.sessionLastEventTime ?: 0)
    if (interruptionDurationMillis < 0) {
      interruptionDurationMillis = 0L
      logClockSkewEvent()
    }
    var sessionLength = sessionInfo.sessionLength
    if (sessionLength < 0) {
      logClockSkewEvent()
      sessionLength = 0L
    }
    val eventParams = Bundle()
    eventParams.putInt(
        AppEventsConstants.EVENT_NAME_SESSION_INTERRUPTIONS, sessionInfo.interruptionCount)
    eventParams.putString(
        AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS,
        String.format(Locale.ROOT, "session_quanta_%d", getQuantaIndex(interruptionDurationMillis)))
    val sourceApplicationInfo = sessionInfo.sourceApplicationInfo
    val sourAppInfoStr = sourceApplicationInfo?.toString() ?: "Unclassified"
    eventParams.putString(AppEventsConstants.EVENT_PARAM_SOURCE_APPLICATION, sourAppInfoStr)
    eventParams.putLong(
        Constants.LOG_TIME_APP_EVENT_KEY, (sessionInfo.sessionLastEventTime ?: 0) / 1000)
    InternalAppEventsLogger.createInstance(activityName, appId, null)
        .logEvent(
            AppEventsConstants.EVENT_NAME_DEACTIVATED_APP,
            sessionLength.toDouble() / DateUtils.SECOND_IN_MILLIS,
            eventParams)
  }

  private fun logClockSkewEvent() {
    log(LoggingBehavior.APP_EVENTS, TAG!!, "Clock skew detected")
  }

  @JvmStatic
  fun getQuantaIndex(timeBetweenSessions: Long): Int {
    var quantaIndex = 0
    while (quantaIndex < INACTIVE_SECONDS_QUANTA.size &&
        INACTIVE_SECONDS_QUANTA[quantaIndex] < timeBetweenSessions) {
      ++quantaIndex
    }
    return quantaIndex
  }

  private fun computePackageChecksum(context: Context): String? {
    return try {
      // First, try to check if package hash already computed
      val pm = context.packageManager
      val packageVersion = pm.getPackageInfo(context.packageName, 0).versionName
      val packageHashSharedPrefKey = PACKAGE_CHECKSUM + ";" + packageVersion
      val preferences =
          context.getSharedPreferences(FacebookSdk.APP_EVENT_PREFERENCES, Context.MODE_PRIVATE)
      var packageHash = preferences.getString(packageHashSharedPrefKey, null)
      if (packageHash != null && packageHash.length == 32) {
        return packageHash
      }
      // Second, try to get the checksum through Android S checksum API
      val androidPackageManagerChecksum = HashUtils.computeChecksumWithPackageManager(context, null)
      packageHash =
          if (androidPackageManagerChecksum != null) {
            androidPackageManagerChecksum
          } else {
            // Finally, compute checksum and cache it.
            val ai = pm.getApplicationInfo(context.packageName, 0)
            computeChecksum(ai.sourceDir)
          }
      preferences.edit().putString(packageHashSharedPrefKey, packageHash).apply()
      packageHash
    } catch (e: Exception) {
      null
    }
  }
}
