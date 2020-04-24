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
import com.facebook.internal.logging.LogCategory;
import com.facebook.internal.logging.LogEvent;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.Executor;

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.*;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.*;
import static org.mockito.Mockito.when;

@PrepareForTest({FacebookSdk.class})
public class MonitorLogTest extends FacebookPowerMockTestCase {

    private final Executor mockExecutor = new FacebookSerialExecutor();
    @Before
    public void init() {
        PowerMockito.spy(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
        PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(
                RuntimeEnvironment.application);
    }

    @Test
    public void testMonitorLogBuilder() {
        MonitorLog log = new MonitorLog.LogBuilder(TEST_LOG_EVENT)
                .timeStart(TEST_TIME_START)
                .timeSpent(TEST_TIME_SPENT)
                .build();

        Assert.assertEquals(TEST_CATEGORY, log.getLogCategory());
        Assert.assertEquals(TEST_EVENT_NAME, log.getEventName());
        Assert.assertEquals(TEST_TIME_START, log.getTimeStart());
        Assert.assertEquals(TEST_TIME_SPENT, log.getTimeSpent());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMonitorLogBuilderWithInvalidEventNameForPerformance() {
        LogEvent invalidEvent = new LogEvent("invalid", LogCategory.PERFORMANCE);
        new MonitorLog.LogBuilder(invalidEvent)
                .timeStart(TEST_TIME_START)
                .timeSpent(TEST_TIME_SPENT)
                .build();
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
        MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
        JSONObject json = log.convertToJSONObject();

        Assert.assertEquals(TEST_EVENT_NAME, json.getString(PARAM_EVENT_NAME));
        Assert.assertEquals(TEST_CATEGORY.name(), json.getString(PARAM_CATEGORY));
        Assert.assertEquals(TEST_TIME_START, json.getLong(PARAM_TIME_START));
        Assert.assertEquals(TEST_TIME_SPENT, json.getInt(PARAM_TIME_SPENT));
    }
}
