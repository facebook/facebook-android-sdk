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
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(FacebookSdk::class, Utility::class, AttributionIdentifiers::class, Looper::class)
class AttributionIdentifiersTest : FacebookPowerMockTestCase() {
  private lateinit var mockMainLooper: Looper
  private lateinit var mockId: AttributionIdentifiers
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(Looper::class.java)
    mockMainLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.getMainLooper()).thenReturn(mockMainLooper)
    PowerMockito.spy(AttributionIdentifiers::class.java)
    PowerMockito.spy(Utility::class.java)

    mockId = PowerMockito.mock(AttributionIdentifiers::class.java)
    mockContext = PowerMockito.mock(Context::class.java)
    mockPackageManager = PowerMockito.mock(PackageManager::class.java)
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)
    AttributionIdentifiers.cachedIdentifiers = null
  }

  @Test
  fun `test is tracking limited`() {
    val mockAttributionIdentifierCompanion =
        PowerMockito.mock(AttributionIdentifiers.Companion::class.java)
    WhiteboxImpl.setInternalState(
        AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifierCompanion)
    whenever(mockAttributionIdentifierCompanion.isTrackingLimited(any())).thenCallRealMethod()
    whenever(mockAttributionIdentifierCompanion.getAttributionIdentifiers(mockContext))
        .thenReturn(mockId)

    whenever(mockId.isTrackingLimited).thenReturn(true)
    Assert.assertTrue(AttributionIdentifiers.isTrackingLimited(mockContext))
    whenever(mockId.isTrackingLimited).thenReturn(false)
    Assert.assertFalse(AttributionIdentifiers.isTrackingLimited(mockContext))
  }

  @Test
  fun `test get attribution id on main thread`() {
    whenever(Looper.myLooper()).thenReturn(mockMainLooper)
    PowerMockito.whenNew(AttributionIdentifiers::class.java).withNoArguments().thenReturn(mockId)
    Assert.assertNull(AttributionIdentifiers.getAttributionIdentifiers(mockContext))
  }

  @Test
  fun `test return new identifier if not google play available`() {
    val newLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.myLooper()).thenReturn(newLooper)

    PowerMockito.whenNew(AttributionIdentifiers::class.java).withNoArguments().thenReturn(mockId)
    whenever(Utility.getMethodQuietly(anyString(), anyString(), any())).thenReturn(null)
    val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    Assert.assertEquals(mockId, obtainedId)
    Assert.assertEquals(mockId, AttributionIdentifiers.cachedIdentifiers)
  }

  @Test
  fun `test return new identifier if bindService throws SecurityException`() {
    val newLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.myLooper()).thenReturn(newLooper)

    whenever(mockContext.bindService(any(), any(), any())).thenThrow(SecurityException())
    whenever(Utility.getMethodQuietly(anyString(), anyString(), any())).thenReturn(null)
    PowerMockito.whenNew(AttributionIdentifiers::class.java).withNoArguments().thenReturn(mockId)
    val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    Assert.assertEquals(mockId, obtainedId)
    Assert.assertEquals(mockId, AttributionIdentifiers.cachedIdentifiers)
  }
}
