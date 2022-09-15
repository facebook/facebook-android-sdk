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

/**
 * com.facebook.share.internal is solely for the use of other packages within the Facebook SDK for
 * Android. Use of any of the classes in this package is unsupported, and they may be modified or
 * removed without warning at any time.
 */
object ShareConstants {
  // Common Web Params
  const val WEB_DIALOG_PARAM_ACTION_TYPE = "action_type"

  // Game Request Dialog Params
  const val WEB_DIALOG_PARAM_DATA = "data"
  const val WEB_DIALOG_PARAM_MESSAGE = "message"
  const val WEB_DIALOG_PARAM_TO = "to"
  const val WEB_DIALOG_PARAM_TITLE = "title"
  const val WEB_DIALOG_PARAM_OBJECT_ID = "object_id"
  const val WEB_DIALOG_PARAM_FILTERS = "filters"
  const val WEB_DIALOG_PARAM_SUGGESTIONS = "suggestions"

  // Web Share Dialog Params
  const val WEB_DIALOG_PARAM_HREF = "href"
  const val WEB_DIALOG_PARAM_ACTION_PROPERTIES = "action_properties"
  const val WEB_DIALOG_PARAM_QUOTE = "quote"
  const val WEB_DIALOG_PARAM_HASHTAG = "hashtag"

  // Images from a SharePhotoContent
  const val WEB_DIALOG_PARAM_MEDIA = "media"

  // Feed Dialog Params
  const val WEB_DIALOG_PARAM_LINK = "link"
  const val WEB_DIALOG_PARAM_PICTURE = "picture"
  const val WEB_DIALOG_PARAM_NAME = "name"
  const val WEB_DIALOG_PARAM_DESCRIPTION = "description"

  // Join App Group Dialog Params
  const val WEB_DIALOG_PARAM_ID = "id"

  // Create App Group Dialog Params
  const val WEB_DIALOG_PARAM_PRIVACY = "privacy"
  const val WEB_DIALOG_RESULT_PARAM_POST_ID = "post_id"
  const val WEB_DIALOG_RESULT_PARAM_REQUEST_ID = "request"
  const val WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER = "to[%d]"

  // Extras supported for ACTION_FEED_DIALOG:
  const val LEGACY_PLACE_TAG = "com.facebook.platform.extra.PLACE"
  const val LEGACY_FRIEND_TAGS = "com.facebook.platform.extra.FRIENDS"
  const val LEGACY_LINK = "com.facebook.platform.extra.LINK"
  const val LEGACY_IMAGE = "com.facebook.platform.extra.IMAGE"
  const val LEGACY_TITLE = "com.facebook.platform.extra.TITLE"
  const val LEGACY_DESCRIPTION = "com.facebook.platform.extra.DESCRIPTION"
  const val LEGACY_REF = "com.facebook.platform.extra.REF"
  const val LEGACY_DATA_FAILURES_FATAL = "com.facebook.platform.extra.DATA_FAILURES_FATAL"
  const val LEGACY_PHOTOS = "com.facebook.platform.extra.PHOTOS"
  const val PLACE_ID = "PLACE"
  const val PEOPLE_IDS = "FRIENDS"
  const val PAGE_ID = "PAGE"
  const val CONTENT_URL = "LINK"
  const val MESSENGER_URL = "MESSENGER_LINK"
  const val HASHTAG = "HASHTAG"
  const val IMAGE_URL = "IMAGE"
  const val TITLE = "TITLE"
  const val SUBTITLE = "SUBTITLE"
  const val ITEM_URL = "ITEM_URL"
  const val BUTTON_TITLE = "BUTTON_TITLE"
  const val BUTTON_URL = "BUTTON_URL"
  const val PREVIEW_TYPE = "PREVIEW_TYPE"
  const val TARGET_DISPLAY = "TARGET_DISPLAY"
  const val ATTACHMENT_ID = "ATTACHMENT_ID"
  const val OPEN_GRAPH_URL = "OPEN_GRAPH_URL"
  const val DESCRIPTION = "DESCRIPTION"
  const val REF = "REF"
  const val DATA_FAILURES_FATAL = "DATA_FAILURES_FATAL"
  const val PHOTOS = "PHOTOS"
  const val VIDEO_URL = "VIDEO"
  const val QUOTE = "QUOTE"
  const val MEDIA = "MEDIA"
  const val MESSENGER_PLATFORM_CONTENT = "MESSENGER_PLATFORM_CONTENT"

