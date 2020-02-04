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
package com.facebook.gamingservices;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;

import com.facebook.AccessToken;
import com.facebook.FacebookCallback;
import com.facebook.FacebookRequestError;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;

import java.util.List;

public class FriendFinderDialog extends FacebookDialogBase<Void, FriendFinderDialog.Result> {

    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.GamingFriendFinder.toRequestCode();
    /**
     * Constructs a new FriendFinderDialog.
     * @param activity Activity to use to trigger this Dialog.
     */
    public FriendFinderDialog(final Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }
    /**
     * Constructs a new FriendFinderDialog.
     * @param fragment fragment to use to trigger this Dialog.
     */
    public FriendFinderDialog(final Fragment fragment) {
        super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
    }
    /**
     * Constructs a new FriendFinderDialog.
     * @param fragment fragment to use to trigger this Dialog.
     */
    public FriendFinderDialog(final android.support.v4.app.Fragment fragment) {
        super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
    }

    /**
     * Shows the FriendFinderDialog.
     *
     */
    public void show() {
        showImpl();
    }

    @Override
    public void show(final Void content) {
        showImpl();
    }

    protected void showImpl() {

        AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
        if (currentAccessToken == null || currentAccessToken.isExpired()) {
            return;
        }

        String app_id = currentAccessToken.getApplicationId();
        String dialog_uri = "https://fb.gg/me/friendfinder/" + app_id;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(dialog_uri));
        startActivityForResult(intent, getRequestCode());
    }

    @Override
    protected void registerCallbackImpl(final CallbackManagerImpl callbackManager,
                                        final FacebookCallback callback) {
        callbackManager.registerCallback(
                getRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        if (data != null && data.hasExtra("error")) {
                            FacebookRequestError error = data.getParcelableExtra("error");
                            callback.onError(error.getException());
                            return true;
                        }
                        callback.onSuccess(new Result());
                        return true;
                    }
                });
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        return null;
    }

    @Override
    protected AppCall createBaseAppCall() {
        return null;
    }

    /*
     * Describes the result of a Friend Finder Dialog.
     * This class is intentionally empty.
     */
    public static class Result {

    }
}
