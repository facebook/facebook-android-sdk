/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.internal

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.appevents.AppEventsConstants
import com.facebook.appevents.AppEventsLogger
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.appevents.iap.InAppPurchase
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.iap.InAppPurchaseUtils
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import org.assertj.core.api.Assertions
import java.math.BigDecimal
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.spy
import org.powermock.api.mockito.PowerMockito.verifyNew
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

@PrepareForTest(
    Log::class,
    FacebookSdk::class,
    InternalAppEventsLogger::class,
    FetchedAppSettings::class,
    FetchedAppSettingsManager::class,
    AutomaticAnalyticsLogger::class,
    FetchedAppGateKeepersManager::class,
    InAppPurchaseManager::class,
    FeatureManager::class
)
class AutomaticAnalyticsLoggerTest : FacebookPowerMockTestCase() {

    private val appID = "123"
    private val activityName = "activity name"
    private val timeSpent = 5L
    private val subscriptionPurchase =
        "{\"productId\":\"id123\", \"purchaseTime\":\"12345\", \"purchaseToken\": \"token123\", \"packageName\": \"examplePackageName\", \"autoRenewing\": true}"
    private val oneTimePurchase =
        "{\"productId\":\"id123\", \"purchaseTime\":\"12345\", \"purchaseToken\": \"token123\", \"packageName\": \"examplePackageName\"}"
    private val oneTimePurchaseDetailsGPBLV2V4 =
        "{\"productId\":\"id123\",\"type\":\"inapp\",\"title\":\"ExampleTitle\",\"name\":\"ExampleName\",\"iconUrl\":\"exampleIconUrl\",\"description\":\"Exampledescription.\",\"price\":\"$12.00\",\"price_amount_micros\":12000000,\"price_currency_code\":\"USD\",\"skuDetailsToken\":\"sampleToken\"}"
    private val oneTimePurchaseDetailsGPBLV5V7 =
        "{\"productId\":\"id123\",\"type\":\"inapp\",\"title\":\"ExampleTitle\",\"name\":\"ExampleName\",\"description\":\"Exampledescription.\",\"localizedIn\":[\"en-US\"],\"skuDetailsToken\":\"detailsToken=\",\"oneTimePurchaseOfferDetails\":{\"priceAmountMicros\":12000000,\"priceCurrencyCode\":\"USD\",\"formattedPrice\":\"$12.00\",\"offerIdToken\":\"offerIdToken==\"}}"
    private val subscriptionDetailsGPBLV2V4 =
        "{\"productId\":\"id123\",\"type\":\"subs\",\"title\":\"ExampleTitle\",\"name\":\"ExampleName\",\"iconUrl\":\"exampleIconUrl\",\"price\":\"$3.99\",\"price_amount_micros\":3990000,\"price_currency_code\":\"USD\",\"skuDetailsToken\":\"exampleDetailsToken=\",\"subscriptionPeriod\":\"P1W\",\"freeTrialPeriod\":\"P1W\",\"introductoryPriceAmountMicros\":3590000,\"introductoryPricePeriod\":\"P1W\",\"introductoryPrice\":\"$3.59\", \"introductoryPricePeriod\": \"P1W\"}"
    private val subscriptionDetailsWithNoFreeTrialGPBLV2V4 =
        "{\"productId\":\"id123\",\"type\":\"subs\",\"title\":\"ExampleTitle\",\"name\":\"ExampleName\",\"iconUrl\":\"exampleIconUrl\",\"price\":\"$3.99\",\"price_amount_micros\":3990000,\"price_currency_code\":\"USD\",\"skuDetailsToken\":\"exampleDetailsToken=\",\"subscriptionPeriod\":\"P1W\",\"introductoryPriceAmountMicros\":3590000,\"introductoryPricePeriod\":\"P1W\",\"introductoryPrice\":\"$3.59\", \"introductoryPricePeriod\": \"P1W\"}"
    private val subscriptionDetailsGPBLV5V7 =
        "{\"productId\":\"id123\",\"type\":\"subs\",\"title\":\"ExampleTitle\",\"name\":\"ExampleName\",\"description\":\"Exampledescription.\",\"localizedIn\":[\"en-US\"],\"skuDetailsToken\":\"detailsToken\",\"subscriptionOfferDetails\":[{\"offerIdToken\":\"offerIdToken1=\",\"basePlanId\":\"baseplanId\",\"offerId\":\"offerId\",\"pricingPhases\":[{\"priceAmountMicros\":0,\"priceCurrencyCode\":\"USD\",\"formattedPrice\":\"Free\",\"billingPeriod\":\"P1W\",\"recurrenceMode\":2,\"billingCycleCount\":1},{\"priceAmountMicros\":3590000,\"priceCurrencyCode\":\"USD\",\"formattedPrice\":\"$3.59\",\"billingPeriod\":\"P1W\",\"recurrenceMode\":2,\"billingCycleCount\":2},{\"priceAmountMicros\":3990000,\"priceCurrencyCode\":\"USD\",\"formattedPrice\":\"$3.99\",\"billingPeriod\":\"P1W\",\"recurrenceMode\":1}],\"offerTags\":[]},{\"offerIdToken\":\"offerIdToken2=\",\"basePlanId\":\"basePlanId2\",\"pricingPhases\":[{\"priceAmountMicros\":3990000,\"priceCurrencyCode\":\"USD\",\"formattedPrice\":\"$3.99\",\"billingPeriod\":\"P1W\",\"recurrenceMode\":1}],\"offerTags\":[]}]}"
    private var logWarningCallCount = 0
    private var appEventLoggerCallCount = 0

