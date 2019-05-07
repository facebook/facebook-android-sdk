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

import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.TestUtils;
import com.facebook.appevents.internal.AppEventUtility;
import com.facebook.appevents.internal.AppEventsLoggerUtility;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@PrepareForTest({
        AppEventUtility.class,
        AppEventsLogger.class,
        AppEventsLoggerImpl.class,
        FacebookSdk.class
})
public class AppEventsLoggerTest extends FacebookPowerMockTestCase {

    private final String TAG = AppEventsLoggerTest.class.getCanonicalName();

    private final Executor mockExecutor = new FacebookSerialExecutor();

    private final String mockAppID = "fb_mock_id";

    private AppEventsLoggerImpl logger;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        PowerMockito.spy(FacebookSdk.class);
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        Whitebox.setInternalState(FacebookSdk.class, "applicationId", mockAppID);
        Whitebox.setInternalState(
                FacebookSdk.class, "applicationContext", RuntimeEnvironment.application);
        Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);

        try {
            logger = PowerMockito.mock(AppEventsLoggerImpl.class);
            PowerMockito.whenNew(AppEventsLoggerImpl.class).withAnyArguments().thenReturn(logger);
            // Disable AppEventUtility.isMainThread since executor now runs in main thread
            PowerMockito.spy(AppEventUtility.class);
            PowerMockito.doReturn(false).when(AppEventUtility.class, "isMainThread");
            PowerMockito.spy(AppEventsLoggerImpl.class);
            PowerMockito.doReturn(mockExecutor).when(
                    AppEventsLoggerImpl.class, "getAnalyticsExecutor");
        } catch (Exception e) {
            Log.e(TAG, "Fail to set up AppEventsLoggerTest: " + e.getMessage());
        }
    }

    @Test
    public void testAppEventsLoggerLogFunctions() throws Exception {
        final String mockEventName = "fb_mock_event";
        final Bundle mockPayload = new Bundle();
        final String mockAction = "fb_mock_action";
        final BigDecimal mockVal = new BigDecimal(1.0);
        final Currency mockCurrency = Currency.getInstance(Locale.US);

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName);
        Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName);

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, 1);
        Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName, 1);

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, null);
        Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName, null);

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logEvent(mockEventName, 1, null);
        Mockito.verify(logger, Mockito.times(1)).logEvent(mockEventName, 1, null);

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logPushNotificationOpen(mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logPushNotificationOpen(
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)),
                Matchers.isNull(String.class)
        );

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logPushNotificationOpen(
                mockPayload, mockAction);
        Mockito.verify(logger, Mockito.times(1)).logPushNotificationOpen(
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)),
                Matchers.eq(mockAction)
        );

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logProductItem(
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
                mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logProductItem(
                Matchers.eq("F40CEE4E-471E-45DB-8541-1526043F4B21"),
                Matchers.eq(AppEventsLogger.ProductAvailability.IN_STOCK),
                Matchers.eq(AppEventsLogger.ProductCondition.NEW),
                Matchers.eq("description"),
                Matchers.eq("https://www.sample.com"),
                Matchers.eq("https://www.link.com"),
                Matchers.eq("title"),
                Matchers.eq(mockVal),
                Matchers.eq(mockCurrency),
                Matchers.eq("GTIN"),
                Matchers.eq("MPN"),
                Matchers.eq("BRAND"),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload))
        );

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logPurchase(
                mockVal, mockCurrency);
        Mockito.verify(logger, Mockito.times(1)).logPurchase(
                Matchers.eq(mockVal), Matchers.eq(mockCurrency));

        AppEventsLogger.newLogger(RuntimeEnvironment.application).logPurchase(
                mockVal, mockCurrency, mockPayload);
        Mockito.verify(logger, Mockito.times(1)).logPurchase(
                Matchers.eq(mockVal),
                Matchers.eq(mockCurrency),
                Matchers.argThat(new AppEventTestUtilities.BundleMatcher(mockPayload)));
    }

    @Test
    public void testAutoLogAppEventsEnabled() throws Exception {
        Whitebox.setInternalState(
                AppEventsLoggerImpl.class,
                "backgroundExecutor",
                PowerMockito.mock(ScheduledThreadPoolExecutor.class));
        PowerMockito.doReturn(true).when(
                FacebookSdk.class, "getAutoLogAppEventsEnabled");

        AppEventsLogger.initializeLib(FacebookSdk.getApplicationContext(), mockAppID);

        PowerMockito.verifyNew(AppEventsLoggerImpl.class).withArguments(
                Matchers.any(),
                Matchers.eq(mockAppID),
                Matchers.any());
    }

    @Test
    public void testAutoLogAppEventsDisabled() throws Exception {
        Whitebox.setInternalState(
                AppEventsLoggerImpl.class,
                "backgroundExecutor",
                PowerMockito.mock(ScheduledThreadPoolExecutor.class));
        PowerMockito.doReturn(false).when(FacebookSdk.class, "getAutoLogAppEventsEnabled");

        AppEventsLogger.initializeLib(FacebookSdk.getApplicationContext(), mockAppID);

        PowerMockito.verifyNew(AppEventsLoggerImpl.class, Mockito.never()).withArguments(
                Matchers.any(),
                Matchers.any(),
                Matchers.any());
    }

    @Test
    public void testSetAndClearUserData() throws JSONException {
        AppEventsLogger.setUserData(
                "em@gmail.com", "fn", "ln", "123", null, null, null, null, null, null);
        JSONObject actualUserData = new JSONObject(AppEventsLogger.getUserData());
        JSONObject expectedUserData = new JSONObject("{\"ln\":\"e545c2c24e6463d7c4fe3829940627b226c0b9be7a8c7dbe964768da48f1ab9d\",\"ph\":\"a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3\",\"em\":\"5f341666fb1ce60d716e4afc302c8658f09412290aa2ca8bc623861f452f9d33\",\"fn\":\"0f1e18bb4143dc4be22e61ea4deb0491c2bf7018c6504ad631038aed5ca4a0ca\"}");
        TestUtils.assertEquals(expectedUserData, actualUserData);

        AppEventsLogger.clearUserData();
        Assert.assertTrue(AppEventsLogger.getUserData().isEmpty());
    }

    @Test
    public void testSetAndClearUserID() {
        String userID = "12345678";
        AppEventsLogger.setUserID(userID);
        Assert.assertEquals(AppEventsLogger.getUserID(), userID);
        AppEventsLogger.clearUserID();
        Assert.assertNull(AppEventsLogger.getUserID());
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
        Assert.assertEquals(jsonObject.getString("app_user_id"), userID);
    }

    @Test
    public void testActivateApp() throws Exception {
        Application mockApplication = PowerMockito.mock(Application.class);
        PowerMockito.doCallRealMethod().when(
                FacebookSdk.class,
                "publishInstallAsync",
                Matchers.any(Context.class), Matchers.anyString()
        );
        AppEventsLogger.activateApp(mockApplication);
        Mockito.verify(mockApplication, Mockito.times(1)).registerActivityLifecycleCallbacks(
                Matchers.any(Application.ActivityLifecycleCallbacks.class)
        );
    }

    @Test
    public void testSetPushNotificationsRegistrationId() throws Exception {
        String mockNotificationId = "123";
        PowerMockito.doCallRealMethod().when(
                AppEventsLoggerImpl.class,
                "setPushNotificationsRegistrationId",
                mockNotificationId
        );
        AppEventsLogger.setPushNotificationsRegistrationId(mockNotificationId);
    }

}
