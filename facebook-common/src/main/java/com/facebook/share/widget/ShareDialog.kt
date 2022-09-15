/*
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

package com.facebook.share.widget

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.facebook.AccessToken
import com.facebook.FacebookCallback
import com.facebook.FacebookSdk
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.AppCall
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.DialogFeature
import com.facebook.internal.DialogPresenter
import com.facebook.internal.FacebookDialogBase
import com.facebook.internal.FragmentWrapper
import com.facebook.internal.NativeAppCallAttachmentStore
import com.facebook.share.Sharer
import com.facebook.share.internal.CameraEffectFeature
import com.facebook.share.internal.LegacyNativeDialogParameters
import com.facebook.share.internal.NativeDialogParameters
import com.facebook.share.internal.ShareContentValidation
import com.facebook.share.internal.ShareDialogFeature
import com.facebook.share.internal.ShareFeedContent
import com.facebook.share.internal.ShareInternalUtility
import com.facebook.share.internal.ShareStoryFeature
import com.facebook.share.internal.WebDialogParameters
import com.facebook.share.model.ShareCameraEffectContent
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideoContent
import java.util.UUID

/** Provides functionality to share content via the Facebook Share Dialog */
open class ShareDialog : FacebookDialogBase<ShareContent<*, *>, Sharer.Result>, Sharer {
  /** The mode for the share dialog. */
  enum class Mode {
    /** The mode is determined automatically. */
    AUTOMATIC,
    /** The native dialog is used. */
    NATIVE,
    /** The web dialog is used. */
    WEB,
    /** The feed dialog is used. */
    FEED
  }

  private var shouldFailOnDataError = false

  // Keep track of Mode overrides for logging purposes.
  private var isAutomaticMode = true

  /**
   * Constructs a new ShareDialog.
   *
   * @param activity Activity to use to share the provided content.
   */
  constructor(activity: Activity) : this(activity, DEFAULT_REQUEST_CODE)

  /**
   * Constructs a new ShareDialog without any context. For androidx to create ActivityResultContract
   * only.
   */
  constructor(requestCode: Int = DEFAULT_REQUEST_CODE) : super(requestCode) {
    ShareInternalUtility.registerStaticShareCallback(requestCode)
  }

  /**
   * Constructs a new ShareDialog.
   *
   * @param fragment androidx.fragment.app.Fragment to use to share the provided content.
   */
  constructor(fragment: Fragment) : this(FragmentWrapper(fragment))

  /**
   * Constructs a new ShareDialog.
   *
   * @param fragment android.app.Fragment to use to share the provided content.
   */
  constructor(fragment: android.app.Fragment) : this(FragmentWrapper(fragment))

  /** for internal use only */
  constructor(activity: Activity, requestCode: Int) : super(activity, requestCode) {
    ShareInternalUtility.registerStaticShareCallback(requestCode)
  }
  /** for internal use only */
  constructor(fragment: Fragment, requestCode: Int) : this(FragmentWrapper(fragment), requestCode)
  /** for internal use only */
  constructor(
      fragment: android.app.Fragment,
      requestCode: Int
  ) : this(FragmentWrapper(fragment), requestCode)
  /** for internal use only */
  constructor(
      fragmentWrapper: FragmentWrapper,
      requestCode: Int = DEFAULT_REQUEST_CODE
  ) : super(fragmentWrapper, requestCode) {
    ShareInternalUtility.registerStaticShareCallback(requestCode)
  }

  override fun registerCallbackImpl(
      callbackManager: CallbackManagerImpl,
      callback: FacebookCallback<Sharer.Result>
  ) {
    ShareInternalUtility.registerSharerCallback(requestCode, callbackManager, callback)
  }

  override fun getShouldFailOnDataError(): Boolean = shouldFailOnDataError

  override fun setShouldFailOnDataError(shouldFailOnDataError: Boolean) {
    this.shouldFailOnDataError = shouldFailOnDataError
  }

  /**
   * Call this to check if the Share Dialog can be shown in a specific mode.
   *
   * @param mode Mode of the Share Dialog
   * @return True if the dialog can be shown in the passed in Mode
   */
  open fun canShow(content: ShareContent<*, *>, mode: Mode): Boolean {
    return canShowImpl(content, if (mode == Mode.AUTOMATIC) BASE_AUTOMATIC_MODE else mode)
  }

