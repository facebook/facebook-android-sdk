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

import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;
import com.facebook.internal.logging.ExternalLog;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_APP_ID;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_SPENT;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_START;

public class MonitorLoggingStoreTest extends FacebookTestCase {

    private static final int LOGS_BATCH_NUMBER = 3;

    @Before
    public void init() {
        FacebookSdk.setApplicationId(TEST_APP_ID);
        FacebookSdk.sdkInitialize(RuntimeEnvironment.application);
    }

    @Test
    public void testWriteAndReadFromStore() {
        MonitorLoggingStore logStore = MonitorLoggingStore.getInstance();

        List<ExternalLog> firstLogsWriteToStore = new ArrayList<>();
        MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
        firstLogsWriteToStore.add(log);
        logStore.saveLogsToDisk(firstLogsWriteToStore);

        // add logs after adding the first log list
        // to see if the first ones are erased
        List<ExternalLog> secondLogsWriteToStore = new ArrayList<>();
        for (int i = 0; i < LOGS_BATCH_NUMBER; i++) {
            log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START, TEST_TIME_SPENT);
            secondLogsWriteToStore.add(log);
        }

        logStore.saveLogsToDisk(secondLogsWriteToStore);
        List<ExternalLog> logsReadFromStore = logStore.readAndClearStore();

        // Logs read from store should only have the ones we added at second time
        Assert.assertEquals(secondLogsWriteToStore.size(), logsReadFromStore.size());
        for (int i = 0; i < secondLogsWriteToStore.size(); i++) {
            Assert.assertEquals(secondLogsWriteToStore.get(i), logsReadFromStore.get(i));
        }

        // make sure the file is deleted
        logsReadFromStore = logStore.readAndClearStore();
        Assert.assertEquals(0, logsReadFromStore.size());
    }
}
