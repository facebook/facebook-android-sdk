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

package com.facebook.share.internal;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.facebook.FacebookCallback;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogFeature;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FragmentWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class LikeDialog extends FacebookDialogBase<LikeContent, LikeDialog.Result> {

    private static final String TAG = "LikeDialog";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.Like.toRequestCode();

    public static final class Result {
        private final Bundle bundle;

        /**
         * Constructor
         *
         * @param bundle the results bundle
         */
        public Result(Bundle bundle) {
            this.bundle = bundle;
        }

        /**
         * Returns the results data as a Bundle.
         *
         * @return the results bundle
         */
        public Bundle getData() {
            return bundle;
        }
    }

    // Public for internal use
    public static boolean canShowNativeDialog() {
        return DialogPresenter.canPresentNativeDialogWithFeature(getFeature());
    }

    // Public for internal use
    public static boolean canShowWebFallback() {
        return DialogPresenter.canPresentWebFallbackDialogWithFeature(getFeature());
    }

    public LikeDialog(Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }

    public LikeDialog(Fragment fragment) {
        this(new FragmentWrapper(fragment));
    }

    public LikeDialog(android.app.Fragment fragment) {
        this(new FragmentWrapper(fragment));
    }

    public LikeDialog(FragmentWrapper fragmentWrapper) {
        super(fragmentWrapper, DEFAULT_REQUEST_CODE);
    }

    @Override
    protected AppCall createBaseAppCall() {
        return new AppCall(getRequestCode());
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        ArrayList<ModeHandler> handlers = new ArrayList<>();
        handlers.add(new NativeHandler());
        handlers.add(new WebFallbackHandler());

        return handlers;
    }

    @Override
    protected void registerCallbackImpl (
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {
        final ResultProcessor resultProcessor = (callback == null)
                ? null
                : new ResultProcessor(callback) {
            @Override
            public void onSuccess(AppCall appCall, Bundle results) {
                callback.onSuccess(new Result(results));
            }
        };

        CallbackManagerImpl.Callback callbackManagerCallback = new CallbackManagerImpl.Callback() {
            @Override
            public boolean onActivityResult(int resultCode, Intent data) {
                return ShareInternalUtility.handleActivityResult(
                        getRequestCode(),
                        resultCode,
                        data,
                        resultProcessor);
            }
        };

        callbackManager.registerCallback(
                getRequestCode(),
                callbackManagerCallback);
    }

    private class NativeHandler extends ModeHandler {
        @Override
        public boolean canShow(final LikeContent content, boolean isBestEffort) {
            return (content != null) && LikeDialog.canShowNativeDialog();
        }

        @Override
        public AppCall createAppCall(final LikeContent content) {
            final AppCall appCall = createBaseAppCall();

            DialogPresenter.setupAppCallForNativeDialog(
                    appCall,
                    new DialogPresenter.ParameterProvider() {
                        @Override
                        public Bundle getParameters() {
                            return createParameters(content);
                        }

                        @Override
                        public Bundle getLegacyParameters() {
                            // Like is not supported with legacy fb4a devices. Should never get here
                            Log.e(TAG, "Attempting to present the Like Dialog with an outdated " +
                                    "Facebook app on the device");
                            return new Bundle();
                        }
                    },
                    getFeature());

            return appCall;
        }
    }

    private class WebFallbackHandler extends ModeHandler {
        @Override
        public boolean canShow(final LikeContent content, boolean isBestEffort) {
            return (content != null) && LikeDialog.canShowWebFallback();
        }

        @Override
        public AppCall createAppCall(final LikeContent content) {
            final AppCall appCall = createBaseAppCall();

            DialogPresenter.setupAppCallForWebFallbackDialog(
                    appCall,
                    createParameters(content),
                    getFeature());

            return appCall;
        }
    }

    private static DialogFeature getFeature() {
        return LikeDialogFeature.LIKE_DIALOG;
    }

    private static Bundle createParameters(final LikeContent likeContent) {
        Bundle params = new Bundle();

        params.putString(ShareConstants.OBJECT_ID, likeContent.getObjectId());
        params.putString(ShareConstants.OBJECT_TYPE, likeContent.getObjectType());

        return params;
    }
}
