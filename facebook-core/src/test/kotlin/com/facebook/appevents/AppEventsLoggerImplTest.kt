/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents

import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.FacebookSdk.GraphRequestCreator
import com.facebook.UserSettingsManager
import com.facebook.appevents.AppEventsLoggerImpl.Companion.addImplicitPurchaseParameters
import com.facebook.appevents.iap.InAppPurchase
import com.facebook.appevents.iap.InAppPurchaseManager
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.appevents.internal.AutomaticAnalyticsLogger
import com.facebook.appevents.internal.Constants
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.RuntimeEnvironment
import java.util.concurrent.ConcurrentHashMap

@PrepareForTest(
    AppEventQueue::class,
    AppEventUtility::class,
    AttributionIdentifiers::class,
    FetchedAppSettingsManager::class,
    AutomaticAnalyticsLogger::class,
    UserSettingsManager::class,
    FacebookSdk::class,
    FetchedAppGateKeepersManager::class,
    FeatureManager::class,
    RemoteServiceWrapper::class,
    OnDeviceProcessingManager::class,
)
class AppEventsLoggerImplTest : FacebookPowerMockTestCase() {
    private val mockExecutor: Executor = FacebookSerialExecutor()
    private val mockAppID = "12345"
    private val mockClientSecret = "abcdefg"
    private val mockEventName = "fb_mock_event"
    private val mockValueToSum = 1.0
    private val mockCurrency = Currency.getInstance(Locale.US)
    private val mockDecimal = BigDecimal(1.0)
    private val mockAttributionID = "fb_mock_attributionID"
    private val mockAdvertiserID = "fb_mock_advertiserID"
    private val mockAnonID = "fb_mock_anonID"
    private val APP_EVENTS_KILLSWITCH = "app_events_killswitch"
    private var appEventCapture: AppEvent? = null
    private lateinit var mockParams: Bundle
    private lateinit var logger: AppEventsLoggerImpl
    private lateinit var mockFetchedAppSettings: FetchedAppSettings


