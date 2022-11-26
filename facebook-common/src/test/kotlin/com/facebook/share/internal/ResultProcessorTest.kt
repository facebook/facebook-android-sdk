/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.os.Bundle
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookPowerMockTestCase
import com.facebook.internal.AppCall
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

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
