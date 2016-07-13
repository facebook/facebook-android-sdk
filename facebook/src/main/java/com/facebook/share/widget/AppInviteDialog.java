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

package com.facebook.share.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.FacebookCallback;
import com.facebook.internal.*;
import com.facebook.share.internal.*;
import com.facebook.share.model.AppInviteContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog for inviting users.
 */
public class AppInviteDialog
        extends FacebookDialogBase<AppInviteContent, AppInviteDialog.Result> {

    /**
     * Helper object for handling the result from an app invites dialog.
     */
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

    private static final String TAG = "AppInviteDialog";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.AppInvite.toRequestCode();

    /**
     * Indicates whether the app invite dialog can be shown.
     *
     * @return true if the dialog can be shown
     */
    public static boolean canShow() {
        return canShowNativeDialog() || canShowWebFallback();
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.AppInviteContent} using
     * the provided Activity. No callback will be invoked.
     *
     * @param activity          Activity to use to share the provided content
     * @param appInviteContent Content of the app invite to send
     */
    public static void show(
            final Activity activity,
            final AppInviteContent appInviteContent) {
        new AppInviteDialog(activity)
                .show(appInviteContent);
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.AppInviteContent} using
     * the provided Fragment. No callback will be invoked.
     *
     * @param fragment          android.support.v4.app.Fragment to use to share the provided content
     * @param appInviteContent Content of the app invite to send
     */
    public static void show(
            final Fragment fragment,
            final AppInviteContent appInviteContent) {
        show(new FragmentWrapper(fragment), appInviteContent);
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.AppInviteContent} using
     * the provided Fragment. No callback will be invoked.
     *
     * @param fragment          android.app.Fragment to use to share the provided content
     * @param appInviteContent Content of the app invite to send
     */
    public static void show(
            final android.app.Fragment fragment,
            final AppInviteContent appInviteContent) {
        show(new FragmentWrapper(fragment), appInviteContent);
    }

    private static void show(
            final FragmentWrapper fragmentWrapper,
            final AppInviteContent appInviteContent) {
        new AppInviteDialog(fragmentWrapper)
                .show(appInviteContent);
    }

    private static boolean canShowNativeDialog() {
        return DialogPresenter.canPresentNativeDialogWithFeature(getFeature());
    }

    private static boolean canShowWebFallback() {
        return DialogPresenter.canPresentWebFallbackDialogWithFeature(getFeature());
    }

    /**
     * Constructs a new AppInviteDialog.
     *
     * @param activity Activity to use to share the provided content.
     */
    public AppInviteDialog(final Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a new AppInviteDialog.
     *
     * @param fragment android.support.v4.app.Fragment to use to share the provided content.
     */
    public AppInviteDialog(final Fragment fragment) {
        this(new FragmentWrapper(fragment));
    }

    /**
     * Constructs a new AppInviteDialog.
     *
     * @param fragment android.app.Fragment to use to share the provided content.
     */
    public AppInviteDialog(final android.app.Fragment fragment) {
        this(new FragmentWrapper(fragment));
    }

    private AppInviteDialog(final FragmentWrapper fragment) {
        super(fragment, DEFAULT_REQUEST_CODE);
    }

    protected void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {
        final ResultProcessor resultProcessor = (callback == null)
                ? null
                : new ResultProcessor(callback) {
            @Override
            public void onSuccess(AppCall appCall, Bundle results) {
                String gesture = ShareInternalUtility.getNativeDialogCompletionGesture(results);
                if ("cancel".equalsIgnoreCase(gesture)) {
                    callback.onCancel();
                } else {
                    callback.onSuccess(new Result(results));
                }
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

    private class NativeHandler extends ModeHandler {
        @Override
        public boolean canShow(AppInviteContent content, boolean isBestEffort) {
            return AppInviteDialog.canShowNativeDialog();
        }

        @Override
        public AppCall createAppCall(final AppInviteContent content) {
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
                            // App Invites are not supported with legacy fb4a devices.
                            // We should never get here
                            Log.e(TAG, "Attempting to present the AppInviteDialog with " +
                                    "an outdated Facebook app on the device");
                            return new Bundle();
                        }
                    },
                    getFeature());

            return appCall;
        }
    }

    private class WebFallbackHandler extends ModeHandler {
        @Override
        public boolean canShow(final AppInviteContent content, boolean isBestEffort) {
            return AppInviteDialog.canShowWebFallback();
        }

        @Override
        public AppCall createAppCall(final AppInviteContent content) {
            final AppCall appCall = createBaseAppCall();

            DialogPresenter.setupAppCallForWebFallbackDialog(
                    appCall,
                    createParameters(content),
                    getFeature());

            return appCall;
        }
    }

    private static DialogFeature getFeature() {
        return AppInviteDialogFeature.APP_INVITES_DIALOG;
    }

    private static Bundle createParameters(final AppInviteContent content) {
        Bundle params = new Bundle();
        params.putString(ShareConstants.APPLINK_URL, content.getApplinkUrl());
        params.putString(ShareConstants.PREVIEW_IMAGE_URL, content.getPreviewImageUrl());
        params.putString(
                ShareConstants.DESTINATION,
                content.getDestination().toString()
        );

        String promoCode = content.getPromotionCode();
        promoCode = promoCode != null ? promoCode : "";
        String promoText = content.getPromotionText();

        if (!TextUtils.isEmpty(promoText)) {
            // Encode deeplink context as json array.
            try {
                JSONObject deeplinkContent = new JSONObject();
                deeplinkContent.put(ShareConstants.PROMO_CODE, promoCode);
                deeplinkContent.put(ShareConstants.PROMO_TEXT, promoText);

                params.putString(ShareConstants.DEEPLINK_CONTEXT, deeplinkContent.toString());
                params.putString(ShareConstants.PROMO_CODE, promoCode);
                params.putString(ShareConstants.PROMO_TEXT, promoText);
            } catch (JSONException e) {
                Log.e(TAG, "Json Exception in creating deeplink context");
                // Ignore it since this is optional.
            }
        }

        return params;
    }
}
