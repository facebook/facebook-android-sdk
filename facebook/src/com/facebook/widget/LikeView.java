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

package com.facebook.widget;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.facebook.android.R;
import com.facebook.internal.*;

/**
 * This class provides the UI for displaying the Facebook Like button and its associated components.
 */
public class LikeView extends FrameLayout {

    // ***
    // Keep all the enum values in sync with attrs.xml
    // ***

    /**
     * Encapsulates the valid values for the facebook:style attribute for a LikeView
     */
    public enum Style {
        /**
         * Setting the attribute to this value will display the button and a sentence near it that describes the
         * social sentence for the associated object.
         *
         * This is the default value
         */
        STANDARD("standard", 0),

        /**
         * Setting the attribute to this value will display the button by itself, with no other components
         */
        BUTTON("button", 1),

        /**
         * Setting the attribute to this value will display the button and a box near it with the number of likes
         * for the associated object
         */
        BOX_COUNT("box_count", 2);

        static Style DEFAULT = STANDARD;

        static Style fromInt(int enumValue) {
            for (Style style : values()) {
                if (style.getValue() == enumValue) {
                    return style;
                }
            }

            return null;
        }

        private String stringValue;
        private int intValue;
        private Style(String stringValue, int value) {
            this.stringValue = stringValue;
            this.intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        private int getValue() {
            return intValue;
        }
    }

    /**
     * Encapsulates the valid values for the facebook:horizontal_alignment attribute for a LikeView.
     */
    public enum HorizontalAlignment {
        /**
         * Setting the attribute to this value will center the button and auxiliary view in the parent view.
         *
         * This is the default value
         */
        CENTER("center", 0),

        /**
         * Setting the attribute to this value will left-justify the button and auxiliary view in the parent view.
         */
        LEFT("left", 1),

        /**
         * Setting the attribute to this value will right-justify the button and auxiliary view in the parent view.
         * If the facebook:auxiliary_view_position is set to INLINE, then the auxiliary view will be on the
         * left of the button
         */
        RIGHT("right", 2);

        static HorizontalAlignment DEFAULT = CENTER;

        static HorizontalAlignment fromInt(int enumValue) {
            for (HorizontalAlignment horizontalAlignment : values()) {
                if (horizontalAlignment.getValue() == enumValue) {
                    return horizontalAlignment;
                }
            }

            return null;
        }

        private String stringValue;
        private int intValue;
        private HorizontalAlignment(String stringValue, int value) {
            this.stringValue = stringValue;
            this.intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        private int getValue() {
            return intValue;
        }
    }

    /**
     * Encapsulates the valid values for the facebook:auxiliary_view_position attribute for a LikeView.
     */
    public enum AuxiliaryViewPosition {
        /**
         * Setting the attribute to this value will put the social-sentence or box-count below the like button.
         * If the facebook:style is set to BUTTON, then this has no effect.
         *
         * This is the default value
         */
        BOTTOM("bottom", 0),

        /**
         * Setting the attribute to this value will put the social-sentence or box-count inline with the like button.
         * The auxiliary view will be to the left of the button if the facebook:horizontal_alignment is set to RIGHT.
         * In all other cases, it will be to the right of the button.
         * If the facebook:style is set to BUTTON, then this has no effect.
         */
        INLINE("inline", 1),

        /**
         * Setting the attribute to this value will put the social-sentence or box-count above the like button.
         * If the facebook:style is set to BUTTON, then this has no effect.
         */
        TOP("top", 2);

        static AuxiliaryViewPosition DEFAULT = BOTTOM;

        static AuxiliaryViewPosition fromInt(int enumValue) {
            for (AuxiliaryViewPosition auxViewPosition : values()) {
                if (auxViewPosition.getValue() == enumValue) {
                    return auxViewPosition;
                }
            }

            return null;
        }

        private String stringValue;
        private int intValue;
        private AuxiliaryViewPosition(String stringValue, int value) {
            this.stringValue = stringValue;
            this.intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        private int getValue() {
            return intValue;
        }
    }

