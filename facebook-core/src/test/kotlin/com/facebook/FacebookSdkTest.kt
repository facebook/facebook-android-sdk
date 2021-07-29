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
import android.os.ConditionVariable
import androidx.test.core.app.ApplicationProvider
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.ServerProtocol.getGraphUrlBase
import com.facebook.internal.Utility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

@PrepareForTest(
    FacebookSdk::class,
    FetchedAppSettingsManager::class,
    Utility::class,
    UserSettingsManager::class)
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
      val success = condition.block(5000)
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
    FacebookSdk.setApplicationId(null)
    MemberModifier.stub<Any>(MemberMatcher.method(FacebookSdk::class.java, "isInitialized"))
        .toReturn(true)
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
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    assertThatThrownBy { FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1000) }
        .isInstanceOf(FacebookException::class.java)
        .hasMessage(FacebookSdk.CALLBACK_OFFSET_CHANGED_AFTER_INIT)
  }

  @Test
  fun testRequestCodeOffsetNegative() {
    FacebookSdk.setApplicationId("123456789")
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
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1000)
    assertThat(FacebookSdk.getCallbackRequestCodeOffset()).isEqualTo(1000)
  }

  @Test
  fun testRequestCodeRange() {
    FacebookSdk.setApplicationId("123456789")
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application, 1000)
    assertThat(FacebookSdk.isFacebookRequestCode(1000)).isTrue
    assertThat(FacebookSdk.isFacebookRequestCode(1099)).isTrue
    assertThat(FacebookSdk.isFacebookRequestCode(999)).isFalse
    assertThat(FacebookSdk.isFacebookRequestCode(1100)).isFalse
    assertThat(FacebookSdk.isFacebookRequestCode(0)).isFalse
  }

  @Test
  fun testFullyInitialize() {
    FacebookSdk.setApplicationId("123456789")
    MemberModifier.stub<Any>(MemberMatcher.method(FacebookSdk::class.java, "getAutoInitEnabled"))
        .toReturn(true)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    assertThat(FacebookSdk.isFullyInitialized()).isTrue
  }

  @Test
  fun testNotFullyInitialize() {
    FacebookSdk.setApplicationId("123456789")
    val field = FacebookSdk::class.java.getDeclaredField("sdkFullyInitialized")
    field.isAccessible = true
    field[null] = false
    MemberModifier.stub<Any>(MemberMatcher.method(FacebookSdk::class.java, "getAutoInitEnabled"))
        .toReturn(false)
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
