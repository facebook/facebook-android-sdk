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

package com.facebook.share.internal

import com.facebook.FacebookException
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.internal.Utility.isContentUri
import com.facebook.internal.Utility.isFileUri
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.isWebUri
import com.facebook.internal.Validate.hasContentProvider
import com.facebook.share.model.ShareCameraEffectContent
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.ShareMedia
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.ShareMessengerActionButton
import com.facebook.share.model.ShareMessengerGenericTemplateContent
import com.facebook.share.model.ShareMessengerMediaTemplateContent
import com.facebook.share.model.ShareMessengerOpenGraphMusicTemplateContent
import com.facebook.share.model.ShareMessengerURLActionButton
import com.facebook.share.model.ShareOpenGraphAction
import com.facebook.share.model.ShareOpenGraphContent
import com.facebook.share.model.ShareOpenGraphObject
import com.facebook.share.model.ShareOpenGraphValueContainer
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideo
import com.facebook.share.model.ShareVideoContent
import java.util.Locale

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ShareContentValidation {
  private val webShareValidator: Validator = WebShareValidator()
  private val defaultValidator: Validator = Validator()
  private val apiValidator: Validator = ApiValidator()
  private val storyValidator: Validator = StoryShareValidator()

  @JvmStatic
  fun validateForMessage(content: ShareContent<*, *>?) {
    validate(content, defaultValidator)
  }

  @JvmStatic
  fun validateForNativeShare(content: ShareContent<*, *>?) {
    validate(content, defaultValidator)
  }

  @JvmStatic
  fun validateForWebShare(content: ShareContent<*, *>?) {
    validate(content, webShareValidator)
  }

  @JvmStatic
  fun validateForApiShare(content: ShareContent<*, *>?) {
    validate(content, apiValidator)
  }

  @JvmStatic
  fun validateForStoryShare(content: ShareContent<*, *>?) {
    validate(content, storyValidator)
  }

  @Throws(FacebookException::class)
  private fun validate(content: ShareContent<*, *>?, validator: Validator) {
    if (content == null) {
      throw FacebookException("Must provide non-null content to share")
    }
    when (content) {
      is ShareLinkContent -> {
        validator.validate(content)
      }
      is SharePhotoContent -> {
        validator.validate(content)
      }
      is ShareVideoContent -> {
        validator.validate(content)
      }
      is ShareOpenGraphContent -> {
        validator.validate(content)
      }
      is ShareMediaContent -> {
        validator.validate(content)
      }
      is ShareCameraEffectContent -> {
        validator.validate(content)
      }
      is ShareMessengerOpenGraphMusicTemplateContent -> {
        validator.validate(content)
      }
      is ShareMessengerMediaTemplateContent -> {
        validator.validate(content)
      }
      is ShareMessengerGenericTemplateContent -> {
        validator.validate(content)
      }
      is ShareStoryContent -> {
        validator.validate(content as ShareStoryContent?)
      }
    }
  }

  private fun validateStoryContent(storyContent: ShareStoryContent?, validator: Validator) {
    if (storyContent == null ||
        (storyContent.backgroundAsset == null && storyContent.stickerAsset == null)) {
      throw FacebookException(
          "Must pass the Facebook app a background asset, a sticker asset, or both")
    }
    if (storyContent.backgroundAsset != null) {
      validator.validate(storyContent.backgroundAsset)
    }
    if (storyContent.stickerAsset != null) {
      validator.validate(storyContent.stickerAsset)
    }
  }

  private fun validateLinkContent(linkContent: ShareLinkContent, validator: Validator) {
    val contentUrl = linkContent.contentUrl
    if (contentUrl != null && !isWebUri(contentUrl)) {
      throw FacebookException("Content Url must be an http:// or https:// url")
    }
  }

  private fun validatePhotoContent(photoContent: SharePhotoContent, validator: Validator) {
    val photos = photoContent.photos
    if (photos == null || photos.isEmpty()) {
      throw FacebookException("Must specify at least one Photo in SharePhotoContent.")
    }
    if (photos.size > ShareConstants.MAXIMUM_PHOTO_COUNT) {
      throw FacebookException(
          String.format(
              Locale.ROOT, "Cannot add more than %d photos.", ShareConstants.MAXIMUM_PHOTO_COUNT))
    }
    for (photo in photos) {
      validator.validate(photo)
    }
  }

  private fun validatePhoto(photo: SharePhoto?) {
    if (photo == null) {
      throw FacebookException("Cannot share a null SharePhoto")
    }
    val photoBitmap = photo.bitmap
    val photoUri = photo.imageUrl
    if (photoBitmap == null && photoUri == null) {
      throw FacebookException("SharePhoto does not have a Bitmap or ImageUrl specified")
    }
  }

  private fun validatePhotoForApi(photo: SharePhoto, validator: Validator) {
    validatePhoto(photo)
    val photoBitmap = photo.bitmap
    val photoUri = photo.imageUrl
    if (photoBitmap == null && isWebUri(photoUri) && !validator.isOpenGraphContent) {
      throw FacebookException(
          "Cannot set the ImageUrl of a SharePhoto to the Uri of an image on the " +
              "web when sharing SharePhotoContent")
    }
  }

  private fun validatePhotoForNativeDialog(photo: SharePhoto, validator: Validator) {
    validatePhotoForApi(photo, validator)
    if (photo.bitmap != null || !isWebUri(photo.imageUrl)) {
      hasContentProvider(getApplicationContext())
    }
  }

  private fun validatePhotoForWebDialog(photo: SharePhoto, validator: Validator) {
    validatePhoto(photo)
  }

  private fun validateVideoContent(videoContent: ShareVideoContent, validator: Validator) {
    validator.validate(videoContent.video)
    val previewPhoto = videoContent.previewPhoto
    if (previewPhoto != null) {
      validator.validate(previewPhoto)
    }
  }

  private fun validateVideo(video: ShareVideo?, validator: Validator) {
    if (video == null) {
      throw FacebookException("Cannot share a null ShareVideo")
    }
    val localUri =
        video.localUrl ?: throw FacebookException("ShareVideo does not have a LocalUrl specified")
    if (!isContentUri(localUri) && !isFileUri(localUri)) {
      throw FacebookException("ShareVideo must reference a video that is on the device")
    }
  }

  private fun validateMediaContent(mediaContent: ShareMediaContent, validator: Validator) {
    val media = mediaContent.media
    if (media == null || media.isEmpty()) {
      throw FacebookException("Must specify at least one medium in ShareMediaContent.")
    }
    if (media.size > ShareConstants.MAXIMUM_MEDIA_COUNT) {
      throw FacebookException(
          String.format(
              Locale.ROOT, "Cannot add more than %d media.", ShareConstants.MAXIMUM_MEDIA_COUNT))
    }
    for (medium in media) {
      validator.validate(medium)
    }
  }

  @JvmStatic
  fun validateMedium(medium: ShareMedia, validator: Validator) {
    when (medium) {
      is SharePhoto -> {
        validator.validate(medium)
      }
      is ShareVideo -> {
        validator.validate(medium as ShareVideo?)
      }
      else -> {
        throw FacebookException(
            String.format(Locale.ROOT, "Invalid media type: %s", medium.javaClass.simpleName))
      }
    }
  }

  private fun validateCameraEffectContent(cameraEffectContent: ShareCameraEffectContent) {
    val effectId = cameraEffectContent.effectId
    if (isNullOrEmpty(effectId)) {
      throw FacebookException("Must specify a non-empty effectId")
    }
  }

  private fun validateOpenGraphContent(
      openGraphContent: ShareOpenGraphContent,
      validator: Validator
  ) {
    validator.validate(openGraphContent.action)
    val previewPropertyName = openGraphContent.previewPropertyName
    if (isNullOrEmpty(previewPropertyName)) {
      throw FacebookException("Must specify a previewPropertyName.")
    }
    val action = openGraphContent.action
    if (action == null || action[previewPropertyName] == null) {
      val message =
          "Property \"$previewPropertyName\" was not found on the action. The name of the preview property must match the name of an action property."
      throw FacebookException(message)
    }
  }

  private fun validateOpenGraphAction(
      openGraphAction: ShareOpenGraphAction?,
      validator: Validator
  ) {
    if (openGraphAction == null) {
      throw FacebookException("Must specify a non-null ShareOpenGraphAction")
    }
    if (isNullOrEmpty(openGraphAction.actionType)) {
      throw FacebookException("ShareOpenGraphAction must have a non-empty actionType")
    }
    validator.validate(openGraphAction, false)
  }

  private fun validateOpenGraphObject(
      openGraphObject: ShareOpenGraphObject?,
      validator: Validator
  ) {
    if (openGraphObject == null) {
      throw FacebookException("Cannot share a null ShareOpenGraphObject")
    }
    validator.validate(openGraphObject, true)
  }

  private fun validateOpenGraphValueContainer(
      valueContainer: ShareOpenGraphValueContainer<*, *>,
      validator: Validator,
      requireNamespace: Boolean
  ) {
    val keySet = valueContainer.keySet()
    for (key in keySet) {
      validateOpenGraphKey(key, requireNamespace)
      val o = valueContainer[key]
      if (o is List<*>) {
        for (objectInList in o) {
          if (objectInList == null) {
            throw FacebookException(
                "Cannot put null objects in Lists in " +
                    "ShareOpenGraphObjects and ShareOpenGraphActions")
          }
          validateOpenGraphValueContainerObject(objectInList, validator)
        }
      } else {
        validateOpenGraphValueContainerObject(o, validator)
      }
    }
  }

  private fun validateMessengerOpenGraphMusicTemplate(
      content: ShareMessengerOpenGraphMusicTemplateContent
  ) {
    if (isNullOrEmpty(content.pageId)) {
      throw FacebookException(
          "Must specify Page Id for ShareMessengerOpenGraphMusicTemplateContent")
    }
    if (content.url == null) {
      throw FacebookException("Must specify url for ShareMessengerOpenGraphMusicTemplateContent")
    }
    validateShareMessengerActionButton(content.button)
  }

  private fun validateShareMessengerGenericTemplateContent(
      content: ShareMessengerGenericTemplateContent
  ) {
    if (isNullOrEmpty(content.pageId)) {
      throw FacebookException("Must specify Page Id for ShareMessengerGenericTemplateContent")
    }
    if (content.genericTemplateElement == null) {
      throw FacebookException("Must specify element for ShareMessengerGenericTemplateContent")
    }
    if (isNullOrEmpty(content.genericTemplateElement.title)) {
      throw FacebookException("Must specify title for ShareMessengerGenericTemplateElement")
    }
    validateShareMessengerActionButton(content.genericTemplateElement.button)
  }

  private fun validateShareMessengerMediaTemplateContent(
      content: ShareMessengerMediaTemplateContent
  ) {
    if (isNullOrEmpty(content.pageId)) {
      throw FacebookException("Must specify Page Id for ShareMessengerMediaTemplateContent")
    }
    if (content.mediaUrl == null && isNullOrEmpty(content.attachmentId)) {
      throw FacebookException(
          "Must specify either attachmentId or mediaURL for " +
              "ShareMessengerMediaTemplateContent")
    }
    validateShareMessengerActionButton(content.button)
  }

  private fun validateShareMessengerActionButton(button: ShareMessengerActionButton?) {
    if (button == null) {
      return
    }
    if (isNullOrEmpty(button.title)) {
      throw FacebookException("Must specify title for ShareMessengerActionButton")
    }
    if (button is ShareMessengerURLActionButton) {
      validateShareMessengerURLActionButton(button)
    }
  }

  private fun validateShareMessengerURLActionButton(button: ShareMessengerURLActionButton) {
    if (button.url == null) {
      throw FacebookException("Must specify url for ShareMessengerURLActionButton")
    }
  }

  private fun validateOpenGraphKey(key: String, requireNamespace: Boolean) {
    if (!requireNamespace) {
      return
    }
    val components = key.split(":").toTypedArray()
    if (components.size < 2) {
      throw FacebookException("Open Graph keys must be namespaced: %s", key)
    }
    for (component in components) {
      if (component.isEmpty()) {
        throw FacebookException("Invalid key found in Open Graph dictionary: %s", key)
      }
    }
  }

  private fun validateOpenGraphValueContainerObject(o: Any?, validator: Validator) {
    if (o is ShareOpenGraphObject) {
      validator.validate(o as ShareOpenGraphObject?)
    } else if (o is SharePhoto) {
      validator.validate(o)
    }
  }

  private class StoryShareValidator : Validator() {
    override fun validate(storyContent: ShareStoryContent?) {
      validateStoryContent(storyContent, this)
    }
  }

  private class WebShareValidator : Validator() {
    override fun validate(videoContent: ShareVideoContent) {
      throw FacebookException("Cannot share ShareVideoContent via web sharing dialogs")
    }

    override fun validate(mediaContent: ShareMediaContent) {
      throw FacebookException("Cannot share ShareMediaContent via web sharing dialogs")
    }

    override fun validate(photo: SharePhoto) {
      validatePhotoForWebDialog(photo, this)
    }
  }

  private class ApiValidator : Validator() {
    override fun validate(photo: SharePhoto) {
      validatePhotoForApi(photo, this)
    }

    override fun validate(videoContent: ShareVideoContent) {
      if (!isNullOrEmpty(videoContent.placeId)) {
        throw FacebookException("Cannot share video content with place IDs using the share api")
      }
      if (!isNullOrEmpty(videoContent.peopleIds)) {
        throw FacebookException("Cannot share video content with people IDs using the share api")
      }
      if (!isNullOrEmpty(videoContent.ref)) {
        throw FacebookException("Cannot share video content with referrer URL using the share api")
      }
    }

    override fun validate(mediaContent: ShareMediaContent) {
      throw FacebookException("Cannot share ShareMediaContent using the share api")
    }

    override fun validate(linkContent: ShareLinkContent) {
      if (!isNullOrEmpty(linkContent.quote)) {
        throw FacebookException("Cannot share link content with quote using the share api")
      }
    }
  }

  open class Validator {
    var isOpenGraphContent = false
      private set

    open fun validate(linkContent: ShareLinkContent) {
      validateLinkContent(linkContent, this)
    }

    open fun validate(photoContent: SharePhotoContent) {
      validatePhotoContent(photoContent, this)
    }

    open fun validate(videoContent: ShareVideoContent) {
      validateVideoContent(videoContent, this)
    }

    open fun validate(mediaContent: ShareMediaContent) {
      validateMediaContent(mediaContent, this)
    }

    open fun validate(cameraEffectContent: ShareCameraEffectContent) {
      validateCameraEffectContent(cameraEffectContent)
    }

    open fun validate(openGraphContent: ShareOpenGraphContent) {
      isOpenGraphContent = true
      validateOpenGraphContent(openGraphContent, this)
    }

    open fun validate(openGraphAction: ShareOpenGraphAction?) {
      validateOpenGraphAction(openGraphAction, this)
    }

    open fun validate(openGraphObject: ShareOpenGraphObject?) {
      validateOpenGraphObject(openGraphObject, this)
    }

    open fun validate(
        openGraphValueContainer: ShareOpenGraphValueContainer<*, *>,
        requireNamespace: Boolean
    ) {
      validateOpenGraphValueContainer(openGraphValueContainer, this, requireNamespace)
    }

    open fun validate(photo: SharePhoto) {
      validatePhotoForNativeDialog(photo, this)
    }

    open fun validate(video: ShareVideo?) {
      validateVideo(video, this)
    }

    open fun validate(medium: ShareMedia) {
      validateMedium(medium, this)
    }

    open fun validate(content: ShareMessengerOpenGraphMusicTemplateContent) {
      validateMessengerOpenGraphMusicTemplate(content)
    }

    open fun validate(content: ShareMessengerGenericTemplateContent) {
      validateShareMessengerGenericTemplateContent(content)
    }

    open fun validate(content: ShareMessengerMediaTemplateContent) {
      validateShareMessengerMediaTemplateContent(content)
    }

    open fun validate(storyContent: ShareStoryContent?) {
      validateStoryContent(storyContent, this)
    }
  }
}