    private lateinit var context: Context
    private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
    private lateinit var mockBundle: Bundle
    private lateinit var mockFetchedAppSettings: FetchedAppSettings
    private var eventName: String? = null
    private var bundle: Bundle? = null
    private var amount: BigDecimal? = null
    private var currency: Currency? = null

    @Before
    fun init() {
        spy(InAppPurchaseManager::class.java)
        mockStatic(FacebookSdk::class.java)
        mockStatic(Log::class.java)
        mockStatic(FetchedAppSettingsManager::class.java)
        mockStatic(FetchedAppGateKeepersManager::class.java)
        mockStatic(FeatureManager::class.java)

        context = Robolectric.buildActivity(Activity::class.java).get()
        whenever(FacebookSdk.getApplicationContext()).thenReturn(context)
        whenever(FacebookSdk.isInitialized()).thenReturn(true)
        whenever(FacebookSdk.getApplicationId()).thenReturn(appID)
        whenever(InAppPurchaseManager.getSpecificBillingLibraryVersion()).thenReturn("GPBL.5.1.0")
        mockInternalAppEventsLogger = mock(InternalAppEventsLogger::class.java)
        whenNew(InternalAppEventsLogger::class.java)
            .withAnyArguments()
            .thenReturn(mockInternalAppEventsLogger)
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(false)

        Whitebox.setInternalState(
            AutomaticAnalyticsLogger::class.java,
            "internalAppEventsLogger",
            mockInternalAppEventsLogger
        )

        mockBundle = Bundle(1)
        whenNew(Bundle::class.java).withAnyArguments().thenReturn(mockBundle)

        mockFetchedAppSettings = mock(FetchedAppSettings::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(appID, false))
            .thenReturn(mockFetchedAppSettings)
        whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(appID))
            .thenReturn(mockFetchedAppSettings)

        whenever(Log.w(any(), any<String>())).then { logWarningCallCount++ }
        whenever(mockFetchedAppSettings.iAPAutomaticLoggingEnabled).thenReturn(true)

        val mockManager = mock(FetchedAppGateKeepersManager::class.java)
        Whitebox.setInternalState(FetchedAppGateKeepersManager::class.java, "INSTANCE", mockManager)

