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

package com.facebook.appevents;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.os.Bundle;
import com.facebook.AccessToken;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.appevents.internal.AutomaticAnalyticsLogger;
import com.facebook.appevents.internal.Constants;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FetchedAppGateKeepersManager;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.Executor;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@PrepareForTest({
  AppEventQueue.class,
  AppEventUtility.class,
  AttributionIdentifiers.class,
  AutomaticAnalyticsLogger.class,
  FetchedAppGateKeepersManager.class,
})
public class AppEventsLoggerImplTest extends FacebookPowerMockTestCase {

  @Captor public ArgumentCaptor<AppEvent> eventCaptor;

  private final Executor mockExecutor = new FacebookSerialExecutor();

  private final String mockAppID = "12345";
  private final String mockEventName = "fb_mock_event";
  private final double mockValueToSum = 1.0;
  private final Currency mockCurrency = Currency.getInstance(Locale.US);
  private final BigDecimal mockDecimal = new BigDecimal(1.0);
  private final String mockAttributionID = "fb_mock_attributionID";
  private final String mockAdvertiserID = "fb_mock_advertiserID";
  private final String mockAnonID = "fb_mock_anonID";
  private final String APP_EVENTS_KILLSWITCH = "app_events_killswitch";

  private Bundle mockParams;
  private AppEventsLoggerImpl logger;

  @Before
  public void setupTest() throws Exception {
    FacebookSdk.setApplicationId(mockAppID);
    FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);

    mockParams = new Bundle();
    logger =
        new AppEventsLoggerImpl(RuntimeEnvironment.application, mockAppID, mock(AccessToken.class));

    // Stub empty implementations to AppEventQueue to not really flush events
    PowerMockito.mockStatic(AppEventQueue.class);

