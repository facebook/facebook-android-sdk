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

import android.content.Context;
import android.util.AttributeSet;

import com.facebook.CallbackManager;
import com.facebook.FacebookButtonBase;
import com.facebook.FacebookCallback;
import com.facebook.share.Sharer;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.model.ShareContent;

/**
 * A base class for sharing buttons.
 */
public abstract class ShareButtonBase extends FacebookButtonBase {
    private ShareContent shareContent;

    protected ShareButtonBase(
            final Context context,
            final AttributeSet attrs,
            final int defStyleAttr,
            final String analyticsButtonCreatedEventName,
            final int requestCode) {
        super(
                context,
                attrs,
                defStyleAttr,
                0,
                analyticsButtonCreatedEventName,
                requestCode);
    }

    /**
     * Returns the share content from the button.
     * @return The share content.
     */
    public ShareContent getShareContent() {
        return this.shareContent;
    }

    /**
     * Sets the share content on the button.
     * @param shareContent The share content.
     */
    public void setShareContent(final ShareContent shareContent) {
        this.shareContent = shareContent;
    }

    /**
     * Allows registration of a callback for when the share completes. This should be called
     * in the {@link android.app.Activity#onCreate(android.os.Bundle)} or
     * {@link android.support.v4.app.Fragment#onCreate(android.os.Bundle)} methods.
     *
     * @param callbackManager The {@link com.facebook.CallbackManager} instance that will be
     *                        handling results that are received via
     *                        {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param callback The callback that should be called to handle dialog completion.
     */
    public void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<Sharer.Result> callback) {
        ShareInternalUtility.registerSharerCallback(getRequestCode(), callbackManager, callback);
    }

    /**
     * Allows registration of a callback for when the share completes. This should be called
     * in the {@link android.app.Activity#onCreate(android.os.Bundle)} or
     * {@link android.support.v4.app.Fragment#onCreate(android.os.Bundle)} methods.
     *
     * @param callbackManager The {@link com.facebook.CallbackManager} instance that will be
     *                        handling results that are received via
     *                        {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * @param callback The callback that should be called to handle dialog completion.
     * @param requestCode  The request code to use, this should be outside of the range of those
     *                     reserved for the Facebook SDK
     *                     {@link com.facebook.FacebookSdk#isFacebookRequestCode(int)}.
     */
    public void registerCallback(
            final CallbackManager callbackManager,
            final FacebookCallback<Sharer.Result> callback,
            final int requestCode) {
        setRequestCode(requestCode);
        registerCallback(callbackManager, callback);
    }

    @Override
    protected void configureButton(
            final Context context,
            final AttributeSet attrs,
            final int defStyleAttr,
            final int defStyleRes) {
        super.configureButton(context, attrs, defStyleAttr, defStyleRes);
        setInternalOnClickListener(this.getShareOnClickListener());
    }

    abstract protected OnClickListener getShareOnClickListener();
}
