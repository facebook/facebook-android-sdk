/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CancellationTokenTest : FacebookPowerMockTestCase() {
  private lateinit var mockCancellationTokenSource: CancellationTokenSource
  override fun setup() {
    mockCancellationTokenSource = mock()
  }

  @Test
  fun `test register action on cancellation token`() {
    val token = CancellationToken(mockCancellationTokenSource)
    val mockAction = mock<Runnable>()
    token.register(mockAction)
    verify(mockCancellationTokenSource).register(mockAction)
  }

  @Test
  fun `test checking cancel status`() {
    val token = CancellationToken(mockCancellationTokenSource)
    whenever(mockCancellationTokenSource.isCancellationRequested).thenReturn(false)
    assertThat(token.isCancellationRequested).isFalse
    whenever(mockCancellationTokenSource.isCancellationRequested).thenReturn(true)
    assertThat(token.isCancellationRequested).isTrue
  }
}
