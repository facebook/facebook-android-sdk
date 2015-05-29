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

import android.content.Context;
import android.os.Bundle;

import com.facebook.FacebookException;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideoContent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class LegacyNativeDialogParameters {

    public static Bundle create(
            UUID callId,
            ShareContent shareContent,
            boolean shouldFailOnDataError) {
        Validate.notNull(shareContent, "shareContent");
        Validate.notNull(callId, "callId");

        Bundle nativeParams = null;
        if (shareContent instanceof ShareLinkContent) {
            final ShareLinkContent linkContent = (ShareLinkContent)shareContent;
            nativeParams = create(linkContent, shouldFailOnDataError);
        } else if (shareContent instanceof SharePhotoContent) {
            final SharePhotoContent photoContent = (SharePhotoContent)shareContent;
            List<String> photoUrls = ShareInternalUtility.getPhotoUrls(
                    photoContent,
                    callId);

            nativeParams = create(photoContent, photoUrls, shouldFailOnDataError);
        } else if (shareContent instanceof ShareVideoContent) {
            final ShareVideoContent videoContent = (ShareVideoContent)shareContent;
            nativeParams = create(videoContent, shouldFailOnDataError);
        } else if (shareContent instanceof ShareOpenGraphContent) {
            final ShareOpenGraphContent openGraphContent = (ShareOpenGraphContent) shareContent;
            try {
                JSONObject openGraphActionJSON = ShareInternalUtility.toJSONObjectForCall(
                        callId, openGraphContent);

                nativeParams = create(openGraphContent, openGraphActionJSON, shouldFailOnDataError);
            } catch (final JSONException e) {
                throw new FacebookException(
                        "Unable to create a JSON Object from the provided ShareOpenGraphContent: "
                                + e.getMessage());
            }
        }

        return nativeParams;
    }

    private static Bundle create(ShareLinkContent linkContent, boolean dataErrorsFatal) {
        Bundle params = createBaseParameters(linkContent, dataErrorsFatal);

        Utility.putNonEmptyString(
                params, ShareConstants.LEGACY_TITLE, linkContent.getContentTitle());
        Utility.putNonEmptyString(
                params, ShareConstants.LEGACY_DESCRIPTION, linkContent.getContentDescription());
        Utility.putUri(params, ShareConstants.LEGACY_IMAGE, linkContent.getImageUrl());

        return params;
    }

    private static Bundle create(
            SharePhotoContent photoContent,
            List<String> imageUrls,
            boolean dataErrorsFatal) {
        Bundle params = createBaseParameters(photoContent, dataErrorsFatal);

        params.putStringArrayList(ShareConstants.LEGACY_PHOTOS, new ArrayList<>(imageUrls));

        return params;
    }

    private static Bundle create(ShareVideoContent videoContent, boolean dataErrorsFatal) {
        // Not supported
        return null;
    }

    private static Bundle create(
            ShareOpenGraphContent openGraphContent,
            JSONObject openGraphActionJSON,
            boolean dataErrorsFatal) {
        Bundle params = createBaseParameters(openGraphContent, dataErrorsFatal);

        Utility.putNonEmptyString(
                params,
                ShareConstants.LEGACY_PREVIEW_PROPERTY_NAME,
                openGraphContent.getPreviewPropertyName());
        Utility.putNonEmptyString(
                params,
                ShareConstants.LEGACY_ACTION_TYPE,
                openGraphContent.getAction().getActionType());

        Utility.putNonEmptyString(
                params,
                ShareConstants.LEGACY_ACTION,
                openGraphActionJSON.toString());

        return params;
    }

    private static Bundle createBaseParameters(ShareContent content, boolean dataErrorsFatal) {
        Bundle params = new Bundle();

        Utility.putUri(params, ShareConstants.LEGACY_LINK, content.getContentUrl());
        Utility.putNonEmptyString(params, ShareConstants.LEGACY_PLACE_TAG, content.getPlaceId());
        Utility.putNonEmptyString(params, ShareConstants.LEGACY_REF, content.getRef());

        params.putBoolean(ShareConstants.LEGACY_DATA_FAILURES_FATAL, dataErrorsFatal);

        List<String> peopleIds = content.getPeopleIds();
        if (!Utility.isNullOrEmpty(peopleIds)) {
            params.putStringArrayList(
                    ShareConstants.LEGACY_FRIEND_TAGS,
                    new ArrayList<>(peopleIds));
        }

        return params;
    }
}
