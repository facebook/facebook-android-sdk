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

import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.internal.Utility;
import com.facebook.internal.Validate;
import com.facebook.share.model.ShareContent;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.ShareOpenGraphValueContainer;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * com.facebook.share.internal is solely for the use of other packages within the
 * Facebook SDK for Android. Use of any of the classes in this package is
 * unsupported, and they may be modified or removed without warning at any time.
 */
public class ShareContentValidation {

    private static Validator WebShareValidator;
    private static Validator DefaultValidator;
    private static Validator ApiValidator;

    public static void validateForMessage(ShareContent content) {
        validate(content, getDefaultValidator());
    }

    public static void validateForNativeShare(ShareContent content) {
        validate(content, getDefaultValidator());
    }

    public static void validateForWebShare(ShareContent content) {
        validate(content, getWebShareValidator());
    }

    public static void validateForApiShare(ShareContent content) {
        validate(content, getApiValidator());
    }

    private static Validator getDefaultValidator() {
        if (DefaultValidator == null) {
            DefaultValidator = new Validator();
        }
        return DefaultValidator;
    }

    private static Validator getApiValidator() {
        if (ApiValidator == null) {
            ApiValidator = new ApiValidator();
        }
        return ApiValidator;
    }

    private static Validator getWebShareValidator() {
        if (WebShareValidator == null) {
            WebShareValidator = new WebShareValidator();
        }
        return WebShareValidator;
    }

    private static void validate(ShareContent content, Validator validator)
            throws FacebookException {
        if (content == null) {
            throw new FacebookException("Must provide non-null content to share");
        }

        if (content instanceof ShareLinkContent) {
            validator.validate((ShareLinkContent) content);
        } else if (content instanceof SharePhotoContent) {
            validator.validate((SharePhotoContent) content);
        } else if (content instanceof ShareVideoContent) {
            validator.validate((ShareVideoContent) content);
        } else if (content instanceof ShareOpenGraphContent) {
            validator.validate((ShareOpenGraphContent) content);
        }
    }

    private static void validateLinkContent(
            ShareLinkContent linkContent, Validator validator) {
        Uri imageUrl = linkContent.getImageUrl();
        if (imageUrl != null && !Utility.isWebUri(imageUrl)) {
            throw new FacebookException("Image Url must be an http:// or https:// url");
        }
    }

    private static void validatePhotoContent(
            SharePhotoContent photoContent, Validator validator) {
        List<SharePhoto> photos = photoContent.getPhotos();
        if (photos == null || photos.isEmpty()) {
            throw new FacebookException("Must specify at least one Photo in SharePhotoContent.");
        }
        if (photos.size() > ShareConstants.MAXIMUM_PHOTO_COUNT) {
            throw new FacebookException(
                    String.format(
                            Locale.ROOT,
                            "Cannot add more than %d photos.",
                            ShareConstants.MAXIMUM_PHOTO_COUNT));
        }

        for (SharePhoto photo : photos) {
            validator.validate(photo);
        }
    }

    private static void validatePhotoForApi(SharePhoto photo, Validator validator) {
        if (photo == null) {
            throw new FacebookException("Cannot share a null SharePhoto");
        }

        Bitmap photoBitmap = photo.getBitmap();
        Uri photoUri = photo.getImageUrl();

        if (photoBitmap == null) {
            if (photoUri == null) {
                throw new FacebookException(
                        "SharePhoto does not have a Bitmap or ImageUrl specified");
            }

            if (Utility.isWebUri(photoUri) && !validator.isOpenGraphContent()) {
                throw new FacebookException(
                        "Cannot set the ImageUrl of a SharePhoto to the Uri of an image on the " +
                                "web when sharing SharePhotoContent");
            }
        }
    }

    private static void validatePhotoForNativeDialog(SharePhoto photo, Validator validator) {
        validatePhotoForApi(photo, validator);

        if (photo.getBitmap() != null || !Utility.isWebUri(photo.getImageUrl())) {
            Validate.hasContentProvider(FacebookSdk.getApplicationContext());
        }
    }

    private static void validatePhotoForWebDialog(SharePhoto photo, Validator validator) {
        if (photo == null) {
            throw new FacebookException("Cannot share a null SharePhoto");
        }

        Uri imageUri = photo.getImageUrl();
        if (imageUri == null || !Utility.isWebUri(imageUri)) {
            throw new FacebookException(
                    "SharePhoto must have a non-null imageUrl set to the Uri of an image " +
                            "on the web");
        }
    }

    private static void validateVideoContent(
            ShareVideoContent videoContent, Validator validator) {
        validator.validate(videoContent.getVideo());

        SharePhoto previewPhoto = videoContent.getPreviewPhoto();
        if (previewPhoto != null) {
            validator.validate(previewPhoto);
        }
    }

    private static void validateVideo(ShareVideo video, Validator validator) {
        if (video == null) {
            throw new FacebookException("Cannot share a null ShareVideo");
        }

        Uri localUri = video.getLocalUrl();
        if (localUri == null) {
            throw new FacebookException("ShareVideo does not have a LocalUrl specified");
        }

        if (!Utility.isContentUri(localUri) && !Utility.isFileUri(localUri)) {
            throw new FacebookException("ShareVideo must reference a video that is on the device");
        }
    }

