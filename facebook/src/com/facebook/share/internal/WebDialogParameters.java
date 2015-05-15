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
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphContent;

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

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_PRIVACY,
                appGroupCreationContent
                        .getAppGroupPrivacy().toString().toLowerCase(Locale.ENGLISH));

        return webParams;
    }

    public static Bundle create(GameRequestContent gameRequestContent) {
        Bundle webParams = new Bundle();

        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_MESSAGE,
                gameRequestContent.getMessage());
        Utility.putNonEmptyString(
                webParams,
                ShareConstants.WEB_DIALOG_PARAM_TO,
                gameRequestContent.getTo());
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
        Bundle params = new Bundle();
        Utility.putUri(
                params,
                ShareConstants.WEB_DIALOG_PARAM_HREF,
                shareLinkContent.getContentUrl());

        return params;
    }

    public static Bundle create(ShareOpenGraphContent shareOpenGraphContent) {
        Bundle params = new Bundle();

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

        return webParams;
    }
}
