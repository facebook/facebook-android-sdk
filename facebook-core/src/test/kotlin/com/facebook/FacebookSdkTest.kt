/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Bundle
import android.os.ConditionVariable
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.ServerProtocol.getGraphUrlBase
import com.facebook.internal.Utility
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

@PrepareForTest(FetchedAppSettingsManager::class, Utility::class, UserSettingsManager::class)
class FacebookSdkTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    val mockPreference = MockSharedPreference()
    Whitebox.setInternalState(UserSettingsManager::class.java, "userSettingPref", mockPreference)
    Whitebox.setInternalState(UserSettingsManager::class.java, "isInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(FacebookSdk::class.java, "callbackRequestCodeOffset", 0xface)
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(false))
    MemberModifier.stub<Any?>(
            MemberMatcher.method(FetchedAppSettingsManager::class.java, "loadAppSettingsAsync"))
        .toReturn(null)
    FacebookSdk.setAutoLogAppEventsEnabled(false)
  }

  @Test
  fun testSetExecutor() {
    val condition = ConditionVariable()
    val runnable = Runnable {}
    val executor = Executor { command ->
      assertThat(command).isSameAs(runnable)
      command.run()
      condition.open()
    }
    val original = FacebookSdk.getExecutor()
    try {
      FacebookSdk.setExecutor(executor)
      FacebookSdk.getExecutor().execute(runnable)
      val success = condition.block(5_000)
      assertThat(success).isTrue
    } finally {
      FacebookSdk.setExecutor(original)
    }
  }

  @Test
  fun testFacebookDomain() {
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    Whitebox.setInternalState(
        FacebookSdk::class.java,
        "applicationContext",
        ApplicationProvider.getApplicationContext<Context>())
    FacebookSdk.setFacebookDomain("beta.facebook.com")
    val graphUrlBase = getGraphUrlBase()
    assertThat(graphUrlBase).isEqualTo("https://graph.beta.facebook.com")
    FacebookSdk.setFacebookDomain("facebook.com")
  }

  @Test
  fun testLoadDefaults() {
    // Set to null since the value might have been set by another test
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationId", null as String?)
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    FacebookSdk.loadDefaultsFromMetadata(mockContextWithAppIdAndClientToken())
    assertThat(FacebookSdk.getApplicationId()).isEqualTo(TEST_APPLICATION_ID)
    assertThat(FacebookSdk.getClientToken()).isEqualTo(TEST_CLIENT_TOKEN)
  }

  @Test
  fun testLoadDefaultsDoesNotOverwrite() {
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    FacebookSdk.setApplicationId("hello")
    FacebookSdk.setClientToken("world")
    FacebookSdk.loadDefaultsFromMetadata(mockContextWithAppIdAndClientToken())
    assertThat(FacebookSdk.getAutoLogAppEventsEnabled()).isFalse
    assertThat(FacebookSdk.getApplicationId()).isEqualTo("hello")
    assertThat(FacebookSdk.getClientToken()).isEqualTo("world")
  }

  @Test
  fun testRequestCodeOffsetAfterInit() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    assertThatThrownBy { FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1_000) }
        .isInstanceOf(FacebookException::class.java)
        .hasMessage(FacebookSdk.CALLBACK_OFFSET_CHANGED_AFTER_INIT)
  }

  @Test
  fun testRequestCodeOffsetNegative() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    assertThatThrownBy {
          // last bit set, so negative
          FacebookSdk.sdkInitialize(RuntimeEnvironment.application, -0x5314ff4)
        }
        .isInstanceOf(FacebookException::class.java)
        .hasMessage(FacebookSdk.CALLBACK_OFFSET_NEGATIVE)
  }

  @Test
  fun testRequestCodeOffset() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1_000)
    assertThat(FacebookSdk.getCallbackRequestCodeOffset()).isEqualTo(1_000)
  }

  @Test
  fun testRequestCodeRange() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1_000)
    assertThat(FacebookSdk.isFacebookRequestCode(1_000)).isTrue
    assertThat(FacebookSdk.isFacebookRequestCode(1_099)).isTrue
    assertThat(FacebookSdk.isFacebookRequestCode(999)).isFalse
    assertThat(FacebookSdk.isFacebookRequestCode(1_100)).isFalse
    assertThat(FacebookSdk.isFacebookRequestCode(0)).isFalse
  }

  @Test
  fun testFullyInitialize() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    MemberModifier.stub<Any>(MemberMatcher.method(FacebookSdk::class.java, "getAutoInitEnabled"))
        .toReturn(true)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    assertThat(FacebookSdk.isFullyInitialized()).isTrue
  }

  @Test
  fun testNotFullyInitialize() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.setClientToken(TEST_CLIENT_TOKEN)
    val field = FacebookSdk::class.java.getDeclaredField("isFullyInitialized")
    field.isAccessible = true
    field[null] = false
    UserSettingsManager.setAutoInitEnabled(false)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    assertThat(FacebookSdk.isFullyInitialized()).isFalse
  }

  @Test
  fun `test set data processing options will update shared preferences`() {
    val expectedDataProcessingOptions = arrayOf("option1", "option2")
    val expectedDataProcessingOptionsJSONObject = JSONObject()
    expectedDataProcessingOptionsJSONObject.put(
        FacebookSdk.DATA_PROCESSION_OPTIONS, JSONArray(expectedDataProcessingOptions.toList()))
    expectedDataProcessingOptionsJSONObject.put(FacebookSdk.DATA_PROCESSION_OPTIONS_COUNTRY, 1)
    expectedDataProcessingOptionsJSONObject.put(FacebookSdk.DATA_PROCESSION_OPTIONS_STATE, 10)
    val applicationContext = mock<Context>()
    val mockSharedPreference = MockSharedPreference()
    whenever(applicationContext.getSharedPreferences(any<String>(), any<Int>()))
        .thenReturn(mockSharedPreference)
    FacebookSdk.setApplicationId(TEST_APPLICATION_ID)
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationContext", applicationContext)

    FacebookSdk.setDataProcessingOptions(expectedDataProcessingOptions, 1, 10)

    assertThat(mockSharedPreference.getString(FacebookSdk.DATA_PROCESSION_OPTIONS, ""))
        .isEqualTo(expectedDataProcessingOptionsJSONObject.toString())
  }

  @Test
  fun `test set data processing options to null`() {
    val expectedDataProcessingOptionsJSONObject = JSONObject()
    expectedDataProcessingOptionsJSONObject.put(FacebookSdk.DATA_PROCESSION_OPTIONS, JSONArray())
    expectedDataProcessingOptionsJSONObject.put(FacebookSdk.DATA_PROCESSION_OPTIONS_COUNTRY, 0)
    expectedDataProcessingOptionsJSONObject.put(FacebookSdk.DATA_PROCESSION_OPTIONS_STATE, 0)
    val applicationContext = mock<Context>()
    val mockSharedPreference = MockSharedPreference()
    whenever(applicationContext.getSharedPreferences(any<String>(), any<Int>()))
        .thenReturn(mockSharedPreference)
    FacebookSdk.setApplicationId(TEST_APPLICATION_ID)
    Whitebox.setInternalState(FacebookSdk::class.java, "applicationContext", applicationContext)

    FacebookSdk.setDataProcessingOptions(null)

    assertThat(mockSharedPreference.getString(FacebookSdk.DATA_PROCESSION_OPTIONS, ""))
        .isEqualTo(expectedDataProcessingOptionsJSONObject.toString())
  }

  @Test
  fun `test get signature return a valid base64 result`() {
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    val mockContext = mock<Context>()
    val mockPackageManager = mock<PackageManager>()
    val mockPackageInfo = mock<PackageInfo>()
    val mockSignature = mock<Signature>()
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getPackageInfo(any<String>(), any())).thenReturn(mockPackageInfo)
    whenever(mockSignature.toByteArray()).thenReturn(byteArrayOf(1, 2, 3, 4))
    mockPackageInfo.signatures = arrayOf(mockSignature)

    val obtainedSignature = FacebookSdk.getApplicationSignature(mockContext)

    Base64.decode(obtainedSignature, Base64.URL_SAFE or Base64.NO_PADDING)
  }

  @Test
  fun `test get signature return null if package info is not available`() {
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(true))
    val mockContext = mock<Context>()
    val mockPackageManager = mock<PackageManager>()
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    whenever(mockContext.packageName).thenReturn("com.facebook.test")
    whenever(mockPackageManager.getPackageInfo(any<String>(), any()))
        .thenThrow(PackageManager.NameNotFoundException())

    assertThat(FacebookSdk.getApplicationSignature(mockContext)).isNull()
  }

  @Test(expected = FacebookException::class)
  fun `test sdk initialization will throw an exception if client token is null`() {
    Whitebox.setInternalState(FacebookSdk::class.java, "sdkInitialized", AtomicBoolean(false))
    FacebookSdk.setApplicationId(TEST_APPLICATION_ID)
    FacebookSdk.setClientToken(null)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
  }

  companion object {
    private const val TEST_APPLICATION_ID = "1234"
    private const val TEST_CLIENT_TOKEN = "abcd"

    private fun mockContextWithAppIdAndClientToken(): Context {
      val bundle = Bundle()
      bundle.putString(FacebookSdk.APPLICATION_ID_PROPERTY, TEST_APPLICATION_ID)
      bundle.putString(FacebookSdk.CLIENT_TOKEN_PROPERTY, TEST_CLIENT_TOKEN)
      val applicationInfo = mock<ApplicationInfo>()
      applicationInfo.metaData = bundle
      val packageManager = mock<PackageManager>()
      whenever(packageManager.getApplicationInfo("packageName", PackageManager.GET_META_DATA))
          .thenReturn(applicationInfo)
      val context = mock<Context>()
      whenever(context.packageName).thenReturn("packageName")
      whenever(context.packageManager).thenReturn(packageManager)
      return context
    }
  }
}
