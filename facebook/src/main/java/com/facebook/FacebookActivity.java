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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.facebook.internal.FacebookDialogFragment;
import com.facebook.internal.NativeProtocol;
import com.facebook.login.LoginFragment;

/**
 * This Activity is a necessary part of the overall Facebook SDK,
 * but is not meant to be used directly. Add this Activity to your
 * AndroidManifest.xml to ensure proper handling of Facebook SDK features.
 * <pre>
 * {@code
 * <activity android:name="com.facebook.FacebookActivity"
 *           android:theme="@android:style/Theme.Translucent.NoTitleBar"
 *           android:configChanges="keyboard|keyboardHidden|screenLayout|screenSize|orientation"
 *           android:label="@string/app_name" />
 * }
 * </pre>
 * Do not start this activity directly.
 */
public class FacebookActivity extends FragmentActivity {

    public static String PASS_THROUGH_CANCEL_ACTION = "PassThrough";
    private static String FRAGMENT_TAG = "SingleFragment";

    private Fragment singleFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.com_facebook_activity_layout);

        Intent intent = getIntent();
        if (PASS_THROUGH_CANCEL_ACTION.equals(intent.getAction())) {
            handlePassThroughError();
            return;
        }

        FragmentManager manager = getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(FRAGMENT_TAG);

        if (fragment == null) {
            if (FacebookDialogFragment.TAG.equals(intent.getAction())) {
                FacebookDialogFragment dialogFragment = new FacebookDialogFragment();
                dialogFragment.setRetainInstance(true);
                dialogFragment.show(manager, FRAGMENT_TAG);

                fragment = dialogFragment;
            } else {
                fragment = new LoginFragment();
                fragment.setRetainInstance(true);
                manager.beginTransaction()
                        .add(R.id.com_facebook_fragment_container, fragment, FRAGMENT_TAG)
                        .commit();
            }
        }

        singleFragment = fragment;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (singleFragment != null) {
            singleFragment.onConfigurationChanged(newConfig);
        }
    }

    private void handlePassThroughError() {
        Intent requestIntent = getIntent();

        // The error we need to respond with is passed to us as method arguments.
        Bundle errorResults = NativeProtocol.getMethodArgumentsFromIntent(requestIntent);
        FacebookException exception = NativeProtocol.getExceptionFromErrorData(errorResults);

        // Create a result intent that is formed based on the request intent
        Intent resultIntent = NativeProtocol.createProtocolResultIntent(
                requestIntent,
                null,
                exception);

        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }
}
