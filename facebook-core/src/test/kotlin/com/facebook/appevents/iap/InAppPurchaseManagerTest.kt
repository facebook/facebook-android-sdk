/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.iap

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.internal.FeatureManager
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(
    FacebookSdk::class,
    FeatureManager::class,
    InAppPurchaseActivityLifecycleTracker::class,
    InAppPurchaseAutoLogger::class,
    InAppPurchaseManager::class
)
class InAppPurchaseManagerTest : FacebookPowerMockTestCase() {
    private lateinit var mockContext: Context
    override fun setup() {
        super.setup()
        mockContext = mock()
        PowerMockito.mockStatic(FeatureManager::class.java)
        PowerMockito.mockStatic(InAppPurchaseActivityLifecycleTracker::class.java)
        PowerMockito.mockStatic(InAppPurchaseAutoLogger::class.java)
        PowerMockito.mockStatic(FeatureManager::class.java)
        PowerMockito.mockStatic(FacebookSdk::class.java)
        Whitebox.setInternalState(InAppPurchaseManager::class.java, "enabled", AtomicBoolean(false))
        whenever(FacebookSdk.getApplicationContext()).thenReturn(mockContext)
    }

    @Test
    fun `test start iap logging when billing lib 2+ is not available`() {
        MemberModifier.stub<InAppPurchaseUtils.BillingClientVersion>(
            PowerMockito.method(InAppPurchaseManager::class.java, "getBillingClientVersion")
        )
            .toReturn(InAppPurchaseUtils.BillingClientVersion.V1)
        var isStartIapLoggingCalled = false
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isTrue
    }

    @Test
    fun `test start iap logging when cant find dependency`() {
        MemberModifier.stub<InAppPurchaseUtils.BillingClientVersion>(
            PowerMockito.method(InAppPurchaseManager::class.java, "getBillingClientVersion")
        )
            .toReturn(InAppPurchaseUtils.BillingClientVersion.NONE)
        var isStartIapLoggingCalled = false
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isFalse
    }

    @Test
    fun `test start iap logging when billing lib 2+ is available but feature is off`() {
        MemberModifier.stub<InAppPurchaseUtils.BillingClientVersion>(
            PowerMockito.method(InAppPurchaseManager::class.java, "getBillingClientVersion")
        )
            .toReturn(InAppPurchaseUtils.BillingClientVersion.V2_V4)
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2)).thenReturn(false)
        var isStartIapLoggingCalled = false
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging()).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isTrue
    }

    @Test
    fun `test start iap logging when billing lib 2+ is available and feature is on`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib2)).thenReturn(true)
        val mockPackageManager: PackageManager = mock()
        val mockApplicationInfo = ApplicationInfo()
        val metaData = Bundle()
        metaData.putString("com.google.android.play.billingclient.version", "2.0.3")
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn("com.facebook.test")
        whenever(
            mockPackageManager.getApplicationInfo(
                any(),
                any()
            )
        ).thenReturn(mockApplicationInfo)
        mockApplicationInfo.metaData = metaData

        var isStartIapLoggingCalled = false
        whenever(
            InAppPurchaseAutoLogger.startIapLogging(
                any(),
                eq(InAppPurchaseUtils.BillingClientVersion.V2_V4)
            )
        ).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isTrue
    }

    @Test
    fun `test start iap logging when billing library is v5_`() {
        val mockPackageManager: PackageManager = mock()
        val mockApplicationInfo = ApplicationInfo()
        val metaData = Bundle()
        metaData.putString("com.google.android.play.billingclient.version", "5.0.3")
        whenever(mockContext.packageManager).thenReturn(mockPackageManager)
        whenever(mockContext.packageName).thenReturn("com.facebook.test")
        whenever(
            mockPackageManager.getApplicationInfo(
                any(),
                any()
            )
        ).thenReturn(mockApplicationInfo)
        mockApplicationInfo.metaData = metaData

        var isStartIapLoggingCalled = false
        whenever(
            InAppPurchaseAutoLogger.startIapLogging(
                any(),
                eq(InAppPurchaseUtils.BillingClientVersion.V5_Plus)
            )
        ).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isTrue

    }
}
