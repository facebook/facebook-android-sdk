package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.lang.IllegalStateException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(TaskCompletionSource::class)
class TaskCompletionSourceTest : FacebookPowerMockTestCase() {
  private lateinit var mockTask: Task<Void>

  override fun setup() {
    mockTask = mock()
    PowerMockito.whenNew(Task::class.java).withNoArguments().thenReturn(mockTask)
  }

  @Test
  fun `test get task`() {
    val source = TaskCompletionSource<Void>()
    assertThat(source.task).isEqualTo(mockTask)
  }

  @Test
  fun `test set result`() {
    whenever(mockTask.trySetResult(any())).thenReturn(true)
    val source = TaskCompletionSource<Void>()
    source.setResult(mock())
    verify(mockTask).trySetResult(any())
  }

  @Test(expected = IllegalStateException::class)
  fun `test set result if task completed`() {
    whenever(mockTask.trySetResult(any())).thenReturn(false)
    val source = TaskCompletionSource<Void>()
    source.setResult(mock())
  }

  @Test
  fun `test set cancelled`() {
    whenever(mockTask.trySetCancelled()).thenReturn(true)
    val source = TaskCompletionSource<Void>()
    source.setCancelled()
    verify(mockTask).trySetCancelled()
  }

  @Test(expected = IllegalStateException::class)
  fun `test set cancelled if task completed`() {
    whenever(mockTask.trySetCancelled()).thenReturn(false)
    val source = TaskCompletionSource<Void>()
    source.setCancelled()
  }

  @Test
  fun `test set error`() {
    whenever(mockTask.trySetError(any())).thenReturn(true)
    val source = TaskCompletionSource<Void>()
    source.setError(mock())
    verify(mockTask).trySetError(any())
  }

  @Test(expected = IllegalStateException::class)
  fun `test set error if task completed`() {
    whenever(mockTask.trySetError(any())).thenReturn(false)
    val source = TaskCompletionSource<Void>()
    source.setError(mock())
  }
}
