/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import java.util.concurrent.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test

class CancellationTest {
  @Test
  fun testTokenIsCancelled() {
    val cts = CancellationTokenSource()
    val token = cts.token
    assertThat(token.isCancellationRequested).isFalse
    assertThat(cts.isCancellationRequested).isFalse
    cts.cancel()
    assertThat(token.isCancellationRequested).isTrue
    assertThat(cts.isCancellationRequested).isTrue
  }

  @Test
  fun testTokenIsCancelledAfterNoDelay() {
    val cts = CancellationTokenSource()
    val token = cts.token
    assertThat(token.isCancellationRequested).isFalse
    cts.cancelAfter(0)
    assertThat(token.isCancellationRequested).isTrue
    assertThat(cts.isCancellationRequested).isTrue
  }

  @Test
  fun testTokenIsCancelledAfterDelay() {
    val cts = CancellationTokenSource()
    val token = cts.token
    assertThat(token.isCancellationRequested).isFalse
    cts.cancelAfter(100)
    assertThat(token.isCancellationRequested).isFalse
    assertThat(cts.isCancellationRequested).isFalse
    Thread.sleep(150)
    assertThat(token.isCancellationRequested).isTrue
    assertThat(cts.isCancellationRequested).isTrue
  }

  @Test
  fun testTokenCancelAfterDelayCancellation() {
    val cts = CancellationTokenSource()
    val token = cts.token
    assertThat(token.isCancellationRequested).isFalse
    cts.cancelAfter(100)
    assertThat(token.isCancellationRequested).isFalse
    assertThat(cts.isCancellationRequested).isFalse
    cts.cancelAfter(-1)
    Thread.sleep(150)
    assertThat(token.isCancellationRequested).isFalse
    assertThat(cts.isCancellationRequested).isFalse
  }

  @Test
  fun testTokenThrowsWhenCancelled() {
    val cts = CancellationTokenSource()
    val token = cts.token
    try {
      token.throwIfCancellationRequested()
    } catch (e: CancellationException) {
      Assert.fail(
          "Token has not been cancelled yet, " +
              CancellationException::class.java.simpleName +
              " should not be thrown")
    }
    cts.cancel()
    try {
      token.throwIfCancellationRequested()
      Assert.fail(CancellationException::class.java.simpleName + " should be thrown")
    } catch (e: CancellationException) {
      // Do nothing
    }
  }

  @Test
  fun testTokenCallsRegisteredActionWhenCancelled() {
    val cts = CancellationTokenSource()
    val token = cts.token
    var result: Any? = null
    token.register { result = "Run" }
    Assert.assertNull(result)
    cts.cancel()
    Assert.assertNotNull(result)
  }

  @Test
  fun testCancelledTokenCallsRegisteredActionImmediately() {
    val cts = CancellationTokenSource()
    val token = cts.token
    var result: Any? = null
    cts.cancel()
    token.register { result = "Run" }
    Assert.assertNotNull(result)
  }

  @Test
  fun testTokenDoesNotCallUnregisteredAction() {
    val cts = CancellationTokenSource()
    val token = cts.token
    var result1: Any? = null
    var result2: Any? = null
    val registration1 = token.register { result1 = "Run!" }
    token.register { result2 = "Run!" }
    registration1.close()
    cts.cancel()
    Assert.assertNull(result1)
    Assert.assertNotNull(result2)
  }
}
