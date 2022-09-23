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
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.appevents.internal.AutomaticAnalyticsLogger
import com.facebook.appevents.internal.Constants
import com.facebook.appevents.ondeviceprocessing.OnDeviceProcessingManager
import com.facebook.appevents.ondeviceprocessing.RemoteServiceWrapper
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FeatureManager
import com.facebook.internal.FetchedAppGateKeepersManager
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
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
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.RuntimeEnvironment

@PrepareForTest(
    AppEventQueue::class,
    AppEventUtility::class,
    AttributionIdentifiers::class,
    AutomaticAnalyticsLogger::class,
    FetchedAppGateKeepersManager::class,
    FeatureManager::class,
    RemoteServiceWrapper::class,
    OnDeviceProcessingManager::class)
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
  private lateinit var mockParams: Bundle
  private lateinit var logger: AppEventsLoggerImpl

  @Before
  fun setupTest() {
    FacebookSdk.setApplicationId(mockAppID)
    FacebookSdk.setClientToken(mockClientSecret)
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application)
    FacebookSdk.setExecutor(mockExecutor)

    mockParams = Bundle()
    logger = AppEventsLoggerImpl(RuntimeEnvironment.application, mockAppID, mock())

    // Stub empty implementations to AppEventQueue to not really flush events
    PowerMockito.mockStatic(AppEventQueue::class.java)

    // Disable Gatekeeper
    PowerMockito.mockStatic(FetchedAppGateKeepersManager::class.java)
    whenever(FetchedAppGateKeepersManager.getGateKeeperForKey(any(), any(), any()))
        .thenReturn(false)

    // Enable on-device event processing
    PowerMockito.mockStatic(FeatureManager::class.java)
    whenever(FeatureManager.isEnabled(FeatureManager.Feature.OnDeviceEventProcessing))
        .thenReturn(true)

    // Stub mock IDs for AttributionIdentifiers
    val mockIdentifiers = PowerMockito.mock(AttributionIdentifiers::class.java)
    whenever(mockIdentifiers.androidAdvertiserId).thenReturn(mockAdvertiserID)
    whenever(mockIdentifiers.attributionId).thenReturn(mockAttributionID)
    val mockCompanion: AttributionIdentifiers.Companion = mock()
    WhiteboxImpl.setInternalState(AttributionIdentifiers::class.java, "Companion", mockCompanion)
    whenever(mockCompanion.getAttributionIdentifiers(any())).thenReturn(mockIdentifiers)
    Whitebox.setInternalState(AppEventsLoggerImpl::class.java, "anonymousAppDeviceGUID", mockAnonID)
    PowerMockito.mockStatic(AutomaticAnalyticsLogger::class.java)
    whenever(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true)

    // Disable AppEventUtility.isMainThread since executor now runs in main thread
    PowerMockito.mockStatic(AppEventUtility::class.java)
    whenever(AppEventUtility.assertIsNotMainThread()).doAnswer {}
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
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
      Unit
    }
    logger.logEvent(mockEventName)
    assertThat(appEventCapture?.name).isEqualTo(mockEventName)
  }

  @Test
  fun testLogPurchase() {
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
      Unit
    }

    logger.logPurchase(BigDecimal(1.0), Currency.getInstance(Locale.US))
    val parameters = Bundle()
    parameters.putString(
        AppEventsConstants.EVENT_PARAM_CURRENCY, Currency.getInstance(Locale.US).currencyCode)
    assertThat(appEventCapture?.name).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED)

    val jsonObject = appEventCapture?.getJSONObject()
    assertThat(jsonObject?.getDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)).isEqualTo(1.0)
    assertJsonHasParams(jsonObject, parameters)
  }

  @Test
  fun testLogProductItemWithGtinMpnBrand() {
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
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
        "BLUE MOUNTAIN",
        "BLUE MOUNTAIN",
        "PHILZ",
        null)
    val parameters = Bundle()
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21")
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
        AppEventsLogger.ProductAvailability.IN_STOCK.name)
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name)
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title")
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
        BigDecimal(1.0).setScale(3, BigDecimal.ROUND_HALF_UP).toString())
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY, Currency.getInstance(Locale.US).currencyCode)
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
        null)
    val parameters = Bundle()
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21")
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
        AppEventsLogger.ProductAvailability.IN_STOCK.name)
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name)
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com")
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title")
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
        BigDecimal(1.0).setScale(3, BigDecimal.ROUND_HALF_UP).toString())
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY, Currency.getInstance(Locale.US).currencyCode)

    assertThat(appEventQueueCalledTimes).isEqualTo(0)
  }

  @Test
  fun testLogPushNotificationOpen() {
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
      Unit
    }

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
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
      Unit
    }

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
        FacebookSdk.getApplicationContext(), FacebookSdk.getApplicationId())
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
  fun testSetPushNotificationsRegistrationId() {
    val mockNotificationId = "123"
    var appEventCapture: AppEvent? = null
    whenever(AppEventQueue.add(any(), any())).thenAnswer {
      appEventCapture = it.arguments[1] as AppEvent
      Unit
    }

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
                eq(APP_EVENTS_KILLSWITCH), any(), any()))
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
        mockParams)

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
                eq(APP_EVENTS_KILLSWITCH), any(), any()))
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
        mockParams)
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
