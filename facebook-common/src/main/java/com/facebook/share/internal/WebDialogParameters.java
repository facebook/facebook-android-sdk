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

import android.os.Bundle;

import com.facebook.FacebookException;
import com.facebook.internal.Utility;
import com.facebook.share.model.AppGroupCreationContent;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareHashtag;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class WebDialogParameters {

    public static Bundle create(AppGroupCreationContent appGroupCreationContent) {
        Bundle webParams = new Bundle();

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_NAME,
                appGroupCreationContent.getName());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_DESCRIPTION,
                appGroupCreationContent.getDescription());

        AppGroupCreationContent.AppGroupPrivacy privacy =
                appGroupCreationContent.getAppGroupPrivacy();
        if (privacy != null) {
            Utility.putNonEmptyString(
                    webParams,
                    ShareConstants.WEB_DIALOG_PARAM_PRIVACY,
                    privacy.toString().toLowerCase(Locale.ENGLISH));
        }

        return webParams;
    }

    public static Bundle create(GameRequestContent gameRequestContent) {
        Bundle webParams = new Bundle();

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_MESSAGE,
                gameRequestContent.getMessage());
        Utility.putCommaSeparatedStringList(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_TO,
                gameRequestContent.getRecipients());
        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_TITLE,
                gameRequestContent.getTitle());
        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_DATA,
                gameRequestContent.getData());
        if (gameRequestContent.getActionType() != null) {
            Utility.putNonEmptyString(
                    webParams,
                    ShareConstants.WEB_DIALOG_PARAM_ACTION_TYPE,
                    gameRequestContent.getActionType().toString().toLowerCase(Locale.ENGLISH));
        }
        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_OBJECT_ID,
                gameRequestContent.getObjectId());
        if (gameRequestContent.getFilters() != null) {
            Utility.putNonEmptyString(
                    webParams,
                    ShareConstants.WEB_DIALOG_PARAM_FILTERS,
                    gameRequestContent.getFilters().toString().toLowerCase(Locale.ENGLISH));
        }
        Utility.putCommaSeparatedStringList(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_SUGGESTIONS,
                gameRequestContent.getSuggestions());
        return webParams;
    }

    public static Bundle create(ShareLinkContent shareLinkContent) {
        Bundle params = createBaseParameters(shareLinkContent);
        Utility.putUri(
                params,
                ShareConstants.WEB_DIALOG_PARAM_HREF,
                shareLinkContent.getContentUrl());

        Utility.putNonEmptyString(
                params,
                ShareConstants.WEB_DIALOG_PARAM_QUOTE,
                shareLinkContent.getQuote());

        return params;
    }

    public static Bundle create(ShareOpenGraphContent shareOpenGraphContent) {
        Bundle params = createBaseParameters(shareOpenGraphContent);

        Utility.putNonEmptyString(
                params,
                ShareConstants.WEB_DIALOG_PARAM_ACTION_TYPE,
                shareOpenGraphContent.getAction().getActionType());

        try {
            JSONObject ogJSON = ShareInternalUtility.toJSONObjectForWeb(shareOpenGraphContent);
            ogJSON = ShareInternalUtility.removeNamespacesFromOGJsonObject(ogJSON, false);
            if (ogJSON != null) {
                Utility.putNonEmptyString(
                        params,
                        ShareConstants.WEB_DIALOG_PARAM_ACTION_PROPERTIES,
                        ogJSON.toString());
            }
        } catch (JSONException e) {
            throw new FacebookException("Unable to serialize the ShareOpenGraphContent to JSON", e);
        }

        return params;
    }

    public static Bundle create(SharePhotoContent sharePhotoContent) {
        final Bundle params = createBaseParameters(sharePhotoContent);

        final String[] urls = new String[sharePhotoContent.getPhotos().size()];
        Utility.map(
                sharePhotoContent.getPhotos(),
                new Utility.Mapper<SharePhoto, String>() {
                    @Override
                    public String apply(SharePhoto item) {
                        return item.getImageUrl().toString();
                    }
                }).toArray(urls);

        params.putStringArray(ShareConstants.WEB_DIALOG_PARAM_MEDIA ,urls);

        return params;
    }

    public static Bundle createBaseParameters(ShareContent shareContent) {
        Bundle params = new Bundle();

        ShareHashtag shareHashtag = shareContent.getShareHashtag();
        if (shareHashtag != null) {
            Utility.putNonEmptyString(
                    params,
                    ShareConstants.WEB_DIALOG_PARAM_HASHTAG,
                    shareHashtag.getHashtag());
        }

        return params;
    }

    public static Bundle createForFeed(ShareLinkContent shareLinkContent) {
        Bundle webParams = new Bundle();

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_NAME,
                shareLinkContent.getContentTitle());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_DESCRIPTION,
                shareLinkContent.getContentDescription());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_LINK,
                Utility.getUriString(shareLinkContent.getContentUrl()));

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_PICTURE,
                Utility.getUriString(shareLinkContent.getImageUrl()));

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_QUOTE,
                shareLinkContent.getQuote());

        ShareHashtag shareHashtag = shareLinkContent.getShareHashtag();
        if (shareHashtag != null) {
            Utility.putNonEmptyString(
                    webParams,
                    ShareConstants.WEB_DIALOG_PARAM_HASHTAG,
                    shareLinkContent.getShareHashtag().getHashtag());
        }
        return webParams;
    }

    public static Bundle createForFeed(ShareFeedContent shareFeedContent) {
        Bundle webParams = new Bundle();

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_TO_PARAM,
                shareFeedContent.getToId());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_LINK_PARAM,
                shareFeedContent.getLink());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_PICTURE_PARAM,
                shareFeedContent.getPicture());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_SOURCE_PARAM,
                shareFeedContent.getMediaSource());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_NAME_PARAM,
                shareFeedContent.getLinkName());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_CAPTION_PARAM,
                shareFeedContent.getLinkCaption());

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.FEED_DESCRIPTION_PARAM,
                shareFeedContent.getLinkDescription());

        return webParams;
    }
}
