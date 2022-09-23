/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream

class ProgressOutputStream(
    out: OutputStream,
    private val requests: GraphRequestBatch,
    private val progressMap: Map<GraphRequest, RequestProgress>,
    val maxProgress: Long
) : FilterOutputStream(out), RequestOutputStream {
  private val threshold: Long = FacebookSdk.getOnProgressThreshold()
  var batchProgress: Long = 0
    private set
  private var lastReportedProgress: Long = 0
  private var currentRequestProgress: RequestProgress? = null

  private fun addProgress(size: Long) {
    currentRequestProgress?.addProgress(size)
    batchProgress += size
    if (batchProgress >= lastReportedProgress + threshold || batchProgress >= maxProgress) {
      reportBatchProgress()
    }
  }

  private fun reportBatchProgress() {
    if (batchProgress > lastReportedProgress) {
      for (callback in requests.callbacks) {
        if (callback is GraphRequestBatch.OnProgressCallback) {
          requests.callbackHandler?.post {
            callback.onBatchProgress(requests, batchProgress, maxProgress)
          }
              ?: callback.onBatchProgress(requests, batchProgress, maxProgress)
        }
      }
      lastReportedProgress = batchProgress
    }
  }

  override fun setCurrentRequest(request: GraphRequest?) {
    currentRequestProgress = if (request != null) progressMap[request] else null
  }

  @Throws(IOException::class)
  override fun write(buffer: ByteArray) {
    out.write(buffer)
    addProgress(buffer.size.toLong())
  }

  @Throws(IOException::class)
  override fun write(buffer: ByteArray, offset: Int, length: Int) {
    out.write(buffer, offset, length)
    addProgress(length.toLong())
  }

  @Throws(IOException::class)
  override fun write(oneByte: Int) {
    out.write(oneByte)
    addProgress(1)
  }

  @Throws(IOException::class)
  override fun close() {
    super.close()
    for (p in progressMap.values) {
      p.reportProgress()
    }
    reportBatchProgress()
  }
}
