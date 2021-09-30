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
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class ProfileCacheTest : FacebookPowerMockTestCase() {
  @Before
  fun before() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.getApplicationId()).thenReturn("123456789")
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    val context = ApplicationProvider.getApplicationContext<Context>()
    context
        .getSharedPreferences(ProfileCache.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        .edit()
        .clear()
        .commit()
    whenever(FacebookSdk.getApplicationContext()).thenReturn(context)
  }

  @Test
  fun testEmptyCase() {
    val cache = ProfileCache()
    Assert.assertNull(cache.load())
  }

  @Test
  fun testSaveGetAndClear() {
    val cache = ProfileCache()
    var profile1 = createDefaultProfile()
    cache.save(profile1)
    var profile2 = cache.load()
    assertDefaultObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)
    profile1 = createMostlyNullsProfile()
    cache.save(profile1)
    profile2 = cache.load()
    assertMostlyNullsObjectGetters(profile2)
    Assert.assertEquals(profile1, profile2)
    cache.clear()
    Assert.assertNull(cache.load())
  }
}
