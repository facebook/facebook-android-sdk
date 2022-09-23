/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Pair
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookGraphResponseException
import com.facebook.FacebookOperationCanceledException
import com.facebook.FacebookSdk.getApplicationContext
import com.facebook.GraphRequest
import com.facebook.GraphResponse
import com.facebook.HttpMethod
import com.facebook.appevents.InternalAppEventsLogger
import com.facebook.internal.AnalyticsEvents
import com.facebook.internal.AppCall
import com.facebook.internal.AppCall.Companion.finishPendingCall
import com.facebook.internal.CallbackManagerImpl
import com.facebook.internal.CallbackManagerImpl.Companion.registerStaticCallback
import com.facebook.internal.NativeAppCallAttachmentStore
import com.facebook.internal.NativeAppCallAttachmentStore.addAttachments
import com.facebook.internal.NativeAppCallAttachmentStore.cleanupAttachmentsForCall
import com.facebook.internal.NativeAppCallAttachmentStore.createAttachment
import com.facebook.internal.NativeProtocol
import com.facebook.internal.NativeProtocol.getCallIdFromIntent
import com.facebook.internal.NativeProtocol.getErrorDataFromResultIntent
import com.facebook.internal.NativeProtocol.getExceptionFromErrorData
import com.facebook.internal.NativeProtocol.getSuccessResultsFromIntent
import com.facebook.internal.Utility.isContentUri
import com.facebook.internal.Utility.isFileUri
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.putNonEmptyString
import com.facebook.share.Sharer
import com.facebook.share.model.CameraEffectTextures
import com.facebook.share.model.ShareCameraEffectContent
import com.facebook.share.model.ShareMedia
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideo
import com.facebook.share.model.ShareVideoContent
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.util.UUID
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ShareInternalUtility {
  const val MY_STAGING_RESOURCES = "me/staging_resources"
  // Parameter names/values
  const val STAGING_PARAM = "file"

  @JvmStatic
  fun invokeCallbackWithException(
      callback: FacebookCallback<Sharer.Result>?,
      exception: Exception
  ) {
    if (exception is FacebookException) {
      invokeOnErrorCallback(callback, exception)
      return
    }
    invokeCallbackWithError(
        callback, "Error preparing share content: " + exception.localizedMessage)
  }

  @JvmStatic
  fun invokeCallbackWithError(callback: FacebookCallback<Sharer.Result>?, error: String?) {
    invokeOnErrorCallback(callback, error)
  }

  @JvmStatic
  fun invokeCallbackWithResults(
      callback: FacebookCallback<Sharer.Result>?,
      postId: String?,
      graphResponse: GraphResponse
  ) {
    val requestError = graphResponse.error
    if (requestError != null) {
      var errorMessage = requestError.errorMessage
      if (isNullOrEmpty(errorMessage)) {
        errorMessage = "Unexpected error sharing."
      }
      invokeOnErrorCallback(callback, graphResponse, errorMessage)
    } else {
      invokeOnSuccessCallback(callback, postId)
    }
  }

  /**
   * Returns the gesture with which the user completed the native dialog. This is only returned if
   * the user has previously authorized the calling app with basic permissions.
   *
   * @param result the bundle passed back to onActivityResult
   * @return "post" or "cancel" as the completion gesture
   */
  @JvmStatic
  fun getNativeDialogCompletionGesture(result: Bundle): String? {
    return if (result.containsKey(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY)) {
      result.getString(NativeProtocol.RESULT_ARGS_DIALOG_COMPLETION_GESTURE_KEY)
    } else result.getString(NativeProtocol.EXTRA_DIALOG_COMPLETION_GESTURE_KEY)
  }

  /**
   * Returns the id of the published post. This is only returned if the user has previously given
   * the app publish permissions.
   *
   * @param result the bundle passed back to onActivityResult
   * @return the id of the published post
   */
  @JvmStatic
  fun getShareDialogPostId(result: Bundle): String? {
    if (result.containsKey(ShareConstants.RESULT_POST_ID)) {
      return result.getString(ShareConstants.RESULT_POST_ID)
    }
    return if (result.containsKey(ShareConstants.EXTRA_RESULT_POST_ID)) {
      result.getString(ShareConstants.EXTRA_RESULT_POST_ID)
    } else result.getString(ShareConstants.WEB_DIALOG_RESULT_PARAM_POST_ID)
  }

  @JvmStatic
  fun handleActivityResult(
      requestCode: Int,
      resultCode: Int,
      data: Intent?,
      resultProcessor: ResultProcessor?
  ): Boolean {
    val appCall = getAppCallFromActivityResult(requestCode, resultCode, data) ?: return false
    cleanupAttachmentsForCall(appCall.callId)
    if (resultProcessor == null) {
      return true
    }
    val exception =
        if (data != null) getExceptionFromErrorData(getErrorDataFromResultIntent(data)) else null
    if (exception != null) {
      if (exception is FacebookOperationCanceledException) {
        resultProcessor.onCancel(appCall)
      } else {
        resultProcessor.onError(appCall, exception)
      }
    } else {
      // If here, we did not find an error in the result.
      val results = if (data != null) getSuccessResultsFromIntent(data) else null
      resultProcessor.onSuccess(appCall, results)
    }
    return true
  }

  // Custom handling for Share so that we can log results
  @JvmStatic
  fun getShareResultProcessor(callback: FacebookCallback<Sharer.Result>?): ResultProcessor {
    return object : ResultProcessor(callback) {
      override fun onSuccess(appCall: AppCall, results: Bundle?) {
        if (results != null) {
          val gesture = getNativeDialogCompletionGesture(results)
          if (gesture == null || "post".equals(gesture, ignoreCase = true)) {
            invokeOnSuccessCallback(callback, getShareDialogPostId(results))
          } else if ("cancel".equals(gesture, ignoreCase = true)) {
            invokeOnCancelCallback(callback)
          } else {
            invokeOnErrorCallback(callback, FacebookException(NativeProtocol.ERROR_UNKNOWN_ERROR))
          }
        }
      }

      override fun onCancel(appCall: AppCall) {
        invokeOnCancelCallback(callback)
      }

      override fun onError(appCall: AppCall, error: FacebookException) {
        invokeOnErrorCallback(callback, error)
      }
    }
  }

  private fun getAppCallFromActivityResult(
      requestCode: Int,
      resultCode: Int,
      data: Intent?
  ): AppCall? {
    val callId = getCallIdFromIntent(data) ?: return null
    return finishPendingCall(callId, requestCode)
  }

  @JvmStatic
  fun registerStaticShareCallback(requestCode: Int) {
    registerStaticCallback(requestCode) { resultCode, data ->
      handleActivityResult(requestCode, resultCode, data, getShareResultProcessor(null))
    }
  }

  @JvmStatic
  fun registerSharerCallback(
      requestCode: Int,
      callbackManager: CallbackManager?,
      callback: FacebookCallback<Sharer.Result>?
  ) {
    if (callbackManager !is CallbackManagerImpl) {
      throw FacebookException("Unexpected CallbackManager, " + "please use the provided Factory.")
    }
    callbackManager.registerCallback(requestCode) { resultCode, data ->
      handleActivityResult(requestCode, resultCode, data, getShareResultProcessor(callback))
    }
  }

  @JvmStatic
  fun getPhotoUrls(photoContent: SharePhotoContent?, appCallId: UUID): List<String>? {
    val photos: List<SharePhoto> = photoContent?.photos ?: return null
    val attachments = photos.mapNotNull { getAttachment(appCallId, it) }
    val attachmentUrls = attachments.map { it.attachmentUrl }
    addAttachments(attachments)
    return attachmentUrls
  }

  @JvmStatic
  fun getVideoUrl(videoContent: ShareVideoContent?, appCallId: UUID): String? {
    val videoLocalUrl = videoContent?.video?.localUrl ?: return null
    val attachment = createAttachment(appCallId, videoLocalUrl)
    addAttachments(listOf(attachment))
    return attachment.attachmentUrl
  }

  @JvmStatic
  fun getMediaInfos(mediaContent: ShareMediaContent?, appCallId: UUID): List<Bundle>? {
    val media = mediaContent?.media ?: return null
    val attachments: MutableList<NativeAppCallAttachmentStore.Attachment> = ArrayList()
    val mediaInfos =
        media.mapNotNull {
          val attachment = getAttachment(appCallId, it) ?: return@mapNotNull null
          attachments.add(attachment)
          val mediaInfo = Bundle()
          mediaInfo.putString(ShareConstants.MEDIA_TYPE, it.mediaType.name)
          mediaInfo.putString(ShareConstants.MEDIA_URI, attachment.attachmentUrl)
          return@mapNotNull mediaInfo
        }
    addAttachments(attachments)
    return mediaInfos
  }

  @JvmStatic
  fun getTextureUrlBundle(
      cameraEffectContent: ShareCameraEffectContent?,
      appCallId: UUID
  ): Bundle? {
    val textures: CameraEffectTextures = cameraEffectContent?.textures ?: return null
    val attachmentUrlsBundle = Bundle()
    val attachments: MutableList<NativeAppCallAttachmentStore.Attachment> = ArrayList()
    for (key in textures.keySet()) {
      val attachment =
          getAttachment(appCallId, textures.getTextureUri(key), textures.getTextureBitmap(key))
      if (attachment != null) {
        attachments.add(attachment)
        attachmentUrlsBundle.putString(key, attachment.attachmentUrl)
      }
    }
    addAttachments(attachments)
    return attachmentUrlsBundle
  }

  @Throws(JSONException::class)
  @JvmStatic
  fun removeNamespacesFromOGJsonArray(jsonArray: JSONArray, requireNamespace: Boolean): JSONArray {
    val newArray = JSONArray()
    for (i in 0 until jsonArray.length()) {
      var value = jsonArray[i]
      if (value is JSONArray) {
        value = removeNamespacesFromOGJsonArray(value, requireNamespace)
      } else if (value is JSONObject) {
        value = removeNamespacesFromOGJsonObject(value, requireNamespace)
      }
      newArray.put(value)
    }
    return newArray
  }

  @JvmStatic
  fun removeNamespacesFromOGJsonObject(
      jsonObject: JSONObject?,
      requireNamespace: Boolean
  ): JSONObject? {
    return if (jsonObject == null) {
      null
    } else
        try {
          val newJsonObject = JSONObject()
          val data = JSONObject()
          val names = jsonObject.names() ?: return null
          for (i in 0 until names.length()) {
            val key = names.getString(i)
            var value: Any?
            value = jsonObject[key]
            if (value is JSONObject) {
              value = removeNamespacesFromOGJsonObject(value as JSONObject?, true)
            } else if (value is JSONArray) {
              value = removeNamespacesFromOGJsonArray(value, true)
            }
            val fieldNameAndNamespace = getFieldNameAndNamespaceFromFullName(key)
            val namespace = fieldNameAndNamespace.first
            val fieldName = fieldNameAndNamespace.second
            if (requireNamespace) {
              if (namespace != null && namespace == "fbsdk") {
                newJsonObject.put(key, value)
              } else if (namespace == null || namespace == "og") {
                newJsonObject.put(fieldName, value)
              } else {
                data.put(fieldName, value)
              }
            } else if (namespace != null && namespace == "fb") {
              newJsonObject.put(key, value)
            } else {
              newJsonObject.put(fieldName, value)
            }
          }
          if (data.length() > 0) {
            newJsonObject.put("data", data)
          }
          newJsonObject
        } catch (e: JSONException) {
          throw FacebookException("Failed to create json object from share content")
        }
  }

  @JvmStatic
  fun getFieldNameAndNamespaceFromFullName(fullName: String): Pair<String?, String> {
    var namespace: String? = null
    val fieldName: String
    val index = fullName.indexOf(':')
    if (index != -1 && fullName.length > index + 1) {
      namespace = fullName.substring(0, index)
      fieldName = fullName.substring(index + 1)
    } else {
      fieldName = fullName
    }
    return Pair(namespace, fieldName)
  }

  private fun getAttachment(
      callId: UUID,
      medium: ShareMedia<*, *>
  ): NativeAppCallAttachmentStore.Attachment? {
    var bitmap: Bitmap? = null
    var uri: Uri? = null
    if (medium is SharePhoto) {
      bitmap = medium.bitmap
      uri = medium.imageUrl
    } else if (medium is ShareVideo) {
      uri = medium.localUrl
    }
    return getAttachment(callId, uri, bitmap)
  }

  private fun getAttachment(
      callId: UUID,
      uri: Uri?,
      bitmap: Bitmap?
  ): NativeAppCallAttachmentStore.Attachment? {
    var attachment: NativeAppCallAttachmentStore.Attachment? = null
    if (bitmap != null) {
      attachment = createAttachment(callId, bitmap)
    } else if (uri != null) {
      attachment = createAttachment(callId, uri)
    }
    return attachment
  }

  @JvmStatic
  fun invokeOnCancelCallback(callback: FacebookCallback<Sharer.Result>?) {
    logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_CANCELLED, null)
    callback?.onCancel()
  }

  @JvmStatic
  fun invokeOnSuccessCallback(callback: FacebookCallback<Sharer.Result>?, postId: String?) {
    logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_SUCCEEDED, null)
    callback?.onSuccess(Sharer.Result(postId))
  }

  @JvmStatic
  fun invokeOnErrorCallback(
      callback: FacebookCallback<Sharer.Result>?,
      response: GraphResponse?,
      message: String?
  ) {
    logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message)
    callback?.onError(FacebookGraphResponseException(response, message))
  }

  @JvmStatic
  fun invokeOnErrorCallback(callback: FacebookCallback<Sharer.Result>?, message: String?) {
    logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, message)
    callback?.onError(FacebookException(message))
  }

  @JvmStatic
  fun invokeOnErrorCallback(callback: FacebookCallback<Sharer.Result>?, ex: FacebookException) {
    logShareResult(AnalyticsEvents.PARAMETER_SHARE_OUTCOME_ERROR, ex.message)
    callback?.onError(ex)
  }

  private fun logShareResult(shareOutcome: String, errorMessage: String?) {
    val context = getApplicationContext()
    val logger = InternalAppEventsLogger(context)
    val parameters = Bundle()
    parameters.putString(AnalyticsEvents.PARAMETER_SHARE_OUTCOME, shareOutcome)
    if (errorMessage != null) {
      parameters.putString(AnalyticsEvents.PARAMETER_SHARE_ERROR_MESSAGE, errorMessage)
    }
    logger.logEventImplicitly(AnalyticsEvents.EVENT_SHARE_RESULT, parameters)
  }

  /**
   * Creates a new Request configured to upload an image to create a staging resource. Staging
   * resources allow you to post binary data such as images, in preparation for a post of an Open
   * Graph object or action which references the image. The URI returned when uploading a staging
   * resource may be passed as the image property for an Open Graph object or action.
   *
   * @param accessToken the access token to use, or null
   * @param image the image to upload
   * @param callback a callback that will be called when the request is completed to handle success
   * or error conditions
   * @return a Request that is ready to execute
   */
  @JvmStatic
  fun newUploadStagingResourceWithImageRequest(
      accessToken: AccessToken?,
      image: Bitmap?,
      callback: GraphRequest.Callback?
  ): GraphRequest {
    val parameters = Bundle(1)
    parameters.putParcelable(STAGING_PARAM, image)
    return GraphRequest(accessToken, MY_STAGING_RESOURCES, parameters, HttpMethod.POST, callback)
  }

  /**
   * Creates a new Request configured to upload an image to create a staging resource. Staging
   * resources allow you to post binary data such as images, in preparation for a post of an Open
   * Graph object or action which references the image. The URI returned when uploading a staging
   * resource may be passed as the image property for an Open Graph object or action.
   *
   * @param accessToken the access token to use, or null
   * @param file the file containing the image to upload
   * @param callback a callback that will be called when the request is completed to handle success
   * or error conditions
   * @return a Request that is ready to execute
   * @throws FileNotFoundException
   */
  @Throws(FileNotFoundException::class)
  @JvmStatic
  fun newUploadStagingResourceWithImageRequest(
      accessToken: AccessToken?,
      file: File?,
      callback: GraphRequest.Callback?
  ): GraphRequest {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val resourceWithMimeType = GraphRequest.ParcelableResourceWithMimeType(descriptor, "image/png")
    val parameters = Bundle(1)
    parameters.putParcelable(STAGING_PARAM, resourceWithMimeType)
    return GraphRequest(accessToken, MY_STAGING_RESOURCES, parameters, HttpMethod.POST, callback)
  }

  /**
   * Creates a new Request configured to upload an image to create a staging resource. Staging
   * resources allow you to post binary data such as images, in preparation for a post of an Open
   * Graph object or action which references the image. The URI returned when uploading a staging
   * resource may be passed as the image property for an Open Graph object or action.
   *
   * @param accessToken the access token to use, or null
   * @param imageUri the file:// or content:// Uri pointing to the image to upload
   * @param callback a callback that will be called when the request is completed to handle success
   * or error conditions
   * @return a Request that is ready to execute
   * @throws FileNotFoundException
   */
  @Throws(FileNotFoundException::class)
  @JvmStatic
  fun newUploadStagingResourceWithImageRequest(
      accessToken: AccessToken?,
      imageUri: Uri,
      callback: GraphRequest.Callback?
  ): GraphRequest {
    val imagePath = imageUri.path
    if (isFileUri(imageUri) && imagePath != null) {
      return newUploadStagingResourceWithImageRequest(accessToken, File(imagePath), callback)
    } else if (!isContentUri(imageUri)) {
      throw FacebookException("The image Uri must be either a file:// or content:// Uri")
    }
    val resourceWithMimeType = GraphRequest.ParcelableResourceWithMimeType(imageUri, "image/png")
    val parameters = Bundle(1)
    parameters.putParcelable(STAGING_PARAM, resourceWithMimeType)
    return GraphRequest(accessToken, MY_STAGING_RESOURCES, parameters, HttpMethod.POST, callback)
  }

  @JvmStatic
  fun getStickerUrl(storyContent: ShareStoryContent?, appCallId: UUID): Bundle? {
    if (storyContent == null || storyContent.stickerAsset == null) {
      return null
    }
    val photos: MutableList<SharePhoto> = ArrayList()
    photos.add(storyContent.stickerAsset)
    val attachment = getAttachment(appCallId, storyContent.stickerAsset) ?: return null
    val stickerInfo = Bundle()
    stickerInfo.putString(ShareConstants.MEDIA_URI, attachment.attachmentUrl)
    val extension = getUriExtension(attachment.originalUri)
    if (extension != null) {
      putNonEmptyString(stickerInfo, ShareConstants.MEDIA_EXTENSION, extension)
    }
    addAttachments(listOf(attachment))
    return stickerInfo
  }

  @JvmStatic
  fun getBackgroundAssetMediaInfo(storyContent: ShareStoryContent?, appCallId: UUID): Bundle? {
    if (storyContent == null || storyContent.backgroundAsset == null) {
      return null
    }
    val media = storyContent.backgroundAsset
    val attachment = getAttachment(appCallId, media) ?: return null
    val mediaInfo = Bundle()
    mediaInfo.putString(ShareConstants.MEDIA_TYPE, media.mediaType.name)
    mediaInfo.putString(ShareConstants.MEDIA_URI, attachment.attachmentUrl)
    val extension = getUriExtension(attachment.originalUri)
    if (extension != null) {
      putNonEmptyString(mediaInfo, ShareConstants.MEDIA_EXTENSION, extension)
    }
    addAttachments(listOf(attachment))
    return mediaInfo
  }

  @JvmStatic
  fun getUriExtension(uri: Uri?): String? {
    if (uri == null) {
      return null
    }
    val path = uri.toString()
    val idx = path.lastIndexOf('.')
    return if (idx == -1) {
      null
    } else path.substring(idx)
  }
}
