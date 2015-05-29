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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.FacebookException;
import com.facebook.R;
import com.facebook.internal.*;
import com.facebook.share.internal.LikeActionController;
import com.facebook.share.internal.LikeBoxCountView;
import com.facebook.share.internal.LikeButton;

/**
 * This class provides the UI for displaying the Facebook Like button and its associated components.
 */
public class LikeView extends FrameLayout {

    // ***
    // Keep all the enum values in sync with attrs.xml
    // ***

    /**
     * Encapsulates the valid values for the facebook:object_type attribute for a LikeView
     */
    public enum ObjectType {
        /**
         * This is the default value
         */
        UNKNOWN("unknown", 0),

        /**
         * Indicates that the object id set on this LikeView is an Open Graph object
         */
        OPEN_GRAPH("open_graph", 1),

        /**
         * Indicates that the object id set on this LikeView is a Page.
         */
        PAGE("page", 2);

        public static ObjectType DEFAULT = UNKNOWN;

        public static ObjectType fromInt(int enumValue) {
            for (ObjectType objectType : values()) {
                if (objectType.getValue() == enumValue) {
                    return objectType;
                }
            }

            return null;
        }

        private String stringValue;
        private int intValue;
        private ObjectType(String stringValue, int value) {
            this.stringValue = stringValue;
            this.intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }

        public int getValue() {
            return intValue;
        }
    }

    /**
     * Encapsulates the valid values for the facebook:style attribute for a LikeView
     */
    public enum Style {
        /**
         * Setting the attribute to this value will display the button and a sentence near it that
         * describes the social sentence for the associated object.
         * <p/>
         * This is the default value
         */
        STANDARD("standard", 0),

        /**
         * Setting the attribute to this value will display the button by itself, with no other
         * components
         */
        BUTTON("button", 1),

        /**
         * Setting the attribute to this value will display the button and a box near it with the
         * number of likes for the associated object
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
         * Setting the attribute to this value will center the button and auxiliary view in the
         * parent view.
         * <p/>
         * This is the default value
         */
        CENTER("center", 0),

        /**
         * Setting the attribute to this value will left-justify the button and auxiliary view in
         * the parent view.
         */
        LEFT("left", 1),

        /**
         * Setting the attribute to this value will right-justify the button and auxiliary view in
         * the parent view. If the facebook:auxiliary_view_position is set to INLINE, then the
         * auxiliary view will be on the left of the button
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
     * Encapsulates the valid values for the facebook:auxiliary_view_position attribute for a
     * LikeView.
     */
    public enum AuxiliaryViewPosition {
        /**
         * Setting the attribute to this value will put the social-sentence or box-count below the
         * like button. If the facebook:style is set to BUTTON, then this has no effect.
         * <p/>
         * This is the default value
         */
        BOTTOM("bottom", 0),

        /**
         * Setting the attribute to this value will put the social-sentence or box-count inline with
         * the like button. The auxiliary view will be to the left of the button if the
         * facebook:horizontal_alignment is set to RIGHT. In all other cases, it will be to the
         * right of the button. If the facebook:style is set to BUTTON, then this has no effect.
         */
        INLINE("inline", 1),

        /**
         * Setting the attribute to this value will put the social-sentence or box-count above the
         * like button. If the facebook:style is set to BUTTON, then this has no effect.
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
    private ObjectType objectType;
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

    private Fragment parentFragment;

    private boolean explicitlyDisabled;

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
     * Sets the associated object ID for this LikeView. Can be changed during runtime.
     * @param objectId The object ID, this can be a URL or a Facebook ID.
     */
    public void setObjectIdAndType(String objectId, ObjectType objectType) {
        objectId = Utility.coerceValueIfNullOrEmpty(objectId, null);
        objectType = objectType != null ? objectType : ObjectType.DEFAULT;

        if (!Utility.areObjectsEqual(objectId, this.objectId) || (objectType != this.objectType)) {
            setObjectIdAndTypeForced(objectId, objectType);

            updateLikeStateAndLayout();
        }
    }

