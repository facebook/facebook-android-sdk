package com.facebook.internal.metrics

import android.os.SystemClock as AndroidOsSystemClock
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.logging.LogCategory
import com.facebook.internal.logging.LogEvent
import com.facebook.internal.logging.monitor.MonitorLog
import com.facebook.internal.logging.monitor.PerformanceEventName
import com.facebook.internal.metrics.MetricsUtil.INVALID_TIME
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Before
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.api.mockito.PowerMockito.`when` as whenCalled
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(AndroidOsSystemClock::class)
class MetricsUtilTest : FacebookPowerMockTestCase() {
  private val mockStartTimeFirst = 100L
  private val mockStopTimeFirst = 200L
  private val mockStartTimeSecond = 300L
  private val mockStopTimeSecond = 500L
  private val mockPerformanceEventNameFirst = PerformanceEventName.EVENT_NAME_FOR_TEST_FIRST
  private val mockPerformanceEventNameSecond = PerformanceEventName.EVENT_NAME_FOR_TEST_SECOND
  private val metricsUtil = MetricsUtil.getInstance()
  private val logEventFirst =
      LogEvent(mockPerformanceEventNameFirst.toString(), LogCategory.PERFORMANCE)
  private val logEventSecond =
      LogEvent(mockPerformanceEventNameSecond.toString(), LogCategory.PERFORMANCE)

  @Before
  fun init() {
    mockStatic(AndroidOsSystemClock::class.java)
  }

  @Test
  fun `test calling startMeasureFor and stopMeasureFor in pair`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStopTimeFirst)

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst)
    val expectedMonitorLog =
        MonitorLog.LogBuilder(
                LogEvent(mockPerformanceEventNameFirst.toString(), LogCategory.PERFORMANCE))
            .timeSpent((mockStopTimeFirst - mockStartTimeFirst).toInt())
            .build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test calling stopMeasureFor without startMeasureFor`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(mockStopTimeFirst)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst)
    val expectedMonitorLog = MonitorLog.LogBuilder(logEventFirst).timeSpent(INVALID_TIME).build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test calling startMeasureFor multiple times before stopMeasureFor`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStartTimeSecond, mockStopTimeFirst)

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst)
    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst)
    val expectedMonitorLog =
        MonitorLog.LogBuilder(logEventFirst)
            .timeSpent((mockStopTimeFirst - mockStartTimeSecond).toInt())
            .build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test measuring in pair multiple times`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStopTimeFirst, mockStartTimeSecond, mockStopTimeSecond)

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst)
    val monitorLogFirst = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst)

    metricsUtil.startMeasureFor(mockPerformanceEventNameSecond)
    val monitorLogSecond = metricsUtil.stopMeasureFor(mockPerformanceEventNameSecond)

    val expectedMonitorLogFirst =
        MonitorLog.LogBuilder(logEventFirst)
            .timeSpent((mockStopTimeFirst - mockStartTimeFirst).toInt())
            .build()
    val expectedMonitorLogSecond =
        MonitorLog.LogBuilder(logEventSecond)
            .timeSpent((mockStopTimeSecond - mockStartTimeSecond).toInt())
            .build()

    assertEquals(expectedMonitorLogFirst, monitorLogFirst)
    assertEquals(expectedMonitorLogSecond, monitorLogSecond)
  }

  @Test
  fun `test measuring in pair for multiple interleaved log events`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStartTimeSecond, mockStopTimeFirst, mockStopTimeSecond)

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst)
    metricsUtil.startMeasureFor(mockPerformanceEventNameSecond)

    val monitorLogFirst = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst)
    val monitorLogSecond = metricsUtil.stopMeasureFor(mockPerformanceEventNameSecond)

    val expectedMonitorLogFirst =
        MonitorLog.LogBuilder(logEventFirst)
            .timeSpent((mockStopTimeFirst - mockStartTimeFirst).toInt())
            .build()
    val expectedMonitorLogSecond =
        MonitorLog.LogBuilder(logEventSecond)
            .timeSpent((mockStopTimeSecond - mockStartTimeSecond).toInt())
            .build()

    assertEquals(expectedMonitorLogFirst, monitorLogFirst)
    assertEquals(expectedMonitorLogSecond, monitorLogSecond)
  }
}
