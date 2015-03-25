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
import android.graphics.*;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.facebook.R;

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
public class LikeBoxCountView extends FrameLayout {

    public enum LikeBoxCountViewCaretPosition {
        LEFT,
        TOP,
        RIGHT,
        BOTTOM
    }

    private TextView likeCountLabel;
    private LikeBoxCountViewCaretPosition caretPosition = LikeBoxCountViewCaretPosition.LEFT;

    private float caretHeight;
    private float caretWidth;
    private float borderRadius;
    private Paint borderPaint;
    private int textPadding;
    private int additionalTextPadding;

    /**
     * Constructor
     *
     * @param context Context for this View
     */
    public LikeBoxCountView(Context context) {
        super(context);
        initialize(context);
    }

    /**
     * Sets the text for this view
     * @param text
     */
    public void setText(String text) {
        likeCountLabel.setText(text);
    }

    /**
     * Sets the caret's position. This will trigger a layout of the view.
     * @param caretPosition
     */
    public void setCaretPosition(LikeBoxCountViewCaretPosition caretPosition) {
        this.caretPosition = caretPosition;

        // Since the presence of a caret will move that edge closer to the text, let's add
        // some padding (equal to caretHeight) in that same direction
        switch (caretPosition) {
            case LEFT:
                setAdditionalTextPadding(additionalTextPadding, 0, 0, 0);
                break;
            case TOP:
                setAdditionalTextPadding(0, additionalTextPadding, 0, 0);
                break;
            case RIGHT:
                setAdditionalTextPadding(0, 0, additionalTextPadding, 0);
                break;
            case BOTTOM:
                setAdditionalTextPadding(0, 0, 0, additionalTextPadding);
                break;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int top = getPaddingTop(), left = getPaddingLeft();
        int right = getWidth() - getPaddingRight(), bottom = getHeight() - getPaddingBottom();

        switch (caretPosition) {
            case BOTTOM:
                bottom -= caretHeight;
                break;
            case LEFT:
                left += caretHeight;
                break;
            case TOP:
                top += caretHeight;
                break;
            case RIGHT:
                right -= caretHeight;
                break;
        }

        drawBorder(canvas, left, top, right, bottom);
    }

    private void initialize(Context context) {
        setWillNotDraw(false); // Required for the onDraw() method to be called on a FrameLayout
        caretHeight = getResources().getDimension(R.dimen.com_facebook_likeboxcountview_caret_height);
        caretWidth = getResources().getDimension(R.dimen.com_facebook_likeboxcountview_caret_width);
        borderRadius = getResources().getDimension(R.dimen.com_facebook_likeboxcountview_border_radius);

        borderPaint = new Paint();
        borderPaint.setColor(
                getResources().getColor(R.color.com_facebook_likeboxcountview_border_color));
        borderPaint.setStrokeWidth(getResources().getDimension(R.dimen.com_facebook_likeboxcountview_border_width));
        borderPaint.setStyle(Paint.Style.STROKE);

        initializeLikeCountLabel(context);

        addView(likeCountLabel);

        setCaretPosition(this.caretPosition);
    }

    private void initializeLikeCountLabel(Context context) {
        likeCountLabel = new TextView(context);
        LayoutParams likeCountLabelLayoutParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        likeCountLabel.setLayoutParams(likeCountLabelLayoutParams);
        likeCountLabel.setGravity(Gravity.CENTER);
        likeCountLabel.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.com_facebook_likeboxcountview_text_size));
        likeCountLabel.setTextColor(
                getResources().getColor(R.color.com_facebook_likeboxcountview_text_color));
        textPadding = getResources().getDimensionPixelSize(R.dimen.com_facebook_likeboxcountview_text_padding);

        // Calculate the additional text padding that will be applied in the direction of the caret.
        additionalTextPadding = getResources().getDimensionPixelSize(R.dimen.com_facebook_likeboxcountview_caret_height);
    }

    private void setAdditionalTextPadding(int left, int top, int right, int bottom) {
        likeCountLabel.setPadding(
                textPadding + left,
                textPadding + top,
                textPadding + right,
                textPadding + bottom);
    }

    private void drawBorder(Canvas canvas, float left, float top, float right, float bottom) {
        Path borderPath = new Path();

        float ovalSize = 2.0f * borderRadius;

        // Top left corner
        borderPath.addArc(new RectF(left, top, left + ovalSize, top + ovalSize), -180, 90);

        // Top caret
        if (caretPosition == LikeBoxCountViewCaretPosition.TOP) {
            borderPath.lineTo(left + (right - left - caretWidth) / 2, top);
            borderPath.lineTo(left + (right - left) / 2, top - caretHeight);
            borderPath.lineTo(left + (right - left + caretWidth) / 2, top);
        }

        // Move to top right corner
        borderPath.lineTo(right - borderRadius, top);

        // Top right corner
        borderPath.addArc(new RectF(right - ovalSize, top, right, top + ovalSize), -90, 90);

        // Right caret
        if (caretPosition == LikeBoxCountViewCaretPosition.RIGHT) {
            borderPath.lineTo(right, top + (bottom - top - caretWidth) / 2);
            borderPath.lineTo(right + caretHeight, top + (bottom - top) / 2);
            borderPath.lineTo(right, top + (bottom - top + caretWidth) / 2);
        }

        // Move to bottom right corner
        borderPath.lineTo(right, bottom - borderRadius);

        // Bottom right corner
        borderPath.addArc(new RectF(right - ovalSize, bottom - ovalSize, right, bottom), 0, 90);

        // Bottom caret
        if (caretPosition == LikeBoxCountViewCaretPosition.BOTTOM) {
            borderPath.lineTo(left + (right - left + caretWidth) / 2, bottom);
            borderPath.lineTo(left + (right - left) / 2, bottom + caretHeight);
            borderPath.lineTo(left + (right - left - caretWidth) / 2, bottom);
        }

        // Move to bottom left corner
        borderPath.lineTo(left + borderRadius, bottom);

        // Bottom left corner
        borderPath.addArc(new RectF(left, bottom - ovalSize, left + ovalSize, bottom), 90, 90);

        // Left caret
        if (caretPosition == LikeBoxCountViewCaretPosition.LEFT) {
            borderPath.lineTo(left, top + (bottom - top + caretWidth) / 2);
            borderPath.lineTo(left - caretHeight, top + (bottom - top) / 2);
            borderPath.lineTo(left, top + (bottom - top - caretWidth) / 2);
        }

        // Move back to the beginning
        borderPath.lineTo(left, top + borderRadius);

        canvas.drawPath(borderPath, borderPaint);
    }
}
