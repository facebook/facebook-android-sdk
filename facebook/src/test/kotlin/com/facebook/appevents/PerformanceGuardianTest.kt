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
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.Utility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(PerformanceGuardian::class, FacebookSdk::class, Utility::class)
class PerformanceGuardianTest : FacebookPowerMockTestCase() {

  @Before
  override fun setup() {
    super.setup()
    PowerMockito.spy(PerformanceGuardian::class.java)
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
  }

  @Test
  fun testIsBannedActivity() {
    // Initialize
    val mockPrefs: SharedPreferences = mock()
    val context: Context = mock()
    PowerMockito.`when`(FacebookSdk.getApplicationContext()).thenReturn(context)
    PowerMockito.`when`(context.getSharedPreferences(any<String>(), any())).thenReturn(mockPrefs)

    // Mock return app version
    PowerMockito.`when`(mockPrefs.getString("app_version", "")).thenReturn("1.2.0")
    PowerMockito.mockStatic(Utility::class.java)
    PowerMockito.`when`(Utility.getAppVersion()).thenReturn("1.2.0")

    // Mock return banned activity set
    val mockBannedCodelessActivitySet: MutableSet<String> = HashSet()
    mockBannedCodelessActivitySet.add("banned_activity_1")
    val mockBannedSuggestedEventActivitySet: MutableSet<String> = HashSet()
    mockBannedSuggestedEventActivitySet.add("banned_activity_2")
    PowerMockito.`when`(
            mockPrefs.getStringSet(PerformanceGuardian.UseCase.CODELESS.toString(), HashSet()))
        .thenReturn(mockBannedCodelessActivitySet)
    PowerMockito.`when`(
            mockPrefs.getStringSet(
                PerformanceGuardian.UseCase.SUGGESTED_EVENT.toString(), HashSet()))
        .thenReturn(mockBannedSuggestedEventActivitySet)

    // Banned codeless activity
    val result1 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_1", PerformanceGuardian.UseCase.CODELESS)
    Assertions.assertThat(result1).isTrue

    // Banned suggested event activity is not banned for codeless
    val result2 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_2", PerformanceGuardian.UseCase.CODELESS)
    Assertions.assertThat(result2).isFalse

    // Banned suggested event activity
    val result3 =
        PerformanceGuardian.isBannedActivity(
            "banned_activity_2", PerformanceGuardian.UseCase.SUGGESTED_EVENT)
    Assertions.assertThat(result3).isTrue
  }

  @Test
  fun testLimitProcessTime() {
    // Mock initialize is done
    PowerMockito.doNothing().`when`(PerformanceGuardian::class.java, "initializeIfNotYet")
    val mockPrefs: SharedPreferences = mock()
    Whitebox.setInternalState(PerformanceGuardian::class.java, "sharedPreferences", mockPrefs)
    val editor: SharedPreferences.Editor = mock()
    PowerMockito.`when`(mockPrefs.edit()).thenReturn(editor)
    PowerMockito.`when`(editor.putStringSet(any(), any())).thenReturn(editor)
    PowerMockito.`when`(editor.putString(any(), any())).thenReturn(editor)
    PowerMockito.mockStatic(Utility::class.java)
    PowerMockito.`when`(Utility.getAppVersion()).thenReturn("1.2.0")
    val mockCodelessActivityMap: MutableMap<String, Int> = HashMap()
    mockCodelessActivityMap["activity_1"] = 1
    mockCodelessActivityMap["activity_2"] = 2
    Whitebox.setInternalState(
        PerformanceGuardian::class.java, "activityProcessTimeMapCodeless", mockCodelessActivityMap)
    val mockSuggestedEventActivityMap: MutableMap<String, Int> = HashMap()
    mockSuggestedEventActivityMap["activity_3"] = 1
    Whitebox.setInternalState(
        PerformanceGuardian::class.java, "activityProcessTimeMapSe", mockSuggestedEventActivityMap)
    val bannedCodelessActivitySet =
        Whitebox.getInternalState<Set<String>>(
            PerformanceGuardian::class.java, "bannedCodelessActivitySet")
    val bannedSuggestedEventActivitySet =
        Whitebox.getInternalState<Set<String>>(
            PerformanceGuardian::class.java, "bannedSuggestedEventActivitySet")

    // Test codeless banned activity
    // Test activity not exceed max count
    PerformanceGuardian.limitProcessTime("activity_1", PerformanceGuardian.UseCase.CODELESS, 0, 100)
    Assertions.assertThat(bannedCodelessActivitySet.contains("activity_1")).isFalse
    Assertions.assertThat(mockCodelessActivityMap["activity_1"]).isEqualTo(2)

    // Test activity exceed max count
    PerformanceGuardian.limitProcessTime("activity_2", PerformanceGuardian.UseCase.CODELESS, 0, 100)
    Assertions.assertThat(bannedCodelessActivitySet.contains("activity_2")).isTrue
    Assertions.assertThat(mockCodelessActivityMap["activity_2"]).isEqualTo(3)

    // Test suggested event activity should not effect codeless
    PerformanceGuardian.limitProcessTime(
        "activity_1", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 100)
    Assertions.assertThat(mockSuggestedEventActivityMap["activity_1"]).isEqualTo(1)
    Assertions.assertThat(bannedSuggestedEventActivitySet.contains("activity_1")).isFalse
    Assertions.assertThat(mockCodelessActivityMap["activity_1"]).isEqualTo(2)
    Assertions.assertThat(bannedCodelessActivitySet.contains("activity_1")).isFalse

    // Test suggested event not exceed threshold
    PerformanceGuardian.limitProcessTime(
        "activity_3", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 10)
    Assertions.assertThat(mockSuggestedEventActivityMap["activity_3"]).isEqualTo(1)
    Assertions.assertThat(bannedSuggestedEventActivitySet.contains("activity_3")).isFalse

    // Test new suggested event exceed threshold
    PerformanceGuardian.limitProcessTime(
        "activity_4", PerformanceGuardian.UseCase.SUGGESTED_EVENT, 0, 100)
    Assertions.assertThat(mockSuggestedEventActivityMap["activity_4"]).isEqualTo(1)
    Assertions.assertThat(bannedSuggestedEventActivitySet.contains("activity_4")).isFalse
  }

  @Test
  fun testIsCacheValid() {
    val privateMethod =
        PerformanceGuardian::class.java.getDeclaredMethod("isCacheValid", String::class.java)
    privateMethod.isAccessible = true
    var result: Boolean

    // Current app version returns null and cached version is empty
    PowerMockito.mockStatic(Utility::class.java)
    PowerMockito.`when`(Utility.getAppVersion()).thenReturn(null)
    result = privateMethod.invoke(PerformanceGuardian, "") as Boolean
    Assertions.assertThat(result).isFalse

    // Current app version returns null while cached version returns value
    result = privateMethod.invoke(PerformanceGuardian, "1.2.0") as Boolean
    Assertions.assertThat(result).isFalse

    // Cached app version is empty while current version returns value
    PowerMockito.`when`(Utility.getAppVersion()).thenReturn("1.2.0")
    result = privateMethod.invoke(PerformanceGuardian, "") as Boolean
    Assertions.assertThat(result).isFalse

    // Current app version matches cached app version
    result = privateMethod.invoke(PerformanceGuardian, "1.2.0") as Boolean
    Assertions.assertThat(result).isTrue

    // Current app version does not match cached app version
    result = privateMethod.invoke(PerformanceGuardian, "1.0.0") as Boolean
    Assertions.assertThat(result).isFalse
  }
}
