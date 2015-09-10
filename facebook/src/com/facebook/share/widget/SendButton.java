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
import android.view.View;

import com.facebook.R;
import com.facebook.internal.AnalyticsEvents;
import com.facebook.internal.CallbackManagerImpl;
import com.facebook.internal.FacebookDialogBase;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareContent;

/**
 * A button to share content through Messenger.
 * Tapping the receiver will invoke the {@link com.facebook.share.widget.MessageDialog} with the attached shareContent.
 */
public final class SendButton extends ShareButtonBase {
    public SendButton(final Context context) {
        super(context, null, 0, AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
                                AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
    }

    public SendButton(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0, AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
                                 AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
    }

    public SendButton(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(
                context,
                attrs,
                defStyleAttr,
                AnalyticsEvents.EVENT_SEND_BUTTON_CREATE,
                AnalyticsEvents.EVENT_SEND_BUTTON_DID_TAP);
    }

    @Override
    protected int getDefaultStyleResource() {
        return R.style.com_facebook_button_send;
    }

    @Override
    protected int getDefaultRequestCode() {
        return CallbackManagerImpl.RequestCodeOffset.Message.toRequestCode();
    }

    @Override
    protected FacebookDialogBase<ShareContent, Sharer.Result> getDialog() {
        final MessageDialog dialog;
        if (SendButton.this.getFragment() != null) {
            dialog = new MessageDialog(SendButton.this.getFragment() , getRequestCode());
        } else {
            dialog = new MessageDialog(getActivity(), getRequestCode());
        }
        return dialog;
    }
}
