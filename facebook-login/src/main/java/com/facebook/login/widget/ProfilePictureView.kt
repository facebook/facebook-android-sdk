/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.login.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.facebook.AccessToken
import com.facebook.FacebookException
import com.facebook.LoggingBehavior
import com.facebook.Profile
import com.facebook.ProfileTracker
import com.facebook.internal.ImageDownloader
import com.facebook.internal.ImageRequest
import com.facebook.internal.ImageResponse
import com.facebook.internal.Logger
import com.facebook.internal.instrument.crashshield.AutoHandleExceptions
import com.facebook.login.R

/**
 * View that displays the profile photo of a supplied profile ID, while conforming to user specified
 * dimensions.
 */
class ProfilePictureView : FrameLayout {
  /**
   * Callback interface that will be called when a network or other error is encountered while
   * retrieving profile pictures.
   */
  interface OnErrorListener {
    /**
     * Called when a network or other error is encountered.
     *
     * @param error a FacebookException representing the error that was encountered.
     */
    fun onError(error: FacebookException)
  }

  private val image = ImageView(context)

  private var queryHeight = ImageRequest.UNSPECIFIED_DIMENSION
  private var queryWidth = ImageRequest.UNSPECIFIED_DIMENSION
  private var imageContents: Bitmap? = null
  private var lastRequest: ImageRequest? = null
  private var customizedDefaultProfilePicture: Bitmap? = null
  private var profileTracker: ProfileTracker? = null

  /** Profile Id for the current profile photo */
  var profileId: String? = null
    set(value) {
      var force = false
      if (field.isNullOrEmpty() || !field.equals(value, ignoreCase = true)) {
        // Clear out the old profilePicture before requesting for the new one.
        setBlankProfilePicture()
        force = true
      }
      field = value
      refreshImage(force)
    }

  /** Indicates whether the cropped version of the profile photo has been chosen */
  var isCropped = IS_CROPPED_DEFAULT_VALUE
    set(value) {
      field = value
      // No need to force the refresh since we will catch the change in required dimensions
      refreshImage(false)
    }

  /**
   * OnErrorListener for this instance of ProfilePictureView to call into when certain
   * errors occur.
   */
  var onErrorListener: OnErrorListener? = null

  /** Preset size of this profile photo (SMALL, NORMAL or LARGE) */
  var presetSize = CUSTOM
    set(value) {
      when (value) {
        SMALL, NORMAL, LARGE, CUSTOM -> field = value
        else -> throw IllegalArgumentException("Must use a predefined preset size")
      }
      requestLayout()
    }

  /** Indicates whether the ProfilePictureView should subscribe to Profile updates */
  var shouldUpdateOnProfileChange: Boolean
    get() = profileTracker?.isTracking ?: false
    set(value) {
      if (value) {
        profileTracker?.startTracking()
      } else {
        profileTracker?.stopTracking()
      }
    }

  /**
   * Constructor
   *
   * @param context Context for this View
   */
  constructor(context: Context) : super(context) {
    initialize()
  }