    private static final int NO_FOREGROUND_COLOR = -1;

    private String objectId;
    private LinearLayout containerView;
    private LikeButton likeButton;
    private LikeBoxCountView likeBoxCountView;
    private TextView socialSentenceView;
    private LikeActionController likeActionController;
    private OnErrorListener onErrorListener;
    private BroadcastReceiver broadcastReceiver;
    private LikeActionControllerCreationCallback creationCallback;

    private Style likeViewStyle = Style.DEFAULT;
    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.DEFAULT;
    private AuxiliaryViewPosition auxiliaryViewPosition = AuxiliaryViewPosition.DEFAULT;
    private int foregroundColor = NO_FOREGROUND_COLOR;

    private int edgePadding;
    private int internalPadding;

    /**
     * If your app does not use UiLifeCycleHelper, then you must call this method in the calling activity's
     * onActivityResult method, to process any pending like actions, where tapping the button had resulted in
     * the Like dialog being shown in the Facebook application.
     *
     * @param context Hosting context
     * @param requestCode From the originating call to onActivityResult
     * @param resultCode From the originating call to onActivityResult
     * @param data From the originating call to onActivityResult
     * @return Indication of whether the Intent was handled
     */
    public static boolean handleOnActivityResult(Context context,
                                                 int requestCode,
                                                 int resultCode,
                                                 Intent data) {
        return LikeActionController.handleOnActivityResult(context, requestCode, resultCode, data);
    }

    /**
     * Constructor
     *
     * @param context Context for this View
     */
    public LikeView(Context context) {
        super(context);
        initialize(context);
    }

    /**
     * Constructor
     *
     * @param context Context for this View
     * @param attrs   AttributeSet for this View.
     */
    public LikeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributes(attrs);
        initialize(context);
    }

    /**
     * Sets the associated object for this LikeView. Can be changed during runtime.
     * @param objectId Object Id
     */
    public void setObjectId(String objectId) {
        objectId = Utility.coerceValueIfNullOrEmpty(objectId, null);
        if (!Utility.areObjectsEqual(objectId, this.objectId)) {
            setObjectIdForced(objectId);

            updateLikeStateAndLayout();
        }
    }

    /**
     * Sets the facebook:style for this LikeView. Can be changed during runtime.
     * @param likeViewStyle Should be either LikeView.STANDARD, LikeView.BUTTON or LikeView.BOX_COUNT
     */
    public void setLikeViewStyle(Style likeViewStyle) {
        likeViewStyle = likeViewStyle != null ? likeViewStyle : Style.DEFAULT;
        if (this.likeViewStyle != likeViewStyle) {
            this.likeViewStyle = likeViewStyle;

            updateLayout();
        }
    }

    /**
     * Sets the facebook:auxiliary_view_position for this LikeView. Can be changed during runtime.
     * @param auxiliaryViewPosition Should be either LikeView.TOP, LikeView.INLINE or LikeView.BOTTOM
     */
    public void setAuxiliaryViewPosition(AuxiliaryViewPosition auxiliaryViewPosition) {
        auxiliaryViewPosition = auxiliaryViewPosition != null ? auxiliaryViewPosition : AuxiliaryViewPosition.DEFAULT;
        if (this.auxiliaryViewPosition != auxiliaryViewPosition) {
            this.auxiliaryViewPosition = auxiliaryViewPosition;

            updateLayout();
        }
    }

    /**
     * Sets the facebook:horizontal_alignment for this LikeView. Can be changed during runtime.
     * @param horizontalAlignment Should be either LikeView.LEFT, LikeView.CENTER or LikeView.RIGHT
     */
    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        horizontalAlignment = horizontalAlignment != null ? horizontalAlignment : HorizontalAlignment.DEFAULT;
        if (this.horizontalAlignment != horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;

            updateLayout();
        }
    }

