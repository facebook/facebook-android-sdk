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

import android.os.Bundle
import com.facebook.FacebookException
import com.facebook.internal.Utility
import com.facebook.share.internal.CameraEffectJSONUtility.convertToJSON
import com.facebook.share.model.ShareCameraEffectContent
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.ShareMediaContent
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.model.ShareStoryContent
import com.facebook.share.model.ShareVideoContent
import java.util.UUID
import org.json.JSONException

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object NativeDialogParameters {
  @JvmStatic
  fun create(
      callId: UUID,
      shareContent: ShareContent<*, *>,
      shouldFailOnDataError: Boolean
  ): Bundle? {
    var nativeParams: Bundle? = null
    when (shareContent) {
      is ShareLinkContent -> {
        nativeParams = create(shareContent, shouldFailOnDataError)
      }
      is SharePhotoContent -> {
        val photoUrls = ShareInternalUtility.getPhotoUrls(shareContent, callId) ?: listOf()
        nativeParams = create(shareContent, photoUrls, shouldFailOnDataError)
      }
      is ShareVideoContent -> {
        val videoUrl = ShareInternalUtility.getVideoUrl(shareContent, callId)
        nativeParams = create(shareContent, videoUrl, shouldFailOnDataError)
      }
      is ShareMediaContent -> {
        val mediaInfos = ShareInternalUtility.getMediaInfos(shareContent, callId) ?: listOf()
        nativeParams = create(shareContent, mediaInfos, shouldFailOnDataError)
      }
      is ShareCameraEffectContent -> {
        // Put Bitmaps behind content uris.
        val attachmentUrlsBundle = ShareInternalUtility.getTextureUrlBundle(shareContent, callId)
        nativeParams = create(shareContent, attachmentUrlsBundle, shouldFailOnDataError)
      }
      is ShareStoryContent -> {
        val mediaInfo = ShareInternalUtility.getBackgroundAssetMediaInfo(shareContent, callId)
        val stickerInfo = ShareInternalUtility.getStickerUrl(shareContent, callId)
        nativeParams = create(shareContent, mediaInfo, stickerInfo, shouldFailOnDataError)
      }
    }
    return nativeParams
  }

  private fun create(
      cameraEffectContent: ShareCameraEffectContent,
      attachmentUrlsBundle: Bundle?,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(cameraEffectContent, dataErrorsFatal)
    Utility.putNonEmptyString(params, ShareConstants.EFFECT_ID, cameraEffectContent.effectId)
    if (attachmentUrlsBundle != null) {
      params.putBundle(ShareConstants.EFFECT_TEXTURES, attachmentUrlsBundle)
    }
    try {
      val argsJSON = convertToJSON(cameraEffectContent.arguments)
      if (argsJSON != null) {
        Utility.putNonEmptyString(params, ShareConstants.EFFECT_ARGS, argsJSON.toString())
      }
    } catch (e: JSONException) {
      throw FacebookException(
          "Unable to create a JSON Object from the provided CameraEffectArguments: ${e.message}")
    }
    return params
  }

  private fun create(linkContent: ShareLinkContent, dataErrorsFatal: Boolean): Bundle {
    val params = createBaseParameters(linkContent, dataErrorsFatal)
    Utility.putNonEmptyString(params, ShareConstants.QUOTE, linkContent.quote)
    Utility.putUri(params, ShareConstants.MESSENGER_URL, linkContent.contentUrl)
    Utility.putUri(params, ShareConstants.TARGET_DISPLAY, linkContent.contentUrl)
    return params
  }

  private fun create(
      photoContent: SharePhotoContent,
      imageUrls: List<String>,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(photoContent, dataErrorsFatal)
    params.putStringArrayList(ShareConstants.PHOTOS, ArrayList(imageUrls))
    return params
  }

  private fun create(
      videoContent: ShareVideoContent,
      videoUrl: String?,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(videoContent, dataErrorsFatal)
    Utility.putNonEmptyString(params, ShareConstants.TITLE, videoContent.contentTitle)
    Utility.putNonEmptyString(params, ShareConstants.DESCRIPTION, videoContent.contentDescription)
    Utility.putNonEmptyString(params, ShareConstants.VIDEO_URL, videoUrl)
    return params
  }

  private fun create(
      mediaContent: ShareMediaContent,
      mediaInfos: List<Bundle>,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(mediaContent, dataErrorsFatal)
    params.putParcelableArrayList(ShareConstants.MEDIA, ArrayList(mediaInfos))
    return params
  }

  private fun create(
      storyContent: ShareStoryContent,
      mediaInfo: Bundle?,
      stickerInfo: Bundle?,
      dataErrorsFatal: Boolean
  ): Bundle {
    val params = createBaseParameters(storyContent, dataErrorsFatal)
    if (mediaInfo != null) {
      params.putParcelable(ShareConstants.STORY_BG_ASSET, mediaInfo)
    }
    if (stickerInfo != null) {
      params.putParcelable(ShareConstants.STORY_INTERACTIVE_ASSET_URI, stickerInfo)
    }
    val backgroundColorList = storyContent.backgroundColorList
    if (!backgroundColorList.isNullOrEmpty()) {
      params.putStringArrayList(
          ShareConstants.STORY_INTERACTIVE_COLOR_LIST, ArrayList(backgroundColorList))
    }
    Utility.putNonEmptyString(
        params, ShareConstants.STORY_DEEP_LINK_URL, storyContent.attributionLink)
    return params
  }

  private fun createBaseParameters(content: ShareContent<*, *>, dataErrorsFatal: Boolean): Bundle {
    val params = Bundle()
    Utility.putUri(params, ShareConstants.CONTENT_URL, content.contentUrl)
    Utility.putNonEmptyString(params, ShareConstants.PLACE_ID, content.placeId)
    Utility.putNonEmptyString(params, ShareConstants.PAGE_ID, content.pageId)
    Utility.putNonEmptyString(params, ShareConstants.REF, content.ref)
    Utility.putNonEmptyString(params, ShareConstants.REF, content.ref)
    params.putBoolean(ShareConstants.DATA_FAILURES_FATAL, dataErrorsFatal)
    val peopleIds = content.peopleIds
    if (!peopleIds.isNullOrEmpty()) {
      params.putStringArrayList(ShareConstants.PEOPLE_IDS, ArrayList(peopleIds))
    }
    Utility.putNonEmptyString(params, ShareConstants.HASHTAG, content.shareHashtag?.hashtag)
    return params
  }
}
