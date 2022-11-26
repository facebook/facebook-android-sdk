/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.appevents.suggestedevents

import android.app.Activity
import android.view.View
import android.view.ViewTreeObserver
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.internal.AppEventUtility
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
