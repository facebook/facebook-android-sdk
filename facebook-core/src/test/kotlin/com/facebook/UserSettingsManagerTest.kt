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

import android.content.Context
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(UserSettingsManager::class, FacebookSdk::class)
class UserSettingsManagerTest : FacebookPowerMockTestCase() {
  private lateinit var mockApplicationContext: Context
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockApplicationInfo: ApplicationInfo
  private lateinit var mockLogger: InternalAppEventsLogger
  @Before
  override fun setup() {
    super.setup()
    FacebookSdk.setApplicationId("123456789")
    mockPackageManager = mock()
    mockApplicationContext = mock()
    mockApplicationInfo = mock()
    whenever(mockApplicationContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockApplicationContext.applicationContext).thenReturn(mockApplicationContext)
    whenever(mockApplicationContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getApplicationInfo(any(), any())).thenReturn(mockApplicationInfo)
    whenever(mockApplicationContext.getSharedPreferences(any<String>(), any()))
        .thenReturn(MockSharedPreference())
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
