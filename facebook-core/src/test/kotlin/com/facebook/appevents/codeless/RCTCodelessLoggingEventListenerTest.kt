package com.facebook.appevents.codeless

import android.view.MotionEvent
import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.EventBinding
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(CodelessLoggingEventListener::class)
class RCTCodelessLoggingEventListenerTest : FacebookPowerMockTestCase() {
  private var logTimes = 0
  private lateinit var mapping: EventBinding
  private lateinit var rootView: View
  private lateinit var hostView: View
  private lateinit var motionEvent: MotionEvent

  @Before
  fun init() {
    PowerMockito.mockStatic(CodelessLoggingEventListener::class.java)
    whenever(CodelessLoggingEventListener.logEvent(any(), any(), any())).thenAnswer { logTimes++ }

    mapping = mock()
    rootView = mock()
    hostView = mock()
    motionEvent = mock()
    whenever(motionEvent.action).thenReturn(MotionEvent.ACTION_UP)
  }

  @Test
  fun testGetOnTouchListener() {
    val listener = RCTCodelessLoggingEventListener.getOnTouchListener(mapping, rootView, hostView)
    assertThat(listener.supportCodelessLogging).isTrue
    listener.onTouch(mock(), motionEvent)
    assertThat(logTimes).isEqualTo(1)
  }
}
