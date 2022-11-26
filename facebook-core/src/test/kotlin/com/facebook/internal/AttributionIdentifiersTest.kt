/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import android.content.ContentResolver
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(
    FacebookSdk::class, Utility::class, Looper::class, FacebookSignatureValidator::class)
class AttributionIdentifiersTest : FacebookPowerMockTestCase() {
  private lateinit var mockMainLooper: Looper
  private lateinit var mockId: AttributionIdentifiers
  private lateinit var mockContext: Context
  private lateinit var mockPackageManager: PackageManager
  private lateinit var mockAttributionId: String

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getApplicationContext())
        .thenReturn(ApplicationProvider.getApplicationContext())
    PowerMockito.mockStatic(Looper::class.java)
    mockMainLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.getMainLooper()).thenReturn(mockMainLooper)
    PowerMockito.mockStatic(Utility::class.java)

    mockId = PowerMockito.mock(AttributionIdentifiers::class.java)
    mockContext = PowerMockito.mock(Context::class.java)
    mockPackageManager = PowerMockito.mock(PackageManager::class.java)
    whenever(mockContext.packageManager).thenReturn(mockPackageManager)

    // bypass all signature validation for unit test
    PowerMockito.mockStatic(FacebookSignatureValidator::class.java)
    whenever(FacebookSignatureValidator.validateSignature(anyOrNull(), anyOrNull()))
        .thenReturn(true)

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
    assertThat(AttributionIdentifiers.isTrackingLimited(mockContext)).isTrue
    whenever(mockId.isTrackingLimited).thenReturn(false)
    assertThat(AttributionIdentifiers.isTrackingLimited(mockContext)).isFalse
  }

  @Test
  fun `test get attribution id on main thread`() {
    whenever(Looper.myLooper()).thenReturn(mockMainLooper)
    assertThat(AttributionIdentifiers.getAttributionIdentifiers(mockContext)).isNull()
  }

  @Test
  fun `test return new identifier from attribution id provider if not google play available`() {
    val newLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.myLooper()).thenReturn(newLooper)
    whenever(Utility.getMethodQuietly(any<String>(), any<String>(), any())).thenReturn(null)
    setMockContextWithAttributionIdProvider()

    val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    assertThat(obtainedId?.attributionId).isEqualTo(mockAttributionId)
    assertThat(obtainedId).isEqualTo(AttributionIdentifiers.cachedIdentifiers)
  }

  @Test
  fun `test return new identifier from attribution id provider if bindService throws SecurityException`() {
    val newLooper = PowerMockito.mock(Looper::class.java)
    whenever(Looper.myLooper()).thenReturn(newLooper)
    whenever(mockContext.bindService(any(), any(), any())).thenThrow(SecurityException())
    whenever(Utility.getMethodQuietly(any<String>(), any<String>(), any())).thenReturn(null)
    setMockContextWithAttributionIdProvider()

    val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
    assertThat(obtainedId?.attributionId).isEqualTo(mockAttributionId)
    assertThat(obtainedId).isEqualTo(AttributionIdentifiers.cachedIdentifiers)
  }

  private fun setMockContextWithAttributionIdProvider() {
    mockAttributionId = "aid123456789"
    val mockAttributionIdProviderInfo = ProviderInfo()
    mockAttributionIdProviderInfo.packageName = "com.facebook.katana"
    val debugApplicationInfo = ApplicationInfo()
    debugApplicationInfo.flags = debugApplicationInfo.flags or ApplicationInfo.FLAG_DEBUGGABLE
    val mockContentResolver = mock<ContentResolver>()
    val mockCursor = mock<android.database.Cursor>()
    whenever(mockCursor.getColumnIndex(any())).thenReturn(0)
    whenever(mockCursor.getString(0)).thenReturn(mockAttributionId)
    whenever(mockCursor.moveToFirst()).thenReturn(true)
    whenever(mockContext.applicationInfo).thenReturn(debugApplicationInfo)
    whenever(
            mockPackageManager.resolveContentProvider(
                AttributionIdentifiers.Companion.ATTRIBUTION_ID_CONTENT_PROVIDER, 0))
        .thenReturn(mockAttributionIdProviderInfo)
    whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
    whenever(mockContentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
        .thenReturn(mockCursor)
  }
}
