package com.facebook.internal.instrument.anrreport

import android.app.ActivityManager
import android.os.Process.myUid
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.instrument.InstrumentData
import com.facebook.internal.instrument.InstrumentUtility
import java.lang.reflect.Field
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(
    ActivityManager::class,
    ActivityManager.ProcessErrorStateInfo::class,
    InstrumentUtility::class,
    InstrumentData.Builder::class)
class ANRDetectorTest : FacebookPowerMockTestCase() {
  private lateinit var am: ActivityManager
  private lateinit var info: ActivityManager.ProcessErrorStateInfo

  @Before
  fun init() {
    am = PowerMockito.mock(ActivityManager::class.java)
    info = PowerMockito.mock(ActivityManager.ProcessErrorStateInfo::class.java)

    PowerMockito.mockStatic(InstrumentData.Builder::class.java)

    val stacktrace = "com.facebook.appevents.codeless.CodelessManager.onActivityResumed(file:10)"
    PowerMockito.mockStatic(InstrumentUtility::class.java)
    whenever(InstrumentUtility.getStackTrace(any<Thread>())).thenReturn(stacktrace)
    PowerMockito.`when`(
            InstrumentUtility.writeFile(
                ArgumentMatchers.isA(String::class.java), ArgumentMatchers.isA(String::class.java)))
        .thenCallRealMethod()
    PowerMockito.`when`(
            InstrumentUtility.readFile(
                ArgumentMatchers.isA(String::class.java),
                ArgumentMatchers.isA(Boolean::class.java)))
        .thenCallRealMethod()

    // reset previousStackTrace
    val field: Field = ANRDetector::class.java.getDeclaredField("previousStackTrace")
    field.isAccessible = true
    field[null] = ""
  }

  @Test
  fun `test Anr is not detected when process list is null`() {
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(InstrumentData.Builder.build(any<String>(), any<String>()))
        .thenReturn(mockInstrumentData)
    whenever(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenReturn(true)
    whenever(am.processesInErrorState).thenReturn(null)
    ANRDetector.checkProcessError(am)
    Mockito.verify(mockInstrumentData, never()).save()
  }

  @Test
  fun `test Anr is not detected when process list does not have right cause`() {
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(InstrumentData.Builder.build(any<String>(), any<String>()))
        .thenReturn(mockInstrumentData)
    whenever(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenReturn(true)

    val infoList: MutableList<ActivityManager.ProcessErrorStateInfo> = ArrayList()
    info.condition = ActivityManager.ProcessErrorStateInfo.CRASHED
    info.uid = myUid()
    info.shortMsg = "testCause"
    infoList.add(info)
    whenever(am.processesInErrorState).thenReturn(infoList)

    ANRDetector.checkProcessError(am)
    Mockito.verify(mockInstrumentData, never()).save()
  }

  @Test
  fun `test Anr is not detected when the ANR is not SDK related`() {
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(InstrumentData.Builder.build(any<String>(), any<String>()))
        .thenReturn(mockInstrumentData)
    whenever(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenReturn(false)

    val infoList: MutableList<ActivityManager.ProcessErrorStateInfo> = ArrayList()
    info.condition = ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING
    info.uid = myUid()
    info.shortMsg = "testCause"
    infoList.add(info)
    whenever(am.processesInErrorState).thenReturn(infoList)

    ANRDetector.checkProcessError(am)
    Mockito.verify(mockInstrumentData, never()).save()
  }

  @Test
  fun `test Anr is detected when there is an SDK related ANR`() {
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(InstrumentData.Builder.build(any<String>(), any<String>()))
        .thenReturn(mockInstrumentData)
    whenever(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenReturn(true)

    val infoList: MutableList<ActivityManager.ProcessErrorStateInfo> = ArrayList()
    info.condition = ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING
    info.uid = myUid()
    info.shortMsg = "testCause"
    infoList.add(info)
    whenever(am.processesInErrorState).thenReturn(infoList)

    ANRDetector.checkProcessError(am)
    verify(mockInstrumentData).save()
  }

  @Test
  fun `test Anr is saved only once when detecting the same ANR`() {
    val mockInstrumentData = PowerMockito.mock(InstrumentData::class.java)
    whenever(InstrumentData.Builder.build(any<String>(), any<String>()))
        .thenReturn(mockInstrumentData)
    whenever(InstrumentUtility.isSDKRelatedThread(any<Thread>())).thenReturn(true)

    val infoList: MutableList<ActivityManager.ProcessErrorStateInfo> = ArrayList()
    info.condition = ActivityManager.ProcessErrorStateInfo.NOT_RESPONDING
    info.uid = myUid()
    info.shortMsg = "testCause"
    infoList.add(info)
    whenever(am.processesInErrorState).thenReturn(infoList)

    ANRDetector.checkProcessError(am)
    ANRDetector.checkProcessError(am)
    ANRDetector.checkProcessError(am)
    Mockito.verify(mockInstrumentData, times(1)).save()
  }
}
