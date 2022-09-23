/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.internal

import com.facebook.FacebookPowerMockTestCase
import com.facebook.FacebookSdk
import com.facebook.util.common.CaptureExecutor
import java.util.concurrent.Callable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.whenever
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
    assertThat(fetchValue).isFalse
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
