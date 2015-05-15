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
import com.facebook.FacebookException;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.share.internal.ResultProcessor;
import com.facebook.share.internal.ShareConstants;
import com.facebook.share.internal.ShareInternalUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog for joining app groups
 */
public class JoinAppGroupDialog extends FacebookDialogBase<String, JoinAppGroupDialog.Result> {

    private static final String JOIN_GAME_GROUP_DIALOG = "game_group_join";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.AppGroupJoin.toRequestCode();

    /**
     * Helper object for handling the result from a join app group dialog.
     */
    public static final class Result {
        private final Bundle data;

        private Result(Bundle bundle) {
            this.data = bundle;
        }

        /**
         * Returns the result data from the dialog;
         * @return the result data.
         */
        public Bundle getData() {
            return data;
        }
    }

    /**
     * Indicates whether the join app group dialog can be shown.
     *
     * @return true if the dialog can be shown
     */
    public static boolean canShow() {
        return true;
    }

    /**
     * Shows an {@link JoinAppGroupDialog} to join a group with the passed in Id, using
     * the passed in activity. No callback will be invoked.
     *
     * @param activity Activity hosting the dialog
     * @param groupId Id of the group to join
     */
    public static void show(
            final Activity activity,
            final String groupId) {
        new JoinAppGroupDialog(activity).show(groupId);
    }

    /**
     * Shows an {@link JoinAppGroupDialog} to join a group with the passed in Id, using
     * the passed in fragment. No callback will be invoked.
     *
     * @param fragment Fragment hosting the dialog
     * @param groupId Id of the group to join
     */
    public static void show(
            final Fragment fragment,
            final String groupId) {
        new JoinAppGroupDialog(fragment).show(groupId);
    }

    /**
     * Constructs a JoinAppGroupDialog.
     * @param activity Activity hosting the dialog.
     */
    public JoinAppGroupDialog(final Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a JoinAppGroupDialog.
     * @param fragment Fragment hosting the dialog.
     */
    public JoinAppGroupDialog(final Fragment fragment) {
        super(fragment, DEFAULT_REQUEST_CODE);
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

        callbackManager.registerCallback(getRequestCode(), callbackManagerCallback);
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
        public boolean canShow(final String content) {
            return true;
        }

        @Override
        public AppCall createAppCall(final String content) {
            AppCall appCall = createBaseAppCall();
            Bundle params = new Bundle();
            params.putString(ShareConstants.WEB_DIALOG_PARAM_ID, content);

            DialogPresenter.setupAppCallForWebDialog(
                    appCall,
                    JOIN_GAME_GROUP_DIALOG,
                    params);

            return appCall;
        }
    }
}