  /**
   * Call this to show the Share Dialog in a specific mode
   *
   * @param mode Mode of the Share Dialog
   */
  open fun show(content: ShareContent<*, *>, mode: Mode) {
    isAutomaticMode = mode == Mode.AUTOMATIC
    showImpl(content, if (isAutomaticMode) BASE_AUTOMATIC_MODE else mode)
  }

  override fun createBaseAppCall(): AppCall = AppCall(requestCode)

  // Feed takes precedence for link-shares for Mode.AUTOMATIC
  // Share into story
  override val orderedModeHandlers: List<ModeHandler> =
      arrayListOf(
          NativeHandler(),
          FeedHandler(),
          WebShareHandler(),
          CameraEffectHandler(),
          ShareStoryHandler())

  private inner class NativeHandler : ModeHandler() {
    override var mode: Any = Mode.NATIVE

    override fun canShow(content: ShareContent<*, *>, isBestEffort: Boolean): Boolean {
      if (content is ShareCameraEffectContent || content is ShareStoryContent) {
        return false
      }
      var canShowResult = true
      if (!isBestEffort) {
        // The following features are considered best-effort and will not prevent the
        // native share dialog from being presented, even if the installed version does
        // not support the feature.
        // However, to let apps pivot to a different approach or dialog (for example, Web),
        // we need to be able to signal back when native support is lacking.
        if (content.shareHashtag != null) {
          canShowResult =
              DialogPresenter.canPresentNativeDialogWithFeature(ShareDialogFeature.HASHTAG)
        }
        if (content is ShareLinkContent && !content.quote.isNullOrEmpty()) {
          canShowResult =
              canShowResult &&
                  DialogPresenter.canPresentNativeDialogWithFeature(
                      ShareDialogFeature.LINK_SHARE_QUOTES)
        }
      }
      return canShowResult && canShowNative(content.javaClass)
    }

    override fun createAppCall(content: ShareContent<*, *>): AppCall? {
      logDialogShare(activityContext, content, Mode.NATIVE)
      ShareContentValidation.validateForNativeShare(content)
      val appCall = createBaseAppCall()
      val shouldFailOnDataError = getShouldFailOnDataError()
      val feature = getFeature(content.javaClass) ?: return null
      DialogPresenter.setupAppCallForNativeDialog(
          appCall,
          object : DialogPresenter.ParameterProvider {
            override val parameters: Bundle?
              get() = NativeDialogParameters.create(appCall.callId, content, shouldFailOnDataError)
            override val legacyParameters: Bundle?
              get() =
                  LegacyNativeDialogParameters.create(
                      appCall.callId, content, shouldFailOnDataError)
          },
          feature)
      return appCall
    }
  }

  private inner class WebShareHandler : ModeHandler() {
    override var mode: Any = Mode.WEB

    override fun canShow(content: ShareContent<*, *>, isBestEffort: Boolean): Boolean {
      return canShowWebCheck(content)
    }

    override fun createAppCall(content: ShareContent<*, *>): AppCall? {
      logDialogShare(activityContext, content, Mode.WEB)
      val appCall = createBaseAppCall()
      ShareContentValidation.validateForWebShare(content)
      val params =
          when (content) {
            is ShareLinkContent -> {
              WebDialogParameters.create(content)
            }
            is SharePhotoContent -> {
              val photoContent = createAndMapAttachments(content, appCall.callId)
              WebDialogParameters.create(photoContent)
            }
            else -> {
              return null
            }
          }
      DialogPresenter.setupAppCallForWebDialog(appCall, getActionName(content), params)
      return appCall
    }

    private fun getActionName(shareContent: ShareContent<*, *>): String? {
      return when {
        (shareContent is ShareLinkContent || shareContent is SharePhotoContent) -> WEB_SHARE_DIALOG
        else -> null
      }
    }

    private fun createAndMapAttachments(
        content: SharePhotoContent,
        callId: UUID
    ): SharePhotoContent {
      val contentBuilder = SharePhotoContent.Builder().readFrom(content)
      val photos: MutableList<SharePhoto> = ArrayList()
      val attachments: MutableList<NativeAppCallAttachmentStore.Attachment> = ArrayList()
      for (i in content.photos.indices) {
        var sharePhoto = content.photos[i]
        val photoBitmap = sharePhoto.bitmap
        if (photoBitmap != null) {
          val attachment = NativeAppCallAttachmentStore.createAttachment(callId, photoBitmap)
          sharePhoto =
              SharePhoto.Builder()
                  .readFrom(sharePhoto)
                  .setImageUrl(Uri.parse(attachment.attachmentUrl))
                  .setBitmap(null)
                  .build()
          attachments.add(attachment)
        }
        photos.add(sharePhoto)
      }
      contentBuilder.setPhotos(photos)
      NativeAppCallAttachmentStore.addAttachments(attachments)
      return contentBuilder.build()
    }
  }

