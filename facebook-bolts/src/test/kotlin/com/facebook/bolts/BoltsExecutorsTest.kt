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