    /**
     * Sets the facebook:style for this LikeView. Can be changed during runtime.
     *
     * @param likeViewStyle Should be either LikeView.STANDARD, LikeView.BUTTON or
     *                      LikeView.BOX_COUNT
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
     *
     * @param auxiliaryViewPosition Should be either LikeView.TOP, LikeView.INLINE or
     *                              LikeView.BOTTOM
     */
    public void setAuxiliaryViewPosition(AuxiliaryViewPosition auxiliaryViewPosition) {
        auxiliaryViewPosition = auxiliaryViewPosition != null
                ? auxiliaryViewPosition : AuxiliaryViewPosition.DEFAULT;
        if (this.auxiliaryViewPosition != auxiliaryViewPosition) {
            this.auxiliaryViewPosition = auxiliaryViewPosition;

            updateLayout();
        }
    }

    /**
     * Sets the facebook:horizontal_alignment for this LikeView. Can be changed during runtime.
     *
     * @param horizontalAlignment Should be either LikeView.LEFT, LikeView.CENTER or LikeView.RIGHT
     */
    public void setHorizontalAlignment(HorizontalAlignment horizontalAlignment) {
        horizontalAlignment = horizontalAlignment != null
                ? horizontalAlignment : HorizontalAlignment.DEFAULT;
        if (this.horizontalAlignment != horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;

            updateLayout();
        }
    }

    /**
     * Sets the facebook:foreground_color for this LikeView. Can be changed during runtime. The
     * color is only used for the social sentence text.
     *
     * @param foregroundColor And valid android.graphics.Color value.
     */
    public void setForegroundColor(int foregroundColor) {
        if (this.foregroundColor != foregroundColor) {
            socialSentenceView.setTextColor(foregroundColor);
        }
    }

    /**
     * Sets an OnErrorListener for this instance of LikeView to call into when
     * certain exceptions occur.
     *
     * @param onErrorListener The listener object to set
     */
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.onErrorListener = onErrorListener;
    }

    /**
     * Returns the current OnErrorListener for this instance of LikeView.
     *
     * @return The OnErrorListener
     */
    public OnErrorListener getOnErrorListener() {
        return onErrorListener;
    }

    /**
     * Sets the parent Fragment which is hosting this LikeView. This allows the LikeView to be
     * embedded inside a Fragment, and will allow the fragment to receive the
     * {@link Fragment#onActivityResult(int, int, android.content.Intent) onActivityResult}
     * call rather than the Activity, upon completion of Likes from this view.
     *
     * @param fragment Fragment that is hosting the LikeView.
     */
    public void setFragment(Fragment fragment) {
        this.parentFragment = fragment;
    }

    @Override
    public void setEnabled(boolean enabled) {
        explicitlyDisabled = !enabled;

        updateLikeStateAndLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        // Disassociate from the object
        setObjectIdAndType(null, ObjectType.UNKNOWN);

        super.onDetachedFromWindow();
    }

    private void parseAttributes(AttributeSet attrs) {
        if (attrs == null || getContext() == null) {
            return;
        }

        TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.com_facebook_like_view);
        if (a == null) {
            return;
        }

