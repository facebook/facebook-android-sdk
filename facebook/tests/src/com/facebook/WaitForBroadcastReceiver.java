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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.ConditionVariable;
import android.os.Looper;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WaitForBroadcastReceiver extends BroadcastReceiver {
    static final int DEFAULT_TIMEOUT_MILLISECONDS = 10 * 1000;
    static int idGenerator = 0;
    final int id = idGenerator++;

    ConditionVariable condition = new ConditionVariable(true);
    int expectCount;
    int actualCount;
    List<Intent> receivedIntents = new ArrayList<Intent>();
    List<String> expectedActions = new ArrayList<String>();
    List<Intent> unexpectedIntents = new ArrayList<Intent>();

    public WaitForBroadcastReceiver() {
    }

    public WaitForBroadcastReceiver(String... expectedActions) {
        this.expectedActions = Arrays.asList(expectedActions);
    }

    public void incrementExpectCount() {
        incrementExpectCount(1);
    }

    public void incrementExpectCount(int n) {
        expectCount += n;
        if (actualCount < expectCount) {
            condition.close();
        }
    }

    public void waitForExpectedCalls() {
        this.waitForExpectedCalls(DEFAULT_TIMEOUT_MILLISECONDS);
    }

    public void waitForExpectedCalls(long timeoutMillis) {
        if (!condition.block(timeoutMillis)) {
            Assert.assertTrue(false);
        }
    }

    public List<Intent> getReceivedIntents() {
        return receivedIntents;
    }

    public List<Intent> getUnexpectedIntents() {
        return unexpectedIntents;
    }

    public static void incrementExpectCounts(WaitForBroadcastReceiver... receivers) {
        for (WaitForBroadcastReceiver receiver : receivers) {
            receiver.incrementExpectCount();
        }
    }

    public static void waitForExpectedCalls(WaitForBroadcastReceiver... receivers) {
        for (WaitForBroadcastReceiver receiver : receivers) {
            receiver.waitForExpectedCalls();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        receivedIntents.add(intent);

        if (!expectedActions.isEmpty()) {
            String action = intent.getAction();
            if (!expectedActions.contains(action)) {
                unexpectedIntents.add(intent);
                return;
            }
        }

        if (++actualCount == expectCount) {
            condition.open();
        }

        Assert.assertTrue("expecting " + expectCount + "broadcasts, but received " + actualCount,
                actualCount <= expectCount);
        Assert.assertEquals("BroadcastReceiver should receive on main UI thread",
                Thread.currentThread(), Looper.getMainLooper().getThread());
    }
}
