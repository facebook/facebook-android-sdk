package com.facebook.internal.logging.monitor

import android.os.SystemClock as AndroidOsSystemClock
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.logging.LogCategory
import com.facebook.internal.logging.LogEvent
import com.facebook.internal.logging.monitor.MetricsUtil.INVALID_TIME
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
  private val extraId = 1L
  private val logEventFirst =
      LogEvent(mockPerformanceEventNameFirst.toString(), LogCategory.PERFORMANCE)
  private val logEventSecond =
      LogEvent(mockPerformanceEventNameSecond.toString(), LogCategory.PERFORMANCE)

  @Before
  fun init() {
    mockStatic(AndroidOsSystemClock::class.java)
  }

  @Test
  fun `test calling startMeasureFor and stopMeasureFor in pair with same extra ID`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStopTimeFirst)
    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)
    val expectedMonitorLog =
        MonitorLog.LogBuilder(
                LogEvent(mockPerformanceEventNameFirst.toString(), LogCategory.PERFORMANCE))
            .timeSpent((mockStopTimeFirst - mockStartTimeFirst).toInt())
            .build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test calling startMeasureFor and stopMeasureFor in pair with different extra ID`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStopTimeFirst)
    val extraIdFirst = 1L
    val extraIdSecond = 2L

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraIdFirst)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraIdSecond)
    val expectedMonitorLog = MonitorLog.LogBuilder(logEventFirst).timeSpent(INVALID_TIME).build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test calling stopMeasureFor without startMeasureFor`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(mockStopTimeFirst)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)
    val expectedMonitorLog = MonitorLog.LogBuilder(logEventFirst).timeSpent(INVALID_TIME).build()
    assertEquals(expectedMonitorLog, monitorLog)
  }

  @Test
  fun `test calling startMeasureFor multiple times before stopMeasureFor`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStartTimeSecond, mockStopTimeFirst)
    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)
    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)
    val monitorLog = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)
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

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)
    val monitorLogFirst = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)

    metricsUtil.startMeasureFor(mockPerformanceEventNameSecond, extraId)
    val monitorLogSecond = metricsUtil.stopMeasureFor(mockPerformanceEventNameSecond, extraId)

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

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)
    metricsUtil.startMeasureFor(mockPerformanceEventNameSecond, extraId)

    val monitorLogFirst = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)
    val monitorLogSecond = metricsUtil.stopMeasureFor(mockPerformanceEventNameSecond, extraId)

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
  fun `test remove temp metrics data`() {
    whenCalled(AndroidOsSystemClock.elapsedRealtime()).thenReturn(
        mockStartTimeFirst, mockStopTimeFirst)

    metricsUtil.startMeasureFor(mockPerformanceEventNameFirst, extraId)

    // after removeTempMetricsDataFor has been called, when stopMeasureFor has been called, the
    // behavior should be as same as startMeasureFor never been called
    metricsUtil.removeTempMetricsDataFor(mockPerformanceEventNameFirst, extraId)

    val monitorLogFirst = metricsUtil.stopMeasureFor(mockPerformanceEventNameFirst, extraId)
    val expectedMonitorLogFirst =
        MonitorLog.LogBuilder(logEventFirst).timeSpent(INVALID_TIME).build()

    assertEquals(expectedMonitorLogFirst, monitorLogFirst)
  }
}
