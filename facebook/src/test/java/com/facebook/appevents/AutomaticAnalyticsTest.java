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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.internal.AttributionIdentifiers;
import com.facebook.internal.FeatureManager;
import com.facebook.internal.FetchedAppGateKeepersManager;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

@PrepareForTest({
  AppEventQueue.class,
  AppEventStore.class,
  AttributionIdentifiers.class,
  ActivityLifecycleTracker.class,
  FacebookSdk.class,
  FeatureManager.class,
  FetchedAppSettingsManager.class,
  FetchedAppGateKeepersManager.class,
  Executors.class,
  GraphRequest.class,
})
public class AutomaticAnalyticsTest extends FacebookPowerMockTestCase {

  @Test
  public void testAutomaticLoggingEnabledServerConfiguration() throws Exception {
    JSONObject settingsJSON = new JSONObject();
    settingsJSON.put("app_events_feature_bitmask", "0");
    FetchedAppSettings settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
    assertFalse(settings.getAutomaticLoggingEnabled());

    settingsJSON.put("app_events_feature_bitmask", "7");
    settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
    assertFalse(settings.getAutomaticLoggingEnabled());

    settingsJSON.put("app_events_feature_bitmask", "23");
    settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
    assertFalse(settings.getAutomaticLoggingEnabled());

    settingsJSON.put("app_events_feature_bitmask", "8");
    settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
    assertTrue(settings.getAutomaticLoggingEnabled());

    settingsJSON.put("app_events_feature_bitmask", "9");
    settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
    assertTrue(settings.getAutomaticLoggingEnabled());

    JSONObject noBitmaskFieldSettings = new JSONObject();
    settings =
        Whitebox.invokeMethod(
            FetchedAppSettingsManager.class,
            "parseAppSettingsFromJSON",
            "123",
            noBitmaskFieldSettings);
    assertFalse(settings.getAutomaticLoggingEnabled());
  }

  @Test
  public void testAutoTrackingWhenInitialized() throws Exception {
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
    Whitebox.setInternalState(FacebookSdk.class, "applicationId", "1234");
    Whitebox.setInternalState(
        FacebookSdk.class, "applicationContext", RuntimeEnvironment.application);
    ScheduledExecutorService mockExecutor =
        new FacebookPowerMockTestCase.FacebookSerialThreadPoolExecutor(1);
    Whitebox.setInternalState(ActivityLifecycleTracker.class, "singleThreadExecutor", mockExecutor);

    PowerMockito.mockStatic(FetchedAppSettingsManager.class);

    PowerMockito.mockStatic(ActivityLifecycleTracker.class);

    Activity activity =
        Robolectric.buildActivity(Activity.class).create().start().resume().visible().get();

    PowerMockito.doCallRealMethod()
        .when(ActivityLifecycleTracker.class, "onActivityResumed", activity);
  }

  @Test
  public void testLogAndSendAppEvent() throws Exception {
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
    Whitebox.setInternalState(FacebookSdk.class, "applicationId", "1234");
    Whitebox.setInternalState(
        FacebookSdk.class, "applicationContext", RuntimeEnvironment.application);
    ScheduledExecutorService mockExecutor =
        new FacebookPowerMockTestCase.FacebookSerialThreadPoolExecutor(1);
    Whitebox.setInternalState(AppEventQueue.class, "singleThreadExecutor", mockExecutor);
    // Mock App Settings to avoid App Setting request
    PowerMockito.mockStatic(FetchedAppSettingsManager.class);

    // Disable Gatekeeper
    PowerMockito.mockStatic(FetchedAppGateKeepersManager.class);
    PowerMockito.when(
            FetchedAppGateKeepersManager.getGateKeeperForKey(
                Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
        .thenReturn(false);

    // Mock FeatureManger to avoid GK request
    PowerMockito.mockStatic(FeatureManager.class);

    // Stub mock IDs for AttributionIdentifiers
    String mockAdvertiserID = "fb_mock_advertiserID";
    String mockAttributionID = "fb_mock_attributionID";
    AttributionIdentifiers mockIdentifiers = PowerMockito.mock(AttributionIdentifiers.class);
    PowerMockito.when(mockIdentifiers.getAndroidAdvertiserId()).thenReturn(mockAdvertiserID);
    PowerMockito.when(mockIdentifiers.getAttributionId()).thenReturn(mockAttributionID);
    PowerMockito.mockStatic(AttributionIdentifiers.class);
    PowerMockito.when(AttributionIdentifiers.getAttributionIdentifiers(Matchers.any(Context.class)))
        .thenReturn(mockIdentifiers);

    // Mock App Event Store
    PowerMockito.mockStatic(AppEventStore.class);
    PowerMockito.when(AppEventStore.readAndClearStore()).thenReturn(null);

    // Mock graph request
    GraphRequest mockRequest = PowerMockito.mock(GraphRequest.class);
    PowerMockito.whenNew(GraphRequest.class).withAnyArguments().thenReturn(mockRequest);
    PowerMockito.spy(AppEventQueue.class);
    PowerMockito.doReturn(mockRequest)
        .when(
            AppEventQueue.class,
            "buildRequestForSession",
            Matchers.any(),
            Matchers.any(),
            Matchers.anyBoolean(),
            Matchers.any());

    AppEventsLoggerImpl loggerImpl =
        new AppEventsLoggerImpl(RuntimeEnvironment.application, "1234", null);
    loggerImpl.logEvent("fb_mock_event", 1.0, new Bundle(), true, null);
    loggerImpl.flush();
    Thread.sleep(200);

    Mockito.verify(mockRequest).executeAndWait();
  }
}
