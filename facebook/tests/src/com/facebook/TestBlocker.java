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
import android.os.HandlerThread;

public class TestBlocker extends HandlerThread {
    private Exception exception;
    public int signals;
    private volatile Handler handler;

    private TestBlocker() {
        super("TestBlocker");
    }

    public synchronized static TestBlocker createTestBlocker() {
        TestBlocker blocker = new TestBlocker();
        blocker.start();

        // Wait until we have a Looper and Handler.
        synchronized (blocker) {
            while (blocker.handler == null) {
                try {
                    blocker.wait();
                } catch (InterruptedException e) {
                }
            }
        }

        return blocker;
    }

    @Override
    public void run() {
        try {
            super.run();
        } catch (Exception e) {
            setException(e);
        }
        synchronized (this) {
            notifyAll();
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public void assertSuccess() throws Exception {
        Exception e = getException();
        if (e != null) {
            throw e;
        }
    }

    public synchronized void signal() {
        ++signals;
        notifyAll();
    }

    public void waitForSignals(int numSignals) throws Exception {
        // Make sure we aren't sitting on an unhandled exception before we even start, because that means our
        // thread isn't around anymore.
        assertSuccess();

        setException(null);

        synchronized (this) {
            while (getException() == null && signals < numSignals) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            signals = 0;
        }
    }

    public void waitForSignalsAndAssertSuccess(int numSignals) throws Exception {
        waitForSignals(numSignals);
        assertSuccess();
    }

    public synchronized Exception getException() {
        return exception;
    }

    public synchronized void setException(Exception e) {
        exception = e;
        notifyAll();
    }

    @Override
    protected void onLooperPrepared() {
        synchronized (this) {
            handler = new Handler(getLooper());
            notifyAll();
        }
    }
}
