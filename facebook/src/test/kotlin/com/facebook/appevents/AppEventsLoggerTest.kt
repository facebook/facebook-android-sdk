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
package com.facebook.appevents

import android.app.Application
import android.os.Bundle
import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.TestUtils
import com.facebook.appevents.AppEventTestUtilities.BundleMatcher
import com.facebook.appevents.internal.AppEventUtility
import com.facebook.appevents.internal.AppEventsLoggerUtility
import com.facebook.appevents.internal.AppEventsLoggerUtility.getJSONObjectForGraphAPICall
import com.facebook.internal.AttributionIdentifiers
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledThreadPoolExecutor
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.robolectric.RuntimeEnvironment

@PrepareForTest(
    AppEventUtility::class,
    AppEventsLogger::class,
    AppEventsLoggerImpl::class,
    FacebookSdk::class,
    AttributionIdentifiers::class,
    FetchedAppSettingsManager::class)
class AppEventsLoggerTest : FacebookPowerMockTestCase() {
  private val mockExecutor: Executor = FacebookSerialExecutor()
  private val mockAppID = "fb_mock_id"
  private lateinit var logger: AppEventsLoggerImpl
  @Before
  fun setupTest() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    PowerMockito.`when`(FacebookSdk.isInitialized()).thenReturn(true)
    PowerMockito.`when`(FacebookSdk.getApplicationId()).thenReturn(mockAppID)
    PowerMockito.`when`(FacebookSdk.getApplicationContext())
        .thenReturn(RuntimeEnvironment.application)
    PowerMockito.`when`(FacebookSdk.getExecutor()).thenReturn(mockExecutor)
    logger = mock()
    PowerMockito.whenNew(AppEventsLoggerImpl::class.java).withAnyArguments().thenReturn(logger)
    // Disable AppEventUtility.isMainThread since executor now runs in main thread
    PowerMockito.spy(AppEventUtility::class.java)
    PowerMockito.doReturn(false).`when`(AppEventUtility::class.java, "isMainThread")
    PowerMockito.spy(AppEventsLoggerImpl::class.java)
    PowerMockito.`when`(AppEventsLoggerImpl.getAnalyticsExecutor()).thenReturn(mockExecutor)
    PowerMockito.mockStatic(FetchedAppSettingsManager::class.java)
  }

  @Test
  fun testAppEventsLoggerLogFunctions() {
    val mockEventName = "fb_mock_event"
    val mockPayload = Bundle()
    val mockAction = "fb_mock_action"
    val mockVal = BigDecimal(1.0)
    val mockCurrency = Currency.getInstance(Locale.US)
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName)
    verify(logger, times(1)).logEvent(mockEventName)
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, 1.0)
    verify(logger, times(1)).logEvent(mockEventName, 1.0)
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, null)
    verify(logger, times(1)).logEvent(mockEventName, null)
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, 1.0, null)
    verify(logger, times(1)).logEvent(mockEventName, 1.0, null)
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logPushNotificationOpen(mockPayload)
    verify(logger, times(1)).logPushNotificationOpen(argThat(BundleMatcher(mockPayload)), isNull())
    AppEventsLogger.newLogger(RuntimeEnvironment.application)
        .logPushNotificationOpen(mockPayload, mockAction)
    verify(logger, times(1))
        .logPushNotificationOpen(argThat(BundleMatcher(mockPayload)), eq(mockAction))
    AppEventsLogger.newLogger(RuntimeEnvironment.application)
        .logProductItem(
            "F40CEE4E-471E-45DB-8541-1526043F4B21",
            AppEventsLogger.ProductAvailability.IN_STOCK,
            AppEventsLogger.ProductCondition.NEW,
            "description",
            "https://www.sample.com",
            "https://www.link.com",
            "title",
            mockVal,
            mockCurrency,
            "GTIN",
            "MPN",
            "BRAND",
            mockPayload)
    verify(logger, times(1))
        .logProductItem(
            eq("F40CEE4E-471E-45DB-8541-1526043F4B21"),
            eq(AppEventsLogger.ProductAvailability.IN_STOCK),
            eq(AppEventsLogger.ProductCondition.NEW),
            eq("description"),
            eq("https://www.sample.com"),
            eq("https://www.link.com"),
            eq("title"),
            eq(mockVal),
            eq(mockCurrency),
            eq("GTIN"),
            eq("MPN"),
            eq("BRAND"),
            argThat(BundleMatcher(mockPayload)))
    AppEventsLogger.newLogger(RuntimeEnvironment.application).logPurchase(mockVal, mockCurrency)
    verify(logger, times(1)).logPurchase(eq(mockVal), eq(mockCurrency))
    AppEventsLogger.newLogger(RuntimeEnvironment.application)
        .logPurchase(mockVal, mockCurrency, mockPayload)
    verify(logger, times(1))
        .logPurchase(eq(mockVal), eq(mockCurrency), argThat(BundleMatcher(mockPayload)))
  }

  @Test
  fun testAutoLogAppEventsEnabled() {
    Whitebox.setInternalState(
        AppEventsLoggerImpl::class.java, "backgroundExecutor", mock<ScheduledThreadPoolExecutor>())
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
    AppEventsLogger.initializeLib(FacebookSdk.getApplicationContext(), mockAppID)
    PowerMockito.verifyNew(AppEventsLoggerImpl::class.java)
        .withArguments(any(), eq(mockAppID), isNull())
  }

  @Test
  fun testAutoLogAppEventsDisabled() {
    Whitebox.setInternalState(
        AppEventsLoggerImpl::class.java, "backgroundExecutor", mock<ScheduledThreadPoolExecutor>())
    PowerMockito.`when`(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    AppEventsLogger.initializeLib(FacebookSdk.getApplicationContext(), mockAppID)
    PowerMockito.verifyNew(AppEventsLoggerImpl::class.java, never())
        .withArguments(any(), any(), any())
  }

  @Test
  fun testSetAndClearUserData() {
    AppEventsLogger.setUserData(
        "em@gmail.com", "fn", "ln", "123", null, null, null, null, null, null)
    val actualUserData = JSONObject(AppEventsLogger.getUserData())
    val expectedUserData =
        JSONObject(
            "{\"ln\":\"e545c2c24e6463d7c4fe3829940627b226c0b9be7a8c7dbe964768da48f1ab9d\",\"ph\":\"a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3\",\"em\":\"5f341666fb1ce60d716e4afc302c8658f09412290aa2ca8bc623861f452f9d33\",\"fn\":\"0f1e18bb4143dc4be22e61ea4deb0491c2bf7018c6504ad631038aed5ca4a0ca\"}")
    TestUtils.assertEquals(expectedUserData, actualUserData)
    AppEventsLogger.clearUserData()
    Assert.assertTrue(AppEventsLogger.getUserData().isEmpty())
  }

  @Test
  fun testSetAndClearUserID() {
    val userID = "12345678"
    AppEventsLogger.setUserID(userID)
    Assert.assertEquals(AppEventsLogger.getUserID(), userID)
    AppEventsLogger.clearUserID()
    Assert.assertNull(AppEventsLogger.getUserID())
  }

  @Test
  fun testUserIDAddedToAppEvent() {
    PowerMockito.spy(AttributionIdentifiers::class.java)
    PowerMockito.doReturn(true)
        .`when`(AttributionIdentifiers::class.java, "isTrackingLimited", any())
    val userID = "12345678"
    AppEventsLogger.setUserID(userID)
    val jsonObject =
        getJSONObjectForGraphAPICall(
            AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
            null,
            "123",
            true,
            FacebookSdk.getApplicationContext())
    Assert.assertEquals(jsonObject.getString("app_user_id"), userID)
  }

  @Test
  fun testActivateApp() {
    val mockApplication: Application = mock()
    PowerMockito.`when`(FacebookSdk.publishInstallAsync(any(), any())).thenCallRealMethod()
    AppEventsLogger.activateApp(mockApplication)
    verify(mockApplication, times(1)).registerActivityLifecycleCallbacks(any())
  }

  @Test
  fun testSetPushNotificationsRegistrationId() {
    val mockNotificationId = "123"
    PowerMockito.`when`(AppEventsLoggerImpl.setPushNotificationsRegistrationId(mockNotificationId))
        .thenCallRealMethod()
    AppEventsLogger.setPushNotificationsRegistrationId(mockNotificationId)
  }
}
