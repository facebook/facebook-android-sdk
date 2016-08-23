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


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.internal.CustomTab;

public class CustomTabMainActivity extends Activity {
    public static final String EXTRA_PARAMS =
            CustomTabMainActivity.class.getSimpleName() + ".extra_params";
    public static final String EXTRA_CHROME_PACKAGE =
            CustomTabMainActivity.class.getSimpleName() + ".extra_chromePackage";
    public static final String EXTRA_URL =
            CustomTabMainActivity.class.getSimpleName() + ".extra_url";
    public static final String REFRESH_ACTION =
            CustomTabMainActivity.class.getSimpleName() + ".action_refresh";
    public static final String getRedirectUrl() {
        return "fb" + FacebookSdk.getApplicationId() + "://authorize";
    }
    private static final String OAUTH_DIALOG = "oauth";

    private boolean shouldCloseCustomTab = true;
    private BroadcastReceiver redirectReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Custom Tab Redirects should not be creating a new instance of this activity
        if (CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION.equals(getIntent().getAction())) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            Bundle parameters = getIntent().getBundleExtra(EXTRA_PARAMS);
            String chromePackage = getIntent().getStringExtra(EXTRA_CHROME_PACKAGE);

            CustomTab customTab = new CustomTab(OAUTH_DIALOG, parameters);
            customTab.openCustomTab(this, chromePackage);

            shouldCloseCustomTab = false;

            // This activity will receive a broadcast if it can't be opened from the back stack
            redirectReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Remove the custom tab on top of this activity.
                    Intent newIntent =
                            new Intent(CustomTabMainActivity.this, CustomTabMainActivity.class);
                    newIntent.setAction(REFRESH_ACTION);
                    newIntent.putExtra(EXTRA_URL, intent.getStringExtra(EXTRA_URL));
                    newIntent.addFlags(
                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(newIntent);
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    redirectReceiver,
                    new IntentFilter(CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION)
            );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (REFRESH_ACTION.equals(intent.getAction())) {
            // The custom tab is now destroyed so we can finish the redirect activity
            Intent broadcast = new Intent(CustomTabActivity.DESTROY_ACTION);
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
            sendResult(RESULT_OK, intent);
        } else if (CustomTabActivity.CUSTOM_TAB_REDIRECT_ACTION.equals(intent.getAction())) {
            // We have successfully redirected back to this activity. Return the result and close.
            sendResult(RESULT_OK, intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldCloseCustomTab) {
            // The custom tab was closed without getting a result.
            sendResult(RESULT_CANCELED, null);
        }
        shouldCloseCustomTab = true;
    }

    private void sendResult(int resultCode, Intent resultIntent) {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(redirectReceiver);
        if (resultIntent != null) {
            setResult(resultCode, resultIntent);
        } else {
            setResult(resultCode);
        }
        finish();
    }
}
