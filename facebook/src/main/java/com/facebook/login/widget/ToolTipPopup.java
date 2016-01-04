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

package com.facebook.login.widget;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.facebook.R;

import java.lang.ref.WeakReference;

/**
 * This displays a popup tool tip for a specified view.
 */
public class ToolTipPopup {

    /**
     * The values here describe the styles available for the tool tip class.
     */
    public static enum Style {
        /**
         * The tool tip will be shown with a blue style; including a blue background and blue
         * arrows.
         */
        BLUE,
        
        /**
         * The tool tip will be shown with a black style; including a black background and black
         * arrows.
         */
        BLACK
    }
    
    /**
     * The default time that the tool tip will be displayed
     */
    public static final long DEFAULT_POPUP_DISPLAY_TIME = 6000;
    
    private final String mText;
    private final WeakReference<View> mAnchorViewRef;
    private final Context mContext;
    private PopupContentView mPopupContent;
    private PopupWindow mPopupWindow;
    private Style mStyle = Style.BLUE;
    private long mNuxDisplayTime = DEFAULT_POPUP_DISPLAY_TIME;
    
    private final ViewTreeObserver.OnScrollChangedListener mScrollListener = 
            new ViewTreeObserver.OnScrollChangedListener() {
                @Override
                public void onScrollChanged() {
                    if (mAnchorViewRef.get() != null && 
                            mPopupWindow != null && 
                            mPopupWindow.isShowing()) {
                        if (mPopupWindow.isAboveAnchor()) {
                            mPopupContent.showBottomArrow();
                        } else {
                            mPopupContent.showTopArrow();
                        }
                    }
                }
            };
    
    /**
     * Create a new ToolTipPopup
     * @param text The text to be displayed in the tool tip
     * @param anchor The view to anchor this tool tip to.
     */
    public ToolTipPopup(String text, View anchor) {
        mText = text;
        mAnchorViewRef = new WeakReference<View>(anchor);
        mContext = anchor.getContext();
    }
    
    /**
     * Sets the {@link Style} of this tool tip.
     * @param mStyle the style for the tool tip
     */
    public void setStyle(Style mStyle) {
        this.mStyle = mStyle;
    }
    
    /**
     * Display this tool tip to the user
     */
    public void show() {
        if (mAnchorViewRef.get() != null) {
            mPopupContent = new PopupContentView(mContext);
            TextView body = (TextView) mPopupContent.findViewById(
                    R.id.com_facebook_tooltip_bubble_view_text_body);
            body.setText(mText);
            if (mStyle == Style.BLUE) {
                mPopupContent.bodyFrame.setBackgroundResource(
                        R.drawable.com_facebook_tooltip_blue_background);
                mPopupContent.bottomArrow.setImageResource(
                        R.drawable.com_facebook_tooltip_blue_bottomnub);
                mPopupContent.topArrow.setImageResource(
                        R.drawable.com_facebook_tooltip_blue_topnub);
                mPopupContent.xOut.setImageResource(R.drawable.com_facebook_tooltip_blue_xout);
            } else {
                mPopupContent.bodyFrame.setBackgroundResource(
                        R.drawable.com_facebook_tooltip_black_background);
                mPopupContent.bottomArrow.setImageResource(
                        R.drawable.com_facebook_tooltip_black_bottomnub);
                mPopupContent.topArrow.setImageResource(
                        R.drawable.com_facebook_tooltip_black_topnub);
                mPopupContent.xOut.setImageResource(R.drawable.com_facebook_tooltip_black_xout);
            }
            
            final Window window = ((Activity) mContext).getWindow();
            final View decorView = window.getDecorView();
            final int decorWidth = decorView.getWidth();
            final int decorHeight = decorView.getHeight();
            registerObserver();
            mPopupContent.measure(
                    View.MeasureSpec.makeMeasureSpec(decorWidth, View.MeasureSpec.AT_MOST), 
                    View.MeasureSpec.makeMeasureSpec(decorHeight, View.MeasureSpec.AT_MOST));
            mPopupWindow = new PopupWindow(
                    mPopupContent, 
                    mPopupContent.getMeasuredWidth(),
                    mPopupContent.getMeasuredHeight());
            mPopupWindow.showAsDropDown(mAnchorViewRef.get());
            updateArrows();
            if (mNuxDisplayTime > 0) {
                mPopupContent.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                }, mNuxDisplayTime);
            }
            mPopupWindow.setTouchable(true);
            mPopupContent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }
    
    /**
     * Set the time (in milliseconds) the tool tip will be displayed. Any number less than or equal
     * to 0 will cause the tool tip to be displayed indefinitely
     * @param displayTime The amount of time (in milliseconds) to display the tool tip
     */
    public void setNuxDisplayTime(long displayTime) {
        this.mNuxDisplayTime = displayTime;
    }
    
    private void updateArrows() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            if (mPopupWindow.isAboveAnchor()) {
                mPopupContent.showBottomArrow();
            } else {
                mPopupContent.showTopArrow();
            }
        }
    }
    
    /**
     * Dismiss the tool tip
     */
    public void dismiss() {
        unregisterObserver();
        if (mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
    }
    
    private void registerObserver() {
        unregisterObserver();
        if (mAnchorViewRef.get() != null) {
            mAnchorViewRef.get().getViewTreeObserver().addOnScrollChangedListener(mScrollListener);
        }
    }
    
    private void unregisterObserver() {
        if (mAnchorViewRef.get() != null) {
            mAnchorViewRef.get().getViewTreeObserver().removeOnScrollChangedListener(
                    mScrollListener);
        }
    }
    
    private class PopupContentView extends FrameLayout {
        private ImageView topArrow;
        private ImageView bottomArrow;
        private View bodyFrame;
        private ImageView xOut;
        
        public PopupContentView(Context context) {
            super(context);
            init();
        }
        
        private void init() {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.com_facebook_tooltip_bubble, this);
            topArrow = (ImageView) findViewById(R.id.com_facebook_tooltip_bubble_view_top_pointer);
            bottomArrow = (ImageView) findViewById(
                    R.id.com_facebook_tooltip_bubble_view_bottom_pointer);
            bodyFrame = findViewById(R.id.com_facebook_body_frame);
            xOut = (ImageView) findViewById(R.id.com_facebook_button_xout);
        }
        
        public void showTopArrow() {
            topArrow.setVisibility(View.VISIBLE);
            bottomArrow.setVisibility(View.INVISIBLE);
        }
        
        public void showBottomArrow() {
            topArrow.setVisibility(View.INVISIBLE);
            bottomArrow.setVisibility(View.VISIBLE);
        }
    }
}
