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
import androidx.core.os.bundleOf
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.OperationalData
import com.facebook.appevents.OperationalDataEnum
import com.facebook.appevents.internal.Constants
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.facebook.internal.SmartLoginOption
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.support.membermodification.MemberModifier
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@PrepareForTest(
    FacebookSdk::class,
    FeatureManager::class,
    InAppPurchaseActivityLifecycleTracker::class,
    InAppPurchaseAutoLogger::class,
    InAppPurchaseManager::class,
    FetchedAppSettingsManager::class
)
class InAppPurchaseManagerTest : FacebookPowerMockTestCase() {
    @Mock
    private lateinit var mockFacebookRequestErrorClassification: FacebookRequestErrorClassification
    private lateinit var mockContext: Context
    private lateinit var testDedupeParameters: List<Pair<String, List<String>>>
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
        testDedupeParameters = listOf(
            Pair(
                "other_dedup_key",
                listOf("other_dedup_key", "other_dedup_key1")
            ),
        )
        val mockFetchedAppSettings = FetchedAppSettings(
            false,
            "",
            false,
            1,
            SmartLoginOption.parseOptions(0),
            emptyMap(),
            false,
            mockFacebookRequestErrorClassification,
            "",
            "",
            false,
            codelessEventsEnabled = false,
            eventBindings = null,
            sdkUpdateMessage = "",
            trackUninstallEnabled = false,
            monitorViaDialogEnabled = false,
            rawAamRules = "",
            suggestedEventsSetting = "",
            restrictiveDataSetting = "",
            protectedModeStandardParamsSetting = null,
            MACARuleMatchingSetting = null,
            migratedAutoLogValues = null,
            blocklistEvents = null,
            redactedEvents = null,
            sensitiveParams = null,
            schemaRestrictions = null,
            bannedParams = null,
            currencyDedupeParameters = null,
            purchaseValueDedupeParameters = null,
            prodDedupeParameters = null,
            testDedupeParameters = testDedupeParameters,
            dedupeWindow = 60000L,
        )
        PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
        whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(anyOrNull())).thenReturn(
            mockFetchedAppSettings
        )
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "enabled",
            AtomicBoolean(true)
        )
    }

    @Before
    fun removePurchaseHistory() {
        val emptyImplicitMap =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
        val emptyManualMap =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfManualPurchases",
            emptyManualMap
        )

        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            emptyImplicitMap
        )

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
            null
        }
        InAppPurchaseManager.startTracking()
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
            null
        }
        InAppPurchaseManager.startTracking()
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
            null
        }
        InAppPurchaseManager.startTracking()
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
                any<String>(),
                any<Int>()
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
            null
        }
        InAppPurchaseManager.startTracking()
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
                any<String>(),
                any<Int>()
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
            null
        }
        InAppPurchaseManager.startTracking()
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
                any<String>(),
                any<Int>()
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
            null
        }
        InAppPurchaseManager.startTracking()
        assertThat(isStartIapLoggingCalled).isFalse
    }

    @Test
    fun testPerformDedupe() {
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time = System.currentTimeMillis()
        val bundle = Bundle()
        bundle.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        bundle.putCharSequence("other_dedup_key", "val")
        var result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time,
                true,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()
        val purchaseWithDifferentCurrency =
            InAppPurchase(
                AppEventsConstants.EVENT_NAME_PURCHASED,
                10.0,
                Currency.getInstance(Locale.UK)
            )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchaseWithDifferentCurrency),
                time,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 60001,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 120000,
                true,
                listOf(Pair(bundle, null))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 120000,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()


        val oneDollarPurchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                0,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                true,
                listOf(Pair(bundle, null))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                120000,
                true,
                listOf(Pair(bundle, null))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                true,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()


        val oneDollarSubscription = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                true,
                listOf(Pair(bundle, null))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                false,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()

        val oneDollarStartTrial = InAppPurchase(
            AppEventsConstants.EVENT_NAME_START_TRIAL,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                60000,
                true,
                listOf(Pair(bundle, null))
            )
        assertThat(result).isNull()


        val bundleWithImplicitlyLoggedTestParameter = bundleOf(Pair("other_dedup_key", "value"))
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                100000,
                true,
                listOf(Pair(bundleWithImplicitlyLoggedTestParameter, null))
            )
        assertThat(result).isNull()

        val bundleWithManuallyLoggedTestParameter = bundleOf(Pair("other_dedup_key1", "value"))
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                100000,
                false,
                listOf(Pair(bundleWithManuallyLoggedTestParameter, null))
            )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key1"
        )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), null)
    }

    @Test
    fun testPerformDedupeWithOperationalParameters() {
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time = System.currentTimeMillis()
        val operationalData = OperationalData()
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            Constants.IAP_PRODUCT_ID,
            "productID"
        )
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            "other_dedup_key",
            "val"
        )
        var result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time,
                true,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()
        val purchaseWithDifferentCurrency =
            InAppPurchase(
                AppEventsConstants.EVENT_NAME_PURCHASED,
                10.0,
                Currency.getInstance(Locale.UK)
            )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchaseWithDifferentCurrency),
                time,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 60001,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 120000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time + 120000,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()


        val oneDollarPurchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                0,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                120000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarPurchase),
                60000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()


        val oneDollarSubscription = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            result?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key"
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarSubscription),
                60000,
                false,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()

        val oneDollarStartTrial = InAppPurchase(
            AppEventsConstants.EVENT_NAME_START_TRIAL,
            1.0,
            Currency.getInstance(Locale.US)
        )
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                60000,
                true,
                listOf(Pair(null, operationalData))
            )
        assertThat(result).isNull()


        val bundleWithImplicitlyLoggedTestParameter = bundleOf(Pair("other_dedup_key", "value"))
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                100000,
                true,
                listOf(Pair(bundleWithImplicitlyLoggedTestParameter, null))
            )
        assertThat(result).isNull()

        val bundleWithManuallyLoggedTestParameter = bundleOf(Pair("other_dedup_key1", "value"))
        result =
            InAppPurchaseManager.performDedupe(
                listOf(oneDollarStartTrial),
                100000,
                false,
                listOf(Pair(bundleWithManuallyLoggedTestParameter, null))
            )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_RESULT,
            ), "1"
        )
        assertEquals(
            result?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "other_dedup_key1"
        )
        assertEquals(result?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), null)
    }

    @Test
    fun testIsDuplicate_ConcurrentCalls() {
        val bundle = Bundle()
        bundle.putCharSequence(Constants.IAP_PRODUCT_TITLE, "productTitle")
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 100
        var result1: Bundle? = null
        var result2: Bundle? = null
        val thread1 = Thread {
            result1 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time1,
                true,
                listOf(Pair(bundle, null))
            )
        }
        val thread2 = Thread {
            result2 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time2,
                false,
                listOf(Pair(bundle, null))
            )
        }
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()
        var numDuplicates = 0
        if (result1 != null) {
            numDuplicates++
        }
        if (result2 != null) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)

        // Try again with scheduled executors, more similar to real IAP implementation
        val latch = CountDownLatch(2)
        val executor1 = Executors.newSingleThreadExecutor()
        val executor2 = Executors.newSingleThreadExecutor()
        val time3 = time1 + 100000000000
        val time4 = time1 + 100000000001
        var result3: Bundle? = null
        var result4: Bundle? = null
        executor1.execute {
            result3 =
                InAppPurchaseManager.performDedupe(
                    listOf(purchase),
                    time3,
                    true,
                    listOf(Pair(bundle, null))
                )
            latch.countDown()
        }
        executor2.execute {
            result4 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time4,
                false,
                listOf(Pair(bundle, null))
            )
            latch.countDown()
        }
        latch.await()
        numDuplicates = 0
        if (result3 != null) {
            numDuplicates++
        }
        if (result4 != null) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)
    }

    @Test
    fun testIsDuplicate_ConcurrentCallsWithOperationalData() {
        val operationalData = OperationalData()
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            Constants.IAP_PRODUCT_TITLE,
            "productTitle"
        )
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            10.0,
            Currency.getInstance(Locale.US)
        )
        val time1 = System.currentTimeMillis()
        val time2 = time1 + 100
        var result1: Bundle? = null
        var result2: Bundle? = null
        val thread1 = Thread {
            result1 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time1,
                true,
                listOf(Pair(null, operationalData))
            )
        }
        val thread2 = Thread {
            result2 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time2,
                false,
                listOf(Pair(null, operationalData))
            )
        }
        thread1.start()
        thread2.start()
        thread1.join()
        thread2.join()
        var numDuplicates = 0
        if (result1 != null) {
            numDuplicates++
        }
        if (result2 != null) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)

        // Try again with scheduled executors, more similar to real IAP implementation
        val latch = CountDownLatch(2)
        val executor1 = Executors.newSingleThreadExecutor()
        val executor2 = Executors.newSingleThreadExecutor()
        val time3 = time1 + 100000000000
        val time4 = time1 + 100000000001
        var result3: Bundle? = null
        var result4: Bundle? = null
        executor1.execute {
            result3 =
                InAppPurchaseManager.performDedupe(
                    listOf(purchase),
                    time3,
                    true,
                    listOf(Pair(null, operationalData))
                )
            latch.countDown()
        }
        executor2.execute {
            result4 = InAppPurchaseManager.performDedupe(
                listOf(purchase),
                time4,
                false,
                listOf(Pair(null, operationalData))
            )
            latch.countDown()
        }
        latch.await()
        numDuplicates = 0
        if (result3 != null) {
            numDuplicates++
        }
        if (result4 != null) {
            numDuplicates++
        }
        assertEquals(numDuplicates, 1)
    }

    @Test
    fun testGetDedupeParameter() {
        val newParams = Bundle()
        val oldParams = Bundle()
        newParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CONTENT_ID, "productID")
        oldParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CONTENT_ID, "productID")
        var result =
            InAppPurchaseManager.getDedupeParameter(
                newParams,
                null,
                oldParams,
                null,
                false
            )
        assertThat(result).isNull()


        newParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CONTENT_ID, "prductID")
        oldParams.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        result =
            InAppPurchaseManager.getDedupeParameter(
                newParams,
                null,
                oldParams,
                null,
                true
            )
        assertThat(result).isNull()
        newParams.clear()
        oldParams.clear()

        newParams.putCharSequence(Constants.IAP_PRODUCT_DESCRIPTION, "description ")
        oldParams.putCharSequence(Constants.IAP_PRODUCT_DESCRIPTION, "description ")
        result =
            InAppPurchaseManager.getDedupeParameter(
                newParams,
                null,
                oldParams,
                null,
                true
            )
        assertThat(result).isNotNull()
        newParams.clear()
        oldParams.clear()

        newParams.putCharSequence(Constants.IAP_PRODUCT_TITLE, "title ")
        oldParams.putCharSequence(Constants.IAP_PRODUCT_TITLE, "title ")
        result =
            InAppPurchaseManager.getDedupeParameter(
                newParams,
                null,
                oldParams,
                null,
                true
            )
        assertThat(result).isNotNull()
        newParams.clear()
        oldParams.clear()
        newParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token ")
        oldParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token ")
        result =
            InAppPurchaseManager.getDedupeParameter(
                newParams,
                null,
                oldParams,
                null,
                true
            )
        assertThat(result).isNotNull()
    }
}