    /**
     * Sets the facebook:foreground_color for this LikeView. Can be changed during runtime.
     * The color is only used for the social sentence text.
     * @param foregroundColor And valid android.graphics.Color value.
     */
    public void setForegroundColor(int foregroundColor) {
        if (this.foregroundColor != foregroundColor) {
            socialSentenceView.setTextColor(foregroundColor);
        }
    }

    /**
     * Sets an OnErrorListener for this instance of LoginButton to call into when
     * certain exceptions occur.
     *
     * @param onErrorListener The listener object to set
     */
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    /**
     * Returns the current OnErrorListener for this instance of LoginButton.
     *
     * @return The OnErrorListener
     */
    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    @Override
    protected void onDetachedFromWindow() {
        // Disassociate from the object
        setObjectId(null);

        super.onDetachedFromWindow();
    }

    private void parseAttributes(AttributeSet attrs) {
        if (attrs == null || getContext() == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.com_facebook_like_view);
        if (a == null) {
            return;
        }

        objectId = Utility.coerceValueIfNullOrEmpty(a.getString(R.styleable.com_facebook_like_view_object_id), null);
        likeViewStyle = Style.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_style,
                        Style.DEFAULT.getValue()));
        if (likeViewStyle == null) {
            throw new IllegalArgumentException("Unsupported value for LikeView 'style'");
        }

