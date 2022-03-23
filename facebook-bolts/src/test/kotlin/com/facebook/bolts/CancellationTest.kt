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

import java.util.concurrent.CancellationException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test

class CancellationTest {
  @Test
  fun testTokenIsCancelled() {
    val cts = CancellationTokenSource()
    val token = cts.token
    Assert.assertFalse(token.isCancellationRequested)
    Assert.assertFalse(cts.isCancellationRequested)
    cts.cancel()
    assertThat(token.isCancellationRequested).isTrue
    assertThat(cts.isCancellationRequested).isTrue
  }

  @Test
  fun testTokenIsCancelledAfterNoDelay() {
    val cts = CancellationTokenSource()
    val token = cts.token
    Assert.assertFalse(token.isCancellationRequested)
    cts.cancelAfter(0)
    assertThat(token.isCancellationRequested).isTrue
    assertThat(cts.isCancellationRequested).isTrue
  }

  @Test
  fun testTokenIsCancelledAfterDelay() {
    val cts = CancellationTokenSource()
    val token = cts.token
    Assert.assertFalse(token.isCancellationRequested)
    cts.cancelAfter(100)
    Assert.assertFalse(token.isCancellationRequested)
    Assert.assertFalse(cts.isCancellationRequested)
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
    Assert.assertFalse(token.isCancellationRequested)
    Assert.assertFalse(cts.isCancellationRequested)
    cts.cancelAfter(-1)
    Thread.sleep(150)
    Assert.assertFalse(token.isCancellationRequested)
    Assert.assertFalse(cts.isCancellationRequested)
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
