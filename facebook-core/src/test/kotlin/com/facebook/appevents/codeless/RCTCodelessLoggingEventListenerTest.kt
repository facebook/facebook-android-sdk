package com.facebook.appevents.codeless

import android.view.MotionEvent
import android.view.View
import com.facebook.FacebookPowerMockTestCase
import com.facebook.appevents.codeless.internal.EventBinding
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
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
    PowerMockito.`when`(CodelessLoggingEventListener.logEvent(any(), any(), any())).thenAnswer {
      logTimes++
    }

    mapping = mock()
    rootView = mock()
    hostView = mock()
    motionEvent = mock()
    PowerMockito.`when`(motionEvent.action).thenReturn(MotionEvent.ACTION_UP)
  }

  @Test
  fun testGetOnTouchListener() {
    val listener = RCTCodelessLoggingEventListener.getOnTouchListener(mapping, rootView, hostView)
    assertThat(listener.supportCodelessLogging).isTrue
    listener.onTouch(mock(), motionEvent)
    assertThat(logTimes).isEqualTo(1)
  }
}
