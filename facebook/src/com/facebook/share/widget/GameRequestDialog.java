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

import com.facebook.FacebookCallback;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogPresenter;
import com.facebook.share.internal.GameRequestValidation;
import com.facebook.share.internal.ResultProcessor;
import com.facebook.share.internal.ShareConstants;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.internal.WebDialogParameters;
import com.facebook.share.model.GameRequestContent;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides functionality to send requests in games.
 * see https://developers.facebook.com/docs/games/requests
 */
public class GameRequestDialog
        extends FacebookDialogBase<GameRequestContent, GameRequestDialog.Result> {

    /**
     * Helper object for handling the result from a requests dialog
     */
    public static final class Result {
        String requestId;
        List<String> to;

        private Result(Bundle results) {
            this.requestId = results.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_REQUEST_ID);
            this.to = new ArrayList<String>();
            while (results.containsKey(String.format(
                    ShareConstants.WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER, this.to.size()))) {
                this.to.add(results.getString(String.format(
                        ShareConstants.WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER, this.to.size())));
            }
        }

        /**
         * Returns the request ID.
         * @return the request ID.
         */
        public String getRequestId() {
            return requestId;
        }

        /**
         * Returns request recipients.
         * @return request recipients
         */
        public List<String> getRequestRecipients() {
            return to;
        }
    }

    // The actual value of the string is different since that is what the web dialog is actually
    // called on the server.
    private static final String GAME_REQUEST_DIALOG = "apprequests";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.GameRequest.toRequestCode();

    /**
     * Indicates whether the game request dialog can be shown.
     *
     * @return true if the dialog can be shown
     */
    public static boolean canShow() {
        return true;
    }

    /**
     * Shows a {@link GameRequestDialog} to send a request, using
     * the passed in activity. No callback will be invoked.
     *
     * @param activity Activity hosting the dialog.
     * @param gameRequestContent Content of the request.
     */
    public static void show(final Activity activity, final GameRequestContent gameRequestContent) {
        new GameRequestDialog(activity).show(gameRequestContent);
    }

    /**
     * Shows a {@link GameRequestDialog} to send a request, using
     * the passed in activity. No callback will be invoked.
     *
     * @param fragment Fragment hosting the dialog.
     * @param gameRequestContent Content of the request.
     */
    public static void show(final Fragment fragment, final GameRequestContent gameRequestContent) {
        new GameRequestDialog(fragment).show(gameRequestContent);
    }

    /**
     * Constructs a new RequestDialog.
     * @param activity Activity hosting the dialog.
     */
    public GameRequestDialog(Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a new RequestDialog.
     * @param fragment Fragment hosting the dialog.
     */
    public GameRequestDialog(Fragment fragment) {
        super(fragment, DEFAULT_REQUEST_CODE);
    }

    @Override
    protected void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {
        final ResultProcessor resultProcessor = (callback == null)
                ? null
                : new ResultProcessor(callback) {
            @Override
            public void onSuccess(AppCall appCall, Bundle results) {
                if (results != null) {
                    callback.onSuccess(new Result(results));
                } else {
                    onCancel(appCall);
                }
            }
        };

       callbackManager.registerCallback(
                getRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        return ShareInternalUtility.handleActivityResult(
                                getRequestCode(),
                                resultCode,
                                data,
                                resultProcessor);
                    }
                });
    }

    @Override
    protected AppCall createBaseAppCall() {
        return new AppCall(getRequestCode());
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        ArrayList<ModeHandler> handlers = new ArrayList<>();
        handlers.add(new WebHandler());

        return handlers;
    }

    private class WebHandler extends ModeHandler {
        @Override
        public boolean canShow(final GameRequestContent content) {
            return true;
        }

        @Override
        public AppCall createAppCall(final GameRequestContent content) {
            GameRequestValidation.validate(content);
            AppCall appCall = createBaseAppCall();
            DialogPresenter.setupAppCallForWebDialog(
                    appCall,
                    GAME_REQUEST_DIALOG,
                    WebDialogParameters.create(content));

            return appCall;
        }

    }
}
