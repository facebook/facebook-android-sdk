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

package com.facebook.share.internal;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class ShareConstants {

    public static final int MIN_API_VERSION_FOR_WEB_FALLBACK_DIALOGS = 14;

    public static final String WEB_DIALOG_PARAM_DATA = "data";
    public static final String WEB_DIALOG_PARAM_MESSAGE = "message";
    public static final String WEB_DIALOG_PARAM_TO = "to";
    public static final String WEB_DIALOG_PARAM_TITLE = "title";
    public static final String WEB_DIALOG_PARAM_ACTION_TYPE = "action_type";
    public static final String WEB_DIALOG_PARAM_OBJECT_ID = "object_id";
    public static final String WEB_DIALOG_PARAM_FILTERS = "filters";
    public static final String WEB_DIALOG_PARAM_SUGGESTIONS = "suggestions";

    public static final String WEB_DIALOG_PARAM_HREF = "href";
    public static final String WEB_DIALOG_PARAM_ACTION_PROPERTIES = "action_properties";

    public static final String WEB_DIALOG_PARAM_LINK = "link";
    public static final String WEB_DIALOG_PARAM_PICTURE = "picture";
    public static final String WEB_DIALOG_PARAM_NAME = "name";
    public static final String WEB_DIALOG_PARAM_DESCRIPTION = "description";

    public static final String WEB_DIALOG_PARAM_ID = "id";

    public static final String WEB_DIALOG_PARAM_PRIVACY = "privacy";

    public static final String WEB_DIALOG_RESULT_PARAM_POST_ID = "post_id";
    public static final String WEB_DIALOG_RESULT_PARAM_REQUEST_ID = "request";
    public static final String WEB_DIALOG_RESULT_PARAM_TO_ARRAY_MEMBER = "to[%d]";

    // Extras supported for ACTION_FEED_DIALOG:
    public static final String LEGACY_PLACE_TAG = "com.facebook.platform.extra.PLACE";
    public static final String LEGACY_FRIEND_TAGS = "com.facebook.platform.extra.FRIENDS";
    public static final String LEGACY_LINK = "com.facebook.platform.extra.LINK";
    public static final String LEGACY_IMAGE = "com.facebook.platform.extra.IMAGE";
    public static final String LEGACY_TITLE = "com.facebook.platform.extra.TITLE";
    public static final String LEGACY_DESCRIPTION = "com.facebook.platform.extra.DESCRIPTION";
    public static final String LEGACY_REF = "com.facebook.platform.extra.REF";
    public static final String LEGACY_DATA_FAILURES_FATAL =
            "com.facebook.platform.extra.DATA_FAILURES_FATAL";
    public static final String LEGACY_PHOTOS = "com.facebook.platform.extra.PHOTOS";

    public static final String PLACE_ID = "PLACE";
    public static final String PEOPLE_IDS = "FRIENDS";
    public static final String CONTENT_URL = "LINK";
    public static final String IMAGE_URL = "IMAGE";
    public static final String TITLE = "TITLE";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String REF = "REF";
    public static final String DATA_FAILURES_FATAL = "DATA_FAILURES_FATAL";
    public static final String PHOTOS = "PHOTOS";
    public static final String VIDEO_URL = "VIDEO";

    // Extras supported for ACTION_OGACTIONPUBLISH_DIALOG:
    public static final String LEGACY_ACTION = "com.facebook.platform.extra.ACTION";
    public static final String LEGACY_ACTION_TYPE = "com.facebook.platform.extra.ACTION_TYPE";
    public static final String LEGACY_PREVIEW_PROPERTY_NAME =
            "com.facebook.platform.extra.PREVIEW_PROPERTY_NAME";

    public static final String ACTION = "ACTION";
    public static final String ACTION_TYPE = "ACTION_TYPE";
    public static final String PREVIEW_PROPERTY_NAME = "PREVIEW_PROPERTY_NAME";

    // Method args supported for ACTION_LIKE_DIALOG
    public static final String OBJECT_ID = "object_id";
    public static final String OBJECT_TYPE = "object_type";

    // Method args supported for ACTION_APPINVITE_DIALOG
    public static final String APPLINK_URL = "app_link_url";
    public static final String PREVIEW_IMAGE_URL = "preview_image_url";

    // Extras supported for MESSAGE_GET_LIKE_STATUS_REQUEST:
    public static final String EXTRA_OBJECT_ID = "com.facebook.platform.extra.OBJECT_ID";

    // Extras supported in MESSAGE_GET_LIKE_STATUS_REPLY:
    public static final String EXTRA_OBJECT_IS_LIKED =
            "com.facebook.platform.extra.OBJECT_IS_LIKED";
    public static final String EXTRA_LIKE_COUNT_STRING_WITH_LIKE =
            "com.facebook.platform.extra.LIKE_COUNT_STRING_WITH_LIKE";
    public static final String EXTRA_LIKE_COUNT_STRING_WITHOUT_LIKE =
            "com.facebook.platform.extra.LIKE_COUNT_STRING_WITHOUT_LIKE";
    public static final String EXTRA_SOCIAL_SENTENCE_WITH_LIKE =
            "com.facebook.platform.extra.SOCIAL_SENTENCE_WITH_LIKE";
    public static final String EXTRA_SOCIAL_SENTENCE_WITHOUT_LIKE =
            "com.facebook.platform.extra.SOCIAL_SENTENCE_WITHOUT_LIKE";
    public static final String EXTRA_UNLIKE_TOKEN = "com.facebook.platform.extra.UNLIKE_TOKEN";

    // Result keys from Native sharing dialogs
    public static final String EXTRA_RESULT_POST_ID = "com.facebook.platform.extra.POST_ID";
    public static final String RESULT_POST_ID = "postId";

    public static final int MAXIMUM_PHOTO_COUNT = 6;
    static final String MY_VIDEOS = "me/videos";

    // Feed Dialog
    public static final String FEED_TO_PARAM = "to";
    public static final String FEED_LINK_PARAM = "link";
    public static final String FEED_PICTURE_PARAM = "picture";
    public static final String FEED_SOURCE_PARAM = "source";
    public static final String FEED_NAME_PARAM = "name";
    public static final String FEED_CAPTION_PARAM = "caption";
    public static final String FEED_DESCRIPTION_PARAM = "description";
}
