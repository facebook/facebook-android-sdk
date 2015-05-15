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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

class ProgressOutputStream extends FilterOutputStream implements RequestOutputStream {
    private final Map<GraphRequest, RequestProgress> progressMap;
    private final GraphRequestBatch requests;
    private final long threshold;

    private long batchProgress, lastReportedProgress, maxProgress;
    private RequestProgress currentRequestProgress;

    ProgressOutputStream(
            OutputStream out,
            GraphRequestBatch requests,
            Map<GraphRequest, RequestProgress> progressMap,
            long maxProgress) {
        super(out);
        this.requests = requests;
        this.progressMap = progressMap;
        this.maxProgress = maxProgress;

        this.threshold = FacebookSdk.getOnProgressThreshold();
    }

    private void addProgress(long size) {
        if (currentRequestProgress != null) {
            currentRequestProgress.addProgress(size);
        }

        batchProgress += size;

        if (batchProgress >= lastReportedProgress + threshold || batchProgress >= maxProgress) {
            reportBatchProgress();
        }
    }

    private void reportBatchProgress() {
        if (batchProgress > lastReportedProgress) {
            for (GraphRequestBatch.Callback callback : requests.getCallbacks()) {
                if (callback instanceof GraphRequestBatch.OnProgressCallback) {
                    final Handler callbackHandler = requests.getCallbackHandler();

                    // Keep copies to avoid threading issues
                    final GraphRequestBatch.OnProgressCallback progressCallback =
                            (GraphRequestBatch.OnProgressCallback) callback;
                    if (callbackHandler == null) {
                        progressCallback.onBatchProgress(requests, batchProgress, maxProgress);
                    }
                    else {
                        callbackHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressCallback.onBatchProgress(
                                        requests,
                                        batchProgress,
                                        maxProgress);
                            }
                        });
                    }
                }
            }

            lastReportedProgress = batchProgress;
        }
    }

    public void setCurrentRequest(GraphRequest request) {
        currentRequestProgress = request != null? progressMap.get(request) : null;
    }

    long getBatchProgress() {
        return batchProgress;
    }

    long getMaxProgress() {
        return maxProgress;
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        out.write(buffer);
        addProgress(buffer.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        out.write(buffer, offset, length);
        addProgress(length);
    }

    @Override
    public void write(int oneByte) throws IOException {
        out.write(oneByte);
        addProgress(1);
    }

    @Override
    public void close() throws IOException {
        super.close();

        for (RequestProgress p : progressMap.values()) {
            p.reportProgress();
        }

        reportBatchProgress();
    }
}
