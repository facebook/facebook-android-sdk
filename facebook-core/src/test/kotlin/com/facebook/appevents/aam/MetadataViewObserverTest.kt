package com.facebook.appevents.aam

import android.app.Activity
import android.text.Editable
import android.widget.EditText
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.InternalAppEventsLogger
import com.nhaarman.mockitokotlin2.anyOrNull
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

@PrepareForTest(MetadataViewObserver::class, MetadataMatcher::class, InternalAppEventsLogger::class)
class MetadataViewObserverTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity
  private lateinit var mockObserver: MetadataViewObserver

  @Before
  fun init() {
    mockActivity = mock()
    mockObserver = mock()
    PowerMockito.whenNew(MetadataViewObserver::class.java)
        .withAnyArguments()
        .thenReturn(mockObserver)
    Whitebox.setInternalState(mockObserver, "activityWeakReference", WeakReference(mockActivity))
    PowerMockito.mockStatic(InternalAppEventsLogger::class.java)
    Whitebox.setInternalState(
        InternalAppEventsLogger::class.java, "Companion", mock<InternalAppEventsLogger.Companion>())
  }

  @Test
  fun testStartTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(false))
    Whitebox.setInternalState(
        MetadataViewObserver::class.java, "observers", hashMapOf<Int, MetadataViewObserver>())

    MetadataViewObserver.startTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Int, MetadataViewObserver>>(
            MetadataViewObserver::class.java, "observers")
    assertThat(observers).isNotEmpty
    assertThat(observers).isEqualTo(hashMapOf(mockActivity.hashCode() to mockObserver))

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isTrue
  }

  @Test
  fun testStopTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(true))
    Whitebox.setInternalState(
        MetadataViewObserver::class.java,
        "observers",
        hashMapOf(mockActivity.hashCode() to mockObserver))
    MetadataViewObserver.stopTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Int, MetadataViewObserver>>(
            MetadataViewObserver::class.java, "observers")
    assertThat(observers).isEmpty()

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isFalse
  }

  @Test
  fun `test onGlobalFocusChanged will retrieve text in EditText`() {
    whenever(mockObserver.onGlobalFocusChanged(anyOrNull(), anyOrNull())).thenCallRealMethod()
    PowerMockito.mockStatic(MetadataMatcher::class.java)
    Whitebox.setInternalState(mockObserver, "processedText", mutableSetOf<String>())
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(false))
    Whitebox.setInternalState(
        MetadataViewObserver::class.java, "observers", hashMapOf<Int, MetadataViewObserver>())
    MetadataViewObserver.startTrackingActivity(mockActivity)
    val mockEditView: EditText = mock()
    val mockEditable: Editable = mock()
    whenever(mockEditable.toString()).thenReturn("test")
    whenever(mockEditView.text).thenReturn(mockEditable)
    whenever(MetadataMatcher.getCurrentViewIndicators(mockEditView)).thenReturn(listOf())

    val observers =
        Whitebox.getInternalState<Map<Int, MetadataViewObserver>>(
            MetadataViewObserver::class.java, "observers")
    val observer = checkNotNull(observers[mockActivity.hashCode()])
    observer.onGlobalFocusChanged(null, mockEditView)
    verify(mockEditView).text
  }
}
