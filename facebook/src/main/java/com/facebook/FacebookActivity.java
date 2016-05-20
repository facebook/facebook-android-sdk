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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import com.facebook.internal.FacebookDialogFragment;
import com.facebook.internal.NativeProtocol;
import com.facebook.internal.Utility;
import com.facebook.login.LoginFragment;
import com.facebook.login.LoginManager;
import com.facebook.share.DeviceShareDialog;
import com.facebook.share.internal.DeviceShareDialogFragment;
import com.facebook.share.model.ShareContent;

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
    private static final int API_EC_DIALOG_CANCEL = 4201;
    private static final String TAG = FacebookActivity.class.getName();

    private Fragment singleFragment;

    private static final String getRedirectUrl() {
        return "fb" + FacebookSdk.getApplicationId() + "://authorize";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Some apps using this sdk don't put the sdk initialize code in the application
        // on create method. This can cause issues when opening this activity after an application
        // has been killed since the sdk won't be initialized. Attempt to initialize the sdk
        // here if it hasn't already been initialized.
        if (!FacebookSdk.isInitialized()) {
            Log.d(
                TAG,
                "Facebook SDK not initialized. Make sure you call sdkInitialize inside " +
                        "your Application's onCreate method.");
            FacebookSdk.sdkInitialize(getApplicationContext());
        }

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
            } else if (DeviceShareDialogFragment.TAG.equals(intent.getAction())) {
                DeviceShareDialogFragment dialogFragment = new DeviceShareDialogFragment();
                dialogFragment.setRetainInstance(true);
                dialogFragment.setShareContent((ShareContent) intent.getParcelableExtra("content"));
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String url = intent.getStringExtra("url");
        handlePassThroughUrl(url);
    }

    public Fragment getCurrentFragment() {
        return singleFragment;
    }

    private void handlePassThroughError() {
        Intent requestIntent = getIntent();

        // The error we need to respond with is passed to us as method arguments.
        Bundle errorResults = NativeProtocol.getMethodArgumentsFromIntent(requestIntent);
        FacebookException exception = NativeProtocol.getExceptionFromErrorData(errorResults);

        sendResult(null, exception);
    }

    private void handlePassThroughUrl(String url) {
        if (url != null && url.startsWith(getRedirectUrl())) {
            Uri uri = Uri.parse(url);
            Bundle values = Utility.parseUrlQueryString(uri.getQuery());
            values.putAll(Utility.parseUrlQueryString(uri.getFragment()));

            if (!(singleFragment instanceof LoginFragment)
                    || !((LoginFragment) singleFragment).validateChallengeParam(values)) {
                sendResult(null, new FacebookException("Invalid state parameter"));
            }

            String error = values.getString("error");
            if (error == null) {
                error = values.getString("error_type");
            }

            String errorMessage = values.getString("error_msg");
            if (errorMessage == null) {
                errorMessage = values.getString("error_message");
            }
            if (errorMessage == null) {
                errorMessage = values.getString("error_description");
            }
            String errorCodeString = values.getString("error_code");
            int errorCode = FacebookRequestError.INVALID_ERROR_CODE;
            if (!Utility.isNullOrEmpty(errorCodeString)) {
                try {
                    errorCode = Integer.parseInt(errorCodeString);
                } catch (NumberFormatException ex) {
                    errorCode = FacebookRequestError.INVALID_ERROR_CODE;
                }
            }

            if (Utility.isNullOrEmpty(error) && Utility.isNullOrEmpty(errorMessage)
                    && errorCode == FacebookRequestError.INVALID_ERROR_CODE) {
                sendResult(values, null);
            } else if (error != null && (error.equals("access_denied") ||
                    error.equals("OAuthAccessDeniedException"))) {
                sendResult(null, new FacebookOperationCanceledException());
            } else if (errorCode == API_EC_DIALOG_CANCEL) {
                sendResult(null, new FacebookOperationCanceledException());
            } else {
                FacebookRequestError requestError =
                        new FacebookRequestError(errorCode, error, errorMessage);
                sendResult(null, new FacebookServiceException(requestError, errorMessage));
            }
        }
    }

    public void sendResult(Bundle results, FacebookException error) {
        int resultCode;
        Intent resultIntent = getIntent();
        if (error == null) {
            resultCode = RESULT_OK;
            LoginManager.setSuccessResult(resultIntent, results);
        } else {
            resultCode = RESULT_CANCELED;
            resultIntent = NativeProtocol.createProtocolResultIntent(
                    resultIntent,
                    results,
                    error);
        }
        setResult(resultCode, resultIntent);
        finish();
    }
}
