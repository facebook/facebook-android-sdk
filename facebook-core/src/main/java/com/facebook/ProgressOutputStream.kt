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