        auxiliaryViewPosition = AuxiliaryViewPosition.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_auxiliary_view_position,
                        AuxiliaryViewPosition.DEFAULT.getValue()));
        if (auxiliaryViewPosition == null) {
            throw new IllegalArgumentException("Unsupported value for LikeView 'auxiliary_view_position'");
        }

        horizontalAlignment = HorizontalAlignment.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_horizontal_alignment,
                        HorizontalAlignment.DEFAULT.getValue()));
        if (horizontalAlignment == null) {
            throw new IllegalArgumentException("Unsupported value for LikeView 'horizontal_alignment'");
        }

        foregroundColor = a.getColor(R.styleable.com_facebook_like_view_foreground_color, NO_FOREGROUND_COLOR);

        a.recycle();
    }

    // If attributes were present, parseAttributes MUST be called before initialize() to ensure proper behavior
    private void initialize(Context context) {
        edgePadding = getResources().getDimensionPixelSize(R.dimen.com_facebook_likeview_edge_padding);
        internalPadding = getResources().getDimensionPixelSize(R.dimen.com_facebook_likeview_internal_padding);
        if (foregroundColor == NO_FOREGROUND_COLOR) {
            foregroundColor = getResources().getColor(R.color.com_facebook_likeview_text_color);
        }

        setBackgroundColor(Color.TRANSPARENT);

        containerView = new LinearLayout(context);
        LayoutParams containerViewLayoutParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        containerView.setLayoutParams(containerViewLayoutParams);

        initializeLikeButton(context);
        initializeSocialSentenceView(context);
        initializeLikeCountView(context);

        containerView.addView(likeButton);
        containerView.addView(socialSentenceView);
        containerView.addView(likeBoxCountView);

        addView(containerView);

        setObjectIdForced(this.objectId);
        updateLikeStateAndLayout();
    }

    private void initializeLikeButton(Context context) {
        likeButton = new LikeButton(
                context,
                likeActionController != null ? likeActionController.isObjectLiked() : false);
        likeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLike();
            }
        });

        LinearLayout.LayoutParams buttonLayout = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);

        likeButton.setLayoutParams(buttonLayout);
    }

    private void initializeSocialSentenceView(Context context) {
        socialSentenceView = new TextView(context);
        socialSentenceView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimension(R.dimen.com_facebook_likeview_text_size));
        socialSentenceView.setMaxLines(2);
        socialSentenceView.setTextColor(foregroundColor);
        socialSentenceView.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams socialSentenceViewLayout = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.MATCH_PARENT);
        socialSentenceView.setLayoutParams(socialSentenceViewLayout);
    }

    private void initializeLikeCountView(Context context) {
        likeBoxCountView = new LikeBoxCountView(context);

        LinearLayout.LayoutParams likeCountViewLayout = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        likeBoxCountView.setLayoutParams(likeCountViewLayout);
    }

    private void toggleLike() {
        if (likeActionController != null) {
            Activity activity = (Activity)getContext();
            likeActionController.toggleLike(activity, getAnalyticsParameters());
        }
    }

    private Bundle getAnalyticsParameters() {
        Bundle params = new Bundle();
        params.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_STYLE, likeViewStyle.toString());
        params.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_AUXILIARY_POSITION, auxiliaryViewPosition.toString());
        params.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_HORIZONTAL_ALIGNMENT, horizontalAlignment.toString());
        params.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_OBJECT_ID, Utility.coerceValueIfNullOrEmpty(objectId, ""));
        return params;
    }

    private void setObjectIdForced(String newObjectId) {
        tearDownObjectAssociations();

        objectId = newObjectId;
        if (Utility.isNullOrEmpty(newObjectId)) {
            return;
        }

        creationCallback = new LikeActionControllerCreationCallback();
        LikeActionController.getControllerForObjectId(
                getContext(),
                newObjectId,
                creationCallback);
    }

    private void associateWithLikeActionController(LikeActionController likeActionController) {
        this.likeActionController = likeActionController;

        this.broadcastReceiver = new LikeControllerBroadcastReceiver();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());

        // add the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_UPDATED);
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR);
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_RESET);

        localBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    private void tearDownObjectAssociations() {
        if (broadcastReceiver != null) {
            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getContext());
            localBroadcastManager.unregisterReceiver(broadcastReceiver);

            broadcastReceiver = null;
        }

        // If we were already waiting on a controller to be given back, make sure we aren't waiting anymore.
        // Otherwise when that controller is given back to the callback, it will go and register a broadcast receiver
        // for it.
        if (creationCallback != null) {
            creationCallback.cancel();

            creationCallback = null;
        }

        likeActionController = null;
    }

    private void updateLikeStateAndLayout() {
        if (likeActionController == null) {
            likeButton.setLikeState(false);
            socialSentenceView.setText(null);
            likeBoxCountView.setText(null);
        } else {
            likeButton.setLikeState(likeActionController.isObjectLiked());
            socialSentenceView.setText(likeActionController.getSocialSentence());
            likeBoxCountView.setText(likeActionController.getLikeCountString());
        }

        updateLayout();
    }

    private void updateLayout() {
        // Make sure the container is horizontally aligned according to specifications.
        LayoutParams containerViewLayoutParams = (LayoutParams)containerView.getLayoutParams();
        LinearLayout.LayoutParams buttonLayoutParams = (LinearLayout.LayoutParams)likeButton.getLayoutParams();
        int viewGravity =
                horizontalAlignment == HorizontalAlignment.LEFT ? Gravity.LEFT :
                        horizontalAlignment == HorizontalAlignment.CENTER ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT;

        containerViewLayoutParams.gravity = viewGravity | Gravity.TOP;
        buttonLayoutParams.gravity = viewGravity;

        // Choose the right auxiliary view to make visible.
        socialSentenceView.setVisibility(GONE);
        likeBoxCountView.setVisibility(GONE);

        View auxView;
        if (likeViewStyle == Style.STANDARD &&
                likeActionController != null &&
                !Utility.isNullOrEmpty(likeActionController.getSocialSentence())) {
            auxView = socialSentenceView;
        } else if (likeViewStyle == Style.BOX_COUNT &&
                likeActionController != null &&
                !Utility.isNullOrEmpty(likeActionController.getLikeCountString())) {
            updateBoxCountCaretPosition();
            auxView = likeBoxCountView;
        } else {
            // No more work to be done.
            return;
        }
        auxView.setVisibility(VISIBLE);

        // Now position the auxiliary view properly
        LinearLayout.LayoutParams auxViewLayoutParams = (LinearLayout.LayoutParams)auxView.getLayoutParams();
        auxViewLayoutParams.gravity = viewGravity;

        containerView.setOrientation(
                auxiliaryViewPosition == AuxiliaryViewPosition.INLINE ?
                        LinearLayout.HORIZONTAL :
                        LinearLayout.VERTICAL);

        if (auxiliaryViewPosition == AuxiliaryViewPosition.TOP ||
                (auxiliaryViewPosition == AuxiliaryViewPosition.INLINE &&
                        horizontalAlignment == HorizontalAlignment.RIGHT)) {
            // Button comes after the auxiliary view. Make sure it is at the end
            containerView.removeView(likeButton);
            containerView.addView(likeButton);
        } else {
            // In all other cases, the button comes first
            containerView.removeView(auxView);
            containerView.addView(auxView);
        }

        switch (auxiliaryViewPosition) {
            case TOP:
                auxView.setPadding(edgePadding, edgePadding, edgePadding, internalPadding);
                break;
            case BOTTOM:
                auxView.setPadding(edgePadding, internalPadding, edgePadding, edgePadding);
                break;
            case INLINE:
                if (horizontalAlignment == HorizontalAlignment.RIGHT) {
                    auxView.setPadding(edgePadding, edgePadding, internalPadding, edgePadding);
                } else {
                    auxView.setPadding(internalPadding, edgePadding, edgePadding, edgePadding);
                }
                break;
        }
    }

    private void updateBoxCountCaretPosition() {
        switch (auxiliaryViewPosition) {
            case TOP:
                likeBoxCountView.setCaretPosition(LikeBoxCountView.LikeBoxCountViewCaretPosition.BOTTOM);
                break;
            case BOTTOM:
                likeBoxCountView.setCaretPosition(LikeBoxCountView.LikeBoxCountViewCaretPosition.TOP);
                break;
            case INLINE:
                likeBoxCountView.setCaretPosition(
                        horizontalAlignment == HorizontalAlignment.RIGHT ?
                                LikeBoxCountView.LikeBoxCountViewCaretPosition.RIGHT :
                                LikeBoxCountView.LikeBoxCountViewCaretPosition.LEFT);
                break;
        }
    }

    /**
     * Callback interface that will be called when a network or other error is encountered
     * while logging in.
     */
    public interface OnErrorListener {
        /**
         * Called when a network or other error is encountered.
         * @param errorBundle     a FacebookException representing the error that was encountered.
         */
        void onError(Bundle errorBundle);
    }

    private class LikeControllerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            Bundle extras = intent.getExtras();
            boolean shouldRespond = true;
            if (extras != null) {
                // See if an Id was set in the broadcast Intent. If it was, treat it as a filter.
                String broadcastObjectId = extras.getString(LikeActionController.ACTION_OBJECT_ID_KEY);
                shouldRespond = Utility.isNullOrEmpty(broadcastObjectId) ||
                        Utility.areObjectsEqual(objectId, broadcastObjectId);
            }

            if (!shouldRespond) {
                return;
            }

            if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_UPDATED.equals(intentAction)) {
                updateLikeStateAndLayout();
            } else if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR.equals(intentAction)) {
                if (onErrorListener != null) {
                    onErrorListener.onError(extras);
                }
            } else if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_RESET.equals(intentAction)) {
                // This will recreate the controller and associated objects
                setObjectIdForced(objectId);
                updateLikeStateAndLayout();
            }
        }
    }

    private class LikeActionControllerCreationCallback implements LikeActionController.CreationCallback {
        private boolean isCancelled;

        public void cancel() {
            isCancelled = true;
        }

        @Override
        public void onComplete(LikeActionController likeActionController) {
            if (isCancelled) {
                return;
            }

            associateWithLikeActionController(likeActionController);
            updateLikeStateAndLayout();

            LikeView.this.creationCallback = null;
        }
    }
}
