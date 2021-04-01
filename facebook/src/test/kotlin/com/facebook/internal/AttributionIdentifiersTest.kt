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

package com.facebook.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class, Utility::class, AttributionIdentifiers::class, Looper::class)
class AttributionIdentifiersTest : FacebookPowerMockTestCase() {
  private lateinit var mockMainLooper: Looper
  private lateinit var mockId: AttributionIdentifiers
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(Looper::class.java)
    mockMainLooper = PowerMockito.mock(Looper::class.java)
    PowerMockito.`when`(Looper.getMainLooper()).thenReturn(mockMainLooper)
    PowerMockito.spy(AttributionIdentifiers::class.java)
    PowerMockito.spy(Utility::class.java)

    mockId = PowerMockito.mock(AttributionIdentifiers::class.java)
    mockContext = PowerMockito.mock(Context::class.java)
    mockPackageManager = PowerMockito.mock(PackageManager::class.java)
    PowerMockito.`when`(mockContext.packageManager).thenReturn(mockPackageManager)
  }

  @Test
  fun `test is tracking limited`() {
    PowerMockito.doReturn(mockId)
        .`when`(AttributionIdentifiers::class.java, "getAttributionIdentifiers", mockContext)
    PowerMockito.`when`(mockId.isTrackingLimited).thenReturn(true)
    Assert.assertTrue(AttributionIdentifiers.isTrackingLimited(mockContext))
    PowerMockito.`when`(mockId.isTrackingLimited).thenReturn(false)
    Assert.assertFalse(AttributionIdentifiers.isTrackingLimited(mockContext))
  }

  @Test
  fun `test get attribution id on main thread`() {
    PowerMockito.`when`(Looper.myLooper()).thenReturn(mockMainLooper)
    PowerMockito.whenNew(AttributionIdentifiers::class.java).withNoArguments().thenReturn(mockId)
    Assert.assertNull(AttributionIdentifiers.getAttributionIdentifiers(mockContext))
  }

  @Test
  fun `test return new identifier if not google play available`() {
    val newLooper = PowerMockito.mock(Looper::class.java)
    PowerMockito.`when`(Looper.myLooper()).thenReturn(newLooper)

    val id = PowerMockito.mock(AttributionIdentifiers::class.java)
    PowerMockito.whenNew(AttributionIdentifiers::class.java).withNoArguments().thenReturn(id)
    PowerMockito.`when`(Utility.getMethodQuietly(anyString(), anyString(), any())).thenReturn(null)
    val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    Assert.assertEquals(id, obtainedId)
    Assert.assertEquals(id, AttributionIdentifiers.getCachedIdentifiers())
  }
}
