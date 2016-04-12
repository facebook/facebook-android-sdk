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
package com.facebook.share;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.FacebookActivity;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookRequestError;
import com.facebook.FacebookSdk;
import com.facebook.internal.AppCall;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.internal.FragmentWrapper;
import com.facebook.share.internal.DeviceShareDialogFragment;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphContent;

import java.util.List;

/*
 * Provides functionality to share from devices.
 * See https://developers.facebook.com/docs/android/devices
 *
 * Only ShareLinkContent and ShareOpenGraphContent are supported.
 *
 * The dialog does not indicate if the person completed a share. Therefore,
 * the callback will always either invoke onSuccess or onError.
 *
 * The dialog can also dismiss itself after the device code has expired.
 */
public class DeviceShareDialog
        extends FacebookDialogBase<ShareContent, DeviceShareDialog.Result> {
    private static final int DEFAULT_REQUEST_CODE =
            CallbackManagerImpl.RequestCodeOffset.DeviceShare.toRequestCode();
    /**
     * Constructs a new DeviceShareDialog.
     * @param activity Activity to use to share the provided content
     */
    public DeviceShareDialog(final Activity activity) {
        super(activity, DEFAULT_REQUEST_CODE);
    }
    /**
     * Constructs a new DeviceShareDialog.
     * @param fragment fragment to use to share the provided content
     */
    public DeviceShareDialog(final Fragment fragment) {
        super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
    }
    /**
     * Constructs a new DeviceShareDialog.
     * @param fragment fragment to use to share the provided content
     */
    public DeviceShareDialog(final android.support.v4.app.Fragment fragment) {
        super(new FragmentWrapper(fragment), DEFAULT_REQUEST_CODE);
    }

    @Override
    protected boolean canShowImpl(ShareContent content, Object mode) {
        return (content instanceof ShareLinkContent ||
                content instanceof ShareOpenGraphContent);
    }

    @Override
    protected void showImpl(final ShareContent content, final Object mode) {
        if (content == null) {
            throw new FacebookException("Must provide non-null content to share");
        }

        if (!(content instanceof ShareLinkContent) &&
            !(content instanceof ShareOpenGraphContent)) {
            throw new FacebookException(this.getClass().getSimpleName() +
                    " only supports ShareLinkContent or ShareOpenGraphContent");
        }
        Intent intent = new Intent();
        intent.setClass(FacebookSdk.getApplicationContext(), FacebookActivity.class);
        intent.setAction(DeviceShareDialogFragment.TAG);
        intent.putExtra("content", content);
        startActivityForResult(intent, getRequestCode());
    }

    @Override
    protected List<ModeHandler> getOrderedModeHandlers() {
        return null;
    }

    @Override
    protected AppCall createBaseAppCall() {
       return null;
    }

    @Override
    protected void registerCallbackImpl(
            final CallbackManagerImpl callbackManager,
            final FacebookCallback<Result> callback) {

        callbackManager.registerCallback(
                getRequestCode(),
                new CallbackManagerImpl.Callback() {
                    @Override
                    public boolean onActivityResult(int resultCode, Intent data) {
                        Bundle extras = data.getExtras();
                        if (data.hasExtra("error")) {
                            FacebookRequestError error = data.getParcelableExtra("error");
                            callback.onError(error.getException());
                            return true;
                        }
                        callback.onSuccess(new Result());
                        return true;
                    }
                });
    }

    /*
     * Describes the result of a device share.
     * This class is intentionally empty.
     */
    public static class Result {

    }
}