        whenever(mockManager.getGateKeeperForKey(any<String>(), any<String>(), any<Boolean>()))
            .thenReturn(true)


        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
        whenever(
            mockInternalAppEventsLogger.logEventImplicitly(
                any(),
                any(),
                any(),
                any()
            )
        ).thenAnswer {
            eventName = it.getArgument(0) as String
            amount = it.getArgument(1) as BigDecimal
            currency = it.getArgument(2) as Currency
            bundle = it.getArgument(3) as Bundle
            Unit
        }
        whenever(
            mockInternalAppEventsLogger.logPurchaseImplicitly(
                any(),
                any(),
                any(),
            )
        ).thenAnswer {
            amount = it.getArgument(0) as BigDecimal
            currency = it.getArgument(1) as Currency
            bundle = it.getArgument(2) as Bundle
            Unit
        }
    }

    @Test
    fun `test log activate app event when autoLogAppEvent disable`() {
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

        AutomaticAnalyticsLogger.logActivateAppEvent()

        assertEquals(0, logWarningCallCount)
        assertEquals(0, appEventLoggerCallCount)
    }

    @Test
    fun `test log activate app event when autoLogAppEvent enable`() {
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

        AutomaticAnalyticsLogger.logActivateAppEvent()

        assertEquals(1, logWarningCallCount)
    }

    @Test
    fun `test log activate app event when autoLogAppEvent enable & context is application`() {
        val appContext = RuntimeEnvironment.application
        whenever(FacebookSdk.getApplicationContext()).thenReturn(appContext)
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
        val mockCompanion = mock(AppEventsLogger.Companion::class.java)
        WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
        whenever(mockCompanion.activateApp(appContext, appID)).then { appEventLoggerCallCount++ }

        AutomaticAnalyticsLogger.logActivateAppEvent()

        assertEquals(1, appEventLoggerCallCount)
    }

    @Test
    fun `test log activity time spent event when automatic logging disable`() {
        whenever(mockFetchedAppSettings.automaticLoggingEnabled).thenReturn(false)

        AutomaticAnalyticsLogger.logActivityTimeSpentEvent(activityName, timeSpent)

        verify(mockFetchedAppSettings).automaticLoggingEnabled
        verifyNew(Bundle::class.java, never()).withArguments(any())
        verify(mockInternalAppEventsLogger, never()).logEvent(any(), any())
    }

    @Test
    fun `test log activity time spent event when automatic logging enable`() {
        whenever(mockFetchedAppSettings.automaticLoggingEnabled).thenReturn(true)

        AutomaticAnalyticsLogger.logActivityTimeSpentEvent(activityName, timeSpent)

        verify(mockFetchedAppSettings).automaticLoggingEnabled
        verifyNew(Bundle::class.java).withArguments(eq(1))
        verify(mockInternalAppEventsLogger)
            .logEvent(eq(Constants.AA_TIME_SPENT_EVENT_NAME), eq(5.0), eq(mockBundle))
    }

    @Test
    fun `test log purchase when implicit purchase logging disable`() {
        var appGateKeepersManagerCallCount = 0
        val mockManager = mock(FetchedAppGateKeepersManager::class.java)
        Whitebox.setInternalState(FetchedAppGateKeepersManager::class.java, "INSTANCE", mockManager)
        whenever(
            mockManager.getGateKeeperForKey(
                any<String>(),
                any<String>(),
                any<Boolean>()
            )
        ).then {
            appGateKeepersManagerCallCount++
        }
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

        AutomaticAnalyticsLogger.logPurchase(
            oneTimePurchase,
            oneTimePurchaseDetailsGPBLV2V4,
            true,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )

        assertEquals(0, appGateKeepersManagerCallCount)
    }

    @Test
    fun `test log purchase when implicit purchase logging enable & start trial with GPBL v2 - v4`() {
        AutomaticAnalyticsLogger.logPurchase(
            subscriptionPurchase,
            subscriptionDetailsGPBLV2V4,
            true,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        verify(mockInternalAppEventsLogger)
            .logEventImplicitly(
                eq(AppEventsConstants.EVENT_NAME_START_TRIAL),
                any<BigDecimal>(),
                any<Currency>(),
                any<Bundle>()
            )

        Assertions.assertThat(bundle).isNotNull
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_AUTOLOG_IMPLEMENTATION))
            .isEqualTo(InAppPurchaseUtils.BillingClientVersion.V2_V4.type)
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_ID)).isEqualTo("id123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TIME))
            .isEqualTo("12345")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TOKEN))
            .isEqualTo("token123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PACKAGE_NAME))
            .isEqualTo("examplePackageName")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TITLE))
            .isEqualTo("ExampleTitle")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TYPE))
            .isEqualTo("subs")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_AUTORENEWING))
            .isEqualTo("true")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_FREE_TRIAL_PERIOD))
            .isEqualTo("P1W")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_INTRO_PERIOD))
            .isEqualTo("P1W")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_INTRO_PRICE_AMOUNT_MICROS))
            .isEqualTo("3590000")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_PERIOD))
            .isEqualTo("P1W")
        Assertions.assertThat(currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(amount).isEqualTo(BigDecimal(3.99))
        Assertions.assertThat(eventName).isEqualTo(AppEventsConstants.EVENT_NAME_START_TRIAL)
        Assertions.assertThat(
            bundle?.getCharSequence(
                Constants.IAP_BILLING_LIBRARY_VERSION,
            )
        ).isEqualTo("GPBL.5.1.0")

    }

    @Test
    fun `test log purchase when implicit purchase logging enable & subscribe with GPBL v2 - v4`() {
        AutomaticAnalyticsLogger.logPurchase(
            subscriptionPurchase,
            subscriptionDetailsWithNoFreeTrialGPBLV2V4,
            true,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        verify(mockInternalAppEventsLogger)
            .logEventImplicitly(
                eq(AppEventsConstants.EVENT_NAME_SUBSCRIBE),
                any<BigDecimal>(),
                any<Currency>(),
                any<Bundle>()
            )

        Assertions.assertThat(bundle).isNotNull
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_AUTOLOG_IMPLEMENTATION))
            .isEqualTo(InAppPurchaseUtils.BillingClientVersion.V2_V4.type)
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_ID)).isEqualTo("id123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TIME))
            .isEqualTo("12345")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TOKEN))
            .isEqualTo("token123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PACKAGE_NAME))
            .isEqualTo("examplePackageName")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TITLE))
            .isEqualTo("ExampleTitle")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TYPE))
            .isEqualTo("subs")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_AUTORENEWING))
            .isEqualTo("true")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_INTRO_PRICE_AMOUNT_MICROS))
            .isEqualTo("3590000")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_INTRO_PERIOD))
            .isEqualTo("P1W")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_PERIOD))
            .isEqualTo("P1W")
        Assertions.assertThat(currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(amount).isEqualTo(BigDecimal(3.99))
        Assertions.assertThat(eventName).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
        Assertions.assertThat(
            bundle?.getCharSequence(
                Constants.IAP_BILLING_LIBRARY_VERSION,
            )
        ).isEqualTo("GPBL.5.1.0")
    }


    @Test
    fun `test log purchase when implicit purchase logging enable & subscribed with GPBL v5 - v7`() {
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
        AutomaticAnalyticsLogger.logPurchase(
            subscriptionPurchase,
            subscriptionDetailsGPBLV5V7,
            true,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        verify(mockInternalAppEventsLogger)
            .logEventImplicitly(
                eq(AppEventsConstants.EVENT_NAME_SUBSCRIBE),
                any<BigDecimal>(),
                any<Currency>(),
                any<Bundle>()
            )

        Assertions.assertThat(bundle).isNotNull
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_AUTOLOG_IMPLEMENTATION))
            .isEqualTo(InAppPurchaseUtils.BillingClientVersion.V5_V7.type)
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_ID)).isEqualTo("id123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TIME))
            .isEqualTo("12345")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TOKEN))
            .isEqualTo("token123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PACKAGE_NAME))
            .isEqualTo("examplePackageName")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TITLE))
            .isEqualTo("ExampleTitle")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_DESCRIPTION))
            .isEqualTo("Exampledescription.")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TYPE))
            .isEqualTo("subs")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_AUTORENEWING))
            .isEqualTo("true")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_SUBSCRIPTION_PERIOD))
            .isEqualTo("P1W")
        val basePlanId = bundle?.getCharSequence(Constants.IAP_BASE_PLAN)
        val validBasePlan = basePlanId == "basePlanId" || basePlanId == "basePlanId2"
        Assertions.assertThat(validBasePlan)
            .isTrue()
        Assertions.assertThat(currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(amount).isEqualTo(BigDecimal(3.99))
        Assertions.assertThat(eventName).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
        Assertions.assertThat(
            bundle?.getCharSequence(
                Constants.IAP_BILLING_LIBRARY_VERSION,
            )
        ).isEqualTo("GPBL.5.1.0")
    }

    @Test
    fun `test dedupe implicit subscription with GPBL v5 - v7`() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(true)

        val manualPurchaseHistory = ConcurrentHashMap<InAppPurchase, MutableList<Long>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            3.99,
            Currency.getInstance(Locale.US)
        )
        manualPurchaseHistory[purchase] = mutableListOf(System.currentTimeMillis())
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfManualPurchases",
            manualPurchaseHistory
        )
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
        AutomaticAnalyticsLogger.logPurchase(
            subscriptionPurchase,
            subscriptionDetailsGPBLV5V7,
            true,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )

        Assertions.assertThat(bundle).isNull()
    }

    @Test
    fun `test log purchase when implicit purchase logging enable & not subscribed with GPBL v2 - v4`() {
        AutomaticAnalyticsLogger.logPurchase(
            oneTimePurchase,
            oneTimePurchaseDetailsGPBLV2V4,
            false,
            InAppPurchaseUtils.BillingClientVersion.V2_V4
        )
        verify(mockInternalAppEventsLogger)
            .logPurchaseImplicitly(any<BigDecimal>(), any<Currency>(), any<Bundle>())
        Assertions.assertThat(bundle).isNotNull
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_AUTOLOG_IMPLEMENTATION))
            .isEqualTo(InAppPurchaseUtils.BillingClientVersion.V2_V4.type)
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_ID)).isEqualTo("id123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TIME))
            .isEqualTo("12345")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TOKEN))
            .isEqualTo("token123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PACKAGE_NAME))
            .isEqualTo("examplePackageName")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TITLE))
            .isEqualTo("ExampleTitle")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_DESCRIPTION))
            .isEqualTo("Exampledescription.")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TYPE))
            .isEqualTo("inapp")
        Assertions.assertThat(currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(amount).isEqualTo(BigDecimal(12))
        Assertions.assertThat(
            bundle?.getCharSequence(
                Constants.IAP_BILLING_LIBRARY_VERSION,
            )
        ).isEqualTo("GPBL.5.1.0")
    }

    @Test
    fun `test log purchase when implicit purchase logging enable & not subscribed with GPBL v5 - v7`() {
        AutomaticAnalyticsLogger.logPurchase(
            oneTimePurchase,
            oneTimePurchaseDetailsGPBLV5V7,
            false,
            InAppPurchaseUtils.BillingClientVersion.V5_V7
        )
        verify(mockInternalAppEventsLogger)
            .logPurchaseImplicitly(any<BigDecimal>(), any<Currency>(), any<Bundle>())
        Assertions.assertThat(bundle).isNotNull
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_AUTOLOG_IMPLEMENTATION))
            .isEqualTo(InAppPurchaseUtils.BillingClientVersion.V5_V7.type)
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_ID)).isEqualTo("id123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TIME))
            .isEqualTo("12345")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PURCHASE_TOKEN))
            .isEqualTo("token123")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PACKAGE_NAME))
            .isEqualTo("examplePackageName")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TITLE))
            .isEqualTo("ExampleTitle")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_DESCRIPTION))
            .isEqualTo("Exampledescription.")
        Assertions.assertThat(bundle?.getCharSequence(Constants.IAP_PRODUCT_TYPE))
            .isEqualTo("inapp")
        Assertions.assertThat(currency).isEqualTo(Currency.getInstance("USD"))
        Assertions.assertThat(amount).isEqualTo(BigDecimal(12))
        Assertions.assertThat(
            bundle?.getCharSequence(
                Constants.IAP_BILLING_LIBRARY_VERSION,
            )
        ).isEqualTo("GPBL.5.1.0")

    }

    @Test
    fun `test is implicit purchase logging enabled when autoLogAppEvent Disable`() {
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
        val result = AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()
        assertEquals(false, result)
    }

    @Test
    fun `test is implicit purchase logging enabled when autoLogAppEvent enable`() {
        whenever(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
        val result2 = AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()
        assertEquals(true, result2)
    }
}