    // Disable Gatekeeper
    PowerMockito.mockStatic(FetchedAppGateKeepersManager.class);
    PowerMockito.when(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                anyString(), anyString(), anyBoolean()))
        .thenReturn(false);

    // Stub mock IDs for AttributionIdentifiers
    AttributionIdentifiers mockIdentifiers = PowerMockito.mock(AttributionIdentifiers.class);
    PowerMockito.when(mockIdentifiers.getAndroidAdvertiserId()).thenReturn(mockAdvertiserID);
    PowerMockito.when(mockIdentifiers.getAttributionId()).thenReturn(mockAttributionID);
    PowerMockito.mockStatic(AttributionIdentifiers.class);
    PowerMockito.when(AttributionIdentifiers.getAttributionIdentifiers(any(Context.class)))
        .thenReturn(mockIdentifiers);

    Whitebox.setInternalState(AppEventsLoggerImpl.class, "anonymousAppDeviceGUID", mockAnonID);
    PowerMockito.mockStatic(AutomaticAnalyticsLogger.class);
    PowerMockito.when(AutomaticAnalyticsLogger.isImplicitPurchaseLoggingEnabled()).thenReturn(true);

    // Disable AppEventUtility.isMainThread since executor now runs in main thread
    PowerMockito.spy(AppEventUtility.class);
    PowerMockito.doReturn(false).when(AppEventUtility.class, "isMainThread");
  }

  @Test
  public void testSetFlushBehavior() {
    AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.AUTO);
    Assert.assertEquals(AppEventsLogger.FlushBehavior.AUTO, AppEventsLogger.getFlushBehavior());

    AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY);
    Assert.assertEquals(
        AppEventsLogger.FlushBehavior.EXPLICIT_ONLY, AppEventsLogger.getFlushBehavior());
  }

  @Test
  public void testLogEvent() throws Exception {
    logger.logEvent(mockEventName);

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName()).isEqualTo(mockEventName);
  }

  @Test
  public void testLogPurchase() throws Exception {
    logger.logPurchase(new BigDecimal(1.0), Currency.getInstance(Locale.US));
    Bundle parameters = new Bundle();
    parameters.putString(
        AppEventsConstants.EVENT_PARAM_CURRENCY, Currency.getInstance(Locale.US).getCurrencyCode());

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName()).isEqualTo(AppEventsConstants.EVENT_NAME_PURCHASED);

    JSONObject jsonObject = eventCaptor.getValue().getJSONObject();
    assertThat(jsonObject.getDouble(AppEventsConstants.EVENT_PARAM_VALUE_TO_SUM)).isEqualTo(1.0);
    assertJsonHasParams(jsonObject, parameters);
  }

  @Test
  public void testLogProductItemWithGtinMpnBrand() throws Exception {
    logger.logProductItem(
        "F40CEE4E-471E-45DB-8541-1526043F4B21",
        AppEventsLogger.ProductAvailability.IN_STOCK,
        AppEventsLogger.ProductCondition.NEW,
        "description",
        "https://www.sample.com",
        "https://www.sample.com",
        "title",
        new BigDecimal(1.0),
        Currency.getInstance(Locale.US),
        "BLUE MOUNTAIN",
        "BLUE MOUNTAIN",
        "PHILZ",
        null);
    Bundle parameters = new Bundle();
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21");
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
        AppEventsLogger.ProductAvailability.IN_STOCK.name());
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name());
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title");
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
        (new BigDecimal(1.0)).setScale(3, BigDecimal.ROUND_HALF_UP).toString());
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
        Currency.getInstance(Locale.US).getCurrencyCode());
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_GTIN, "BLUE MOUNTAIN");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_MPN, "BLUE MOUNTAIN");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_BRAND, "PHILZ");

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName())
        .isEqualTo(AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE);

    JSONObject jsonObject = eventCaptor.getValue().getJSONObject();
    assertJsonHasParams(jsonObject, parameters);
  }

  @Test
  public void testLogProductItemWithoutGtinMpnBrand() throws Exception {
    logger.logProductItem(
        "F40CEE4E-471E-45DB-8541-1526043F4B21",
        AppEventsLogger.ProductAvailability.IN_STOCK,
        AppEventsLogger.ProductCondition.NEW,
        "description",
        "https://www.sample.com",
        "https://www.sample.com",
        "title",
        new BigDecimal(1.0),
        Currency.getInstance(Locale.US),
        null,
        null,
        null,
        null);
    Bundle parameters = new Bundle();
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_ITEM_ID, "F40CEE4E-471E-45DB-8541-1526043F4B21");
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
        AppEventsLogger.ProductAvailability.IN_STOCK.name());
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_CONDITION, AppEventsLogger.ProductCondition.NEW.name());
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com");
    parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title");
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
        (new BigDecimal(1.0)).setScale(3, BigDecimal.ROUND_HALF_UP).toString());
    parameters.putString(
        Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
        Currency.getInstance(Locale.US).getCurrencyCode());

    PowerMockito.verifyStatic(AppEventQueue.class, never());
    AppEventQueue.add(any(AccessTokenAppIdPair.class), any(AppEvent.class));
  }

  @Test
  public void testLogPushNotificationOpen() throws Exception {
    Bundle payload = new Bundle();
    payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}");
    logger.logPushNotificationOpen(payload, null);
    Bundle parameters = new Bundle();
    parameters.putString("fb_push_campaign", "testCampaign");

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName()).isEqualTo("fb_mobile_push_opened");

    JSONObject jsonObject = eventCaptor.getValue().getJSONObject();
    assertJsonHasParams(jsonObject, parameters);
  }

  @Test
  public void testLogPushNotificationOpenWithoutCampaign() throws Exception {
    Bundle payload = new Bundle();
    payload.putString("fb_push_payload", "{}");
    logger.logPushNotificationOpen(payload, null);

    PowerMockito.verifyStatic(AppEventQueue.class, never());
    AppEventQueue.add(any(AccessTokenAppIdPair.class), any(AppEvent.class));
  }

  @Test
  public void testLogPushNotificationOpenWithAction() throws Exception {
    Bundle payload = new Bundle();
    payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}");
    logger.logPushNotificationOpen(payload, "testAction");
    Bundle parameters = new Bundle();
    parameters.putString("fb_push_campaign", "testCampaign");
    parameters.putString("fb_push_action", "testAction");

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName()).isEqualTo("fb_mobile_push_opened");

    JSONObject jsonObject = eventCaptor.getValue().getJSONObject();
    assertJsonHasParams(jsonObject, parameters);
  }

  @Test
  public void testLogPushNotificationOpenWithoutPayload() throws Exception {
    Bundle payload = new Bundle();
    logger.logPushNotificationOpen(payload, null);

    PowerMockito.verifyStatic(AppEventQueue.class, never());
    AppEventQueue.add(any(AccessTokenAppIdPair.class), any(AppEvent.class));
  }

  @Test
  public void testPublishInstall() throws Exception {
    FacebookSdk.setAdvertiserIDCollectionEnabled(true);
    FacebookSdk.GraphRequestCreator mockGraphRequestCreator =
        mock(FacebookSdk.GraphRequestCreator.class);
    FacebookSdk.setGraphRequestCreator(mockGraphRequestCreator);
    String expectedEvent = "MOBILE_APP_INSTALL";
    String expectedUrl = mockAppID + "/activities";
    final ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);

    FacebookSdk.publishInstallAsync(
        FacebookSdk.getApplicationContext(), FacebookSdk.getApplicationId());

    Mockito.verify(mockGraphRequestCreator)
        .createPostRequest(
            isNull(AccessToken.class),
            eq(expectedUrl),
            captor.capture(),
            isNull(GraphRequest.Callback.class));
    Assert.assertEquals(expectedEvent, captor.getValue().getString("event"));
    Assert.assertTrue(captor.getValue().getBoolean("advertiser_tracking_enabled"));
    Assert.assertTrue(captor.getValue().getBoolean("application_tracking_enabled"));
    Assert.assertEquals(mockAdvertiserID, captor.getValue().getString("advertiser_id"));
    Assert.assertEquals(mockAttributionID, captor.getValue().getString("attribution"));
    Assert.assertEquals(mockAnonID, captor.getValue().getString("anon_id"));
  }

  @Test
  public void testSetPushNotificationsRegistrationId() throws Exception {
    String mockNotificationId = "123";
    AppEventsLogger.setPushNotificationsRegistrationId(mockNotificationId);

    PowerMockito.verifyStatic(AppEventQueue.class);
    AppEventQueue.add(any(AccessTokenAppIdPair.class), eventCaptor.capture());
    assertThat(eventCaptor.getValue().getName())
        .isEqualTo(AppEventsConstants.EVENT_NAME_PUSH_TOKEN_OBTAINED);
    Assert.assertEquals(
        mockNotificationId, InternalAppEventsLogger.getPushNotificationsRegistrationId());
  }

  @Test
  public void testAppEventsKillSwitchDisabled() {
    PowerMockito.mockStatic(FetchedAppGateKeepersManager.class);
    PowerMockito.when(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                eq(APP_EVENTS_KILLSWITCH), anyString(), anyBoolean()))
        .thenReturn(false);

    logger.logEvent(mockEventName, mockValueToSum, mockParams, true, null);
    logger.logEventImplicitly(mockEventName, mockDecimal, mockCurrency, mockParams);
    logger.logSdkEvent(mockEventName, mockValueToSum, mockParams);
    logger.logPurchase(mockDecimal, mockCurrency, mockParams, true);
    logger.logPurchaseImplicitly(mockDecimal, mockCurrency, mockParams);
    logger.logPushNotificationOpen(mockParams, null);
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
        mockParams);

    PowerMockito.verifyStatic(AppEventQueue.class, times(5));
    AppEventQueue.add(any(AccessTokenAppIdPair.class), any(AppEvent.class));
  }

  @Test
  public void testAppEventsKillSwitchEnabled() {
    PowerMockito.mockStatic(FetchedAppGateKeepersManager.class);
    PowerMockito.when(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                eq(APP_EVENTS_KILLSWITCH), anyString(), anyBoolean()))
        .thenReturn(true);

    AppEventsLoggerImpl logger =
        new AppEventsLoggerImpl(RuntimeEnvironment.application, mockAppID, null);

    logger.logEvent(mockEventName, mockValueToSum, mockParams, true, null);
    logger.logEventImplicitly(mockEventName, mockDecimal, mockCurrency, mockParams);
    logger.logSdkEvent(mockEventName, mockValueToSum, mockParams);
    logger.logPurchase(mockDecimal, mockCurrency, mockParams, true);
    logger.logPurchaseImplicitly(mockDecimal, mockCurrency, mockParams);
    logger.logPushNotificationOpen(mockParams, null);
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
        mockParams);

    PowerMockito.verifyStatic(AppEventQueue.class, never());
    AppEventQueue.add(any(AccessTokenAppIdPair.class), any(AppEvent.class));
  }

  private static void assertJsonHasParams(JSONObject jsonObject, Bundle params)
      throws JSONException {
    for (String key : params.keySet()) {
      assertThat(jsonObject.has(key)).isTrue();
      assertThat(jsonObject.get(key)).isEqualTo(params.get(key));
    }
  }
}
