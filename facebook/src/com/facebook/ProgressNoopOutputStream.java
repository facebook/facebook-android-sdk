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

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

class ProgressNoopOutputStream extends OutputStream implements RequestOutputStream {
    private final Map<GraphRequest, RequestProgress> progressMap = new HashMap<GraphRequest, RequestProgress>();
    private final Handler callbackHandler;

    private GraphRequest currentRequest;
    private RequestProgress currentRequestProgress;
    private int batchMax;

    ProgressNoopOutputStream(Handler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public void setCurrentRequest(GraphRequest currentRequest) {
        this.currentRequest = currentRequest;
        this.currentRequestProgress =
                currentRequest != null? progressMap.get(currentRequest) : null;
    }

    int getMaxProgress() {
        return batchMax;
    }

    Map<GraphRequest,RequestProgress> getProgressMap() {
        return progressMap;
    }

    void addProgress(long size) {
        if (currentRequestProgress == null) {
            currentRequestProgress = new RequestProgress(callbackHandler, currentRequest);
            progressMap.put(currentRequest, currentRequestProgress);
        }

        currentRequestProgress.addToMax(size);
        batchMax += size;
    }

    @Override
    public void write(byte[] buffer) {
        addProgress(buffer.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) {
        addProgress(length);
    }

    @Override
    public void write(int oneByte) {
        addProgress(1);
    }
}
