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
import com.facebook.internal.FetchedAppGateKeepersManager
import com.facebook.internal.FetchedAppSettings
import com.facebook.internal.FetchedAppSettingsManager
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import java.math.BigDecimal
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito.mock
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.verifyNew
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox
import org.powermock.reflect.internal.WhiteboxImpl
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment

@PrepareForTest(
    Log::class,
    FacebookSdk::class,
    InternalAppEventsLogger::class,
    FetchedAppSettings::class,
    FetchedAppSettingsManager::class,
    AutomaticAnalyticsLogger::class,
    FetchedAppGateKeepersManager::class)
class AutomaticAnalyticsLoggerTest : FacebookPowerMockTestCase() {

  private val appID = "123"
  private val activityName = "activity name"
  private val timeSpent = 5L
  private val purchase =
      "{\"productId\":\"id123\", \"purchaseTime\":\"12345\", \"purchaseToken\": \"token123\"}"
  private val skuDetails = "{\"price_currency_code\":\"USD\",\"price_amount_micros\":5000}"

  private var logWarningCallCount = 0
  private var appEventLoggerCallCount = 0

  private lateinit var context: Context
  private lateinit var mockInternalAppEventsLogger: InternalAppEventsLogger
  private lateinit var mockBundle: Bundle
  private lateinit var mockFetchedAppSettings: FetchedAppSettings

  @Before
  fun init() {
    mockStatic(FacebookSdk::class.java)
    mockStatic(Log::class.java)
    mockStatic(FetchedAppSettingsManager::class.java)
    mockStatic(FetchedAppGateKeepersManager::class.java)

    context = Robolectric.buildActivity(Activity::class.java).get()
    whenCalled(FacebookSdk.getApplicationContext()).thenReturn(context)
    whenCalled(FacebookSdk.isInitialized()).thenReturn(true)
    whenCalled(FacebookSdk.getApplicationId()).thenReturn(appID)

    mockInternalAppEventsLogger = mock(InternalAppEventsLogger::class.java)
    whenNew(InternalAppEventsLogger::class.java)
        .withAnyArguments()
        .thenReturn(mockInternalAppEventsLogger)

    Whitebox.setInternalState(
        AutomaticAnalyticsLogger::class.java,
        "internalAppEventsLogger",
        mockInternalAppEventsLogger)

    mockBundle = mock(Bundle::class.java)
    whenNew(Bundle::class.java).withAnyArguments().thenReturn(mockBundle)

    mockFetchedAppSettings = mock(FetchedAppSettings::class.java)
    whenCalled(FetchedAppSettingsManager.queryAppSettings(appID, false))
        .thenReturn(mockFetchedAppSettings)
    whenCalled(FetchedAppSettingsManager.getAppSettingsWithoutQuery(appID))
        .thenReturn(mockFetchedAppSettings)

    whenCalled(Log.w(any(), any<String>())).then { logWarningCallCount++ }
    whenCalled(mockFetchedAppSettings.iAPAutomaticLoggingEnabled).thenReturn(true)

    val mockManager = mock(FetchedAppGateKeepersManager::class.java)
    Whitebox.setInternalState(FetchedAppGateKeepersManager::class.java, "INSTANCE", mockManager)

    whenCalled(mockManager.getGateKeeperForKey(any<String>(), any<String>(), any<Boolean>()))
        .thenReturn(true)
  }

  @Test
  fun `test log activate app event when autoLogAppEvent disable`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

    AutomaticAnalyticsLogger.logActivateAppEvent()

    assertEquals(0, logWarningCallCount)
    assertEquals(0, appEventLoggerCallCount)
  }

  @Test
  fun `test log activate app event when autoLogAppEvent enable`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)

    AutomaticAnalyticsLogger.logActivateAppEvent()

    assertEquals(1, logWarningCallCount)
  }

  @Test
  fun `test log activate app event when autoLogAppEvent enable & context is application`() {
    val appContext = RuntimeEnvironment.application
    whenCalled(FacebookSdk.getApplicationContext()).thenReturn(appContext)
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
    val mockCompanion = mock(AppEventsLogger.Companion::class.java)
    WhiteboxImpl.setInternalState(AppEventsLogger::class.java, "Companion", mockCompanion)
    whenCalled(mockCompanion.activateApp(appContext, appID)).then { appEventLoggerCallCount++ }

    AutomaticAnalyticsLogger.logActivateAppEvent()

    assertEquals(1, appEventLoggerCallCount)
  }

  @Test
  fun `test log activity time spent event when automatic logging disable`() {
    whenCalled(mockFetchedAppSettings.automaticLoggingEnabled).thenReturn(false)

    AutomaticAnalyticsLogger.logActivityTimeSpentEvent(activityName, timeSpent)

    verify(mockFetchedAppSettings).automaticLoggingEnabled
    verifyNew(Bundle::class.java, never()).withArguments(any())
    verify(mockInternalAppEventsLogger, never()).logEvent(any(), any())
  }

  @Test
  fun `test log activity time spent event when automatic logging enable`() {
    whenCalled(mockFetchedAppSettings.automaticLoggingEnabled).thenReturn(true)

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
    whenCalled(mockManager.getGateKeeperForKey(any<String>(), any<String>(), any<Boolean>())).then {
      appGateKeepersManagerCallCount++
    }
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)

    AutomaticAnalyticsLogger.logPurchase(purchase, skuDetails, true)

    assertEquals(0, appGateKeepersManagerCallCount)
  }

  @Test
  fun `test log purchase when implicit purchase logging enable & subscribed`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
    AutomaticAnalyticsLogger.logPurchase(purchase, skuDetails, true)
    verify(mockInternalAppEventsLogger)
        .logEventImplicitly(
            eq(AppEventsConstants.EVENT_NAME_SUBSCRIBE),
            any<BigDecimal>(),
            any<Currency>(),
            any<Bundle>())
  }

  @Test
  fun `test log purchase when implicit purchase logging enable & not subscribed`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
    AutomaticAnalyticsLogger.logPurchase(purchase, skuDetails, false)
    verify(mockInternalAppEventsLogger)
        .logPurchaseImplicitly(any<BigDecimal>(), any<Currency>(), any<Bundle>())
  }

  @Test
  fun `test is implicit purchase logging enabled when autoLogAppEvent Disable`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(false)
    val result = AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()
    assertEquals(false, result)
  }

  @Test
  fun `test is implicit purchase logging enabled when autoLogAppEvent enable`() {
    whenCalled(FacebookSdk.getAutoLogAppEventsEnabled()).thenReturn(true)
    val result2 = AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()
    assertEquals(true, result2)
  }
}
