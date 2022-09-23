/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Handler
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
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
