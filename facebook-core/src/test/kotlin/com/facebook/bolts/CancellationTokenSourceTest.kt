package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.reflect.Whitebox

class CancellationTokenSourceTest : FacebookPowerMockTestCase() {
  @Test(expected = IllegalStateException::class)
  fun `test get token after close`() {
    val tokenSource = CancellationTokenSource()
    tokenSource.close()
    tokenSource.token
  }

  @Test
  fun `test cancel twice`() {
    val mockListener = mock<Runnable>()
    val tokenSource = CancellationTokenSource()
    tokenSource.register(mockListener)
    tokenSource.cancel()
    tokenSource.cancel()
    verify(mockListener, times(1)).run()
    assertThat(tokenSource.isCancellationRequested).isTrue
  }

  @Test
  fun `test unregister a listener`() {
    val mockListener = mock<Runnable>()
    val tokenSource = CancellationTokenSource()
    val registration = tokenSource.register(mockListener)
    tokenSource.unregister(registration)
    tokenSource.cancel()
    verify(mockListener, never()).run()
  }

  @Test
  fun `test cancelAfter`() {
    val mockExecutor = mock<ScheduledExecutorService>()
    var capturedRunnable: Runnable? = null
    whenever(mockExecutor.schedule(any<Runnable>(), any(), any())).then {
      capturedRunnable = it.arguments[0] as Runnable
      mock<ScheduledFuture<Any>>()
    }
    val tokenSource = CancellationTokenSource()
    Whitebox.setInternalState(tokenSource, "executor", mockExecutor)
    val mockListener = mock<Runnable>()

    tokenSource.register(mockListener)
    tokenSource.cancelAfter(1)
    assertThat(capturedRunnable).isNotNull
    capturedRunnable?.run()
    verify(mockListener, times(1)).run()
  }
}
