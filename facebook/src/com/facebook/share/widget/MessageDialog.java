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
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.facebook.FacebookCallback;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogFeature;
import com.facebook.internal.DialogPresenter;
import com.facebook.share.Sharer;
import com.facebook.share.internal.LegacyNativeDialogParameters;
import com.facebook.share.internal.MessageDialogFeature;
import com.facebook.share.internal.NativeDialogParameters;
import com.facebook.share.internal.OpenGraphMessageDialogFeature;
import com.facebook.share.internal.ShareContentValidation;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides functionality to send content via the Facebook Message Dialog
 */
public final class MessageDialog
        extends FacebookDialogBase<ShareContent, Sharer.Result>
        implements Sharer {

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.Message.toRequestCode();

    private boolean shouldFailOnDataError = false;

    /**
     * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
     * Activity. No callback will be invoked.
     *
     * @param activity Activity to use to send the provided content
     * @param shareContent Content to send
     */
    public static void show(
            final Activity activity,
            final ShareContent shareContent) {
        new MessageDialog(activity).show(shareContent);
    }

    /**
     * Helper to show the provided {@link com.facebook.share.model.ShareContent} using the provided
     * Fragment. No callback will be invoked.
     *
     * @param fragment Fragment to use to send the provided content
     * @param shareContent Content to send
     */
    public static void show(
            final Fragment fragment,
            final ShareContent shareContent) {
        new MessageDialog(fragment).show(shareContent);
    }

    /**
     * Indicates whether it is possible to show the dialog for
     * {@link com.facebook.share.model.ShareContent} of the specified type.
     *
     * @param contentType Class of the intended {@link com.facebook.share.model.ShareContent} to
     *                    send.
     * @return True if the specified content type can be shown via the dialog
     */
    public static boolean canShow(Class<? extends ShareContent> contentType) {
        DialogFeature feature = getFeature(contentType);

        return feature != null && DialogPresenter.canPresentNativeDialogWithFeature(feature);
    }

    /**
     * Constructs a MessageDialog.
     * @param activity Activity to use to send the provided content.
     */
    public MessageDialog(Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);

        ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a MessageDialog.
     * @param fragment Fragment to use to send the provided content.
     */
    public MessageDialog(Fragment fragment) {
        super(fragment, DEFAULT_REQUEST_CODE);

        ShareInternalUtility.registerStaticShareCallback(DEFAULT_REQUEST_CODE);
    }

    // for SendButton use only
    MessageDialog(Activity activity, int requestCode) {
        super(activity, requestCode);

        ShareInternalUtility.registerStaticShareCallback(requestCode);
    }

    // for SendButton use only
    MessageDialog(Fragment fragment, int requestCode) {
        super(fragment, requestCode);

        ShareInternalUtility.registerStaticShareCallback(requestCode);
    }

    @Override
    protected void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {
        ShareInternalUtility.registerSharerCallback(getRequestCode(), callbackManager, callback);
    }

    @Override
    public boolean getShouldFailOnDataError() {
        return this.shouldFailOnDataError;
    }

    @Override
    public void setShouldFailOnDataError(boolean shouldFailOnDataError) {
        this.shouldFailOnDataError = shouldFailOnDataError;
    }

    @Override
    protected AppCall createBaseAppCall() {
        return new AppCall(getRequestCode());
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        ArrayList<ModeHandler> handlers = new ArrayList<>();
        handlers.add(new NativeHandler());

        return handlers;
    }

    private class NativeHandler extends ModeHandler {
        @Override
        public boolean canShow(final ShareContent shareContent) {
            return shareContent != null && MessageDialog.canShow(shareContent.getClass());
        }

        @Override
        public AppCall createAppCall(final ShareContent content) {
            ShareContentValidation.validateForMessage(content);

            final AppCall appCall = createBaseAppCall();
            final boolean shouldFailOnDataError = getShouldFailOnDataError();
            final Activity activity = getActivityContext();

            DialogPresenter.setupAppCallForNativeDialog(
                    appCall,
                    new DialogPresenter.ParameterProvider() {
                        @Override
                        public Bundle getParameters() {
                            return NativeDialogParameters.create(
                                    appCall.getCallId(),
                                    content,
                                    shouldFailOnDataError);
                        }

                        @Override
                        public Bundle getLegacyParameters() {
                            return LegacyNativeDialogParameters.create(
                                    appCall.getCallId(),
                                    content,
                                    shouldFailOnDataError);
                        }
                    },
                    getFeature(content.getClass()));

            return appCall;
        }
    }

    private static DialogFeature getFeature(
            Class<? extends ShareContent> contentType) {
        if (ShareLinkContent.class.isAssignableFrom(contentType)) {
            return MessageDialogFeature.MESSAGE_DIALOG;
        } else if (SharePhotoContent.class.isAssignableFrom(contentType)) {
            return MessageDialogFeature.PHOTOS;
        } else if (ShareVideoContent.class.isAssignableFrom(contentType)) {
            return MessageDialogFeature.VIDEO;
        } else if (ShareOpenGraphContent.class.isAssignableFrom(contentType)) {
            return OpenGraphMessageDialogFeature.OG_MESSAGE_DIALOG;
        }
        return null;
    }
}
