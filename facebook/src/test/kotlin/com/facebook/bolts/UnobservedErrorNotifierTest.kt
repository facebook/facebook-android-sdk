/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.lang.RuntimeException
import org.junit.Test

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