        objectId = Utility.coerceValueIfNullOrEmpty(
                a.getString(R.styleable.com_facebook_like_view_com_facebook_object_id), null);
        objectType = ObjectType.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_com_facebook_object_type,
                        ObjectType.DEFAULT.getValue()));
        likeViewStyle = Style.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_com_facebook_style,
                        Style.DEFAULT.getValue()));
        if (likeViewStyle == null) {
            throw new IllegalArgumentException("Unsupported value for LikeView 'style'");
        }

        auxiliaryViewPosition = AuxiliaryViewPosition.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_com_facebook_auxiliary_view_position,
                        AuxiliaryViewPosition.DEFAULT.getValue()));
        if (auxiliaryViewPosition == null) {
            throw new IllegalArgumentException(
                    "Unsupported value for LikeView 'auxiliary_view_position'");
        }

        horizontalAlignment = HorizontalAlignment.fromInt(
                a.getInt(R.styleable.com_facebook_like_view_com_facebook_horizontal_alignment,
                        HorizontalAlignment.DEFAULT.getValue()));
        if (horizontalAlignment == null) {
            throw new IllegalArgumentException(
                    "Unsupported value for LikeView 'horizontal_alignment'");
        }

        foregroundColor = a.getColor(
                R.styleable.com_facebook_like_view_com_facebook_foreground_color, NO_FOREGROUND_COLOR);

        a.recycle();
    }

    // If attributes were present, parseAttributes MUST be called before initialize() to ensure
    // proper behavior
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

        setObjectIdAndTypeForced(this.objectId, this.objectType);
        updateLikeStateAndLayout();
    }

    private void initializeLikeButton(Context context) {
        likeButton = new LikeButton(
                context,
                likeActionController != null && likeActionController.isObjectLiked());
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
            Activity activity = null;
            if (parentFragment == null) {
                activity = getActivity();
            }

            likeActionController.toggleLike(
                    activity,
                    parentFragment,
                    getAnalyticsParameters());
        }
    }

    private Activity getActivity() {
        Context context = getContext();
        while (!(context instanceof Activity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        if (context instanceof Activity) {
            return (Activity) context;
        }
        throw new FacebookException("Unable to get Activity.");
    }

    private Bundle getAnalyticsParameters() {
        Bundle params = new Bundle();
        params.putString(AnalyticsEvents.PARAMETER_LIKE_VIEW_STYLE, likeViewStyle.toString());
        params.putString(
                AnalyticsEvents.PARAMETER_LIKE_VIEW_AUXILIARY_POSITION,
                auxiliaryViewPosition.toString());
        params.putString(
                AnalyticsEvents.PARAMETER_LIKE_VIEW_HORIZONTAL_ALIGNMENT,
                horizontalAlignment.toString());
        params.putString(
                AnalyticsEvents.PARAMETER_LIKE_VIEW_OBJECT_ID,
                Utility.coerceValueIfNullOrEmpty(objectId, ""));
        params.putString(
                AnalyticsEvents.PARAMETER_LIKE_VIEW_OBJECT_TYPE,
                objectType.toString());

        return params;
    }

    private void setObjectIdAndTypeForced(String newObjectId, ObjectType newObjectType) {
        tearDownObjectAssociations();

        objectId = newObjectId;
        objectType = newObjectType;

        if (Utility.isNullOrEmpty(newObjectId)) {
            return;
        }

        creationCallback = new LikeActionControllerCreationCallback();
        if (!isInEditMode()) {
            LikeActionController.getControllerForObjectId(
                    newObjectId,
                    newObjectType,
                    creationCallback);
        }
    }

    private void associateWithLikeActionController(LikeActionController likeActionController) {
        this.likeActionController = likeActionController;

        this.broadcastReceiver = new LikeControllerBroadcastReceiver();
        LocalBroadcastManager localBroadcastManager =
                LocalBroadcastManager.getInstance(getContext());

        // add the broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_UPDATED);
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR);
        filter.addAction(LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_RESET);

        localBroadcastManager.registerReceiver(broadcastReceiver, filter);
    }

    private void tearDownObjectAssociations() {
        if (broadcastReceiver != null) {
            LocalBroadcastManager localBroadcastManager =
                    LocalBroadcastManager.getInstance(getContext());
            localBroadcastManager.unregisterReceiver(broadcastReceiver);

            broadcastReceiver = null;
        }

        // If we were already waiting on a controller to be given back, make sure we aren't waiting
        // anymore. Otherwise when that controller is given back to the callback, it will go and
        // register a broadcast receiver for it.
        if (creationCallback != null) {
            creationCallback.cancel();

            creationCallback = null;
        }

        likeActionController = null;
    }

    private void updateLikeStateAndLayout() {
        boolean enabled = !explicitlyDisabled;

        if (likeActionController == null) {
            likeButton.setSelected(false);
            socialSentenceView.setText(null);
            likeBoxCountView.setText(null);
        } else {
            likeButton.setSelected(likeActionController.isObjectLiked());
            socialSentenceView.setText(likeActionController.getSocialSentence());
            likeBoxCountView.setText(likeActionController.getLikeCountString());

            enabled &= likeActionController.shouldEnableView();
        }

        super.setEnabled(enabled);
        likeButton.setEnabled(enabled);

        updateLayout();
    }

    private void updateLayout() {
        // Make sure the container is horizontally aligned according to specifications.
        LayoutParams containerViewLayoutParams = (LayoutParams)containerView.getLayoutParams();
        LinearLayout.LayoutParams buttonLayoutParams =
                (LinearLayout.LayoutParams)likeButton.getLayoutParams();
        int viewGravity =
                horizontalAlignment == HorizontalAlignment.LEFT ? Gravity.LEFT :
                        horizontalAlignment == HorizontalAlignment.CENTER
                                ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT;

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
        LinearLayout.LayoutParams auxViewLayoutParams =
                (LinearLayout.LayoutParams)auxView.getLayoutParams();
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
                likeBoxCountView.setCaretPosition(
                        LikeBoxCountView.LikeBoxCountViewCaretPosition.BOTTOM);
                break;
            case BOTTOM:
                likeBoxCountView.setCaretPosition(
                        LikeBoxCountView.LikeBoxCountViewCaretPosition.TOP);
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
         * Called when the share action encounters an error.
         * @param error The error that occurred
         */
        public void onError(FacebookException error);
    }

    private class LikeControllerBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            Bundle extras = intent.getExtras();
            boolean shouldRespond = true;
            if (extras != null) {
                // See if an Id was set in the broadcast Intent. If it was, treat it as a filter.
                String broadcastObjectId = extras.getString(
                        LikeActionController.ACTION_OBJECT_ID_KEY);
                shouldRespond = Utility.isNullOrEmpty(broadcastObjectId) ||
                        Utility.areObjectsEqual(objectId, broadcastObjectId);
            }

            if (!shouldRespond) {
                return;
            }

            if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_UPDATED.equals(intentAction)) {
                updateLikeStateAndLayout();
            } else if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_ERROR.equals(
                    intentAction)) {
                if (onErrorListener != null) {
                    onErrorListener.onError(NativeProtocol.getExceptionFromErrorData(extras));
                }
            } else if (LikeActionController.ACTION_LIKE_ACTION_CONTROLLER_DID_RESET.equals(
                    intentAction)) {
                // This will recreate the controller and associated objects
                setObjectIdAndTypeForced(objectId, objectType);
                updateLikeStateAndLayout();
            }
        }
    }

    private class LikeActionControllerCreationCallback
            implements LikeActionController.CreationCallback {
        private boolean isCancelled;

        public void cancel() {
            isCancelled = true;
        }

        @Override
        public void onComplete(
                LikeActionController likeActionController,
                FacebookException error) {
            if (isCancelled) {
                return;
            }

            if (likeActionController != null) {
                if (!likeActionController.shouldEnableView()) {
                    error = new FacebookException(
                            "Cannot use LikeView. The device may not be supported.");
                }

                // Always associate with the controller, so it can get updates if the view gets
                // enabled again.
                associateWithLikeActionController(likeActionController);
                updateLikeStateAndLayout();
            }

            if (error != null) {
                if (onErrorListener != null) {
                    onErrorListener.onError(error);
                }
            }

            LikeView.this.creationCallback = null;
        }
    }
}
