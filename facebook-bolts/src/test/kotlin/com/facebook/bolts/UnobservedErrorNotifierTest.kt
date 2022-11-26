/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import java.lang.RuntimeException
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UnobservedErrorNotifierTest : FacebookPowerMockTestCase() {
  private lateinit var mockTask: Task<*>
  private lateinit var testException: Exception
  private lateinit var notifier: UnobservedErrorNotifier

  override fun setup() {
    super.setup()
    mockTask = mock()
    testException = RuntimeException("test exception")
    whenever(mockTask.error).thenReturn(testException)
    notifier = UnobservedErrorNotifier(mockTask)
  }

  @Test
  fun `test notifier call unobserved exception handler`() {
    val unobservedExceptionHandler = mock<Task.UnobservedExceptionHandler>()
    Task.setUnobservedExceptionHandler(unobservedExceptionHandler)
    notifier.finalize()
    verify(unobservedExceptionHandler).unobservedException(eq(mockTask), any())
  }

  @Test
  fun `test when unobserved exception handler is null`() {
    Task.setUnobservedExceptionHandler(null)
    notifier.finalize()
  }

  @Test
  fun `test after setObserved no handler will be called`() {
    notifier.setObserved()
    val unobservedExceptionHandler = mock<Task.UnobservedExceptionHandler>()
    Task.setUnobservedExceptionHandler(unobservedExceptionHandler)
    notifier.finalize()
    verify(unobservedExceptionHandler, never()).unobservedException(eq(mockTask), any())
  }
}
