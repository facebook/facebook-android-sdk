/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use, copy, modify,
 * and distribute this software in source code or binary form for use in connection with the web
 * services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of this software is
 * subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be included in all copies
 * or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.eventdeactivation;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import com.facebook.appevents.AppEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.mockito.Matchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PrepareForTest({
        EventDeactivationManager.class,
        FacebookSdk.class,
        FetchedAppSettings.class,
        FetchedAppSettingsManager.class,
})

public class EventDeactivationManagerTest extends FacebookPowerMockTestCase {

    private final Executor mockExecutor = new FacebookSerialExecutor();

    @Before
    @Override
    public void setup() {
        super.setup();
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
        Whitebox.setInternalState(EventDeactivationManager.class, "enabled", true);
    }

    private static AppEvent getAppEvent(String eventName) throws JSONException {
        return new AppEvent("", eventName, 0., null, false, false, null);
    }

    private static Map<String, String> getEventParam() {
        Map<String, String> eventParam = new HashMap<>();

        eventParam.put("last_name", "ln");
        eventParam.put("first_name", "fn");
        eventParam.put("ssn", "val3");

        return eventParam;
    }

    @Test
    public void testEnable() throws JSONException {
        Map<String, String> expectedParam = new HashMap<>();
        expectedParam.put("last_name", "0");
        expectedParam.put("first_name", "0");
        expectedParam.put("first name", "0");

        Map<String, Boolean> map = new HashMap<>();
        map.put("is_deprecated_event", true);
        List<String> expectDeprecatedParam = new ArrayList<>();
        expectDeprecatedParam.add("ssn");
        expectDeprecatedParam.add("mid_name");
        JSONObject jsonObject = new JSONObject();

        JSONObject jsonObject1 = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        jsonObject1.put("restrictive_param", new JSONObject(expectedParam));
        jsonObject2.put("deprecated_param", new JSONArray(expectDeprecatedParam));
        jsonObject.put("fb_deprecated_event", new JSONObject(map));
        jsonObject.put("fb_test_event", jsonObject1);
        jsonObject.put("fb_test_deprecated_event", jsonObject2);

        String mockResponse = jsonObject.toString();

        FetchedAppSettings fetchedAppSettings = mock(FetchedAppSettings.class);
        when(fetchedAppSettings.getRestrictiveDataSetting()).thenReturn(mockResponse);
        PowerMockito.mockStatic(FetchedAppSettingsManager.class);
        BDDMockito.given(FetchedAppSettingsManager.queryAppSettings(Matchers.anyString(),
                Matchers.anyBoolean())).willReturn(fetchedAppSettings);

        EventDeactivationManager.enable();
        List<EventDeactivationManager.DeprecatedParamFilter> deprecatedParams =
                Whitebox.getInternalState(EventDeactivationManager.class, "deprecatedParamFilters");
        Set<String> deprecatedEvents =
                Whitebox.getInternalState(EventDeactivationManager.class, "deprecatedEvents");

        assertThat(deprecatedParams.size()).isEqualTo(2);
        EventDeactivationManager.DeprecatedParamFilter rule = deprecatedParams.get(0);
        assertThat(rule.eventName).isEqualTo("fb_test_event");
        assertThat(deprecatedEvents.size()).isEqualTo(1);
        assertThat(deprecatedEvents.contains("fb_deprecated_event")).isEqualTo(true);

        EventDeactivationManager.DeprecatedParamFilter real = deprecatedParams.get(1);
        assertThat(real.eventName).isEqualTo("fb_test_deprecated_event");
        assertThat(real.deprecateParams).isEqualTo(expectDeprecatedParam);
    }

    @Test
    public void testProcessEvents() throws JSONException {
        Set<String> deprecatedEvents = new HashSet<>();
        deprecatedEvents.add("fb_deprecated_event");
        Whitebox.setInternalState(
                EventDeactivationManager.class, "deprecatedEvents", deprecatedEvents);
        List<AppEvent> mockAppEvents = new ArrayList<>();
        mockAppEvents.add(getAppEvent("fb_mobile_install"));
        mockAppEvents.add(getAppEvent("fb_deprecated_event"));
        mockAppEvents.add(getAppEvent("fb_sdk_initialized"));
        String[] expectedEventNames = new String[]{"fb_mobile_install", "fb_sdk_initialized"};

        EventDeactivationManager.processEvents(mockAppEvents);

        assertThat(mockAppEvents.size()).isEqualTo(2);
        for (int i = 0; i < expectedEventNames.length; i++) {
            assertThat(mockAppEvents.get(i).getName()).isEqualTo(expectedEventNames[i]);
        }
    }

    @Test
    public void testProcessDeprecatedParameters() {
        List<EventDeactivationManager.DeprecatedParamFilter> mockDeprecatedParams = new ArrayList<>();
        Map<String, String> mockParameters = new HashMap<>();
        mockParameters.put("last_name", "0");
        mockParameters.put("first_name", "1");
        List<String> mockDeprecatedParam = new ArrayList<>();
        mockDeprecatedParam.add("ssn");

        mockDeprecatedParams.add(new EventDeactivationManager
                        .DeprecatedParamFilter("fb_restrictive_event", mockDeprecatedParam));
        Whitebox.setInternalState(
                EventDeactivationManager.class, "deprecatedParamFilters", mockDeprecatedParams);

        Map<String, String> mockEventParam = getEventParam();
        EventDeactivationManager.processDeprecatedParameters(mockEventParam, "fb_test_event");
        assertThat(mockEventParam).isEqualTo(getEventParam());

        mockEventParam = getEventParam();
        EventDeactivationManager.processDeprecatedParameters(mockEventParam,
                "fb_restrictive_event");
        assertThat(mockEventParam.containsKey("last_name")).isEqualTo(true);
        assertThat(mockEventParam.containsKey("first_name")).isEqualTo(true);
        assertThat(mockEventParam.containsKey("ssn")).isEqualTo(false);
    }
}