  // Multimedia args
  const val MEDIA_TYPE = "type"
  const val MEDIA_URI = "uri"
  const val MEDIA_EXTENSION = "extension"

  // Camera-share args
  const val EFFECT_ID = "effect_id"
  const val EFFECT_ARGS = "effect_arguments"
  const val EFFECT_TEXTURES = "effect_textures"

  // Extras supported for ACTION_OGACTIONPUBLISH_DIALOG:
  const val LEGACY_ACTION = "com.facebook.platform.extra.ACTION"
  const val LEGACY_ACTION_TYPE = "com.facebook.platform.extra.ACTION_TYPE"
  const val LEGACY_PREVIEW_PROPERTY_NAME = "com.facebook.platform.extra.PREVIEW_PROPERTY_NAME"
  const val ACTION = "ACTION"
  const val ACTION_TYPE = "ACTION_TYPE"
  const val PREVIEW_PROPERTY_NAME = "PREVIEW_PROPERTY_NAME"

  // Method args supported for ACTION_LIKE_DIALOG
  const val OBJECT_ID = "object_id"
  const val OBJECT_TYPE = "object_type"

  // Method args supported for ACTION_APPINVITE_DIALOG
  const val APPLINK_URL = "app_link_url"
  const val PREVIEW_IMAGE_URL = "preview_image_url"
  const val PROMO_CODE = "promo_code"
  const val PROMO_TEXT = "promo_text"
  const val DEEPLINK_CONTEXT = "deeplink_context"
  const val DESTINATION = "destination"

  // Extras supported for MESSAGE_GET_LIKE_STATUS_REQUEST:
  const val EXTRA_OBJECT_ID = "com.facebook.platform.extra.OBJECT_ID"

  // Extras supported in MESSAGE_GET_LIKE_STATUS_REPLY:
  const val EXTRA_OBJECT_IS_LIKED = "com.facebook.platform.extra.OBJECT_IS_LIKED"
  const val EXTRA_LIKE_COUNT_STRING_WITH_LIKE =
      "com.facebook.platform.extra.LIKE_COUNT_STRING_WITH_LIKE"
  const val EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE =
      "com.facebook.platform.extra.LIKE_COUNT_STRING_WITHOUT_LIKE"
  const val EXTRA_SOCIAL_SENTENCE_WITH_LIKE =
      "com.facebook.platform.extra.SOCIAL_SENTENCE_WITH_LIKE"
  const val EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE =
      "com.facebook.platform.extra.SOCIAL_SENTENCE_WITHOUT_LIKE"
  const val EXTRA_UNLIKE_TOKEN = "com.facebook.platform.extra.UNLIKE_TOKEN"

  // Result keys from Native sharing dialogs
  const val EXTRA_RESULT_POST_ID = "com.facebook.platform.extra.POST_ID"
  const val RESULT_POST_ID = "postId"
  const val MAXIMUM_PHOTO_COUNT = 6
  const val MAXIMUM_MEDIA_COUNT = 6
  const val MY_VIDEOS = "me/videos"

  // Feed Dialog
  const val FEED_TO_PARAM = "to"
  const val FEED_LINK_PARAM = "link"
  const val FEED_PICTURE_PARAM = "picture"
  const val FEED_SOURCE_PARAM = "source"
  const val FEED_NAME_PARAM = "name"
  const val FEED_CAPTION_PARAM = "caption"
  const val FEED_DESCRIPTION_PARAM = "description"

  // Share into Story
  const val STORY_INTERACTIVE_COLOR_LIST = "top_background_color_list"
  const val STORY_DEEP_LINK_URL = "content_url"
  const val STORY_BG_ASSET = "bg_asset"
  const val STORY_INTERACTIVE_ASSET_URI = "interactive_asset_uri"
}
