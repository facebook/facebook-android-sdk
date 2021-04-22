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

class RequestProgress
constructor(private val callbackHandler: Handler?, private val request: GraphRequest) {
  private val threshold = FacebookSdk.getOnProgressThreshold()
  var progress: Long = 0
    private set
  private var lastReportedProgress: Long = 0
  var maxProgress: Long = 0
    private set

  fun addProgress(size: Long) {
    progress += size
    if (progress >= lastReportedProgress + threshold || progress >= maxProgress) {
      reportProgress()
    }
  }

  fun addToMax(size: Long) {
    maxProgress += size
  }

  fun reportProgress() {
    if (progress > lastReportedProgress) {
      val callback = request.callback
      if (maxProgress > 0 && callback is GraphRequest.OnProgressCallback) {
        // Keep copies to avoid threading issues
        val progressCopy = progress
        val maxProgressCopy = maxProgress
        callbackHandler?.post { callback.onProgress(progressCopy, maxProgressCopy) }
            ?: callback.onProgress(progressCopy, maxProgressCopy)
        lastReportedProgress = progress
      }
    }
  }
}
