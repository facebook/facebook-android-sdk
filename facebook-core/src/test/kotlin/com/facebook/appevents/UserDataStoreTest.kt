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

import android.os.Bundle
import android.preference.PreferenceManager
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookTestUtility.assertEquals
import com.facebook.MockSharedPreference
import com.facebook.internal.Utility.sha256hash
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.RuntimeEnvironment

@PrepareForTest(FacebookSdk::class, PreferenceManager::class)
class UserDataStoreTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  @Before
  override fun setup() {
    super.setup()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.getApplicationContext()).thenReturn(RuntimeEnvironment.application)
    whenever(FacebookSdk.getExecutor()).thenReturn(mockExecutor)

    PowerMockito.mockStatic(PreferenceManager::class.java)

    val mockCompanion: InternalAppEventsLogger.Companion = mock()
    WhiteboxImpl.setInternalState(InternalAppEventsLogger::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.getAnalyticsExecutor()).thenReturn(mockExecutor)
  }

  @Test
  fun testInitStore() {
    // Test initStore without cache in SharedPreference
    Whitebox.setInternalState(UserDataStore::class.java, "initialized", AtomicBoolean(false))
    val mockPreference = MockSharedPreference()
    whenever(PreferenceManager.getDefaultSharedPreferences(any())).thenReturn(mockPreference)
    Whitebox.setInternalState(
        UserDataStore::class.java, "externalHashedUserData", ConcurrentHashMap<String, String>())
    UserDataStore.initStore()
    var externalHashedUserData =
        Whitebox.getInternalState<ConcurrentHashMap<String?, String?>>(
            UserDataStore::class.java, "externalHashedUserData")
    assertThat(externalHashedUserData).isEmpty()

    // Test initStore with cache in SharedPreference
    val cacheData: MutableMap<String?, String?> = HashMap()
    cacheData["key1"] = "val1"
    cacheData["key2"] = "val2"
    Whitebox.setInternalState(UserDataStore::class.java, "initialized", AtomicBoolean(false))
    mockPreference
        .edit()
        .putString(
            "com.facebook.appevents.UserDataStore.userData",
            JSONObject(cacheData as Map<*, *>).toString())
    whenever(PreferenceManager.getDefaultSharedPreferences(any())).thenReturn(mockPreference)
    UserDataStore.initStore()
    externalHashedUserData =
        Whitebox.getInternalState(UserDataStore::class.java, "externalHashedUserData")
    assertThat(externalHashedUserData).isEqualTo(cacheData)
  }

  @Test
  fun testSetUserDataAndHash() {
    val mockPreference = MockSharedPreference()
    whenever(PreferenceManager.getDefaultSharedPreferences(any())).thenReturn(mockPreference)
    Whitebox.setInternalState(UserDataStore::class.java, "initialized", AtomicBoolean(false))
    val email = "test@fb.com"
    val phone = "8008007000"
    UserDataStore.setUserDataAndHash(email, null, null, phone, null, null, null, null, null, null)
    val expectedData: MutableMap<String?, String?> = HashMap()
    expectedData[UserDataStore.EMAIL] = sha256hash(email)
    expectedData[UserDataStore.PHONE] = sha256hash(phone)
    var expected = JSONObject(expectedData as Map<*, *>)
    var actual = JSONObject(UserDataStore.getHashedUserData())
    assertEquals(expected, actual)
    val bundleData = Bundle()
    bundleData.putString(UserDataStore.EMAIL, "android@fb.com")
    UserDataStore.setUserDataAndHash(bundleData)
    expectedData[UserDataStore.EMAIL] = sha256hash("android@fb.com")
    expected = JSONObject(expectedData as Map<*, *>)
    actual = JSONObject(UserDataStore.getHashedUserData())
    assertEquals(expected, actual)
  }

  @Test
  fun testClear() {
    val mockPreference = MockSharedPreference()
    whenever(PreferenceManager.getDefaultSharedPreferences(any())).thenReturn(mockPreference)
    Whitebox.setInternalState(UserDataStore::class.java, "initialized", AtomicBoolean(false))
    UserDataStore.setUserDataAndHash(
        "test@fb.com", null, null, "8008007000", null, null, null, null, null, null)
    UserDataStore.clear()
    assertThat(UserDataStore.getHashedUserData()).isEmpty()
  }
}
