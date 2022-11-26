/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.annotation.SuppressLint
import android.os.Bundle
import com.facebook.internal.Utility.getUriString
import com.facebook.internal.Utility.putCommaSeparatedStringList
import com.facebook.internal.Utility.putNonEmptyString
import com.facebook.internal.Utility.putUri
import com.facebook.share.model.AppGroupCreationContent
import com.facebook.share.model.GameRequestContent
import com.facebook.share.model.ShareContent
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhotoContent
import java.util.Locale

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object WebDialogParameters {
  @JvmStatic
  fun create(appGroupCreationContent: AppGroupCreationContent): Bundle {
    val webParams = Bundle()
    putNonEmptyString(webParams, ShareConstants.WEB_DIALOG_PARAM_NAME, appGroupCreationContent.name)
    putNonEmptyString(
        webParams, ShareConstants.WEB_DIALOG_PARAM_DESCRIPTION, appGroupCreationContent.description)
    putNonEmptyString(
        webParams,
        ShareConstants.WEB_DIALOG_PARAM_PRIVACY,
        appGroupCreationContent.appGroupPrivacy?.toString()?.lowercase(Locale.ENGLISH))
    return webParams
  }

  @JvmStatic
  fun create(gameRequestContent: GameRequestContent): Bundle {
    val webParams = Bundle()
    putNonEmptyString(
        webParams, ShareConstants.WEB_DIALOG_PARAM_MESSAGE, gameRequestContent.message)
    putCommaSeparatedStringList(
        webParams, ShareConstants.WEB_DIALOG_PARAM_TO, gameRequestContent.recipients)
    putNonEmptyString(webParams, ShareConstants.WEB_DIALOG_PARAM_TITLE, gameRequestContent.title)
    putNonEmptyString(webParams, ShareConstants.WEB_DIALOG_PARAM_DATA, gameRequestContent.data)
    putNonEmptyString(
        webParams,
        ShareConstants.WEB_DIALOG_PARAM_ACTION_TYPE,
        gameRequestContent.actionType?.toString()?.lowercase(Locale.ENGLISH))
    putNonEmptyString(
        webParams, ShareConstants.WEB_DIALOG_PARAM_OBJECT_ID, gameRequestContent.objectId)
    putNonEmptyString(
        webParams,
        ShareConstants.WEB_DIALOG_PARAM_FILTERS,
        gameRequestContent.filters?.toString()?.lowercase(Locale.ENGLISH))
    putCommaSeparatedStringList(
        webParams, ShareConstants.WEB_DIALOG_PARAM_SUGGESTIONS, gameRequestContent.suggestions)
    return webParams
  }

  @JvmStatic
  fun create(shareLinkContent: ShareLinkContent): Bundle {
    val params = createBaseParameters(shareLinkContent)
    putUri(params, ShareConstants.WEB_DIALOG_PARAM_HREF, shareLinkContent.contentUrl)
    putNonEmptyString(params, ShareConstants.WEB_DIALOG_PARAM_QUOTE, shareLinkContent.quote)
    return params
  }

  @JvmStatic
  fun create(sharePhotoContent: SharePhotoContent): Bundle {
    val params = createBaseParameters(sharePhotoContent)
    val photos = sharePhotoContent.photos ?: emptyList()
    val urls = photos.map { it.imageUrl.toString() }.toTypedArray()
    params.putStringArray(ShareConstants.WEB_DIALOG_PARAM_MEDIA, urls)
    return params
  }

  @JvmStatic
  fun createBaseParameters(shareContent: ShareContent<*, *>): Bundle {
    val params = Bundle()
    putNonEmptyString(
        params, ShareConstants.WEB_DIALOG_PARAM_HASHTAG, shareContent.shareHashtag?.hashtag)
    return params
  }

  @JvmStatic
  @SuppressLint("DeprecatedMethod")
  fun createForFeed(shareLinkContent: ShareLinkContent): Bundle {
    val webParams = Bundle()
    putNonEmptyString(
        webParams, ShareConstants.WEB_DIALOG_PARAM_LINK, getUriString(shareLinkContent.contentUrl))
    putNonEmptyString(webParams, ShareConstants.WEB_DIALOG_PARAM_QUOTE, shareLinkContent.quote)
    putNonEmptyString(
        webParams, ShareConstants.WEB_DIALOG_PARAM_HASHTAG, shareLinkContent.shareHashtag?.hashtag)
    return webParams
  }

  @JvmStatic
  fun createForFeed(shareFeedContent: ShareFeedContent): Bundle {
    val webParams = Bundle()
    putNonEmptyString(webParams, ShareConstants.FEED_TO_PARAM, shareFeedContent.toId)
    putNonEmptyString(webParams, ShareConstants.FEED_LINK_PARAM, shareFeedContent.link)
    putNonEmptyString(webParams, ShareConstants.FEED_PICTURE_PARAM, shareFeedContent.picture)
    putNonEmptyString(webParams, ShareConstants.FEED_SOURCE_PARAM, shareFeedContent.mediaSource)
    putNonEmptyString(webParams, ShareConstants.FEED_NAME_PARAM, shareFeedContent.linkName)
    putNonEmptyString(webParams, ShareConstants.FEED_CAPTION_PARAM, shareFeedContent.linkCaption)
    putNonEmptyString(
        webParams, ShareConstants.FEED_DESCRIPTION_PARAM, shareFeedContent.linkDescription)
    return webParams
  }
}