  private inner class FeedHandler : ModeHandler() {
    override var mode: Any = Mode.FEED

    override fun canShow(content: ShareContent<*, *>, isBestEffort: Boolean): Boolean {
      return content is ShareLinkContent || content is ShareFeedContent
    }

    override fun createAppCall(content: ShareContent<*, *>): AppCall? {
      logDialogShare(activityContext, content, Mode.FEED)
      val appCall = createBaseAppCall()
      val params =
          when (content) {
            is ShareLinkContent -> {
              ShareContentValidation.validateForWebShare(content)
              WebDialogParameters.createForFeed(content)
            }
            is ShareFeedContent -> {
              WebDialogParameters.createForFeed(content)
            }
            else -> {
              return null
            }
          }
      DialogPresenter.setupAppCallForWebDialog(appCall, FEED_DIALOG, params)
      return appCall
    }
  }

  private inner class CameraEffectHandler : ModeHandler() {
    override var mode: Any = Mode.NATIVE

    override fun canShow(content: ShareContent<*, *>, isBestEffort: Boolean): Boolean {
      val canShowResult = content is ShareCameraEffectContent
      return canShowResult && canShowNative(content.javaClass)
    }

    override fun createAppCall(content: ShareContent<*, *>): AppCall? {
      ShareContentValidation.validateForNativeShare(content)
      val appCall = createBaseAppCall()
      val shouldFailOnDataError = getShouldFailOnDataError()
      val feature = getFeature(content.javaClass) ?: return null
      DialogPresenter.setupAppCallForNativeDialog(
          appCall,
          object : DialogPresenter.ParameterProvider {
            override val parameters: Bundle?
              get() = NativeDialogParameters.create(appCall.callId, content, shouldFailOnDataError)
            override val legacyParameters: Bundle?
              get() =
                  LegacyNativeDialogParameters.create(
                      appCall.callId, content, shouldFailOnDataError)
          },
          feature)
      return appCall
    }
  }

  private inner class ShareStoryHandler : ModeHandler() {
    override var mode: Any = Mode.NATIVE

    override fun canShow(content: ShareContent<*, *>, isBestEffort: Boolean): Boolean {
      val canShowResult = content is ShareStoryContent
      return canShowResult && canShowNative(content.javaClass)
    }

    override fun createAppCall(content: ShareContent<*, *>): AppCall? {
      ShareContentValidation.validateForStoryShare(content)
      val appCall = createBaseAppCall()
      val shouldFailOnDataError = getShouldFailOnDataError()
      val feature = getFeature(content.javaClass) ?: return null
      DialogPresenter.setupAppCallForNativeDialog(
          appCall,
          object : DialogPresenter.ParameterProvider {
            override val parameters: Bundle?
              get() = NativeDialogParameters.create(appCall.callId, content, shouldFailOnDataError)
            override val legacyParameters: Bundle?
              get() =
                  LegacyNativeDialogParameters.create(
                      appCall.callId, content, shouldFailOnDataError)
          },
          feature)
      return appCall
    }
  }

  private fun logDialogShare(context: Context?, content: ShareContent<*, *>, mode: Mode) {
    val displayMode = if (isAutomaticMode) Mode.AUTOMATIC else mode
    val displayType =
        when (displayMode) {
          Mode.AUTOMATIC -> AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_AUTOMATIC
          Mode.WEB -> AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_WEB
          Mode.NATIVE -> AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_NATIVE
          else -> AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW_UNKNOWN
        }
    val contentType: String
    val dialogFeature = getFeature(content.javaClass)
    contentType =
        when {
          dialogFeature === ShareDialogFeature.SHARE_DIALOG -> {
            AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_STATUS
          }
          dialogFeature === ShareDialogFeature.PHOTOS -> {
            AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_PHOTO
          }
          dialogFeature === ShareDialogFeature.VIDEO -> {
            AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_VIDEO
          }
          else -> {
            AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_UNKNOWN
          }
        }
    val logger = InternalAppEventsLogger.createInstance(context, FacebookSdk.getApplicationId())
    val parameters = Bundle()
    parameters.putString(AnalyticsEvents.PARAMETER_SHARE_DIALOG_SHOW, displayType)
    parameters.putString(AnalyticsEvents.PARAMETER_SHARE_DIALOG_CONTENT_TYPE, contentType)
    logger.logEventImplicitly(AnalyticsEvents.EVENT_SHARE_DIALOG_SHOW, parameters)
  }

