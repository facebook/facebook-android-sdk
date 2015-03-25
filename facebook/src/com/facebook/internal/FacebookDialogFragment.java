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

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import com.facebook.FacebookException;
import com.facebook.FacebookSdk;

/**
 * com.facebook.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class FacebookDialogFragment extends DialogFragment {
    private Dialog dialog;

    public static final String TAG = "FacebookDialogFragment";

    /**
     * Setter for dialog. The dialog should be set before the show method is called.
     * @param dialog The dialog that is wrapped.
     */
    public void setDialog(Dialog dialog) {
        this.dialog = dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (this.dialog == null) {
            final FragmentActivity activity = getActivity();
            Intent intent = activity.getIntent();
            Bundle params = NativeProtocol.getMethodArgumentsFromIntent(intent);

            boolean isWebFallback = params.getBoolean(NativeProtocol.WEB_DIALOG_IS_FALLBACK, false);
            WebDialog webDialog;
            if (!isWebFallback) {
                String actionName = params.getString(NativeProtocol.WEB_DIALOG_ACTION);
                Bundle webParams = params.getBundle(NativeProtocol.WEB_DIALOG_PARAMS);
                if (Utility.isNullOrEmpty(actionName)) {
                    Utility.logd(
                            TAG,
                            "Cannot start a WebDialog with an empty/missing 'actionName'");
                    activity.finish();
                    return;
                }

                webDialog = new WebDialog.Builder(activity, actionName, webParams)
                        .setOnCompleteListener(new WebDialog.OnCompleteListener() {
                            @Override
                            public void onComplete(Bundle values, FacebookException error) {
                                onCompleteWebDialog(values, error);
                            }
                        })
                        .build();
            } else {
                String url = params.getString(NativeProtocol.WEB_DIALOG_URL);
                if (Utility.isNullOrEmpty(url)) {
                    Utility.logd(
                            TAG,
                            "Cannot start a fallback WebDialog with an empty/missing 'url'");
                    activity.finish();
                    return;
                }

                String redirectUrl =
                        String.format("fb%s://bridge/", FacebookSdk.getApplicationId());
                webDialog = new FacebookWebFallbackDialog(activity, url, redirectUrl);
                webDialog.setOnCompleteListener(new WebDialog.OnCompleteListener() {
                    @Override
                    public void onComplete(Bundle values, FacebookException error) {
                        // Error data is nested in the values since this is in the form of a
                        // Native protocol response
                        onCompleteWebFallbackDialog(values);
                    }
                });
            }

            this.dialog = webDialog;
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return dialog;
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (this.dialog instanceof WebDialog) {
            ((WebDialog)this.dialog).resize();
        }
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private void onCompleteWebDialog(Bundle values, FacebookException error) {
        FragmentActivity fragmentActivity = getActivity();

        Intent resultIntent = NativeProtocol.createProtocolResultIntent(
                fragmentActivity.getIntent(),
                values,
                error);

        int resultCode = error == null ? Activity.RESULT_OK : Activity.RESULT_CANCELED;

        fragmentActivity.setResult(resultCode, resultIntent);
        fragmentActivity.finish();
    }

    private void onCompleteWebFallbackDialog(Bundle values) {
        FragmentActivity fragmentActivity = getActivity();

        Intent resultIntent = new Intent();
        resultIntent.putExtras(values == null ? new Bundle() : values);

        fragmentActivity.setResult(Activity.RESULT_OK, resultIntent);
        fragmentActivity.finish();
    }
}
