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

package com.facebook.share.internal

import android.os.Bundle
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.AppCall
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test

class ResultProcessorTest : FacebookPowerMockTestCase() {
  private lateinit var mockCallback: FacebookCallback<Void>
  private lateinit var resultProcessor: ResultProcessor
  private lateinit var resultProcessorNullCallback: ResultProcessor
  private val appCall = AppCall(0)

  override fun setup() {
    super.setup()

    mockCallback = mock()
    resultProcessor = getResultProcessorWithCallback(mockCallback)
    resultProcessorNullCallback = getResultProcessorWithCallback(null)
  }

  private fun getResultProcessorWithCallback(callback: FacebookCallback<Void>?): ResultProcessor {
    return object : ResultProcessor(callback) {
      override fun onSuccess(appCall: AppCall, results: Bundle?) = Unit
    }
  }

  @Test
  fun `test onCancel calls callback onCancel`() {
    resultProcessor.onCancel(appCall)
    verify(mockCallback).onCancel()
  }

  @Test
  fun `test onError calls callback onError`() {
    val exception = FacebookException("error")
    resultProcessor.onError(appCall, exception)
    verify(mockCallback).onError(exception)
  }

  @Test
  fun `test onCancel with null callback`() {
    resultProcessorNullCallback.onCancel(appCall)
    verify(mockCallback, never()).onCancel()
  }

  @Test
  fun `test onError with null callback`() {
    val exception = FacebookException("error")
    resultProcessorNullCallback.onError(appCall, exception)
    verify(mockCallback, never()).onError(exception)
  }
}
