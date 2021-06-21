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
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test

class CancellationTokenRegistrationTest : FacebookPowerMockTestCase() {
  private lateinit var mockTokenSource: CancellationTokenSource
  private lateinit var mockAction: Runnable

  override fun setup() {
    mockTokenSource = mock()
    mockAction = mock()
  }

  @Test
  fun `test closing registration unregister from token source only once`() {
    val registration = CancellationTokenRegistration(mockTokenSource, mockAction)
    registration.close()
    registration.close()
    verify(mockTokenSource, times(1)).unregister(registration)
  }

  @Test
  fun `test run action will also unregister`() {
    val registration = CancellationTokenRegistration(mockTokenSource, mockAction)
    registration.runAction()
    verify(mockAction).run()
    verify(mockTokenSource).unregister(registration)
  }

  @Test(expected = IllegalStateException::class)
  fun `test run action can only be executed once`() {
    val registration = CancellationTokenRegistration(mockTokenSource, mockAction)
    registration.runAction()
    registration.runAction()
  }
}
