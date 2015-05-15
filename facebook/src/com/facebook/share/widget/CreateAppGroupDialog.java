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
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.DialogPresenter;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.share.internal.ResultProcessor;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.internal.WebDialogParameters;
import com.facebook.share.model.AppGroupCreationContent;

import java.util.ArrayList;
import java.util.List;

/**
 * A dialog for creating app groups
 */
public class CreateAppGroupDialog
        extends FacebookDialogBase<AppGroupCreationContent, CreateAppGroupDialog.Result> {

    private static final String GAME_GROUP_CREATION_DIALOG = "game_group_create";

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.AppGroupCreate.toRequestCode();

    /**
     * Helper object for handling the result from a create app group dialog.
     */
    public static final class Result {
        private final String id;

        private Result(String id) {
            this.id = id;
        }

        /**
         * Get the ID of the created group.
         * @return the id of the group.
         */
        public String getId() {
            return id;
        }
    }

    /**
     * Indicates whether the create app group dialog can be shown.
     *
     * @return true if the dialog can be shown
     */
    public static boolean canShow() {
        return true;
    }

    /**
     * Shows an {@link CreateAppGroupDialog} to create a group with the passed in content, using
     * the passed in activity. No callback will be invoked.
     *
     * @param activity Activity hosting the dialog
     * @param appGroupCreationContent Content describing the group to be created
     */
    public static void show(
            final Activity activity,
            final AppGroupCreationContent appGroupCreationContent) {
        new CreateAppGroupDialog(activity).show(appGroupCreationContent);
    }

    /**
     * Shows an {@link CreateAppGroupDialog} to create a group with the passed in content, using
     * the passed in fragment. No callback will be invoked.
     *
     * @param fragment Fragment hosting the dialog
     * @param appGroupCreationContent Content describing the group to be created
     */
    public static void show(
            final Fragment fragment,
            AppGroupCreationContent appGroupCreationContent) {
        new CreateAppGroupDialog(fragment).show(appGroupCreationContent);
    }

    /**
     * Constructs a new CreateAppGroupDialog.
     * @param activity Activity hosting the dialog.
     */
    public CreateAppGroupDialog(final Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }

    /**
     * Constructs a new CreateAppGroupDialog.
     * @param fragment Fragment hosting the dialog.
     */
    public CreateAppGroupDialog(final Fragment fragment) {
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
                callback.onSuccess(new Result(results.getString("id")));
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
        public boolean canShow(final AppGroupCreationContent content) {
            return true;
        }

        @Override
        public AppCall createAppCall(final AppGroupCreationContent content) {
            AppCall appCall = createBaseAppCall();
            DialogPresenter.setupAppCallForWebDialog(
                    appCall,
                    GAME_GROUP_CREATION_DIALOG,
                    WebDialogParameters.create(content));

            return appCall;
        }
    }
}
