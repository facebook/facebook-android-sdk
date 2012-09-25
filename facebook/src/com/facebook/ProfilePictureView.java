/**
 * Copyright 2010 Facebook, Inc.
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

package com.facebook;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import com.facebook.android.R;

import java.net.MalformedURLException;

/**
 * View that displays the profile photo of a supplied user ID, while conforming
 * to user specified dimensions.
 */
public class ProfilePictureView extends FrameLayout {

    public static final String TAG = ProfilePictureView.class.getSimpleName();

    public static final int CUSTOM = -1;
    public static final int SMALL = -2;
    public static final int NORMAL = -3;
    public static final int LARGE = -4;

    private static final int MIN_SIZE = 1;

    private String userId;
    private int queryHeight = ImageRequest.UNSPECIFIED_DIMENSION;
    private int queryWidth = ImageRequest.UNSPECIFIED_DIMENSION;
    private boolean isCropped;
    private ImageView image;
    private int presetSizeType = CUSTOM;
    private ImageRequest lastRequest;

    /**
     * Constructor
     *
     * @param context Context for this View
     */
    public ProfilePictureView(Context context) {
        super(context);
        initialize(context);
    }

    /**
     * Constructor
     *
     * @param context Context for this View
     * @param attrs   AttributeSet for this View.
     *                The attribute 'preset_size' is processed here
     */
    public ProfilePictureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
        parseAttributes(attrs);
    }

    /**
     * Constructor
     *
     * @param context  Context for this View
     * @param attrs    AttributeSet for this View.
     *                 The attribute 'preset_size' is processed here
     * @param defStyle Default style for this View
     */
    public ProfilePictureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
        parseAttributes(attrs);
    }

    /**
     * Gets the current preset size type
     *
     * @return The current preset size type, if set; CUSTOM if not
     */
    public final int getPresetSize() {
        return presetSizeType;
    }

    /**
     * Apply a preset size to this profile photo
     *
     * @param sizeType The size type to apply: SMALL, NORMAL or LARGE
     */
    public final void setPresetSize(int sizeType) {
        switch (sizeType) {
            case SMALL:
            case NORMAL:
            case LARGE:
            case CUSTOM:
                this.presetSizeType = sizeType;
                break;

            default:
                throw new IllegalArgumentException("Must use a predefined preset size");
        }

        requestLayout();
    }

    /**
     * Indicates whether the cropped version of the profile photo has been chosen
     *
     * @return True if the cropped version is chosen, false if not.
     */
    public final boolean isCropped() {
        return isCropped;
    }

    /**
     * Sets the profile photo to be the cropped version, or the original version
     *
     * @param showCroppedVersion True to select the cropped version
     *                           False to select the standard version
     */
    public final void setCropped(boolean showCroppedVersion) {
        isCropped = showCroppedVersion;
        // No need to force the refresh since we will catch the change in required dimensions
        refreshImage(false);
    }

    /**
     * Returns the user Id for the current profile photo
     *
     * @return The user Id
     */
    public final String getUserId() {
        return userId;
    }

    /**
     * Sets the user Id for this profile photo
     *
     * @param userId The userId
     *               NULL/Empty String will show the blank profile photo
     */
    public final void setUserId(String userId) {
        boolean force = Utility.isNullOrEmpty(this.userId) || !this.userId.equalsIgnoreCase(userId);
        this.userId = userId;

        refreshImage(force);
    }

    /**
     * Overriding onMeasure to handle the case where WRAP_CONTENT might be
     * specified in the layout. Since we don't know the dimensions of the profile
     * photo, we need to handle this case specifically.
     * <p/>
     * The approach is the default to a NORMAL sized amount of space in the case that
     * a preset size is not specified either. This logic is applied to each dimension.
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup.LayoutParams params = getLayoutParams();
        boolean customMeasure = false;
        int newHeight = MeasureSpec.getSize(heightMeasureSpec);
        int newWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY &&
                params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            newHeight = getPresetSizeInPixels(true); // Default to a preset size
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY);
            customMeasure = true;
        }

        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY &&
                params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
            newWidth = getPresetSizeInPixels(true); // Default to a preset size
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY);
            customMeasure = true;
        }

        if (customMeasure) {
            // Since we are providing custom dimensions, we need to handle the measure
            // phase from here
            setMeasuredDimension(newWidth, newHeight);
            measureChildren(widthMeasureSpec, heightMeasureSpec);
        } else {
            // Rely on FrameLayout to do the right thing
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right,
                bottom);

        // See if the image needs redrawing
        refreshImage(false);
    }

    private void initialize(Context context) {
        // We only want our ImageView in here. Nothing else is permitted
        removeAllViews();

        image = new ImageView(context);

        LayoutParams imageLayout = new LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        image.setLayoutParams(imageLayout);

        // We want to prevent up-scaling the image, but still have it fit within
        // the layout bounds as best as possible.
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        addView(image);
    }

    private void parseAttributes(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.com_facebook_profile_picture_view);
        setPresetSize(a.getInt(R.styleable.com_facebook_profile_picture_view_preset_size, CUSTOM));
        a.recycle();
    }

    private void refreshImage(boolean force) {
        boolean changed = updateImageQueryParameters();
        if (Utility.isNullOrEmpty(userId) ||
                ((queryWidth == ImageRequest.UNSPECIFIED_DIMENSION) &&
                        (queryHeight == ImageRequest.UNSPECIFIED_DIMENSION))) {
            int blankImage = isCropped() ?
                    R.drawable.com_facebook_profile_picture_blank_square :
                    R.drawable.com_facebook_profile_picture_blank_portrait;

            image.setImageDrawable(getResources().getDrawable(blankImage));
        } else if (changed || force) {
            try {
                ImageRequest request = ImageRequest.createProfilePictureImageRequest(
                        userId,
                        queryWidth,
                        queryHeight,
                        new ImageRequest.Callback() {
                            @Override
                            public void onCompleted(ImageResponse response) {
                                processResponse(response);
                            }
                        });

                ImageDownloader.downloadAsync(request);

                if (lastRequest != null) {
                    lastRequest.cancel();
                }
                lastRequest = request;
            } catch (MalformedURLException e) {
                Logger.log(LoggingBehaviors.REQUESTS, Log.ERROR, TAG, e.toString());
            }
        }
    }

    private void processResponse(ImageResponse response) {
        Bitmap bitmap = response.getBitmap();
        Exception error = response.getError();
        if (error != null) {
            Logger.log(LoggingBehaviors.REQUESTS, Log.ERROR, TAG, error.toString());
        } else if (bitmap != null) {
            image.setImageBitmap(bitmap);
        }
    }

    private boolean updateImageQueryParameters() {
        int newHeightPx = getHeight();
        int newWidthPx = getWidth();
        if (newWidthPx < MIN_SIZE || newHeightPx < MIN_SIZE) {
            // Not enough space laid out for this View yet. Or something else is awry.
            return false;
        }

        int presetSize = getPresetSizeInPixels(false);
        if (presetSize != ImageRequest.UNSPECIFIED_DIMENSION) {
            newWidthPx = presetSize;
            newHeightPx = presetSize;
        }

        // The cropped version is square
        // If full version is desired, then only one dimension is required.
        // TODO : Ideally the server should take both dimensions and make the best
        //      judgement about which size to return. However, until that is supported
        //      we always send the smaller dimension.
        if (newWidthPx <= newHeightPx) {
            newHeightPx = isCropped() ? newWidthPx : ImageRequest.UNSPECIFIED_DIMENSION;
        } else {
            newWidthPx = isCropped() ? newHeightPx : ImageRequest.UNSPECIFIED_DIMENSION;
        }

        boolean changed = (newWidthPx != queryWidth) || (newHeightPx != queryHeight);

        queryWidth = newWidthPx;
        queryHeight = newHeightPx;

        return changed;
    }

    private int getPresetSizeInPixels(boolean forcePreset) {
        int dimensionId = 0;
        switch (presetSizeType) {
            case SMALL:
                dimensionId = R.dimen.com_facebook_profilepictureview_preset_size_small;
                break;
            case NORMAL:
                dimensionId = R.dimen.com_facebook_profilepictureview_preset_size_normal;
                break;
            case LARGE:
                dimensionId = R.dimen.com_facebook_profilepictureview_preset_size_large;
                break;
            case CUSTOM:
                if (!forcePreset) {
                    return ImageRequest.UNSPECIFIED_DIMENSION;
                } else {
                    dimensionId = R.dimen.com_facebook_profilepictureview_preset_size_normal;
                    break;
                }
            default:
                return ImageRequest.UNSPECIFIED_DIMENSION;
        }

        return getResources().getDimensionPixelSize(dimensionId);
    }
}