  companion object {
    private val TAG = ShareDialog::class.java.simpleName
    private const val FEED_DIALOG = "feed"
    const val WEB_SHARE_DIALOG = "share"
    private const val WEB_OG_SHARE_DIALOG = "share_open_graph"
    private val DEFAULT_REQUEST_CODE = CallbackManagerImpl.RequestCodeOffset.Share.toRequestCode()

    /**
     * Helper to show the provided [com.facebook.share.model.ShareContent] using the provided
     * Activity. No callback will be invoked.
     *
     * @param activity Activity to use to share the provided content
     * @param shareContent Content to share
     */
    @JvmStatic
    open fun show(activity: Activity, shareContent: ShareContent<*, *>) {
      ShareDialog(activity).show(shareContent)
    }

    /**
     * Helper to show the provided [com.facebook.share.model.ShareContent] using the provided
     * Fragment. No callback will be invoked.
     *
     * @param fragment androidx.fragment.app.Fragment to use to share the provided content
     * @param shareContent Content to share
     */
    @JvmStatic
    open fun show(fragment: Fragment, shareContent: ShareContent<*, *>) {
      show(FragmentWrapper(fragment), shareContent)
    }

    /**
     * Helper to show the provided [com.facebook.share.model.ShareContent] using the provided
     * Fragment. No callback will be invoked.
     *
     * @param fragment android.app.Fragment to use to share the provided content
     * @param shareContent Content to share
     */
    @JvmStatic
    open fun show(fragment: android.app.Fragment, shareContent: ShareContent<*, *>) {
      show(FragmentWrapper(fragment), shareContent)
    }

    private fun show(fragmentWrapper: FragmentWrapper, shareContent: ShareContent<*, *>) {
      ShareDialog(fragmentWrapper).show(shareContent)
    }

    /**
     * Indicates whether it is possible to show the dialog for [ ] of the specified type.
     *
     * @param contentType Class of the intended [com.facebook.share.model.ShareContent] to share.
     * @return True if the specified content type can be shown via the dialog
     */
    @JvmStatic
    open fun canShow(contentType: Class<out ShareContent<*, *>>): Boolean {
      return canShowWebTypeCheck(contentType) || canShowNative(contentType)
    }

    private fun canShowNative(contentType: Class<out ShareContent<*, *>>): Boolean {
      val feature = getFeature(contentType)
      return feature != null && DialogPresenter.canPresentNativeDialogWithFeature(feature)
    }

    private fun canShowWebTypeCheck(contentType: Class<out ShareContent<*, *>>): Boolean {
      // If we don't have an instance of a ShareContent, then all we can do is check whether
      // this is a ShareLinkContent, which can be shared if configured properly.
      // The instance method version of this check is more accurate and should be used on
      // ShareDialog instances.

      // SharePhotoContent currently requires the user staging endpoint, so we need a user access
      // token, so we need to see if we have one
      return (ShareLinkContent::class.java.isAssignableFrom(contentType) ||
          (SharePhotoContent::class.java.isAssignableFrom(contentType) &&
              AccessToken.isCurrentAccessTokenActive()))
    }

    private fun canShowWebCheck(content: ShareContent<*, *>): Boolean {
      if (!canShowWebTypeCheck(content.javaClass)) {
        return false
      }
      return true
    }

    private fun getFeature(contentType: Class<out ShareContent<*, *>>): DialogFeature? {
      return when {
        ShareLinkContent::class.java.isAssignableFrom(contentType) -> {
          ShareDialogFeature.SHARE_DIALOG
        }
        SharePhotoContent::class.java.isAssignableFrom(contentType) -> {
          ShareDialogFeature.PHOTOS
        }
        ShareVideoContent::class.java.isAssignableFrom(contentType) -> {
          ShareDialogFeature.VIDEO
        }
        ShareMediaContent::class.java.isAssignableFrom(contentType) -> {
          ShareDialogFeature.MULTIMEDIA
        }
        ShareCameraEffectContent::class.java.isAssignableFrom(contentType) -> {
          CameraEffectFeature.SHARE_CAMERA_EFFECT
        }
        ShareStoryContent::class.java.isAssignableFrom(contentType) -> {
          ShareStoryFeature.SHARE_STORY_ASSET
        }
        else -> null
      }
    }
  }
}
