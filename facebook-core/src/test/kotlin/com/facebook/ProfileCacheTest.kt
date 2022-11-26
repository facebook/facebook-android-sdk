/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.facebook.util.common.ProfileTestHelper.assertDefaultObjectGetters
import com.facebook.util.common.ProfileTestHelper.assertMostlyNullsObjectGetters
import com.facebook.util.common.ProfileTestHelper.createDefaultProfile
import com.facebook.util.common.ProfileTestHelper.createMostlyNullsProfile
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
