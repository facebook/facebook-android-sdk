package com.facebook.appevents.aam

import android.app.Activity
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.mock
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.reflect.Whitebox

@PrepareForTest(MetadataViewObserver::class)
class MetadataViewObserverTest : FacebookPowerMockTestCase() {
  private lateinit var mockActivity: Activity
  private lateinit var mockObserver: MetadataViewObserver
  @Before
  fun init() {
    mockActivity = mock()
    mockObserver = mock()
    PowerMockito.whenNew(MetadataViewObserver::class.java)
        .withArguments(mockActivity)
        .thenReturn(mockObserver)
    Whitebox.setInternalState(
        mockObserver, "activityWeakReference", WeakReference<Activity>(mockActivity))
  }
  @Test
  fun testStartTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(false))
    Whitebox.setInternalState(
        MetadataViewObserver::class.java, "observers", hashMapOf<Integer, MetadataViewObserver>())
    MetadataViewObserver.startTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Integer, MetadataViewObserver>>(
            MetadataViewObserver::class.java, "observers")
    assertThat(observers).isNotEmpty
    assertThat(observers).isEqualTo(hashMapOf(mockActivity.hashCode() as Integer to mockObserver))

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isTrue
  }

  @Test
  fun testStopTrackingActivity() {
    Whitebox.setInternalState(mockObserver, "isTracking", AtomicBoolean(true))
    Whitebox.setInternalState(
        MetadataViewObserver::class.java,
        "observers",
        hashMapOf(mockActivity.hashCode() as Integer to mockObserver))
    MetadataViewObserver.stopTrackingActivity(mockActivity)
    val observers =
        Whitebox.getInternalState<Map<Integer, MetadataViewObserver>>(
            MetadataViewObserver::class.java, "observers")
    assertThat(observers).isEmpty()

    val isTracking = Whitebox.getInternalState<AtomicBoolean>(mockObserver, "isTracking")
    assertThat(isTracking.get()).isFalse
  }
}
