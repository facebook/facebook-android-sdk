/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.share.internal

import android.net.Uri
import com.facebook.FacebookTestCase
import com.facebook.share.model.AppGroupCreationContent
import com.facebook.share.model.GameRequestContent
import com.facebook.share.model.ShareHashtag
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WebDialogParametersTest : FacebookTestCase() {

  @Test
  fun `test create with AppGroupCreationContent`() {
    val appGroupCreationContent =
        AppGroupCreationContent.Builder()
            .setName("name")
            .setDescription("description")
            .setAppGroupPrivacy(AppGroupCreationContent.AppGroupPrivacy.Closed)
            .build()

    val bundle = WebDialogParameters.create(appGroupCreationContent)
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_NAME)).isEqualTo("name")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_DESCRIPTION))
        .isEqualTo("description")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_PRIVACY)).isEqualTo("closed")
  }

  @Test
  fun `test create with GameRequestContent`() {
    val gameRequestContent =
        GameRequestContent.Builder()
            .setMessage("message")
            .setRecipients(listOf("recipient1", "recipient2"))
            .setTitle("title")
            .setData("data")
            .setActionType(GameRequestContent.ActionType.INVITE)
            .setObjectId("objectId")
            .setFilters(GameRequestContent.Filters.APP_USERS)
            .setSuggestions(listOf("suggestion1", "suggestion2"))
            .build()

    val bundle = WebDialogParameters.create(gameRequestContent)
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_MESSAGE)).isEqualTo("message")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_TO))
        .isEqualTo("recipient1,recipient2")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_TITLE)).isEqualTo("title")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_DATA)).isEqualTo("data")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_ACTION_TYPE)).isEqualTo("invite")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_OBJECT_ID)).isEqualTo("objectId")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_FILTERS)).isEqualTo("app_users")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_SUGGESTIONS))
        .isEqualTo("suggestion1,suggestion2")
  }

  @Test
  fun `test create with ShareLinkContent`() {
    val shareLinkContent =
        ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("www.facebook.com/content_url"))
            .setQuote("quote")
            .setShareHashtag(ShareHashtag.Builder().setHashtag("#hashtag").build())
            .build()

    val bundle = WebDialogParameters.create(shareLinkContent)
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_LINK)).isNull()
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_HREF))
        .isEqualTo("www.facebook.com/content_url")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_QUOTE)).isEqualTo("quote")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_HASHTAG)).isEqualTo("#hashtag")
  }

  @Test
  fun `test create with SharePhotoContent`() {
    val photoUrlStrings = arrayListOf("www.facebook.com/photo1", "www.facebook.com/photo2")
    val sharePhotos =
        photoUrlStrings.map { SharePhoto.Builder().setImageUrl(Uri.parse(it)).build() }

    val sharePhotoContent =
        SharePhotoContent.Builder()
            .setPhotos(sharePhotos)
            .setShareHashtag(ShareHashtag.Builder().setHashtag("#hashtag").build())
            .build()

    val bundle = WebDialogParameters.create(sharePhotoContent)
    assertThat(bundle.getStringArray(ShareConstants.WEB_DIALOG_PARAM_MEDIA))
        .isEqualTo(photoUrlStrings.toArray())
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_HASHTAG)).isEqualTo("#hashtag")
  }

  @Test
  fun `test createBaseParameters only sets the hashtag`() {
    val shareLinkContent =
        ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("www.facebook.com/content_url"))
            .setQuote("quote")
            .setShareHashtag(ShareHashtag.Builder().setHashtag("#hashtag").build())
            .build()

    val bundle = WebDialogParameters.createBaseParameters(shareLinkContent)
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_LINK)).isNull()
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_QUOTE)).isNull()
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_HASHTAG)).isEqualTo("#hashtag")
  }

  @Test
  fun `test createForFeed with ShareLinkContent`() {
    val shareLinkContent =
        ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("www.facebook.com/content_url"))
            .setQuote("quote")
            .setShareHashtag(ShareHashtag.Builder().setHashtag("#hashtag").build())
            .build()

    val bundle = WebDialogParameters.createForFeed(shareLinkContent)
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_NAME))
        .isNull() // setContentTitle is deprecated and does nothing
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_DESCRIPTION))
        .isNull() // setContentDescription is deprecated and does nothing
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_LINK))
        .isEqualTo("www.facebook.com/content_url")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_PICTURE))
        .isNull() // setImageUrl does nothing
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_QUOTE)).isEqualTo("quote")
    assertThat(bundle.getString(ShareConstants.WEB_DIALOG_PARAM_HASHTAG)).isEqualTo("#hashtag")
  }

  @Test
  fun `test createForFeed with ShareFeedContent`() {
    val shareFeedContent =
        ShareFeedContent.Builder()
            .setToId("toId")
            .setLink("link")
            .setPicture("picture")
            .setMediaSource("mediaSource")
            .setLinkName("linkName")
            .setLinkCaption("linkCaption")
            .setLinkDescription("linkDescription")
            .build()

    val bundle = WebDialogParameters.createForFeed(shareFeedContent)
    assertThat(bundle.getString(ShareConstants.FEED_TO_PARAM)).isEqualTo("toId")
    assertThat(bundle.getString(ShareConstants.FEED_LINK_PARAM)).isEqualTo("link")
    assertThat(bundle.getString(ShareConstants.FEED_PICTURE_PARAM)).isEqualTo("picture")
    assertThat(bundle.getString(ShareConstants.FEED_SOURCE_PARAM)).isEqualTo("mediaSource")
    assertThat(bundle.getString(ShareConstants.FEED_NAME_PARAM)).isEqualTo("linkName")
    assertThat(bundle.getString(ShareConstants.FEED_CAPTION_PARAM)).isEqualTo("linkCaption")
    assertThat(bundle.getString(ShareConstants.FEED_DESCRIPTION_PARAM)).isEqualTo("linkDescription")
  }
}
