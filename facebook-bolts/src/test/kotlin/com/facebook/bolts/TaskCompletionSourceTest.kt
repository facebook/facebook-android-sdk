/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import java.lang.IllegalStateException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
