/**
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

import android.content.Context;
import android.os.Bundle;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.HttpMethod;
import com.facebook.TestUtils;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.appevents.internal.AppEventsLoggerUtility;
import com.facebook.appevents.internal.Constants;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.Utility;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        AppEvent.class,
        AppEventQueue.class,
        AppEventUtility.class,
        AppEventsLogger.class,
        AppEventsLogger.class,
        AttributionIdentifiers.class,
        FacebookSdk.class,
        GraphRequest.class,
        Utility.class,
})
public class AppEventsLoggerTest extends FacebookPowerMockTestCase {

    private final Executor mockExecutor = new FacebookSerialExecutor();

    @Before
    public void before() throws Exception {
        spy(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        when(FacebookSdk.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        when(FacebookSdk.getExecutor()).thenReturn(mockExecutor);

        // Mock Utility class with empty stub functions, which will be called in
        // AppEventsLoggerUtility.getJSONObjectForGraphAPICall
        mockStatic(Utility.class);
        // Stub empty implementations to AppEventQueue to not really flush events
        mockStatic(AppEventQueue.class);

        // Disable AppEventUtility.isMainThread since executor now runs in main thread
        spy(AppEventUtility.class);
        doReturn(false).when(AppEventUtility.class, "isMainThread");
        mockStatic(AppEventsLogger.class);
        spy(AppEventsLogger.class);
        doReturn(mockExecutor).when(AppEventsLogger.class, "getAnalyticsExecutor");

        AppEvent mockEvent = mock(AppEvent.class);
        when(mockEvent.getIsImplicit()).thenReturn(true);
        PowerMockito.whenNew(AppEvent.class).withAnyArguments().thenReturn(mockEvent);
    }

    @Test
    public void testSetAndClearUserData() throws JSONException {
        AppEventsLogger.setUserData(
                "em@gmail.com", "fn", "ln", "123", null, null, null, null, null, null);
        JSONObject actualUserData = new JSONObject(AppEventsLogger.getUserData());
        JSONObject expectedUserData = new JSONObject("{\"ln\":\"e545c2c24e6463d7c4fe3829940627b226c0b9be7a8c7dbe964768da48f1ab9d\",\"ph\":\"a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3\",\"em\":\"5f341666fb1ce60d716e4afc302c8658f09412290aa2ca8bc623861f452f9d33\",\"fn\":\"0f1e18bb4143dc4be22e61ea4deb0491c2bf7018c6504ad631038aed5ca4a0ca\"}");
        TestUtils.assertEquals(expectedUserData, actualUserData);

        AppEventsLogger.clearUserData();
        assertTrue(AppEventsLogger.getUserData().isEmpty());
    }

    @Test
    public void testSetAndClearUserID() {
        String userID = "12345678";
        AppEventsLogger.setUserID(userID);
        assertEquals(AppEventsLogger.getUserID(), userID);
        AppEventsLogger.clearUserID();
        assertNull(AppEventsLogger.getUserID());
    }

    @Test
    public void testSetFlushBehavior() {
        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.AUTO);
        assertEquals(AppEventsLogger.FlushBehavior.AUTO, AppEventsLogger.getFlushBehavior());

        AppEventsLogger.setFlushBehavior(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY);
        assertEquals(AppEventsLogger.FlushBehavior.EXPLICIT_ONLY, AppEventsLogger.getFlushBehavior());
    }

    @Test
    public void testUserIDAddedToAppEvent() throws Exception {
        String userID = "12345678";
        AppEventsLogger.setUserID(userID);
        JSONObject jsonObject = AppEventsLoggerUtility.getJSONObjectForGraphAPICall(
                AppEventsLoggerUtility.GraphAPIActivityType.MOBILE_INSTALL_EVENT,
                null,
                "123",
                true,
                FacebookSdk.getApplicationContext());
        assertEquals(jsonObject.getString("app_user_id"), userID);
    }

    @Test
    public void testActivateApp() {
        AppEventsLogger.activateApp(RuntimeEnvironment.application);
        mockStatic(ActivityLifecycleTracker.class);
        verifyStatic();
    }

    @Test
    public void testLogEvent() throws Exception {
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logEvent("fb_mock_event");
        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq("fb_mock_event"),
                Matchers.anyDouble(),
                Matchers.any(Bundle.class),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogPurchase() throws Exception {
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logPurchase(
                new BigDecimal(1.0), Currency.getInstance(Locale.US));
        Bundle parameters = new Bundle();
        parameters.putString(
                AppEventsConstants.EVENT_PARAM_CURRENCY,
                Currency.getInstance(Locale.US).getCurrencyCode());

        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq(AppEventsConstants.EVENT_NAME_PURCHASED),
                Matchers.eq(1.0),
                argThat(new AppEventTestUtilities.BundleMatcher(parameters)),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogProductItemWithGtinMpnBrand() throws Exception {
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logProductItem(
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
                Constants.EVENT_PARAM_PRODUCT_ITEM_ID,
                "F40CEE4E-471E-45DB-8541-1526043F4B21");
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
                AppEventsLogger.ProductAvailability.IN_STOCK.name());
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_CONDITION,
                AppEventsLogger.ProductCondition.NEW.name());
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
                (new BigDecimal(1.0)).setScale(3, BigDecimal.ROUND_HALF_UP).toString());
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
                Currency.getInstance(Locale.US).getCurrencyCode());
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_GTIN, "BLUE MOUNTAIN");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_MPN, "BLUE MOUNTAIN");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_BRAND, "PHILZ");

        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq(AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE),
                Matchers.anyDouble(),
                argThat(new AppEventTestUtilities.BundleMatcher(parameters)),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogProductItemWithoutGtinMpnBrand() throws Exception {
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logProductItem(
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
                Constants.EVENT_PARAM_PRODUCT_ITEM_ID,
                "F40CEE4E-471E-45DB-8541-1526043F4B21");
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_AVAILABILITY,
                AppEventsLogger.ProductAvailability.IN_STOCK.name());
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_CONDITION,
                AppEventsLogger.ProductCondition.NEW.name());
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_DESCRIPTION, "description");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_IMAGE_LINK, "https://www.sample.com");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_LINK, "https://www.sample.com");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_TITLE, "title");
        parameters.putString(Constants.EVENT_PARAM_PRODUCT_PRICE_AMOUNT,
                (new BigDecimal(1.0)).setScale(3, BigDecimal.ROUND_HALF_UP).toString());
        parameters.putString(
                Constants.EVENT_PARAM_PRODUCT_PRICE_CURRENCY,
                Currency.getInstance(Locale.US).getCurrencyCode());

        verifyNew(AppEvent.class, never()).withArguments(
                Matchers.anyString(),
                Matchers.eq(AppEventsConstants.EVENT_NAME_PRODUCT_CATALOG_UPDATE),
                Matchers.anyDouble(),
                argThat(new AppEventTestUtilities.BundleMatcher(parameters)),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogPushNotificationOpen() throws Exception {
        Bundle payload = new Bundle();
        payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}");
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logPushNotificationOpen(payload);
        Bundle parameters = new Bundle();
        parameters.putString("fb_push_campaign", "testCampaign");

        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq("fb_mobile_push_opened"),
                Matchers.anyDouble(),
                argThat(new AppEventTestUtilities.BundleMatcher(parameters)),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogPushNotificationOpenWithoutCampaign() throws Exception {
        Bundle payload = new Bundle();
        payload.putString("fb_push_payload", "{}");
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logPushNotificationOpen(payload);

        verifyNew(AppEvent.class, never()).withArguments(
                Matchers.anyString(),
                Matchers.anyString(),
                Matchers.anyDouble(),
                Matchers.any(Bundle.class),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogPushNotificationOpenWithAction() throws Exception {
        Bundle payload = new Bundle();
        payload.putString("fb_push_payload", "{\"campaign\" : \"testCampaign\"}");
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logPushNotificationOpen(payload, "testAction");
        Bundle parameters = new Bundle();
        parameters.putString("fb_push_campaign", "testCampaign");
        parameters.putString("fb_push_action", "testAction");

        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq("fb_mobile_push_opened"),
                Matchers.anyDouble(),
                argThat(new AppEventTestUtilities.BundleMatcher(parameters)),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testLogPushNotificationOpenWithoutPayload() throws Exception {
        when(Utility.isNullOrEmpty(anyString())).thenReturn(true);
        Bundle payload = new Bundle();
        AppEventsLogger.newLogger(FacebookSdk.getApplicationContext()).logPushNotificationOpen(payload);

        verifyNew(AppEvent.class, never()).withArguments(
                Matchers.anyString(),
                Matchers.anyString(),
                Matchers.anyDouble(),
                Matchers.any(Bundle.class),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
    }

    @Test
    public void testSetPushNotificationsRegistrationId()  throws Exception {
        String mockNotificationId = "123";
        AppEventsLogger.setPushNotificationsRegistrationId(mockNotificationId);

        verifyNew(AppEvent.class).withArguments(
                Matchers.anyString(),
                Matchers.eq(AppEventsConstants.EVENT_NAME_PUSH_TOKEN_OBTAINED),
                Matchers.anyDouble(),
                Matchers.any(Bundle.class),
                Matchers.anyBoolean(),
                Matchers.anyBoolean(),
                Matchers.any(UUID.class));
        assertEquals(mockNotificationId, AppEventsLogger.getPushNotificationsRegistrationId());
    }

    @Test
    public void testPublishInstall() throws Exception {
        GraphRequest mockRequest = mock(GraphRequest.class);
        PowerMockito.whenNew(GraphRequest.class).withAnyArguments().thenReturn(mockRequest);
        mockStatic(AttributionIdentifiers.class);
        when(AttributionIdentifiers.getAttributionIdentifiers(any(Context.class))).thenReturn(null);
        String expectedEvent = "MOBILE_APP_INSTALL";
        String expectedUrl = "mockAppID/activities";
        final ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);

        FacebookSdk.publishInstallAsync(FacebookSdk.getApplicationContext(), "mockAppID");

        verifyNew(GraphRequest.class).withArguments(
                Matchers.isNull(),
                Matchers.eq(expectedUrl),
                Matchers.isNull(),
                Matchers.eq(HttpMethod.POST),
                Matchers.isNull()
        );
        verify(mockRequest).setGraphObject(captor.capture());
        assertEquals(expectedEvent, captor.getValue().getString("event"));
    }

}
