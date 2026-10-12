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
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.internal.WhiteboxImpl

@PrepareForTest(
    FacebookSdk::class,
    Utility::class,
    Looper::class,
    FacebookSignatureValidator::class,
    FeatureManager::class
)
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
        PowerMockito.mockStatic(FeatureManager::class.java)

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

    @After
    fun tearDown() {
        AttributionIdentifiers.serviceBindTimeoutMillis = 10_000L
    }

    @Test
    fun `test is tracking limited`() {
        val mockAttributionIdentifierCompanion =
            PowerMockito.mock(AttributionIdentifiers.Companion::class.java)
        WhiteboxImpl.setInternalState(
            AttributionIdentifiers::class.java, "Companion", mockAttributionIdentifierCompanion
        )
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
    fun `test main thread returns before reading the advertising id`() {
        whenever(Looper.myLooper()).thenReturn(mockMainLooper)
        stubGooglePlayServicesAvailable()

        assertThat(AttributionIdentifiers.getAttributionIdentifiers(mockContext)).isNull()
        verify(mockContext, never()).bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
    }

    @Test
    fun `test advertising id service that never connects times out instead of blocking`() {
        whenever(Looper.myLooper()).thenReturn(PowerMockito.mock(Looper::class.java))
        stubGooglePlayServicesAvailable()
        whenever(mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()))
            .thenReturn(true)
        AttributionIdentifiers.serviceBindTimeoutMillis = 200L

        val start = System.currentTimeMillis()
        val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
        val elapsed = System.currentTimeMillis() - start

        assertThat(obtainedId).isNotNull
        assertThat(elapsed).isBetween(200L, 5_000L)
        verify(mockContext).unbindService(any())
    }

    @Test
    fun `test without the feature every lookup reads again while the cache is fresh`() {
        whenever(Looper.myLooper()).thenReturn(PowerMockito.mock(Looper::class.java))
        stubGooglePlayServicesAvailable()

        AttributionIdentifiers.getAttributionIdentifiers(mockContext)
        AttributionIdentifiers.getAttributionIdentifiers(mockContext)

        verify(mockContext, times(2))
            .bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
    }

    @Test
    fun `test with the feature a fresh cache is returned without reading again`() {
        whenever(Looper.myLooper()).thenReturn(PowerMockito.mock(Looper::class.java))
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidAdvertisingIDReliability))
            .thenReturn(true)
        stubGooglePlayServicesAvailable()

        val first = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
        val second = AttributionIdentifiers.getAttributionIdentifiers(mockContext)

        assertThat(second).isSameAs(first)
        verify(mockContext, times(1))
            .bindService(any<Intent>(), any<ServiceConnection>(), any<Int>())
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
        whenever(
            mockContext.bindService(
                any<Intent>(),
                any<ServiceConnection>(),
                any<Int>()
            )
        ).thenThrow(SecurityException())
        whenever(Utility.getMethodQuietly(any<String>(), any<String>(), any())).thenReturn(null)
        setMockContextWithAttributionIdProvider()

        val obtainedId = AttributionIdentifiers.getAttributionIdentifiers(mockContext)
        assertThat(obtainedId?.attributionId).isEqualTo(mockAttributionId)
        assertThat(obtainedId).isEqualTo(AttributionIdentifiers.cachedIdentifiers)
    }

    private fun stubGooglePlayServicesAvailable() {
        val availabilityMethod =
            FakeGooglePlayServicesUtil::class.java.getMethod(
                "isGooglePlayServicesAvailable", Context::class.java
            )
        whenever(
            Utility.getMethodQuietly(
                eq("com.google.android.gms.common.GooglePlayServicesUtil"),
                eq("isGooglePlayServicesAvailable"),
                any()
            )
        )
            .thenReturn(availabilityMethod)
        whenever(Utility.invokeMethodQuietly(isNull(), eq(availabilityMethod), any())).thenReturn(0)
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
                AttributionIdentifiers.Companion.ATTRIBUTION_ID_CONTENT_PROVIDER, 0
            )
        )
            .thenReturn(mockAttributionIdProviderInfo)
        whenever(mockContext.contentResolver).thenReturn(mockContentResolver)
        whenever(mockContentResolver.query(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockCursor)
    }

    object FakeGooglePlayServicesUtil {
        @JvmStatic fun isGooglePlayServicesAvailable(context: Context): Int = 0
    }
}
