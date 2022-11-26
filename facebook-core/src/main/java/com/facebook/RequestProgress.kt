/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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

  /**
   * Increase process size
   *
   * @param size to increase
   */
  fun addProgress(size: Long) {
    progress += size
    if (progress >= lastReportedProgress + threshold || progress >= maxProgress) {
      reportProgress()
    }
  }

  /**
   * Increase maxProgress size
   *
   * @param size to increase
   */
  fun addToMax(size: Long) {
    maxProgress += size
  }

  /** Send requests if process size larger than lastReportedProgress size */
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