    @Before
    fun setupTest() {
        mockStatic(FetchedAppSettingsManager::class.java)
        FacebookSdk.setApplicationId(mockAppID)
        FacebookSdk.setClientToken(mockClientSecret)
        FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
        FacebookSdk.setExecutor(mockExecutor)

        mockParams = Bundle()
        logger = AppEventsLoggerImpl(RuntimeEnvironment.application, mockAppID, mock())

        // Stub empty implementations to AppEventQueue to not really flush events
        mockStatic(AppEventQueue::class.java)

        // Disable Gatekeeper
        mockStatic(FetchedAppGateKeepersManager::class.java)
        whenever(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), any(), any()))
            .thenReturn(false)

        // Enable on-device event processing
        mockStatic(FeatureManager::class.java)
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.OnDeviceEventProcessing))
            .thenReturn(true)

        // Stub mock IDs for AttributionIdentifiers
        val mockIdentifiers = PowerMockito.mock(AttributionIdentifiers::class.java)
        whenever(mockIdentifiers.androidAdvertiserId).thenReturn(mockAdvertiserID)
        whenever(mockIdentifiers.attributionId).thenReturn(mockAttributionID)
        val mockCompanion: AttributionIdentifiers.Companion = mock()
        WhiteboxImpl.setInternalState(
            AttributionIdentifiers::class.java,
            "Companion",
            mockCompanion
        )
        whenever(mockCompanion.getAttributionIdentifiers(any())).thenReturn(mockIdentifiers)
        Whitebox.setInternalState(
            AppEventsLoggerImpl::class.java,
            "anonymousAppDeviceGUID",
            mockAnonID
        )
        PowerMockito.mockStatic(AutomaticAnalyticsLogger::class.java)
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)

        // Disable AppEventUtility.isMainThread since executor now runs in main thread
        PowerMockito.mockStatic(AppEventUtility::class.java)
        whenever(AppEventUtility.assertIsNotMainThread()).doAnswer {}

        // Spy on InAppPurchaseManager to test dedupe functionality
        PowerMockito.spy(InAppPurchaseManager::class.java)
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfManualPurchases",
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        )
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventCapture = it.arguments[1] as AppEvent
            Unit
        }
    }

    fun createDedupeInfo() {
        mockStatic(FacebookSdk::class.java)
        whenever(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
        mockFetchedAppSettings = PowerMockito.mock(FetchedAppSettings::class.java)
        whenever(FetchedAppSettingsManager.queryAppSettings(mockAppID, false))
            .thenReturn(mockFetchedAppSettings)
        whenever(FetchedAppSettingsManager.getAppSettingsWithoutQuery(any()))
            .thenReturn(mockFetchedAppSettings)

        val dedupeParameters = listOf(
            Pair(
                Constants.IAP_PRODUCT_ID,
                listOf(
                    AppEventsConstants.EVENT_PARAM_CONTENT_ID,
                    Constants.EVENT_PARAM_PRODUCT_ITEM_ID,
                    Constants.IAP_PRODUCT_ID
                )
            ),
            Pair(
                Constants.IAP_PRODUCT_TITLE,
                listOf(
                    "fb_content_title",
                    Constants.EVENT_PARAM_PRODUCT_TITLE,
                    Constants.IAP_PRODUCT_TITLE
                )
            ),
            Pair(
                Constants.IAP_PRODUCT_DESCRIPTION,
                listOf(
                    AppEventsConstants.EVENT_PARAM_DESCRIPTION,
                    Constants.IAP_PRODUCT_DESCRIPTION
                )
            ),
        )
        val testDedupeParameters = listOf(
            Pair(
                "fb_iap_purchase_token",
                listOf("fb_iap_purchase_token", "fb_transaction_id", "fb_order_id")
            ),
            Pair(Constants.IAP_SUBSCRIPTION_PERIOD, listOf(Constants.IAP_SUBSCRIPTION_PERIOD))
        )
        whenever(mockFetchedAppSettings.prodDedupeParameters).thenReturn(dedupeParameters)
        whenever(mockFetchedAppSettings.testDedupeParameters).thenReturn(testDedupeParameters)
    }

    @Test
    fun testSetFlushBehavior() {
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.AUTO)
        assertThat(AppEventsLogger.getFlushBehavior()).isEqualTo(AppEventsLogger.FlushBehavior.AUTO)
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)
        assertThat(AppEventsLogger.getFlushBehavior())
            .isEqualTo(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY)
    }

    @Test
    fun testLogEvent() {
        logger.logEvent(mockEventName)
        assertThat(appEventCapture?.name).isEqualTo(mockEventName)
    }

    @Test
    fun testLogManualSubscriptionWithDedupeDisabled() {
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(false)
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0)
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testLogManualSubscriptionWithDedupeEnabled() {
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.2,
            Currency.getInstance(Locale.US)
        )
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(null, null)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(true)
        val params = Bundle()
        params.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0, params)
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testLogManualDuplicateSubscriptionWithDedupeEnabled() {
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val params = Bundle()
        val operationalData = OperationalData()
        params.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        params.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(params, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(true)
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0, params)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testLogManualDuplicateSubscriptionWithDedupeEnabledAndOperationalDedupeKey() {
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val params = Bundle()
        params.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        params.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        val operationalData = OperationalData()
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            AppEventsConstants.EVENT_PARAM_CURRENCY,
            "USD"
        )
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            Constants.IAP_PRODUCT_ID,
            "productID"
        )
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(null, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(true)
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0, params)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testLogManualDuplicateSubscriptionWithDedupeEnabledAndOnlyTestDedupe() {
        createDedupeInfo()
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val implicitParams = Bundle()
        val operationalData = OperationalData()
        implicitParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        implicitParams.putCharSequence(Constants.IAP_PRODUCT_ID, "productiD")
        implicitParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token123")
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(implicitParams, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(true)
        val manualParams = Bundle()
        manualParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        manualParams.putCharSequence(Constants.IAP_PRODUCT_ID, "other product id")
        manualParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token123")
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0, manualParams)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.optString(Constants.IAP_ACTUAL_DEDUP_RESULT), "")
        assertEquals(
            loggedParams?.optString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), ""
        )
        assertEquals(loggedParams?.getString(Constants.IAP_TEST_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), Constants.IAP_PURCHASE_TOKEN
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testLogManualDuplicateSubscriptionWithDedupeEnabledAndOnlyOperationalTestDedupe() {
        createDedupeInfo()
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle?, OperationalData?>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_SUBSCRIBE,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val implicitParams = Bundle()
        val operationalData = OperationalData()
        implicitParams.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        implicitParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        implicitParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token123")
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            Constants.IAP_PURCHASE_TOKEN, "token123"
        )
        operationalData.addParameter(
            OperationalDataEnum.IAPParameters,
            Constants.IAP_PRODUCT_ID,
            "productID"
        )

        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(null, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitSubsDedupe))
            .thenReturn(true)
        val manualParams = Bundle()
        manualParams.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        manualParams.putCharSequence(Constants.IAP_PRODUCT_ID, "other product id")
        manualParams.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token123")
        logger.logEvent(AppEventsConstants.EVENT_NAME_SUBSCRIBE, 1.0, manualParams)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.optString(Constants.IAP_ACTUAL_DEDUP_RESULT), "")
        assertEquals(
            loggedParams?.optString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), ""
        )
        assertEquals(loggedParams?.getString(Constants.IAP_TEST_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), Constants.IAP_PURCHASE_TOKEN
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_SUBSCRIBE)
    }

    @Test
    fun testAddImplicitPurchaseParameters() {
        mockStatic(UserSettingsManager::class.java)

        var params: Bundle? = Bundle(1)
        var operationalData: OperationalData? = OperationalData()
        var paramsAndData: Pair<Bundle?, OperationalData?>?
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)
        whenever(UserSettingsManager.getAutoLogAppEventsEnabled()).thenReturn(true)
        paramsAndData = addImplicitPurchaseParameters(params, operationalData)
        params = paramsAndData.first
        operationalData = paramsAndData.second
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "1"
        )
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "1"
        )

        params = Bundle(1)
        operationalData = OperationalData()
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)
        whenever(UserSettingsManager.getAutoLogAppEventsEnabled()).thenReturn(false)
        paramsAndData = addImplicitPurchaseParameters(params, operationalData)
        params = paramsAndData.first
        operationalData = paramsAndData.second
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "0"
        )
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "1"
        )

        params = Bundle(1)
        operationalData = OperationalData()
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(false)
        whenever(UserSettingsManager.getAutoLogAppEventsEnabled()).thenReturn(true)
        paramsAndData = addImplicitPurchaseParameters(params, operationalData)
        params = paramsAndData.first
        operationalData = paramsAndData.second
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "1"
        )
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "0"
        )

        params = Bundle(1)
        operationalData = OperationalData()
        whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(false)
        whenever(UserSettingsManager.getAutoLogAppEventsEnabled()).thenReturn(false)
        paramsAndData = addImplicitPurchaseParameters(params, operationalData)
        params = paramsAndData.first
        operationalData = paramsAndData.second
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_AUTOLOG_APP_EVENTS_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "0"
        )
        assertThat(
            OperationalData.getParameter(
                OperationalDataEnum.IAPParameters,
                Constants.EVENT_PARAM_IS_IMPLICIT_PURCHASE_LOGGING_ENABLED,
                params,
                operationalData
            )
        ).isEqualTo(
            "0"
        )
    }

    @Test
    fun testLogPurchaseWithDedupeDisabled() {
        createDedupeInfo()
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(false)
        logger.logPurchase(BigDecimal(1.0), Currency.getInstance(Locale.US))
        val parameters = Bundle()
        parameters.putString(
            AppEventsConstants.EVENT_PARAM_CURRENCY, Currency.getInstance(Locale.US).currencyCode
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED)

        val jsonObject = appEventCapture?.getJSONObject()
        assertThat(jsonObject?.getDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)).isEqualTo(1.0)
        assertJsonHasParams(jsonObject, parameters)
    }

    @Test
    fun testLogPurchaseWithDedupeEnabledAndIsNotADuplicate() {
        createDedupeInfo()
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val previouslyLoggedParameters = Bundle()
        val operationalData = OperationalData()
        previouslyLoggedParameters.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(1, Pair(previouslyLoggedParameters, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(true)
        logger.logPurchase(BigDecimal(1.0), Currency.getInstance(Locale.US))
        val parameters = Bundle()
        parameters.putString(
            AppEventsConstants.EVENT_PARAM_CURRENCY, Currency.getInstance(Locale.US).currencyCode
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED)

        val jsonObject = appEventCapture?.getJSONObject()
        assertThat(jsonObject?.getDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)).isEqualTo(1.0)
        assertJsonHasParams(jsonObject, parameters)
    }

    @Test
    fun testLogPurchaseWithDedupeEnabledAndIsADuplicate() {
        createDedupeInfo()
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val parameters = Bundle()
        val operationalData = OperationalData()
        parameters.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        implicitPurchaseHistory[purchase] =
            mutableListOf(Pair(System.currentTimeMillis(), Pair(parameters, operationalData)))
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(true)
        logger.logPurchase(BigDecimal(1.0), Currency.getInstance(Locale.US), parameters)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), Constants.IAP_PRODUCT_ID
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED)
    }

    @Test
    fun testLogDuplicatePurchaseWithTestDedupeInfo() {
        createDedupeInfo()
        val implicitPurchaseHistory =
            ConcurrentHashMap<InAppPurchase, MutableList<Pair<Long, Pair<Bundle, OperationalData>>>>()
        val purchase = InAppPurchase(
            AppEventsConstants.EVENT_NAME_PURCHASED,
            1.0,
            Currency.getInstance(Locale.US)
        )
        val implicitParameters = Bundle()
        val operationalData = OperationalData()
        implicitParameters.putCharSequence(Constants.IAP_PRODUCT_ID, "productID")
        implicitParameters.putCharSequence(Constants.IAP_PURCHASE_TOKEN, "token123")
        implicitPurchaseHistory[purchase] =
            mutableListOf(
                Pair(
                    System.currentTimeMillis(),
                    Pair(implicitParameters, operationalData)
                )
            )
        Whitebox.setInternalState(
            InAppPurchaseManager::class.java,
            "timesOfImplicitPurchases",
            implicitPurchaseHistory
        )
        whenever(FeatureManager.isEnabled(FeatureManager.Feature.AndroidManualImplicitPurchaseDedupe))
            .thenReturn(true)
        val manualParameters = Bundle()
        manualParameters.putCharSequence(AppEventsConstants.EVENT_PARAM_CONTENT_ID, "productID")
        manualParameters.putCharSequence("fb_order_id", "token123")
        manualParameters.putCharSequence(AppEventsConstants.EVENT_PARAM_CURRENCY, "USD")
        logger.logPurchase(BigDecimal(1.0), Currency.getInstance(Locale.US), manualParameters)
        val loggedParams =
            appEventCapture?.getOperationalJSONObject(OperationalDataEnum.IAPParameters)
        assertEquals(loggedParams?.getString(Constants.IAP_ACTUAL_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_ACTUAL_DEDUP_KEY_USED,
            ), AppEventsConstants.EVENT_PARAM_CONTENT_ID
        )
        assertEquals(loggedParams?.getString(Constants.IAP_TEST_DEDUP_RESULT), "1")
        assertEquals(
            loggedParams?.getString(
                Constants.IAP_TEST_DEDUP_KEY_USED,
            ), "fb_order_id"
        )
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED)
    }

    @Test
    fun testLogProductItemWithGtinMpnBrand() {
        logger.logProductItem(
            "F40CEE4E-471E-45DB-8541-1526043F4B21",
            AppEventsLogger.ProductAvailability.IN_STOCK,
            AppEventsLogger.ProductCondition.NEW,
            "description",
            "https://www.sample.com",
            "https://www.sample.com",
            "title",
            BigDecimal(1.0),
            Currency.getInstance(Locale.US),
            "BLUE MOUNTAIN",
            "BLUE MOUNTAIN",
            "PHILZ",
            null
        )
        val parameters = Bundle()
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21"
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
            AppEventsLogger.ProductAvailability.IN_STOCK.name
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name
        )
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title")
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
            BigDecimal(1.0).setScale(3, BigDecimal.ROUND_HALF_UP).toString()
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
            Currency.getInstance(Locale.US).currencyCode
        )
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_GTIN, "BLUE MOUNTAIN")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_MPN, "BLUE MOUNTAIN")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_BRAND, "PHILZ")

        assertThat(appEventCapture?.name)
            .isEqualTo(AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE)
        val jsonObject = appEventCapture?.getJSONObject()
        assertJsonHasParams(jsonObject, parameters)
    }

    @Test
    fun testLogProductItemWithoutGtinMpnBrand() {
        var appEventQueueCalledTimes = 0
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventQueueCalledTimes++
            Unit
        }

        logger.logProductItem(
            "F40CEE4E-471E-45DB-8541-1526043F4B21",
            AppEventsLogger.ProductAvailability.IN_STOCK,
            AppEventsLogger.ProductCondition.NEW,
            "description",
            "https://www.sample.com",
            "https://www.sample.com",
            "title",
            BigDecimal(1.0),
            Currency.getInstance(Locale.US),
            null,
            null,
            null,
            null
        )
        val parameters = Bundle()
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21"
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
            AppEventsLogger.ProductAvailability.IN_STOCK.name
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name
        )
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com")
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title")
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
            BigDecimal(1.0).setScale(3, BigDecimal.ROUND_HALF_UP).toString()
        )
        parameters.putString(
            Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
            Currency.getInstance(Locale.US).currencyCode
        )

        assertThat(appEventQueueCalledTimes).isEqualTo(0)
    }

    @Test
    fun testLogPushNotificationOpen() {
        val payload = Bundle()
        payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}")
        logger.logPushNotificationOpen(payload, null)
        val parameters = Bundle()
        parameters.putString("fb_push_campaign", "testCampaign")

        assertThat(appEventCapture?.name).isEqualTo("fb_mobile_push_opened")
        val jsonObject = appEventCapture?.getJSONObject()
        assertJsonHasParams(jsonObject, parameters)
    }

    @Test
    fun testLogPushNotificationOpenWithoutCampaign() {
        var appEventQueueCalledTimes = 0
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventQueueCalledTimes++
            Unit
        }

        val payload = Bundle()
        payload.putString("fb_push_payload", "{}")
        logger.logPushNotificationOpen(payload, null)
        assertThat(appEventQueueCalledTimes).isEqualTo(0)
    }

    @Test
    fun testLogPushNotificationOpenWithAction() {
        val payload = Bundle()
        payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}")
        logger.logPushNotificationOpen(payload, "testAction")
        val parameters = Bundle()
        parameters.putString("fb_push_campaign", "testCampaign")
        parameters.putString("fb_push_action", "testAction")

        assertThat(appEventCapture?.name).isEqualTo("fb_mobile_push_opened")
        val jsonObject = appEventCapture?.getJSONObject()
        assertJsonHasParams(jsonObject, parameters)
    }

    @Test
    fun testLogPushNotificationOpenWithoutPayload() {
        var appEventQueueCalledTimes = 0
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventQueueCalledTimes++
            Unit
        }

        val payload = Bundle()
        logger.logPushNotificationOpen(payload, null)

        assertThat(appEventQueueCalledTimes).isEqualTo(0)
    }

    @Test
    fun testPublishInstall() {
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        val mockGraphRequestCreator: GraphRequestCreator = mock()
        FacebookSdk.setGraphRequestCreator(mockGraphRequestCreator)
        val expectedEvent = "MOBILE_APP_INSTALL"
        val expectedUrl = "$mockAppID/activities"
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)
        PowerMockito.mockStatic(OnDeviceProcessingManager::class.java)
        whenever(OnDeviceProcessingManager.isOnDeviceProcessingEnabled()).thenReturn(true)
        var sendInstallEventTimes = 0
        whenever(OnDeviceProcessingManager.sendInstallEventAsync(eq(mockAppID), any())).thenAnswer {
            sendInstallEventTimes++
            Unit
        }
        FacebookSdk.publishInstallAsync(
            FacebookSdk.getApplicationContext(), FacebookSdk.getApplicationId()
        )
        verify(mockGraphRequestCreator)
            .createPostRequest(isNull(), eq(expectedUrl), captor.capture(), isNull())
        assertThat(captor.value.getString("event")).isEqualTo(expectedEvent)
        assertThat(captor.value.getBoolean("advertiser_tracking_enabled")).isTrue()
        assertThat(captor.value.getBoolean("application_tracking_enabled")).isTrue()
        assertThat(captor.value.getString("advertiser_id")).isEqualTo(mockAdvertiserID)
        assertThat(captor.value.getString("attribution")).isEqualTo(mockAttributionID)
        assertThat(captor.value.getString("anon_id")).isEqualTo(mockAnonID)

        assertThat(sendInstallEventTimes).isEqualTo(1)
    }

    @Test
    fun testPublishInstallWithAppEventsSkillswitchEnabled() {
        FacebookSdk.setAdvertiserIDCollectionEnabled(true)
        val mockGraphRequestCreator: GraphRequestCreator = mock()
        FacebookSdk.setGraphRequestCreator(mockGraphRequestCreator)
        val expectedEvent = "MOBILE_APP_INSTALL"
        val expectedUrl = "$mockAppID/activities"
        val captor = ArgumentCaptor.forClass(JSONObject::class.java)

        // Should not publish install event given that app_events_killswitch is turned on
        PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
        PowerMockito.`when`(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                eq(APP_EVENTS_KILLSWITCH), any(), any()
            )
        )
            .thenReturn(true)

        FacebookSdk.publishInstallAsync(
            FacebookSdk.getApplicationContext(), FacebookSdk.getApplicationId()
        )
        verify(mockGraphRequestCreator, never())
            .createPostRequest(isNull(), eq(expectedUrl), captor.capture(), isNull())
    }

    @Test
    fun testSetPushNotificationsRegistrationId() {
        val mockNotificationId = "123"
        AppEventsLogger.setPushNotificationsRegistrationId(mockNotificationId)
        assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PUSH_TOKEN_OBTAINED)
        assertThat(InternalAppEventsLogger.getPushNotificationsRegistrationId())
            .isEqualTo(mockNotificationId)
    }

    @Test
    fun testAppEventsKillSwitchDisabled() {
        var appEventQueueCalledTimes = 0
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventQueueCalledTimes++
            Unit
        }
        PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
        PowerMockito.`when`(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                eq(APP_EVENTS_KILLSWITCH), any(), any()
            )
        )
            .thenReturn(false)
        logger.logEvent(mockEventName, mockValueToSum, mockParams, true, null)
        logger.logEventImplicitly(mockEventName, mockDecimal, mockCurrency, mockParams)
        logger.logSdkEvent(mockEventName, mockValueToSum, mockParams)
        logger.logPurchase(mockDecimal, mockCurrency, mockParams, true)
        logger.logPurchaseImplicitly(mockDecimal, mockCurrency, mockParams)
        logger.logPushNotificationOpen(mockParams, null)
        logger.logProductItem(
            "F40CEE4E-471E-45DB-8541-1526043F4B21",
            AppEventsLogger.ProductAvailability.IN_STOCK,
            AppEventsLogger.ProductCondition.NEW,
            "description",
            "https://www.sample.com",
            "https://www.link.com",
            "title",
            mockDecimal,
            mockCurrency,
            "GTIN",
            "MPN",
            "BRAND",
            mockParams
        )

        assertThat(appEventQueueCalledTimes).isEqualTo(5)
    }

    @Test
    fun testAppEventsKillSwitchEnabled() {
        var appEventQueueCalledTimes = 0
        whenever(AppEventQueue.add(any(), any())).thenAnswer {
            appEventQueueCalledTimes++
            Unit
        }
        PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
        PowerMockito.`when`(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                eq(APP_EVENTS_KILLSWITCH), any(), any()
            )
        )
            .thenReturn(true)
        val logger = AppEventsLoggerImpl(RuntimeEnvironment.application, mockAppID, null)
        logger.logEvent(mockEventName, mockValueToSum, mockParams, true, null)
        logger.logEventImplicitly(mockEventName, mockDecimal, mockCurrency, mockParams)
        logger.logSdkEvent(mockEventName, mockValueToSum, mockParams)
        logger.logPurchase(mockDecimal, mockCurrency, mockParams, true)
        logger.logPurchaseImplicitly(mockDecimal, mockCurrency, mockParams)
        logger.logPushNotificationOpen(mockParams, null)
        logger.logProductItem(
            "F40CEE4E-471E-45DB-8541-1526043F4B21",
            AppEventsLogger.ProductAvailability.IN_STOCK,
            AppEventsLogger.ProductCondition.NEW,
            "description",
            "https://www.sample.com",
            "https://www.link.com",
            "title",
            mockDecimal,
            mockCurrency,
            "GTIN",
            "MPN",
            "BRAND",
            mockParams
        )
        assertThat(appEventQueueCalledTimes).isEqualTo(0)
    }

    @Test
    fun `test augmentWebView will run on Android api 17+`() {
        Whitebox.setInternalState(Build.VERSION::class.java, "RELEASE", "4.2.0")
        val mockWebView = mock<WebView>()
        AppEventsLoggerImpl.augmentWebView(mockWebView, RuntimeEnvironment.application)
        verify(mockWebView).addJavascriptInterface(any(), any())
    }

    @Test
    fun `test augmentWebView will not run on Android api less than 17 `() {
        Whitebox.setInternalState(Build.VERSION::class.java, "RELEASE", "4.0.0")
        val mockWebView = mock<WebView>()
        AppEventsLoggerImpl.augmentWebView(mockWebView, RuntimeEnvironment.application)
        verify(mockWebView, never()).addJavascriptInterface(any(), any())
    }

    companion object {
        private fun assertJsonHasParams(jsonObject: JSONObject?, params: Bundle) {
            assertThat(jsonObject).isNotNull
            for (key in params.keySet()) {
                assertThat(jsonObject?.has(key)).isTrue
                assertThat(jsonObject?.get(key)).isEqualTo(params[key])
            }
        }
    }
}
