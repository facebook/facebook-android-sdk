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

package com.facebook.internal;

import android.content.Intent;

import java.util.UUID;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class AppCall {
    private static AppCall currentPendingCall;

    public static AppCall getCurrentPendingCall() {
        return currentPendingCall;
    }

    public static synchronized AppCall finishPendingCall(
            UUID callId,
            int requestCode) {
        AppCall pendingCall = getCurrentPendingCall();
        if (pendingCall == null ||
                !pendingCall.getCallId().equals(callId) ||
                pendingCall.getRequestCode() != requestCode) {
            return null;
        }

        setCurrentPendingCall(null);

        return pendingCall;
    }

    private static synchronized boolean setCurrentPendingCall(
            AppCall appCall) {
        AppCall oldAppCall = getCurrentPendingCall();
        currentPendingCall = appCall;

        return oldAppCall != null;
    }

    private UUID callId;
    private Intent requestIntent;
    private int requestCode;

    /**
     * Constructor.
     *
     * @param requestCode the request code for this app call
     */
    public AppCall(int requestCode) {
        this(requestCode, UUID.randomUUID());
    }

    /**
     * Constructor
     *
     * @param requestCode the request code for this app call
     * @param callId the call Id for this app call
     */
    public AppCall(int requestCode, UUID callId) {
        this.callId = callId;
        this.requestCode = requestCode;
    }

    /**
     * Returns the Intent that was used to initiate this call to the
     * Facebook application.
     *
     * @return the Intent
     */
    public Intent getRequestIntent() {
        return requestIntent;
    }

    /**
     * Returns the unique ID of this call to the Facebook application.
     *
     * @return the unique ID
     */
    public UUID getCallId() {
        return callId;
    }

    /**
     * Gets the request code for this call.
     *
     * @return the request code that will be passed to
     * handleActivityResult upon completion.
     */
    public int getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(int requestCode) {
        this.requestCode = requestCode;
    }

    public void setRequestIntent(Intent requestIntent) {
        this.requestIntent = requestIntent;
    }

    /**
     *
     * @return Returns true if there was another AppCall that was
     * already pending and is now canceled
     */
    public boolean setPending() {
        return setCurrentPendingCall(this);
    }
}
