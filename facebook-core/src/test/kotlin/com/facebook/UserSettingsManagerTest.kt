/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.UserSettingsManager.getAdvertiserIDCollectionEnabled
import com.facebook.UserSettingsManager.getAutoInitEnabled
import com.facebook.UserSettingsManager.getAutoLogAppEventsEnabled
import com.facebook.UserSettingsManager.getCodelessSetupEnabled
import com.facebook.UserSettingsManager.setAdvertiserIDCollectionEnabled
import com.facebook.UserSettingsManager.setAutoInitEnabled
import com.facebook.UserSettingsManager.setAutoLogAppEventsEnabled
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.FetchedAppSettingsManager
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import java.util.concurrent.atomic.AtomicBoolean

@PrepareForTest(UserSettingsManager::class, FacebookSdk::class, FetchedAppSettingsManager::class)
class UserSettingsManagerTest : FacebookPowerMockTestCase() {
  private lateinit var mockApplicationContext: Context
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockApplicationInfo: ApplicationInfo
  private lateinit var mockLogger: InternalAppEventsLogger
  private lateinit var mockSharedPreference: SharedPreferences
  
  @Before
  override fun setup() {
    super.setup()
    FacebookSdk.setApplicationId("123456789")
    mockPackageManager = mock()
    mockApplicationContext = mock()
    mockApplicationInfo = mock()
    mockSharedPreference = MockSharedPreference()
    whenever(mockApplicationContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockApplicationContext.applicationContext).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getApplicationInfo(any(), any())).thenReturn(mockApplicationInfo)
    whenever(mockApplicationContext.getSharedPreferences(any<String>(), any()))
        .thenReturn(mockSharedPreference)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext)
    whenever(FacebookSdk.getAdvertiserIDCollectionEnabled()).thenCallRealMethod()
    whenever(FacebookSdk.setAdvertiserIDCollectionEnabled(any())).thenCallRealMethod()
    whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenCallRealMethod()
    whenever(FacebookSdk.setAutoLogAppEventsEnabled(any())).thenCallRealMethod()
    whenever(FacebookSdk.getAutoInitEnabled()).thenCallRealMethod()
    whenever(FacebookSdk.setAutoInitEnabled(any())).thenCallRealMethod()
    whenever(FacebookSdk.getCodelessSetupEnabled()).thenCallRealMethod()
    whenever(FacebookSdk.getExecutor()).thenReturn(mock())
    mockLogger = mock()
    PowerMockito.whenNew(InternalAppEventsLogger::class.java)
        .withAnyArguments()
        .thenReturn(mockLogger)
  }

  @After
  fun clean() {
    mockSharedPreference.edit().clear().commit()
  }

  @Test
  fun testAutoInitEnabled() {
    PowerMockito.mockStatic(UserSettingsManager::class.java)
    var getAutoInitEnabledTimes = 0
    var setAutoInitEnabledValue: Boolean? = null
    whenever(getAutoInitEnabled()).thenAnswer {
      getAutoInitEnabledTimes++
      true
    }
    whenever(setAutoInitEnabled(any())).thenAnswer {
      setAutoInitEnabledValue = it.arguments[0] as Boolean
      Unit
    }

    val enable = FacebookSdk.getAutoInitEnabled()
    assertThat(enable).isTrue
    assertThat(getAutoInitEnabledTimes).isEqualTo(1)

    FacebookSdk.setAutoInitEnabled(false)
    assertThat(setAutoInitEnabledValue).isFalse
  }

  @Test
  fun testAutoLogEnabled() {
    PowerMockito.mockStatic(UserSettingsManager::class.java)
    var getAutoLogAppEventsEnabledTimes = 0
    var setAutoLogAppEventsEnabledbledValue: Boolean? = null
    whenever(getAutoLogAppEventsEnabled()).thenAnswer {
      getAutoLogAppEventsEnabledTimes++
      true
    }
    whenever(setAutoLogAppEventsEnabled(any())).thenAnswer {
      setAutoLogAppEventsEnabledbledValue = it.arguments[0] as Boolean
      Unit
    }

    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isTrue
    assertThat(getAutoLogAppEventsEnabledTimes).isEqualTo(1)
    FacebookSdk.setAutoLogAppEventsEnabled(false)
    assertThat(setAutoLogAppEventsEnabledbledValue).isFalse
  }

  @Test
  fun `test AutoLogEnabled without values from server`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    whenever(FetchedAppSettingsManager.getCachedMigratedAutoLogValuesInAppSettings()).thenReturn(null)

    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isTrue
  }

  @Test
  fun `test AutoLogEnabled return the enabled value fetched from server`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockedAutoLogAppEventsValues = HashMap<String, Boolean>()
    mockedAutoLogAppEventsValues[FetchedAppSettingsManager.AUTO_LOG_APP_EVENTS_DEFAULT_FIELD] = false
    mockedAutoLogAppEventsValues[FetchedAppSettingsManager.AUTO_LOG_APP_EVENT_ENABLED_FIELD] = false
    whenever(FetchedAppSettingsManager.getCachedMigratedAutoLogValuesInAppSettings()).thenReturn(mockedAutoLogAppEventsValues)

    setAutoLogAppEventsEnabled(true)
    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isFalse
  }

  @Test
  fun `test AutoLogEnabled return the value in cache`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockedAutoLogAppEventsValues = HashMap<String, Boolean>()
    mockedAutoLogAppEventsValues[FetchedAppSettingsManager.AUTO_LOG_APP_EVENTS_DEFAULT_FIELD] = false
    whenever(FetchedAppSettingsManager.getCachedMigratedAutoLogValuesInAppSettings()).thenReturn(mockedAutoLogAppEventsValues)

    val jsonObject = JSONObject()
    jsonObject.put("value", true)
    mockSharedPreference.edit().putString(FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY, jsonObject.toString()).apply()
    Whitebox.setInternalState(UserSettingsManager::class.java,"isInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(UserSettingsManager::class.java,"userSettingPref", mockSharedPreference)

    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isTrue
  }

  @Test
  fun `test AutoLogEnabled return the value set in manifest file`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockedAutoLogAppEventsValues = HashMap<String, Boolean>()
    mockedAutoLogAppEventsValues[FetchedAppSettingsManager.AUTO_LOG_APP_EVENTS_DEFAULT_FIELD] = false
    whenever(FetchedAppSettingsManager.getCachedMigratedAutoLogValuesInAppSettings()).thenReturn(mockedAutoLogAppEventsValues)
    Whitebox.setInternalState(UserSettingsManager::class.java,"isInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(UserSettingsManager::class.java,"userSettingPref", mockSharedPreference)

    val metaData = Bundle()
    metaData.putBoolean(FacebookSdk.AUTO_LOG_APP_EVENTS_ENABLED_PROPERTY, true)
    mockApplicationInfo.metaData = metaData

    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isTrue
  }

  @Test
  fun `test AutoLogEnabled return the default value fetched from server`() {
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
    val mockedAutoLogAppEventsValues = HashMap<String, Boolean>()
    mockedAutoLogAppEventsValues[FetchedAppSettingsManager.AUTO_LOG_APP_EVENTS_DEFAULT_FIELD] = false
    whenever(FetchedAppSettingsManager.getCachedMigratedAutoLogValuesInAppSettings()).thenReturn(mockedAutoLogAppEventsValues)
    Whitebox.setInternalState(UserSettingsManager::class.java,"isInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(UserSettingsManager::class.java,"userSettingPref", mockSharedPreference)

    val enable = FacebookSdk.getAutoLogAppEventsEnabled()
    assertThat(enable).isFalse
  }
  
  @Test
  fun testAdvertiserIDCollectionEnabled() {
    PowerMockito.mockStatic(UserSettingsManager::class.java)
    var getAdvertiserIDCollectionEnabledTimes = 0
    var setAdvertiserIDCollectionEnabledValue: Boolean? = null
    whenever(getAdvertiserIDCollectionEnabled()).thenAnswer {
      getAdvertiserIDCollectionEnabledTimes++
      true
    }
    whenever(setAdvertiserIDCollectionEnabled(any())).thenAnswer {
      setAdvertiserIDCollectionEnabledValue = it.arguments[0] as Boolean
      Unit
    }

    val enable = FacebookSdk.getAdvertiserIDCollectionEnabled()
    assertThat(enable).isTrue
    assertThat(getAdvertiserIDCollectionEnabledTimes).isEqualTo(1)
    FacebookSdk.setAdvertiserIDCollectionEnabled(false)
    assertThat(setAdvertiserIDCollectionEnabledValue).isFalse
  }

  @Test
  fun testCodelessSetupEnabled() {
    PowerMockito.mockStatic(UserSettingsManager::class.java)
    var getCodelessSetupEnabledTimes = 0
    whenever(getCodelessSetupEnabled()).thenAnswer {
      getCodelessSetupEnabledTimes++
      true
    }

    val enable = FacebookSdk.getCodelessSetupEnabled()
    assertThat(enable).isTrue
    assertThat(getCodelessSetupEnabledTimes).isEqualTo(1)
  }

  @Test
  fun testLogIfSDKSettingsChanged() {
    setAdvertiserIDCollectionEnabled(false)
    verify(mockLogger).logChangedSettingsEvent(any())
  }

  @Test
  fun `test logIfAutoAppLinkEnabled`() {
    val metaData = Bundle()
    metaData.putBoolean("com.facebook.sdk.AutoAppLinkEnabled", true)
    mockApplicationInfo.metaData = metaData
    UserSettingsManager.logIfAutoAppLinkEnabled()
    verify(mockLogger).logEvent(eq("fb_auto_applink"), any())
  }

  @Test
  fun `test get and set monitorEnabled`() {
    UserSettingsManager.setMonitorEnabled(true)
    assertThat(UserSettingsManager.getMonitorEnabled()).isTrue
    UserSettingsManager.setMonitorEnabled(false)
    assertThat(UserSettingsManager.getMonitorEnabled()).isFalse
  }
}
