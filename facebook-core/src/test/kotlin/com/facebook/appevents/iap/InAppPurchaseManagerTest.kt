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
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.internal.Constants
import com.facebook.internal.FeatureManager
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging(eq(InAppPurchaseUtils.BillingClientVersion.V1))).thenAnswer {
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
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging(eq(InAppPurchaseUtils.BillingClientVersion.NONE))).thenAnswer {
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
        whenever(InAppPurchaseActivityLifecycleTracker.startIapLogging(InAppPurchaseUtils.BillingClientVersion.V2_V4)).thenAnswer {
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
    fun `test start iap logging when billing library is v5_v7 and feature is enabled`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib5To7)).thenReturn(true)
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
                eq(InAppPurchaseUtils.BillingClientVersion.V5_V7)
            )
        ).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isTrue
    }

    @Test
    fun `test start iap logging when billing library is v5_v7 and feature is disabled`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.IapLoggingLib5To7)).thenReturn(
            false
        )
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
                eq(InAppPurchaseUtils.BillingClientVersion.V5_V7)
            )
        ).thenAnswer {
            isStartIapLoggingCalled = true
            Unit
        }
        InAppPurchaseManager.enableAutoLogging()
        assertThat(isStartIapLoggingCalled).isFalse
    }

    @Test
    fun testIsDuplicate() {
        val params = Bundle()
        params.putCharSequence(Constants.IAP_PRODUCT_TYPE, "inapp")
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time = System.currentTimeMillis()
        assertEquals(false, InAppPurchaseManager.isDuplicate(purchase, time, true))
        AppEventsConstants.EVENT_NAME_PURCHASED
        val purchaseWithDifferentCurrency =
            InAppPurchase(
                AppEventsConstants.EVENT_NAME_PURCHASED,
                10.0,
                Currency.getInstance(Locale.UK)
            )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(purchaseWithDifferentCurrency, time, false)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(purchase, time + 60001, false)
        )
        assertEquals(
            true,
            InAppPurchaseManager.isDuplicate(purchase, time + 120000, true)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(purchase, time + 120000, false)
        )

        val oneDollarPurchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        assertEquals(false, InAppPurchaseManager.isDuplicate(oneDollarPurchase, 0, false))
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(oneDollarPurchase, 60000, false)
        )
        assertEquals(
            true,
            InAppPurchaseManager.isDuplicate(oneDollarPurchase, 60000, true)
        )
        assertEquals(
            true,
            InAppPurchaseManager.isDuplicate(oneDollarPurchase, 120000, true)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(oneDollarPurchase, 60000, true)
        )

        params.putCharSequence(Constants.IAP_PRODUCT_TYPE, "subs")
        val oneDollarSubscription = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(oneDollarSubscription, 60000, false)
        )
        assertEquals(
            true,
            InAppPurchaseManager.isDuplicate(oneDollarSubscription, 60000, true)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(oneDollarSubscription, 60000, false)
        )
        val oneDollarStartTrial = InAppPurchase(
            AppEventsConstants.EVENT_NAME_START_TRIAL,
            1.0,
            Currency.getInstance(Locale.US)
        )
        assertEquals(
            false,
            InAppPurchaseManager.isDuplicate(oneDollarStartTrial, 60000, true)
        )
    }

    @Test
    fun testIsDuplicate_ConcurrentCalls() {
        val params = Bundle()
        params.putCharSequence(Constants.IAP_PRODUCT_TYPE, "inapp")
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 100
        var result1: Boolean? = null
        var result2: Boolean? = null
        val thread1 = Thread {
            result1 = InAppPurchaseManager.isDuplicate(purchase, time1, true)
        }
        val thread2 = Thread {
            result2 = InAppPurchaseManager.isDuplicate(purchase, time2, false)
        }
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()
        var numDuplicates = 0
        if (result1 == true) {
            numDuplicates++
        }
        if (result2 == true) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)

        // Try again with scheduled executors, more similar to real IAP implementation
        val latch = CountDownLatch(2)
        val executor1 = Executors.newSingleThreadExecutor()
        val executor2 = Executors.newSingleThreadExecutor()
        val time3 = time1 + 100000000000
        val time4 = time1 + 100000000001
        var result3: Boolean? = null
        var result4: Boolean? = null
        executor1.execute {
            result3 =
                InAppPurchaseManager.isDuplicate(purchase, time3, true);
            latch.countDown()
        }
        executor2.execute {
            result4 = InAppPurchaseManager.isDuplicate(purchase, time4, false);
            latch.countDown()
        }
        latch.await()
        numDuplicates = 0
        if (result3 == true) {
            numDuplicates++
        }
        if (result4 == true) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)
    }
}
