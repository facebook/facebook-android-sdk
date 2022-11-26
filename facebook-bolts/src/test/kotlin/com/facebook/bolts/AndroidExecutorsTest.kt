/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.bolts

import android.os.Handler
import android.os.Looper
import com.facebook.FacebookPowerMockTestCase
import java.util.concurrent.ThreadPoolExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(AndroidExecutors::class, Handler::class, Looper::class)
class AndroidExecutorsTest : FacebookPowerMockTestCase() {
  private lateinit var mockHandler: Handler
  private lateinit var mockLooper: Looper

  override fun setup() {
    super.setup()
    mockHandler = mock()
    mockLooper = mock()
    PowerMockito.whenNew(Handler::class.java).withArguments(mockLooper).thenReturn(mockHandler)
    PowerMockito.mockStatic(Looper::class.java)
    whenever(Looper.getMainLooper()).thenReturn(mockLooper)
  }

  @Test
  fun `test ui thread executor`() {
    val mockCommand = mock<Runnable>()
    AndroidExecutors.uiThread().execute(mockCommand)
    PowerMockito.verifyNew(Handler::class.java).withArguments(mockLooper)
    verify(mockHandler).post(mockCommand)
  }

  @Test
  fun `test cached thread pool allows core thread timeout`() {
    val executor = AndroidExecutors.newCachedThreadPool() as ThreadPoolExecutor
    assertThat(executor.allowsCoreThreadTimeOut()).isTrue
  }
}