    private static void validateOpenGraphContent(
            ShareOpenGraphContent openGraphContent, Validator validator) {
        validator.validate(openGraphContent.getAction());

        String previewPropertyName = openGraphContent.getPreviewPropertyName();
        if (Utility.isNullOrEmpty(previewPropertyName)) {
            throw new FacebookException("Must specify a previewPropertyName.");
        }

        if (openGraphContent.getAction().get(previewPropertyName) == null) {
            throw new FacebookException(
                    "Property \"" + previewPropertyName + "\" was not found on the action. " +
                            "The name of the preview property must match the name of an " +
                            "action property.");
        }
    }

    private static void validateOpenGraphAction(
            ShareOpenGraphAction openGraphAction,
            Validator validator) {
        if (openGraphAction == null) {
            throw new FacebookException("Must specify a non-null ShareOpenGraphAction");
        }

        if (Utility.isNullOrEmpty(openGraphAction.getActionType())) {
            throw new FacebookException("ShareOpenGraphAction must have a non-empty actionType");
        }

        validator.validate((ShareOpenGraphValueContainer) openGraphAction, false);
    }

    private static void validateOpenGraphObject(
            ShareOpenGraphObject openGraphObject,
            Validator validator) {
        if (openGraphObject == null) {
            throw new FacebookException("Cannot share a null ShareOpenGraphObject");
        }

        validator.validate((ShareOpenGraphValueContainer) openGraphObject, true);
    }

    private static void validateOpenGraphValueContainer(
            ShareOpenGraphValueContainer valueContainer,
            Validator validator,
            boolean requireNamespace) {
        Set<String> keySet = valueContainer.keySet();
        for (String key : keySet) {
            validateOpenGraphKey(key, requireNamespace);
            Object o = valueContainer.get(key);
            if (o instanceof List) {
                List list = (List) o;
                for (Object objectInList : list) {
                    if (objectInList == null) {
                        throw new FacebookException(
                                "Cannot put null objects in Lists in " +
                                        "ShareOpenGraphObjects and ShareOpenGraphActions");
                    }
                    validateOpenGraphValueContainerObject(objectInList, validator);
                }
            } else {
                validateOpenGraphValueContainerObject(o, validator);
            }
        }
    }

    private static void validateOpenGraphKey(String key, boolean requireNamespace) {
        if (!requireNamespace) {
            return;
        }

        String[] components = key.split(":");
        if (components.length < 2) {
            throw new FacebookException("Open Graph keys must be namespaced: %s", key);
        }
        for (String component : components) {
            if (component.isEmpty()) {
                throw new FacebookException("Invalid key found in Open Graph dictionary: %s", key);
            }
        }
    }

    private static void validateOpenGraphValueContainerObject(
            Object o, Validator validator) {
        if (o instanceof ShareOpenGraphObject) {
            validator.validate((ShareOpenGraphObject) o);
        } else if (o instanceof SharePhoto) {
            validator.validate((SharePhoto) o);
        }
    }

    private static class WebShareValidator extends Validator {
        @Override
        public void validate(final SharePhotoContent photoContent) {
            throw new FacebookException("Cannot share SharePhotoContent via web sharing dialogs");
        }

        @Override
        public void validate(final ShareVideoContent videoContent) {
            throw new FacebookException("Cannot share ShareVideoContent via web sharing dialogs");
        }

        @Override
        public void validate(final SharePhoto photo) {
            validatePhotoForWebDialog(photo, this);
        }
    }

    private static class ApiValidator extends Validator {
        @Override
        public void validate(final SharePhoto photo) {
            validatePhotoForApi(photo, this);
        }

    }

    private static class Validator {
        private boolean isOpenGraphContent = false;

        public void validate(final ShareLinkContent linkContent) {
            validateLinkContent(linkContent, this);
        }

        public void validate(final SharePhotoContent photoContent) {
            validatePhotoContent(photoContent, this);
        }

        public void validate(final ShareVideoContent videoContent) {
            validateVideoContent(videoContent, this);
        }

        public void validate(final ShareOpenGraphContent openGraphContent) {
            isOpenGraphContent = true;
            validateOpenGraphContent(openGraphContent, this);
        }

        public void validate(final ShareOpenGraphAction openGraphAction) {
            validateOpenGraphAction(openGraphAction, this);
        }

        public void validate(final ShareOpenGraphObject openGraphObject) {
            validateOpenGraphObject(openGraphObject, this);
        }

        public void validate(final ShareOpenGraphValueContainer openGraphValueContainer,
                             boolean requireNamespace) {
            validateOpenGraphValueContainer(openGraphValueContainer, this, requireNamespace);
        }

        public void validate(final SharePhoto photo) {
            validatePhotoForNativeDialog(photo, this);
        }

        public void validate(final ShareVideo video) {
            validateVideo(video, this);
        }

        public boolean isOpenGraphContent() {
            return isOpenGraphContent;
        }
    }
}
