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

  fun getProgressMap(): Map<GraphRequest, RequestProgress> {
    return progressMap
  }

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
