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

package com.facebook.appevents.restrictivedatafilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.FetchedAppSettings;
import com.facebook.internal.FetchedAppSettingsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.BDDMockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

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
    PowerMockito.spy(RestrictiveDataManager.class);
    Whitebox.setInternalState(FacebookSdk.class, "sdkInitialized", true);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    Whitebox.setInternalState(RestrictiveDataManager.class, "enabled", true);
  }

  private static Map<String, String> getEventParam() {
    Map<String, String> eventParam = new HashMap<>();

    eventParam.put("key1", "val1");
    eventParam.put("key2", "val2");
    eventParam.put("last_name", "ln");
    eventParam.put("first_name", "fn");

    return eventParam;
  }

  @Test
  public void testEnable() {
    Map<String, String> expectedParam = new HashMap<>();
    expectedParam.put("last_name", "0");
    expectedParam.put("first_name", "0");
    expectedParam.put("first name", "0");

    Map<String, String> expectedParamDetail = new HashMap<>();
    expectedParamDetail.put("Quantity", "1");
    expectedParamDetail.put("Product name", "Coffee");
    expectedParamDetail.put("Price", "10");

    JSONObject jsonObject = new JSONObject();
    try {
      JSONObject jsonObject1 = new JSONObject();
      jsonObject1.put("restrictive_param", new JSONObject(expectedParam));
      JSONObject jsonObject2 = new JSONObject();
      jsonObject2.put("restrictive_param", new JSONObject(expectedParamDetail));
      jsonObject2.put("process_event_name", false);

      jsonObject.put("fb_test_event", jsonObject1);
      jsonObject.put("manual_initiated_checkout", jsonObject2);
    } catch (JSONException je) {
      /* No opt */
    }
    String mockResponse = jsonObject.toString();

    FetchedAppSettings fetchedAppSettings = mock(FetchedAppSettings.class);
    when(fetchedAppSettings.getRestrictiveDataSetting()).thenReturn(mockResponse);
    PowerMockito.mockStatic(FetchedAppSettingsManager.class);
    BDDMockito.given(
            FetchedAppSettingsManager.queryAppSettings(nullable(String.class), anyBoolean()))
        .willReturn(fetchedAppSettings);

    List<RestrictiveDataManager.RestrictiveParamFilter> restrictiveParamFilters =
        Whitebox.getInternalState(RestrictiveDataManager.class, "restrictiveParamFilters");
    restrictiveParamFilters.clear();

    Set<String> restrictedEvents =
        Whitebox.getInternalState(RestrictiveDataManager.class, "restrictedEvents");
    restrictedEvents.clear();

    RestrictiveDataManager.enable();

    assertEquals(2, restrictiveParamFilters.size());
    RestrictiveDataManager.RestrictiveParamFilter rule = restrictiveParamFilters.get(0);
    assertEquals("fb_test_event", rule.eventName);
    assertEquals(expectedParam, rule.restrictiveParams);
    assertEquals(1, restrictedEvents.size());
    assertTrue(restrictedEvents.contains("manual_initiated_checkout"));
  }

  @Test
  public void testProcessParameters() {
    List<RestrictiveDataManager.RestrictiveParamFilter> mockRestrictiveParams = new ArrayList<>();
    Map<String, String> mockParam = new HashMap<>();
    mockParam.put("last_name", "0");
    mockParam.put("first_name", "1");

    mockRestrictiveParams.add(
        new RestrictiveDataManager.RestrictiveParamFilter("fb_restrictive_event", mockParam));
    Whitebox.setInternalState(
        RestrictiveDataManager.class, "restrictiveParamFilters", mockRestrictiveParams);

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
  }

  @Test
  public void testProcessEvent() throws Exception {
    String input = "name_should_be_replaced";
    PowerMockito.doReturn(true).when(RestrictiveDataManager.class, "isRestrictedEvent", input);
    String output = RestrictiveDataManager.processEvent(input);
    assertEquals(output, "_removed_");
  }
}
