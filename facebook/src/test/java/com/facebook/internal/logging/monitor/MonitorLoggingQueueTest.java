package com.facebook.internal.logging.monitor;

import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_START;
import static org.mockito.Mockito.when;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.logging.ExternalLog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

@PrepareForTest({FacebookSdk.class})
public class MonitorLoggingQueueTest extends FacebookPowerMockTestCase {

  private final Executor mockExecutor = new FacebookSerialExecutor();
  private MonitorLoggingQueue monitorLoggingQueue;

  private static final int TEST_FLUSH_LIMIT = 3;
  private MonitorLog testLog;

  @Before
  public void init() {
    PowerMockito.spy(FacebookSdk.class);
    when(FacebookSdk.isInitialized()).thenReturn(true);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    PowerMockito.when(FacebookSdk.getApplicationContext())
        .thenReturn(RuntimeEnvironment.application);
    monitorLoggingQueue = MonitorLoggingQueue.getInstance();
    testLog = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
  }

  @Test
  public void testAddLog() {
    monitorLoggingQueue.addLog(testLog);
    ExternalLog log = monitorLoggingQueue.fetchLog();
    Assert.assertEquals(testLog, log);
  }

  @Test
  public void testAddLogs() {
    List<ExternalLog> logList = new ArrayList<>();
    logList.add(testLog);
    monitorLoggingQueue.addLogs(logList);
    ExternalLog log = monitorLoggingQueue.fetchLog();
    Assert.assertEquals(testLog, log);
  }

  @Test
  public void testHasReachedFlushLimit() {
    ReflectionHelpers.setStaticField(MonitorLoggingQueue.class, "FLUSH_LIMIT", TEST_FLUSH_LIMIT);
    boolean hasReachedFlushLimit = false;
    for (int i = 0; i < TEST_FLUSH_LIMIT - 1; i++) {
      MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
      hasReachedFlushLimit = monitorLoggingQueue.addLog(log);
    }
    Assert.assertFalse(hasReachedFlushLimit);

    MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
    hasReachedFlushLimit = monitorLoggingQueue.addLog(log);
    Assert.assertTrue(hasReachedFlushLimit);
  }

  @Test
  public void testFetchAllLogs() {
    monitorLoggingQueue.addLog(testLog);
    Collection expectedLogs = Arrays.asList(testLog);
    Collection<ExternalLog> fetchedLogs = monitorLoggingQueue.fetchAllLogs();

    // compare the size
    Assert.assertEquals(expectedLogs.size(), fetchedLogs.size());

    Iterator<ExternalLog> iteratorOfExpectedLogs = expectedLogs.iterator();
    Iterator<ExternalLog> iteratorOfFetchedLogs = fetchedLogs.iterator();
    while (iteratorOfExpectedLogs.hasNext() && iteratorOfFetchedLogs.hasNext()) {
      Assert.assertEquals(iteratorOfExpectedLogs.next(), iteratorOfFetchedLogs.next());
    }

    Assert.assertTrue(monitorLoggingQueue.isEmpty());
  }

  // make sure we have emptied the monitor logging queue after each test
  @After
  public void tearDown() {
    while (!monitorLoggingQueue.isEmpty()) {
      monitorLoggingQueue.fetchLog();
    }
  }
}