  /**
   * Constructor
   *
   * @param context Context for this View
   * @param attrs AttributeSet for this View. The attribute 'preset_size' is processed here
   */
  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    initialize()
    parseAttributes(attrs)
  }

  /**
   * Constructor
   *
   * @param context Context for this View
   * @param attrs AttributeSet for this View. The attribute 'preset_size' is processed here
   * @param defStyle Default style for this View
   */
  constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
    initialize()
    parseAttributes(attrs)
  }

  @AutoHandleExceptions
  private fun initialize() {
    // We only want our ImageView in here. Nothing else is permitted
    removeAllViews()
    val imageLayout = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    image.layoutParams = imageLayout

    // We want to prevent up-scaling the image, but still have it fit within
    // the layout bounds as best as possible.
    image.scaleType = ImageView.ScaleType.CENTER_INSIDE
    addView(image)
    profileTracker = object : ProfileTracker() {
      override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?) {
        profileId = currentProfile?.id
        refreshImage(true)
      }
    }
  }

  /**
   * The ProfilePictureView will display the provided image while the specified profile is being
   * loaded, or if the specified profile is not available.
   *
   * @param inputBitmap The bitmap to render until the actual profile is loaded.
   */
  fun setDefaultProfilePicture(inputBitmap: Bitmap?) {
    customizedDefaultProfilePicture = inputBitmap
  }

  /**
   * Overriding onMeasure to handle the case where WRAP_CONTENT might be specified in the layout.
   * Since we don't know the dimensions of the profile photo, we need to handle this case
   * specifically.
   *
   * The approach is to default to a NORMAL sized amount of space in the case that a preset size
   * is not specified. This logic is applied to both width and height
   */
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    var widthMeasure = widthMeasureSpec
    var heightMeasure = heightMeasureSpec
    val params = layoutParams
    var customMeasure = false
    var newHeight = MeasureSpec.getSize(heightMeasure)
    var newWidth = MeasureSpec.getSize(widthMeasure)
    if (MeasureSpec.getMode(heightMeasure) != MeasureSpec.EXACTLY
        && params.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
      newHeight = getPresetSizeInPixels(true) // Default to a preset size
      heightMeasure = MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
      customMeasure = true
    }
    if (MeasureSpec.getMode(widthMeasure) != MeasureSpec.EXACTLY
        && params.width == ViewGroup.LayoutParams.WRAP_CONTENT) {
      newWidth = getPresetSizeInPixels(true) // Default to a preset size
      widthMeasure = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY)
      customMeasure = true
    }
    if (customMeasure) {
      // Since we are providing custom dimensions, we need to handle the measure
      // phase from here
      setMeasuredDimension(newWidth, newHeight)
      measureChildren(widthMeasure, heightMeasure)
    } else {
      // Rely on FrameLayout to do the right thing
      super.onMeasure(widthMeasure, heightMeasure)
    }
  }

  /**
   * In addition to calling super.Layout(), we also attempt to get a new image that is properly
   * sized for the layout dimensions
   */
  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    super.onLayout(changed, left, top, right, bottom)

    // See if the image needs redrawing
    refreshImage(false)
  }

  /**
   * Some of the current state is returned as a Bundle to allow quick restoration of the
   * ProfilePictureView object in scenarios like orientation changes.
   *
   * @return a Parcelable containing the current state
   */
  override fun onSaveInstanceState(): Parcelable {
    val superState = super.onSaveInstanceState()
    val instanceState = Bundle()
    instanceState.putParcelable(SUPER_STATE_KEY, superState)
    instanceState.putString(PROFILE_ID_KEY, profileId)
    instanceState.putInt(PRESET_SIZE_KEY, this.presetSize)
    instanceState.putBoolean(IS_CROPPED_KEY, isCropped)
    instanceState.putInt(BITMAP_WIDTH_KEY, queryWidth)
    instanceState.putInt(BITMAP_HEIGHT_KEY, queryHeight)
    instanceState.putBoolean(PENDING_REFRESH_KEY, lastRequest != null)
    return instanceState
  }

  /**
   * If the passed in state is a Bundle, an attempt is made to restore from it.
   *
   * @param state a Parcelable containing the current state
   */
  override fun onRestoreInstanceState(state: Parcelable) {
    if (state.javaClass != Bundle::class.java) {
      super.onRestoreInstanceState(state)
    } else {
      val instanceState = state as Bundle
      super.onRestoreInstanceState(instanceState.getParcelable(SUPER_STATE_KEY))
      profileId = instanceState.getString(PROFILE_ID_KEY)
      this.presetSize = instanceState.getInt(PRESET_SIZE_KEY)
      isCropped = instanceState.getBoolean(IS_CROPPED_KEY)
      queryWidth = instanceState.getInt(BITMAP_WIDTH_KEY)
      queryHeight = instanceState.getInt(BITMAP_HEIGHT_KEY)
      refreshImage(true)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    // Null out lastRequest. This way, when the response is returned, we can ascertain
    // that the view is detached and hence should not attempt to update its contents.
    lastRequest = null
  }

  @AutoHandleExceptions
  private fun parseAttributes(attrs: AttributeSet) {
    val a = context.obtainStyledAttributes(attrs, R.styleable.com_facebook_profile_picture_view)
    this.presetSize = a.getInt(R.styleable.com_facebook_profile_picture_view_com_facebook_preset_size, CUSTOM)
    isCropped = a.getBoolean(
        R.styleable.com_facebook_profile_picture_view_com_facebook_is_cropped,
        IS_CROPPED_DEFAULT_VALUE
    )
    a.recycle()
  }

  @AutoHandleExceptions
  private fun refreshImage(force: Boolean) {
    val changed = updateImageQueryParameters()
    // Note: do not use Utility.isNullOrEmpty here as this will cause the Eclipse
    // Graphical Layout editor to fail in some cases
    val profileId = this.profileId
    if (profileId == null || profileId.isEmpty() || isUnspecifiedDimensions()) {
      setBlankProfilePicture()
    } else if (changed || force) {
      sendImageRequest(true)
    }
  }

  private fun isUnspecifiedDimensions(): Boolean {
    return queryWidth == ImageRequest.UNSPECIFIED_DIMENSION
        && queryHeight == ImageRequest.UNSPECIFIED_DIMENSION
  }

  @AutoHandleExceptions
  private fun setBlankProfilePicture() {
    // If we have a pending image download request cancel it
    lastRequest?.let { ImageDownloader.cancelRequest(it) }
    val customizedPicture = customizedDefaultProfilePicture
    if (customizedPicture == null) {
      val blankImageResource = if (isCropped)
        R.drawable.com_facebook_profile_picture_blank_square
      else
        R.drawable.com_facebook_profile_picture_blank_portrait
      setImageBitmap(BitmapFactory.decodeResource(resources, blankImageResource))
    } else {
      // Update profile image dimensions.
      updateImageQueryParameters()
      // Resize inputBitmap to new dimensions of queryWidth and queryHeight.
      val scaledBitmap = Bitmap.createScaledBitmap(customizedPicture, queryWidth, queryHeight, false)
      setImageBitmap(scaledBitmap)
    }
  }

  @AutoHandleExceptions
  private fun setImageBitmap(imageBitmap: Bitmap?) {
    imageBitmap?.let {
      imageContents = it // Hold for save-restore cycles
      image.setImageBitmap(it)
    }
  }

  @AutoHandleExceptions
  private fun sendImageRequest(allowCachedResponse: Boolean) {
    val accessToken = if (AccessToken.isCurrentAccessTokenActive())
      AccessToken.getCurrentAccessToken()?.token ?: ""
    else
      ""
    val profilePictureUri = getProfilePictureUri(accessToken)
    val request = ImageRequest.Builder(context, profilePictureUri)
        .setAllowCachedRedirects(allowCachedResponse)
        .setCallerTag(this)
        .setCallback { response -> processResponse(response) }
        .build()

    // Make sure to cancel the old request before sending the new one to prevent
    // accidental cancellation of the new request. This could happen if the URL and
    // caller tag stayed the same.
    lastRequest?.let { ImageDownloader.cancelRequest(it) }
    lastRequest = request
    ImageDownloader.downloadAsync(request)
  }

  private fun getProfilePictureUri(accessToken: String): Uri {
    val currentProfile = Profile.getCurrentProfile()
    return if (currentProfile != null && AccessToken.isLoggedInWithInstagram())
      currentProfile.getProfilePictureUri(queryWidth, queryHeight)
    else
      ImageRequest.getProfilePictureUri(profileId, queryWidth, queryHeight, accessToken)
  }

  @AutoHandleExceptions
  private fun processResponse(response: ImageResponse?) {
    // First check if the response is for the right request. We may have:
    // 1. Sent a new request, thus super-ceding this one.
    // 2. Detached this view, in which case the response should be discarded.
    if (response == null || response.request != lastRequest) {
      return
    }

    lastRequest = null
    val responseImage = response.bitmap
    val error = response.error
    if (error != null) {
      val listener = onErrorListener
      if (listener != null) {
        listener.onError(
            FacebookException("Error in downloading profile picture for profileId: $profileId", error)
        )
      } else {
        Logger.log(LoggingBehavior.REQUESTS, Log.ERROR, TAG, error.toString())
      }
    } else {
      responseImage?.let {
        setImageBitmap(it)
        if (response.isCachedRedirect) {
          sendImageRequest(false)
        }
      }
    }
  }

  @AutoHandleExceptions
  private fun updateImageQueryParameters(): Boolean {
    var newHeightPx = height
    var newWidthPx = width
    if (newWidthPx < MIN_SIZE || newHeightPx < MIN_SIZE) {
      // Not enough space laid out for this View yet. Or something else is awry.
      return false
    }
    val presetSize = getPresetSizeInPixels(false)
    if (presetSize != ImageRequest.UNSPECIFIED_DIMENSION) {
      newWidthPx = presetSize
      newHeightPx = presetSize
    }

    // The cropped version is square
    // If full version is desired, then only one dimension is required.
    if (newWidthPx <= newHeightPx) {
      newHeightPx = if (isCropped) newWidthPx else ImageRequest.UNSPECIFIED_DIMENSION
    } else {
      newWidthPx = if (isCropped) newHeightPx else ImageRequest.UNSPECIFIED_DIMENSION
    }
    val changed = newWidthPx != queryWidth || newHeightPx != queryHeight
    queryWidth = newWidthPx
    queryHeight = newHeightPx
    return changed
  }

  @AutoHandleExceptions
  private fun getPresetSizeInPixels(forcePreset: Boolean): Int {
    if (this.presetSize == CUSTOM && !forcePreset) {
      return ImageRequest.UNSPECIFIED_DIMENSION
    }
    val dimensionId: Int = when (this.presetSize) {
      SMALL -> R.dimen.com_facebook_profilepictureview_preset_size_small
      NORMAL -> R.dimen.com_facebook_profilepictureview_preset_size_normal
      LARGE -> R.dimen.com_facebook_profilepictureview_preset_size_large
      CUSTOM -> R.dimen.com_facebook_profilepictureview_preset_size_normal
      else -> return ImageRequest.UNSPECIFIED_DIMENSION
    }
    return resources.getDimensionPixelSize(dimensionId)
  }

  companion object {
    /** Tag used when logging calls are made by ProfilePictureView  */
    val TAG: String = ProfilePictureView::class.java.simpleName

    /**
     * Indicates that the specific size of the View will be set via layout params. ProfilePictureView
     * will default to NORMAL X NORMAL, if the layout params set on this instance do not have a fixed
     * size. Used in calls to setPresetSize() and getPresetSize(). Corresponds with the preset_size
     * Xml attribute that can be set on ProfilePictureView.
     */
    const val CUSTOM = -1

    /**
     * Indicates that the profile image should fit in a SMALL X SMALL space, regardless of whether the
     * cropped or un-cropped version is chosen. Used in calls to setPresetSize() and getPresetSize().
     * Corresponds with the preset_size Xml attribute that can be set on ProfilePictureView.
     */
    const val SMALL = -2

    /**
     * Indicates that the profile image should fit in a NORMAL X NORMAL space, regardless of whether
     * the cropped or un-cropped version is chosen. Used in calls to setPresetSize() and
     * getPresetSize(). Corresponds with the preset_size Xml attribute that can be set on
     * ProfilePictureView.
     */
    const val NORMAL = -3

    /**
     * Indicates that the profile image should fit in a LARGE X LARGE space, regardless of whether the
     * cropped or un-cropped version is chosen. Used in calls to setPresetSize() and getPresetSize().
     * Corresponds with the preset_size Xml attribute that can be set on ProfilePictureView.
     */
    const val LARGE = -4
    private const val MIN_SIZE = 1
    private const val IS_CROPPED_DEFAULT_VALUE = true
    private const val SUPER_STATE_KEY = "ProfilePictureView_superState"
    private const val PROFILE_ID_KEY = "ProfilePictureView_profileId"
    private const val PRESET_SIZE_KEY = "ProfilePictureView_presetSize"
    private const val IS_CROPPED_KEY = "ProfilePictureView_isCropped"
    private const val BITMAP_KEY = "ProfilePictureView_bitmap"
    private const val BITMAP_WIDTH_KEY = "ProfilePictureView_width"
    private const val BITMAP_HEIGHT_KEY = "ProfilePictureView_height"
    private const val PENDING_REFRESH_KEY = "ProfilePictureView_refresh"
  }
}
