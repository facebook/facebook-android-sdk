/**
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

package com.facebook.appevents.restrictivedatafilter;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEvent;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PrepareForTest({
        RestrictiveDataManager.class,
        FacebookSdk.class,
        FetchedAppSettings.class,
        FetchedAppSettingsManager.class,
})

public class RestrictiveDataManagerTest extends FacebookPowerMockTestCase {

    private final Executor mockExecutor = new FacebookSerialExecutor();

    @Before
    @Override
    public void setup() {
        super.setup();
        Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
        Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
        Whitebox.setInternalState(RestrictiveDataManager.class, "enabled", true);
    }

    private static AppEvent getAppEvent(String eventName) throws JSONException {
        return new AppEvent("", eventName, 0., null, false, false, null);
    }

    private static Map<String, String> getEventParam() {
        Map<String, String> eventParam = new HashMap<>();

        eventParam.put("key1", "val1");
        eventParam.put("key2", "val2");
        eventParam.put("last_name", "ln");
        eventParam.put("first_name", "fn");
        eventParam.put("ssn", "val3");

        return eventParam;
    }

    @Test
    public void testEnable() {
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
        try {
            JSONObject jsonObject1 = new JSONObject();
            JSONObject jsonObject2 = new JSONObject();
            jsonObject1.put("restrictive_param", new JSONObject(expectedParam));
            jsonObject2.put("deprecated_param", new JSONArray(expectDeprecatedParam));
            jsonObject.put("fb_deprecated_event", new JSONObject(map));
            jsonObject.put("fb_test_event", jsonObject1);
            jsonObject.put("fb_test_deprecated_event", jsonObject2);
        } catch (JSONException je) {
            /* No opt */
        }
        String mockResponse = jsonObject.toString();

        FetchedAppSettings fetchedAppSettings = mock(FetchedAppSettings.class);
        when(fetchedAppSettings.getRestrictiveDataSetting()).thenReturn(mockResponse);
        PowerMockito.mockStatic(FetchedAppSettingsManager.class);
        BDDMockito.given(FetchedAppSettingsManager.queryAppSettings(Matchers.anyString(),
                Matchers.anyBoolean())).willReturn(fetchedAppSettings);

        List<RestrictiveDataManager.RestrictiveParam> restrictiveParams =
                Whitebox.getInternalState(RestrictiveDataManager.class, "restrictiveParams");
        restrictiveParams.clear();

        Set<String> restrictiveEvents =
                Whitebox.getInternalState(RestrictiveDataManager.class, "restrictiveEvents");

        RestrictiveDataManager.enable();

        assertEquals(2, restrictiveParams.size());
        RestrictiveDataManager.RestrictiveParam rule = restrictiveParams.get(0);
        assertEquals("fb_test_event", rule.eventName);
        assertEquals(expectedParam, rule.restrictiveParams);
        assertEquals(1, restrictiveEvents.size());
        assertTrue(restrictiveEvents.contains("fb_deprecated_event"));

        RestrictiveDataManager.RestrictiveParam real = restrictiveParams.get(1);
        assertEquals("fb_test_deprecated_event", real.eventName);
        assertEquals(expectDeprecatedParam, real.deprecatedParams);
    }

    @Test
    public void testProcessEvents() throws JSONException {
        Set<String> restrictiveEvents = new HashSet<>();
        restrictiveEvents.add("fb_deprecated_event");
        Whitebox.setInternalState(
                RestrictiveDataManager.class, "restrictiveEvents", restrictiveEvents);
        List<AppEvent> mockAppEvents = new ArrayList<>();
        mockAppEvents.add(getAppEvent("fb_mobile_install"));
        mockAppEvents.add(getAppEvent("fb_deprecated_event"));
        mockAppEvents.add(getAppEvent("fb_sdk_initialized"));
        String[] expectedEventNames = new String[]{"fb_mobile_install", "fb_sdk_initialized"};

        RestrictiveDataManager.processEvents(mockAppEvents);

        assertEquals(2, mockAppEvents.size());
        for (int i = 0; i < expectedEventNames.length; i++) {
            assertEquals(expectedEventNames[i], mockAppEvents.get(i).getName());
        }
    }

    @Test
    public void testProcessParameters() {
        List<RestrictiveDataManager.RestrictiveParam> mockRestrictiveParams = new ArrayList<>();
        Map<String, String> mockParam = new HashMap<>();
        List<String> mockDeprecatedParam = new ArrayList<>();
        mockParam.put("last_name", "0");
        mockParam.put("first_name", "1");
        mockDeprecatedParam.add("ssn");

        mockRestrictiveParams.add(
                new RestrictiveDataManager.RestrictiveParam("fb_restrictive_event",
                        mockParam, mockDeprecatedParam));
        Whitebox.setInternalState(
                RestrictiveDataManager.class, "restrictiveParams", mockRestrictiveParams);

        Map<String, String> mockEventParam = getEventParam();
        RestrictiveDataManager.processParameters(mockEventParam, "fb_test_event");
        assertEquals(getEventParam(), mockEventParam);

        mockEventParam = getEventParam();
        RestrictiveDataManager.processParameters(mockEventParam, "fb_restrictive_event");
        assertTrue(mockEventParam.containsKey("key1"));
        assertTrue(mockEventParam.containsKey("key2"));
        assertTrue(mockEventParam.containsKey("_restrictedParams"));
        assertFalse(mockEventParam.containsKey("last_name"));
        assertFalse(mockEventParam.containsKey("first_name"));
        assertFalse(mockEventParam.containsKey("ssn"));
    }
}
