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

package com.facebook

import android.os.Handler
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest

@PrepareForTest(FacebookSdk::class)
class RequestProgressTest : FacebookPowerMockTestCase() {
  private lateinit var mockRequest: GraphRequest
  private lateinit var mockRequestCallback: GraphRequest.Callback
  private lateinit var mockHandler: Handler
  private var capturedProgress: Long = 0
  private var capturedMax: Long = 0

  @Before
  fun init() {
    PowerMockito.mockStatic(FacebookSdk::class.java)
    whenever(FacebookSdk.isInitialized()).thenReturn(true)
    whenever(FacebookSdk.getOnProgressThreshold()).thenReturn(100)
    mockRequest = mock()
    mockRequestCallback =
        object : GraphRequest.OnProgressCallback {
          override fun onProgress(progress: Long, max: Long) {
            capturedProgress = progress
            capturedMax = max
          }
          override fun onCompleted(response: GraphResponse) = Unit
        }
    whenever(mockRequest.callback).thenReturn(mockRequestCallback)
    mockHandler = mock()
    capturedProgress = 0
    capturedMax = 0
  }

  @Test
  fun `test report progress when it reaches the threshold`() {
    val progress = RequestProgress(null, mockRequest)
    progress.addProgress(1)
    Assert.assertEquals(capturedProgress, 0)
    progress.addToMax(10_000)
    progress.addProgress(99)
    Assert.assertEquals(capturedProgress, 100)
    progress.addProgress(100)
    Assert.assertEquals(capturedProgress, 200)
  }

  @Test
  fun `test report progress when it reaches max progress`() {
    val progress = RequestProgress(null, mockRequest)
    progress.addToMax(15)
    progress.addProgress(5)
    Assert.assertEquals(capturedProgress, 0)
    progress.addProgress(10)
    Assert.assertEquals(capturedProgress, 15)
    Assert.assertEquals(capturedMax, 15)
  }

  @Test
  fun `test report progress with callback handler`() {
    val progress = RequestProgress(mockHandler, mockRequest)
    progress.addToMax(15)
    progress.addProgress(20)
    Assert.assertEquals(capturedProgress, 0)
    verify(mockHandler).post(any())
  }

  @Test
  fun `test not to report if the progress is not added`() {
    val progress = RequestProgress(mockHandler, mockRequest)
    progress.addToMax(100)
    progress.addProgress(20)
    progress.reportProgress()
    progress.reportProgress()
    verify(mockHandler, times(1)).post(any())
  }
}
