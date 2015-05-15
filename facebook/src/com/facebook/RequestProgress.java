/**
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

package com.facebook;

import android.os.Handler;

class RequestProgress {
    private final GraphRequest request;
    private final Handler callbackHandler;
    private final long threshold;

    private long progress, lastReportedProgress, maxProgress;

    RequestProgress(Handler callbackHandler, GraphRequest request) {
        this.request = request;
        this.callbackHandler = callbackHandler;

        this.threshold = FacebookSdk.getOnProgressThreshold();
    }

    long getProgress() {
        return progress;
    }

    long getMaxProgress() {
        return maxProgress;
    }

    void addProgress(long size) {
        progress += size;

        if (progress >= lastReportedProgress + threshold || progress >= maxProgress) {
            reportProgress();
        }
    }

    void addToMax(long size) {
        maxProgress += size;
    }

    void reportProgress() {
        if (progress > lastReportedProgress) {
            GraphRequest.Callback callback = request.getCallback();
            if (maxProgress > 0 && callback instanceof GraphRequest.OnProgressCallback) {
                // Keep copies to avoid threading issues
                final long currentCopy = progress;
                final long maxProgressCopy = maxProgress;
                final GraphRequest.OnProgressCallback callbackCopy =
                        (GraphRequest.OnProgressCallback) callback;
                if (callbackHandler == null) {
                    callbackCopy.onProgress(currentCopy, maxProgressCopy);
                }
                else {
                    callbackHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callbackCopy.onProgress(currentCopy, maxProgressCopy);
                        }
                    });
                }
                lastReportedProgress = progress;
            }
        }
    }
}
