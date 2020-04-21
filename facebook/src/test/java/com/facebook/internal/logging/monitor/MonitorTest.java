/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal.logging.monitor;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.HashMap;
import java.util.Map;

import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_APP_ID;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_DEFAULT_SAMPLING_RATE;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_EVENT_NAME;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_SAMPLING_RATE;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_START;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;

@PrepareForTest({
        FacebookSdk.class,
        Monitor.class,
})
public class MonitorTest extends FacebookPowerMockTestCase {

    @Mock
    private MonitorLoggingManager mockMonitorLoggingManager;
    private MonitorLog monitorLog;

    private static final Boolean mockIsEnabled = false;
    private static final Map<String, Integer> mockSamplingRatesMap = new HashMap<>();

    @Before
    public void init() {
        mockStatic(Monitor.class);
        spy(FacebookSdk.class);
        PowerMockito.when(FacebookSdk.isInitialized()).thenReturn(true);
        PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(
                RuntimeEnvironment.application);
        PowerMockito.when(FacebookSdk.getApplicationId()).thenReturn(TEST_APP_ID);

        monitorLog = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);

        spy(Monitor.class);
        ReflectionHelpers.setStaticField(Monitor.class, "samplingRatesMap", mockSamplingRatesMap);
        ReflectionHelpers.setStaticField(Monitor.class, "isEnabled", mockIsEnabled);
        ReflectionHelpers.setStaticField(Monitor.class, "monitorLoggingManager", mockMonitorLoggingManager);
    }

    @Test
    public void testEnable() {
        Assert.assertFalse(Monitor.isEnabled());
        Monitor.enable();

        Assert.assertTrue(Monitor.isEnabled());
        verify(mockMonitorLoggingManager).flushLoggingStore();

        PowerMockito.verifyStatic();
        Monitor.loadSamplingRatesMapAsync();
    }

    @Test
    public void testUpdateSamplingRatesMap() throws JSONException {
        Map<String, Integer> expectedSamplingRatesMap = new HashMap<>();
        expectedSamplingRatesMap.put(TEST_EVENT_NAME, TEST_SAMPLING_RATE);

        String mockResponse = "{\n" +
                "  \"monitoring_config\": {\n" +
                "    \"sample_rates\": [\n" +
                "      {\n" +
                "        \"key\": \"default\",\n" +
                "        \"value\":" + TEST_DEFAULT_SAMPLING_RATE + "\n" +
                "      },\n" +
                "      {\n" +
                "        \"key\": \"" + TEST_EVENT_NAME + "\",\n" +
                "        \"value\":" + TEST_SAMPLING_RATE + "\n" +
                "      }\n" +
                "    ]\n" +
                "  },\n" +
                "  \"id\": \"123456\"\n" +
                "}";
        JSONObject responseJSON = new JSONObject(mockResponse);
        Map<String, Integer> mockSamplingRatesMap = new HashMap<>();
        ReflectionHelpers.setStaticField(Monitor.class, "samplingRatesMap", mockSamplingRatesMap);
        Integer mockDefaultSamplingRate = 10;
        ReflectionHelpers.setStaticField(Monitor.class, "defaultSamplingRate", mockDefaultSamplingRate);
        Monitor.updateSamplingRateMap(responseJSON);
        Assert.assertEquals(expectedSamplingRatesMap, mockSamplingRatesMap);
        Assert.assertEquals(TEST_DEFAULT_SAMPLING_RATE, Monitor.getDefaultSamplingRate());
    }

    @Test
    public void testIsSampledForEventNotInSamplingRatesMap() {
        Integer mockDefaultSamplingRate = 1;
        ReflectionHelpers.setStaticField(Monitor.class, "defaultSamplingRate", mockDefaultSamplingRate);
        Assert.assertTrue(Monitor.isSampled(monitorLog));
    }

    @Test
    public void testAddLog() {
        Monitor.enable();
        Monitor.addLog(monitorLog);
        verify(mockMonitorLoggingManager).addLog(monitorLog);
    }
}
