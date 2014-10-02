/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.internal;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import com.facebook.android.R;

/**
 * com.facebook.internal is solely for the use of other packages within the Facebook SDK for Android. Use of
 * any of the classes in this package is unsupported, and they may be modified or removed without warning at
 * any time.
 */
public class LikeButton extends Button {

    private boolean isLiked;

    /**
     * Create the LikeButton .
     *
     * @see android.view.View#View(android.content.Context)
     */
    public LikeButton(Context context, boolean isLiked) {
        super(context);

        this.isLiked = isLiked;

        initialize();
    }

    public void setLikeState(boolean isLiked) {
        if (isLiked != this.isLiked) {
            this.isLiked = isLiked;
            updateForLikeStatus();
        }
    }

    private void initialize() {
        // apparently there's no method of setting a default style in xml,
        // so in case the users do not explicitly specify a style, we need
        // to use sensible defaults.
        this.setGravity(Gravity.CENTER_VERTICAL);
        this.setTextColor(getResources().getColor(R.color.com_facebook_likebutton_text_color));
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.com_facebook_likebutton_text_size));
        this.setTypeface(Typeface.DEFAULT_BOLD);

        this.setCompoundDrawablePadding(
                getResources().getDimensionPixelSize(R.dimen.com_facebook_likebutton_compound_drawable_padding));
        this.setPadding(
                getResources().getDimensionPixelSize(R.dimen.com_facebook_likebutton_padding_left),
                getResources().getDimensionPixelSize(R.dimen.com_facebook_likebutton_padding_top),
                getResources().getDimensionPixelSize(R.dimen.com_facebook_likebutton_padding_right),
                getResources().getDimensionPixelSize(R.dimen.com_facebook_likebutton_padding_bottom));

        updateForLikeStatus();
    }

    private void updateForLikeStatus() {
        if (isLiked) {
            this.setBackgroundResource(R.drawable.com_facebook_button_like_selected);
            this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.com_facebook_button_like_icon_selected, 0, 0, 0);
            this.setText(getResources().getString(R.string.com_facebook_like_button_liked));
        } else {
            this.setBackgroundResource(R.drawable.com_facebook_button_like);
            this.setCompoundDrawablesWithIntrinsicBounds(R.drawable.com_facebook_button_like_icon, 0, 0, 0);
            this.setText(getResources().getString(R.string.com_facebook_like_button_not_liked));
        }
    }
}
