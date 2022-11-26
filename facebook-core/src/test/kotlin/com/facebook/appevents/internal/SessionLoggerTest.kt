/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateUtils
import com.facebook.FacebookPowerMockTestCase
import com.facebook.MockSharedPreference
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.security.CertificateUtil
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(HashUtils::class, CertificateUtil::class)
class SessionLoggerTest : FacebookPowerMockTestCase() {
  private lateinit var mockSessionInfo: SessionInfo
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
  private lateinit var mockInternalAppEventsLoggerCompanion: InternalAppEventsLogger.Companion
  private lateinit var mockContext: Context
  private lateinit var mockPreferences: SharedPreferences
  private lateinit var mockPackageManager: PackageManager
  private lateinit var doubleArgumentCaptor: KArgumentCaptor<Double>
  private lateinit var bundleArgumentCaptor: KArgumentCaptor<Bundle>

  private val activityName = "swagactivity"
  private val appId = "yoloapplication"
  private val diskRestoreTime = 10L
  private val sessionLastEventTime = 1L
  private val zeroDelta = 0.0
  private val mockChecksum = "1aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

  @Before
  fun init() {
    doubleArgumentCaptor = argumentCaptor()
    bundleArgumentCaptor = argumentCaptor()
    mockSessionInfo = mock()
    whenever(mockSessionInfo.sessionLength).thenReturn(10L)
    Whitebox.setInternalState(mockSessionInfo, "sessionLastEventTime", sessionLastEventTime)
    Whitebox.setInternalState(mockSessionInfo, "diskRestoreTime", diskRestoreTime)

    mockInternalAppEventsLogger = mock()
    mockInternalAppEventsLoggerCompanion = mock()
    whenever(mockInternalAppEventsLoggerCompanion.createInstance(activityName, appId, null))
        .thenReturn(mockInternalAppEventsLogger)
    Whitebox.setInternalState(
        InternalAppEventsLogger::class.java, "Companion", mockInternalAppEventsLoggerCompanion)

    mockContext = mock()
    mockPackageManager = mock()
    whenever(mockContext.packageName).thenReturn(appId)
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    val mockPackageInfo = mock<PackageInfo>()
    mockPackageInfo.versionName = "v0.0"
    whenever(mockPackageManager.getPackageInfo(any<String>(), any())).thenReturn(mockPackageInfo)
    mockPreferences = MockSharedPreference()
    whenever(mockContext.getSharedPreferences(any<String>(), any())).thenReturn(mockPreferences)
    PowerMockito.mockStatic(HashUtils::class.java)
    whenever(HashUtils.computeChecksumWithPackageManager(eq(mockContext), anyOrNull()))
        .thenReturn(mockChecksum)
    PowerMockito.mockStatic(CertificateUtil::class.java)
    whenever(CertificateUtil.getCertificateHash(mockContext)).thenReturn("")
  }

  @Test
  fun `logDeactivateApp when sessionInfo is null`() {
    SessionLogger.logDeactivateApp(activityName, null, appId)
    verify(mockInternalAppEventsLogger, never())
        .logEvent(eq(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP), any(), any())
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null and sessionLength is negative`() {
    val expectedValueToSum = 0.0
    val sessionLengthNegative = -1L
    whenever(mockSessionInfo.sessionLength).thenReturn(sessionLengthNegative)

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)

    verify(mockInternalAppEventsLogger)
        .logEvent(
            eq(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            bundleArgumentCaptor.capture())
    assertThat(doubleArgumentCaptor.firstValue)
        .isEqualTo(expectedValueToSum, Offset.offset(zeroDelta))
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null and interruptionDurationMillis is negative`() {
    val sessionLastEventTime2 = 100L
    val diskRestoreTime2 = 1L
    Whitebox.setInternalState(mockSessionInfo, "sessionLastEventTime", sessionLastEventTime2)
    Whitebox.setInternalState(mockSessionInfo, "diskRestoreTime", diskRestoreTime2)

    val interruptionDurationMillis = 0L
    val fbMobileTimeBetweenSessions =
        String.format(
            Locale.ROOT,
            "session_quanta_%d",
            SessionLogger.getQuantaIndex(interruptionDurationMillis))
    val expectedValueToSum = mockSessionInfo.sessionLength.toDouble() / DateUtils.SECOND_IN_MILLIS

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)

    verify(mockInternalAppEventsLogger)
        .logEvent(
            eq(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            bundleArgumentCaptor.capture())
    assertThat(doubleArgumentCaptor.firstValue)
        .isEqualTo(expectedValueToSum, Offset.offset(zeroDelta))
    assertThat(
            bundleArgumentCaptor.firstValue.getString(
                AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS))
        .isEqualTo(fbMobileTimeBetweenSessions)
  }

  @Test
  fun `logDeactivateApp when sessionInfo is not null, sessionLength is positive, and interruptionDurationMillis is positive`() {
    val interruptionDurationMillis = diskRestoreTime - sessionLastEventTime
    val expectedValueToSum = mockSessionInfo.sessionLength.toDouble() / DateUtils.SECOND_IN_MILLIS
    val fbMobileTimeBetweenSessions =
        String.format(
            Locale.ROOT,
            "session_quanta_%d",
            SessionLogger.getQuantaIndex(interruptionDurationMillis))

    SessionLogger.logDeactivateApp(activityName, mockSessionInfo, appId)

    verify(mockInternalAppEventsLogger)
        .logEvent(
            eq(AppEventsConstants.EVENT_NAME_DEACTIVATED_APP),
            doubleArgumentCaptor.capture(),
            bundleArgumentCaptor.capture())
    assertThat(doubleArgumentCaptor.firstValue)
        .isEqualTo(expectedValueToSum, Offset.offset(zeroDelta))
    assertThat(
            bundleArgumentCaptor.firstValue.getString(
                AppEventsConstants.EVENT_NAME_TIME_BETWEEN_SESSIONS))
        .isEqualTo(fbMobileTimeBetweenSessions)
  }

  @Test
  fun `test logActivateApp will log EVENT_NAME_ACTIVATED_APP event and package checksum`() {
    SessionLogger.logActivateApp(activityName, null, appId, mockContext)

    verify(mockInternalAppEventsLogger)
        .logEvent(eq(AppEventsConstants.EVENT_NAME_ACTIVATED_APP), bundleArgumentCaptor.capture())
    val capturedBundle = bundleArgumentCaptor.firstValue
    assertThat(capturedBundle.getString(AppEventsConstants.EVENT_PARAM_PACKAGE_FP))
        .isEqualTo(mockChecksum)
  }
}
