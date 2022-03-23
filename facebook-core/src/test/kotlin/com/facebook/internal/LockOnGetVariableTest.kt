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

package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.util.common.CaptureExecutor
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.Callable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

object ReturnTrue : Callable<Boolean> {
  override fun call() = true
}

object ReturnNull : Callable<Object> {
  override fun call() = null
}

@PrepareForTest(FacebookSdk::class)
class LockOnGetVariableTest : FacebookPowerMockTestCase() {
  private lateinit var executor: CaptureExecutor
  @Before
  fun init() {
    executor = CaptureExecutor()
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getExecutor()).thenReturn(executor)
  }

  @Test
  fun `test wait for value`() {
    val lock = LockOnGetVariable(ReturnTrue)
    var fetchValue = false
    val thread = Thread { fetchValue = lock.value ?: false }
    thread.start()
    Assert.assertFalse(fetchValue)
    Assert.assertNotNull(executor.capturedRunnable)
    executor.capturedRunnable?.run()

    // wait for at most 10 second before checking the result
    thread.join(10_000)
    assertThat(fetchValue).isTrue
  }

  @Test
  fun `test null value`() {
    val lock = LockOnGetVariable(ReturnNull)
    var fetchValue: Object? = Object()
    val thread = Thread { fetchValue = lock.value }
    thread.start()
    Assert.assertNotNull(fetchValue)
    Assert.assertNotNull(executor.capturedRunnable)
    executor.capturedRunnable?.run()

    // wait for at most 10 second before checking the result
    thread.join(10_000)
    Assert.assertNull(fetchValue)
  }
}
