/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

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
