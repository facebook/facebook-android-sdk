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
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(ViewObserver::class, AppEventUtility::class)
class ViewObserverTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity
  private lateinit var mockObserver: ViewObserver
  private lateinit var mockView: View
  private lateinit var mockViewTreeObserver: ViewTreeObserver
  @Before
  fun init() {
    mockActivity = mock()
    mockObserver = mock()
    mockView = mock()
    mockViewTreeObserver = mock()
    PowerMockito.whenNew(ViewObserver::class.java).withAnyArguments().thenReturn(mockObserver)
    Whitebox.setInternalState(mockObserver, "activityWeakReference", WeakReference(mockActivity))

    PowerMockito.mockStatic(AppEventUtility::class.java)
    whenever(AppEventUtility.getRootView(any())).thenReturn(mockView)
    whenever(mockView.viewTreeObserver).thenReturn(mockViewTreeObserver)
    whenever(mockViewTreeObserver.isAlive).thenReturn(true)
  }

  @Test
  fun testStartTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(false))
    ViewObserver.startTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Integer, ViewObserver>>(ViewObserver::class.java, "observers")
    assertThat(observers).isNotEmpty
    assertThat(observers).isEqualTo(hashMapOf(mockActivity.hashCode() as Integer to mockObserver))

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isTrue

    verify(mockViewTreeObserver).addOnGlobalLayoutListener(any())
  }

  @Test
  fun testStopTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(true))
    Whitebox.setInternalState(
        ViewObserver::class.java,
        "observers",
        hashMapOf(mockActivity.hashCode() as Integer to mockObserver))
    ViewObserver.stopTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Integer, ViewObserver>>(ViewObserver::class.java, "observers")
    assertThat(observers).isEmpty()

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isFalse

    verify(mockViewTreeObserver).removeOnGlobalLayoutListener(any())
  }
}
