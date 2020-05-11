/*
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

import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_MODEL;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_DEVICE_OS_VERSION;
import static com.facebook.internal.logging.monitor.MonitorLogServerProtocol.PARAM_UNIQUE_APPLICATION_ID;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_APP_ID;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_PACKAGE_NAME;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_START;
import static java.lang.Thread.sleep;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

import android.content.Context;
import android.os.Build;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.internal.logging.ExternalLog;
import com.facebook.internal.logging.LoggingCache;
import com.facebook.internal.logging.LoggingStore;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "org.powermock.*"})
@PrepareForTest({
  FacebookSdk.class,
  MonitorLoggingManager.class,
  GraphRequest.class,
})
public class MonitorLoggingManagerTest extends FacebookPowerMockTestCase {

  private final Executor mockExecutor = new FacebookSerialExecutor();
  @Mock private LoggingCache mockMonitorLoggingQueue;
  @Mock private LoggingStore mockMonitorLoggingStore;
  @Mock private MonitorLoggingManager mockMonitorLoggingManager;
  private ScheduledExecutorService mockScheduledExecutor;
  private MonitorLog monitorLog;
  private static final int TEST_MAX_LOG_NUMBER_PER_REQUEST = 3;
  private static final int TIMES = 2;
  private static final String ENTRIES_KEY = "entries";

  @Before
  public void init() {
    spy(FacebookSdk.class);
    PowerMockito.when(FacebookSdk.isInitialized()).thenReturn(true);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    PowerMockito.when(FacebookSdk.getApplicationContext())
        .thenReturn(RuntimeEnvironment.application);
    ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 15);
    Context mockApplicationContext = mock(Context.class);
    PowerMockito.when(FacebookSdk.getApplicationContext()).thenReturn(mockApplicationContext);
    PowerMockito.when(mockApplicationContext.getPackageName()).thenReturn(TEST_PACKAGE_NAME);

    MonitorLoggingQueue monitorLoggingQueue = MonitorLoggingQueue.getInstance();
    mockMonitorLoggingQueue = PowerMockito.spy(monitorLoggingQueue);
    MonitorLoggingManager monitorLoggingManager =
        MonitorLoggingManager.getInstance(mockMonitorLoggingQueue, mockMonitorLoggingStore);
    mockScheduledExecutor = PowerMockito.spy(new FacebookSerialThreadPoolExecutor(1));

    mock(Executors.class);
    Whitebox.setInternalState(monitorLoggingManager, "singleThreadExecutor", mockScheduledExecutor);
    Whitebox.setInternalState(monitorLoggingManager, "logQueue", mockMonitorLoggingQueue);
    mockMonitorLoggingManager = PowerMockito.spy(monitorLoggingManager);
    monitorLog = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
  }

  @Test
  public void testAddLogThenHasNotReachedFlushLimit() throws InterruptedException {
    PowerMockito.when(mockMonitorLoggingQueue.addLog(any(ExternalLog.class))).thenReturn(false);
    mockMonitorLoggingManager.addLog(monitorLog);

    // make sure that singleThreadExecutor has been scheduled a future task successfully
    sleep(300);
    verify(mockScheduledExecutor).schedule(any(Runnable.class), anyInt(), any(TimeUnit.class));
  }

  @Test
  public void testAddLogThenHasReachedFlushLimit() {
    PowerMockito.when(mockMonitorLoggingQueue.addLog(any(ExternalLog.class))).thenReturn(true);
    mockMonitorLoggingManager.addLog(monitorLog);

    verify(mockMonitorLoggingQueue).addLog(monitorLog);
    verify(mockMonitorLoggingManager).flushAndWait();
  }

  @Test
  public void testFlushAndWait() {
    PowerMockito.mockStatic(GraphRequest.class);
    PowerMockito.mockStatic(MonitorLoggingManager.class);
    spy(MonitorLoggingManager.class);
    PowerMockito.when(FacebookSdk.getApplicationId()).thenReturn(TEST_APP_ID);

    mockMonitorLoggingManager.flushAndWait();
    PowerMockito.verifyStatic();
    GraphRequest.executeBatchAsync(any(GraphRequestBatch.class));

    PowerMockito.verifyStatic();
    MonitorLoggingManager.buildRequests(any(MonitorLoggingQueue.class));
  }

  @Test
  public void testBuildRequestsWhenAppIDIsNull() {
    PowerMockito.when(FacebookSdk.getApplicationId()).thenReturn(null);
    List<GraphRequest> requests = MonitorLoggingManager.buildRequests(mockMonitorLoggingQueue);
    verifyNoMoreInteractions(mockMonitorLoggingQueue);
    Assert.assertEquals(0, requests.size());
  }

  @Test
  public void testBuildRequestsWhenAppIDIsNotNull() {
    PowerMockito.when(FacebookSdk.getApplicationId()).thenReturn(TEST_APP_ID);
    ReflectionHelpers.setStaticField(
        MonitorLoggingManager.class, "MAX_LOG_NUMBER_PER_REQUEST", TEST_MAX_LOG_NUMBER_PER_REQUEST);
    for (int i = 0; i < TEST_MAX_LOG_NUMBER_PER_REQUEST * TIMES; i++) {
      mockMonitorLoggingManager.addLog(monitorLog);
    }

    List<GraphRequest> requests = MonitorLoggingManager.buildRequests(mockMonitorLoggingQueue);
    verify(mockMonitorLoggingQueue, times(TEST_MAX_LOG_NUMBER_PER_REQUEST * TIMES)).fetchLog();
    Assert.assertEquals(TIMES, requests.size());
  }

  @Test
  public void testBuildPostRequestFromLogs() throws JSONException {
    GraphRequest request =
        MonitorLoggingManager.buildPostRequestFromLogs(Arrays.asList(monitorLog));
    JSONObject graphObject = request.getGraphObject();
    String deviceOsVersion = Build.VERSION.RELEASE;
    String deviceModel = Build.MODEL;

    Assert.assertEquals(deviceOsVersion, graphObject.getString(PARAM_DEVICE_OS_VERSION));
    Assert.assertEquals(deviceModel, graphObject.getString(PARAM_DEVICE_MODEL));
    Assert.assertEquals(TEST_PACKAGE_NAME, graphObject.getString(PARAM_UNIQUE_APPLICATION_ID));
    Assert.assertNotNull(graphObject.getString(ENTRIES_KEY));
  }

  @After
  public void tearDown() {
    mockScheduledExecutor.shutdown();
    reset(mockMonitorLoggingQueue);

    // empty mockMonitorLoggingQueue
    while (!mockMonitorLoggingQueue.isEmpty()) {
      mockMonitorLoggingQueue.fetchLog();
    }
  }
}
