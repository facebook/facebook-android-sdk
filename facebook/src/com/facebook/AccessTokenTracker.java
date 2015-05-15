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
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.internal.Validate;


/**
 * This class can be extended to receive notifications of access token changes. The {@link
 * #stopTracking()} method should be called in the onDestroy() method of the receiving Activity or
 * Fragment.
 */
public abstract class AccessTokenTracker {

    private final BroadcastReceiver receiver;
    private final LocalBroadcastManager broadcastManager;
    private boolean isTracking = false;

    /**
     * The method that will be called with the access token changes.
     * @param oldAccessToken The access token before the change.
     * @param currentAccessToken The new access token.
     */
    protected abstract void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                        AccessToken currentAccessToken);

    /**
     * The constructor.
     */
    public AccessTokenTracker() {
        Validate.sdkInitialized();

        this.receiver = new CurrentAccessTokenBroadcastReceiver();
        this.broadcastManager = LocalBroadcastManager.getInstance(
                FacebookSdk.getApplicationContext());

        startTracking();
    }

    /**
     * Starts tracking the current access token
     */
    public void startTracking() {
        if (isTracking) {
            return;
        }

        addBroadcastReceiver();

        isTracking = true;
    }

    /**
     * Stops tracking the current access token.
     */
    public void stopTracking() {
        if (!isTracking) {
            return;
        }

        broadcastManager.unregisterReceiver(receiver);
        isTracking = false;
    }

    /**
     * Gets whether the tracker is tracking the current access token.
     * @return true if the tracker is tracking the current access token, false if not
     */
    public boolean isTracking() {
        return isTracking;
    }

    private class CurrentAccessTokenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED.equals(intent.getAction())) {

                AccessToken oldAccessToken = (AccessToken) intent
                        .getParcelableExtra(AccessTokenManager.EXTRA_OLD_ACCESS_TOKEN);
                AccessToken newAccessToken = (AccessToken) intent
                        .getParcelableExtra(AccessTokenManager.EXTRA_NEW_ACCESS_TOKEN);

                onCurrentAccessTokenChanged(oldAccessToken, newAccessToken);
            }
        }
    }

    private void addBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AccessTokenManager.ACTION_CURRENT_ACCESS_TOKEN_CHANGED);

        broadcastManager.registerReceiver(receiver, filter);
    }
}
