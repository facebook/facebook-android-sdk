/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import com.facebook.FacebookPowerMockTestCase
import java.util.concurrent.ExecutorService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.reflect.Whitebox

class BoltsExecutorsTest : FacebookPowerMockTestCase() {
  companion object {
    private const val IMMEDIATE_MAX_DEPTH = 15
  }
  private lateinit var mockBackgroundExecutor: ExecutorService
  override fun setup() {
    mockBackgroundExecutor = mock()

    val mockBoltsExecutorCompanion = mock<BoltsExecutors.Companion>()
    whenever(mockBoltsExecutorCompanion.background()).thenReturn(mockBackgroundExecutor)
    whenever(mockBoltsExecutorCompanion.immediate()).thenCallRealMethod()
    whenever(mockBoltsExecutorCompanion.scheduled()).thenCallRealMethod()
    Whitebox.setInternalState(BoltsExecutors::class.java, "Companion", mockBoltsExecutorCompanion)
  }

  @Test
  fun `test immediate executor`() {
    val mockCommand = mock<Runnable>()
    BoltsExecutors.immediate().execute(mockCommand)
    verify(mockCommand).run()
  }

  @Test
  fun `test execute more than MAX_DEPTH times on the immediate executor will forward to background`() {
    var counter = 0
    var commandToRepeat: Runnable? = null
    val testCommand = Runnable {
      counter += 1
      // run a fixed amount of times to avoid crash in testing
      if (counter < IMMEDIATE_MAX_DEPTH * 2) {
        commandToRepeat?.let { BoltsExecutors.immediate().execute(it) }
      }
    }
    commandToRepeat = testCommand
    BoltsExecutors.immediate().execute(testCommand)
    assertThat(counter).isEqualTo(IMMEDIATE_MAX_DEPTH)
    verify(mockBackgroundExecutor).execute(testCommand)
  }
}
