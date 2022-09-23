/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.os.Handler
import java.io.OutputStream

class ProgressNoopOutputStream(private val callbackHandler: Handler?) :
    OutputStream(), RequestOutputStream {
  private val progressMap: MutableMap<GraphRequest, RequestProgress> = hashMapOf()
  private var currentRequest: GraphRequest? = null
  private var currentRequestProgress: RequestProgress? = null
  var maxProgress = 0
    private set

  override fun setCurrentRequest(currentRequest: GraphRequest?) {
    this.currentRequest = currentRequest
    currentRequestProgress = if (currentRequest != null) progressMap[currentRequest] else null
  }

  fun getProgressMap(): Map<GraphRequest, RequestProgress> = progressMap

  /** Add size to currentRequestProgress's process size */
  fun addProgress(size: Long) {
    val currentRequest = this.currentRequest ?: return
    if (currentRequestProgress == null) {
      val requestProgress = RequestProgress(callbackHandler, currentRequest)
      currentRequestProgress = requestProgress
      progressMap[currentRequest] = requestProgress
    }
    currentRequestProgress?.addToMax(size)
    maxProgress += size.toInt()
  }

  override fun write(buffer: ByteArray) {
    addProgress(buffer.size.toLong())
  }

  override fun write(buffer: ByteArray, offset: Int, length: Int) {
    addProgress(length.toLong())
  }

  override fun write(oneByte: Int) {
    addProgress(1)
  }
}
