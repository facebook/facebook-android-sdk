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

import android.content.Context;
import android.util.AttributeSet;
import com.facebook.FacebookButtonBase;
import com.facebook.R;
import com.facebook.internal.AnalyticsEvents;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class LikeButton extends FacebookButtonBase {
    public LikeButton(Context context, boolean isLiked) {
        super(context, null, 0, 0, AnalyticsEvents.EVENT_LIKE_BUTTON_CREATE,
                                   AnalyticsEvents.EVENT_LIKE_BUTTON_DID_TAP);
        this.setSelected(isLiked);
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        updateForLikeStatus();
    }

    @Override
    protected void configureButton(
            Context context,
            AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super.configureButton(context, attrs, defStyleAttr, defStyleRes);
        updateForLikeStatus();
    }

    @Override
    protected int getDefaultRequestCode() {
        return 0;
    }

    @Override
    protected int getDefaultStyleResource() {
        return R.style.com_facebook_button_like;
    }

    private void updateForLikeStatus() {
        // the compound drawables don't support selectors, so we need to update for the status
        if (isSelected()) {
            this.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.com_facebook_button_like_icon_selected, 0, 0, 0);
            this.setText(getResources().getString(R.string.com_facebook_like_button_liked));
        } else {
            this.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.com_facebook_button_icon, 0, 0, 0);
            this.setText(getResources().getString(R.string.com_facebook_like_button_not_liked));
        }
    }
}
