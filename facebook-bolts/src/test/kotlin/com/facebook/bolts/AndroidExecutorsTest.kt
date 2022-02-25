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

import android.os.Handler
import android.os.Looper
import com.facebook.FacebookPowerMockTestCase
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import java.util.concurrent.ThreadPoolExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
