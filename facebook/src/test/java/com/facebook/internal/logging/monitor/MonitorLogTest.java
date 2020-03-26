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

import android.content.Context;
import android.os.Build;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import static com.facebook.internal.logging.monitor.MonitorEvent.FB_CORE_STARTUP;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.*;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@PrepareForTest({FacebookSdk.class})
public class MonitorLogTest extends FacebookPowerMockTestCase {

    private static final int INVALID_TIME = -1;
    private static final long INVALID_TIME_LONG = -1;
    private static final String SAMPLE_APP_FBLOGINSAMPLE = "com.facebook.fbloginsample";
    private static final long TIME_START = 1000;

    @Before
    public void init() {
        PowerMockito.spy(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(
                RuntimeEnvironment.application);
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 15);
    }

    @Test
    public void testMonitorLogBuilder() {
        MonitorLog log = new MonitorLog.LogBuilder(FB_CORE_STARTUP)
                .timeStart(TIME_START)
                .timeSpent(TEST_TIME_SPENT)
                .build();

        Assert.assertNotNull(log.getDeviceOSVersion());
        Assert.assertNotNull(log.getDeviceModel());
        Assert.assertEquals(TIME_START, log.getTimeStart());
        Assert.assertEquals(FB_CORE_STARTUP, log.getEvent());
        Assert.assertEquals(TIME_START, log.getTimeStart());
        Assert.assertEquals(TEST_TIME_SPENT, log.getTimeSpent());
        Assert.assertNull(log.getSampleAppInformation());
    }

    @Test
    public void testMonitorLogBuilderWithInvalidAttribute() {
        MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(
                TEST_INVALID_TIME_START,
                TEST_INVALID_TIME_SPENT);

        Assert.assertEquals(INVALID_TIME, log.getTimeSpent());
        Assert.assertEquals(INVALID_TIME_LONG, log.getTimeStart());
    }

    @Test
    public void testConvertToJSONObject() throws JSONException {
        String DEVICE_OS_VERSION = Build.VERSION.RELEASE;
        String DEVICE_MODEL = Build.MODEL;
        MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TIME_START);
        Assert.assertEquals(FB_CORE_STARTUP, log.getEvent());
        JSONObject json = log.convertToJSONObject();

        Assert.assertEquals(TIME_START, json.getLong(PARAM_TIME_START));
        Assert.assertEquals(FB_CORE_STARTUP.getName(), json.getString(PARAM_EVENT_NAME));
        Assert.assertEquals(TEST_TIME_SPENT, json.getInt(PARAM_TIME_SPENT));
        Assert.assertEquals(DEVICE_OS_VERSION, json.getString(PARAM_DEVICE_OS_VERSION));
        Assert.assertEquals(DEVICE_MODEL, json.getString(PARAM_DEVICE_MODEL));

        try {
            json.getString(PARAM_SAMPLE_APP_INFO);
        } catch (Exception ex) {
            assertTrue(ex instanceof JSONException);
        }
    }

    @Test
    public void testConvertToJSONObjectCallFromSampleApp() throws JSONException {
        Context mockApplicationContext = mock(Context.class);
        when(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext);
        when(mockApplicationContext.getPackageName()).thenReturn(SAMPLE_APP_FBLOGINSAMPLE);
        MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TIME_START);
        JSONObject json = log.convertToJSONObject();
        Assert.assertEquals(SAMPLE_APP_FBLOGINSAMPLE, json.getString(PARAM_SAMPLE_APP_INFO));
    }
}
