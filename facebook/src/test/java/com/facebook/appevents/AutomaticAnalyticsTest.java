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

import android.app.Activity;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.internal.ActivityLifecycleTracker;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.internal.FetchedAppSettings;

import org.json.JSONObject;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import org.junit.Test;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@PrepareForTest({ ActivityLifecycleTracker.class, FacebookSdk.class, FetchedAppSettingsManager.class,
        Executors.class})
public class AutomaticAnalyticsTest extends FacebookPowerMockTestCase {

    @Test
    public void testAutomaticLoggingEnabledServerConfiguration() throws Exception {
        JSONObject settingsJSON = new JSONObject();
        settingsJSON.put("app_events_feature_bitmask", "0");
        FetchedAppSettings settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
        assertEquals(settings.getAutomaticLoggingEnabled(),false);

        settingsJSON.put("app_events_feature_bitmask", "7");
        settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
        assertEquals(settings.getAutomaticLoggingEnabled(),false);

        settingsJSON.put("app_events_feature_bitmask", "23");
        settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
        assertEquals(settings.getAutomaticLoggingEnabled(),false);

        settingsJSON.put("app_events_feature_bitmask", "8");
        settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
        assertEquals(settings.getAutomaticLoggingEnabled(),true);

        settingsJSON.put("app_events_feature_bitmask", "9");
        settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", settingsJSON);
        assertEquals(settings.getAutomaticLoggingEnabled(),true);

        JSONObject noBitmaskFieldSettings = new JSONObject();
        settings = Whitebox.invokeMethod(
                FetchedAppSettingsManager.class, "parseAppSettingsFromJSON", "123", noBitmaskFieldSettings);
        assertEquals(settings.getAutomaticLoggingEnabled(),false);
    }

    @Test
    public void testAutoTrackingWhenInitialized() throws Exception {
        stub(method(FetchedAppSettingsManager.class, "loadAppSettingsAsync")).toReturn(null);

        ScheduledExecutorService mockExecutor = new FacebookPowerMockTestCase.FacebookSerialThreadPoolExecutor(1);
        PowerMockito.spy(Executors.class);
        PowerMockito.when(Executors.newSingleThreadExecutor()).thenReturn(mockExecutor);
        PowerMockito.mockStatic(ActivityLifecycleTracker.class);

        FacebookSdk.setApplicationId("1234");
        FacebookSdk.sdkInitialize(RuntimeEnvironment.application);

        Activity activity =
                Robolectric.buildActivity(Activity.class).create().start().resume().visible().get();

        PowerMockito.doCallRealMethod().when(ActivityLifecycleTracker.class, "onActivityCreated",
                Matchers.any(Activity.class));
        PowerMockito.doCallRealMethod().when(ActivityLifecycleTracker.class, "onActivityResumed",
                Matchers.any(Activity.class));
    }

}
