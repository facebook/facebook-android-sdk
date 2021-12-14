package com.facebook.appevents.suggestedevents

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.internal.AppEventUtility
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(AppEventUtility::class)
class ViewObserverTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity
  private lateinit var mockView: View
  private lateinit var mockViewTreeObserver: ViewTreeObserver

  @Before
  fun init() {
    mockActivity = mock()
    mockView = mock()
    mockViewTreeObserver = mock()

    PowerMockito.mockStatic(AppEventUtility::class.java)
    whenever(AppEventUtility.getRootView(any())).thenReturn(mockView)
    whenever(mockView.viewTreeObserver).thenReturn(mockViewTreeObserver)
    whenever(mockViewTreeObserver.isAlive).thenReturn(true)
  }

  @Test
  fun testStartTrackingActivity() {
    ViewObserver.startTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Int, ViewObserver>>(ViewObserver::class.java, "observers")
    checkNotNull(observers)
    assertThat(observers).isNotEmpty
    val observer: ViewObserver? = observers[mockActivity.hashCode()]
    val isTracking = Whitebox.getInternalState<AtomicBoolean>(observer, "isTracking")
    assertThat(isTracking.get()).isTrue
    verify(mockViewTreeObserver).addOnGlobalLayoutListener(any())
  }

  @Test
  fun testStopTrackingActivity() {
    ViewObserver.startTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Int, ViewObserver>>(ViewObserver::class.java, "observers")
    checkNotNull(observers)
    val observer: ViewObserver? = observers[mockActivity.hashCode()]

    ViewObserver.stopTrackingActivity(mockActivity)

    assertThat(observers).isEmpty()
    val isTracking = Whitebox.getInternalState<AtomicBoolean>(observer, "isTracking")
    assertThat(isTracking.get()).isFalse
    verify(mockViewTreeObserver).removeOnGlobalLayoutListener(any())
  }
}
