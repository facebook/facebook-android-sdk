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
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.FacebookException;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.Validate;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;

import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.robolectric.RuntimeEnvironment;

import static org.powermock.api.mockito.PowerMockito.doNothing;


/**
 * Tests for {@link ShareContentValidation}
 */
@PrepareForTest(Validate.class)
public class ShareContentValidationTest extends FacebookPowerMockTestCase {

    // Share by Message
    @Test(expected = FacebookException.class)
    public void testItValidatesNullForMessage() {
        ShareContentValidation.validateForMessage(null);
    }

    // -LinkContent
    @Test(expected = FacebookException.class)
    public void testItValidatesNoHttpForShareLinkContentMessage() {
        Uri imageUri = Uri.parse("ftp://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForMessage(linkContent);
    }

    // -PhotoContent
    @Test(expected = FacebookException.class)
    public void testItValidatesNullImageForPhotoShareByMessage() {
        SharePhotoContent.Builder spcBuilder = new SharePhotoContent.Builder();
        SharePhoto sharePhoto = new SharePhoto.Builder().setImageUrl(null).setBitmap(null)
                .build();
        SharePhotoContent sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build();

        ShareContentValidation.validateForMessage(sharePhotoContent);
    }

    @Test(expected = FacebookException.class)
    public void testItValidatesEmptyListOfPhotoForPhotoShareByMessage() {
        SharePhotoContent sharePhoto = new SharePhotoContent.Builder().build();

        ShareContentValidation.validateForMessage(sharePhoto);
    }

    @Test(expected = FacebookException.class)
    public void testItValidatesMaxSizeOfPhotoShareByMessage() {
        SharePhotoContent sharePhotoContent =
                new SharePhotoContent.Builder()
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-1.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-2.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-3.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-4.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-5.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-6.gif"))
                        .addPhoto(buildSharePhoto("https://facebook.com/awesome-7.gif"))
                        .build();

        ShareContentValidation.validateForMessage(sharePhotoContent);
    }

    // -ShareVideoContent
    @Test(expected = FacebookException.class)
    public void testItValidatesEmptyPreviewPhotoForShareVideoContentByMessage() {
        ShareVideoContent sharePhoto = new ShareVideoContent.Builder()
                .setPreviewPhoto(null).build();

        ShareContentValidation.validateForMessage(sharePhoto);
    }

    // -ShareOpenGraphContent
    @Test(expected = FacebookException.class)
    public void testItValidatesShareOpenGraphWithNoActionByMessage() {
        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder().setAction(null).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    @Test(expected = FacebookException.class)
    public void testItValidateShareOpenGraphWithNoTypeByMessage() {
        ShareOpenGraphAction shareOpenGraphAction
                = new ShareOpenGraphAction.Builder().setActionType(null).build();

        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder()
                        .setAction(shareOpenGraphAction).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    @Test(expected = FacebookException.class)
    public void testItValidatesShareOpenGraphWithPreviewPropertyNameByMessage() {
        ShareOpenGraphAction shareOpenGraphAction
                = new ShareOpenGraphAction.Builder().setActionType("foo").build();

        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder()
                        .setAction(shareOpenGraphAction).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    // Share by Native (Is the same as Message)
    @Test(expected = FacebookException.class)
    public void testItValidatesNullContentForNativeShare() {
        ShareContentValidation.validateForNativeShare(null);
    }

    @Test(expected = FacebookException.class)
    public void testItValidatesNotHttpForShareLinkContentByNative() {
        Uri imageUri = Uri.parse("ftp://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForNativeShare(linkContent);
    }

    // Share by Web
    @Test(expected = FacebookException.class)
    public void testItValidatesNullContentForWebShare() {
        ShareContentValidation.validateForWebShare(null);
    }

    @Test
    public void testItDoesAcceptSharePhotoContentByWeb() {
        SharePhoto sharePhoto = buildSharePhoto("https://facebook.com/awesome.gif");
        SharePhotoContent sharePhotoContent =
                new SharePhotoContent.Builder().addPhoto(sharePhoto).build();

        ShareContentValidation.validateForWebShare(sharePhotoContent);
    }

    @Test(expected = FacebookException.class)
    public void testItDoesNotAcceptShareVideoContentByWeb() {
        SharePhoto previewPhoto = buildSharePhoto("https://facebook.com/awesome.gif");
        ShareVideoContent shareVideoContent =
                new ShareVideoContent.Builder().setPreviewPhoto(previewPhoto).build();

        ShareContentValidation.validateForWebShare(shareVideoContent);
    }

    // Share by Api
    @Test(expected = FacebookException.class)
    public void testItValidatesNullContentForApiShare() {
        ShareContentValidation.validateForApiShare(null);
    }

    @Test(expected = FacebookException.class)
    public void testItValidatesNullImageForSharePhotoContentByApi() {
        SharePhotoContent.Builder spcBuilder = new SharePhotoContent.Builder();
        SharePhoto sharePhoto = new SharePhoto.Builder().setImageUrl(null)
                .build();
        SharePhotoContent sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build();

        ShareContentValidation.validateForApiShare(sharePhotoContent);
    }

    // Valid Share Contents
    @Test
    public void testItAcceptNullImageForShareLinkContent() {
        ShareLinkContent nullImageContent = buildShareLinkContent(null);

        ShareContentValidation.validateForApiShare(nullImageContent);
        ShareContentValidation.validateForMessage(nullImageContent);
        ShareContentValidation.validateForNativeShare(nullImageContent);
        ShareContentValidation.validateForWebShare(nullImageContent);
    }

    @Test
    public void testItAcceptsHttpForShareLinkContent() {
        Uri imageUri = Uri.parse("http://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForApiShare(linkContent);
        ShareContentValidation.validateForMessage(linkContent);
        ShareContentValidation.validateForNativeShare(linkContent);
        ShareContentValidation.validateForWebShare(linkContent);
    }

    @Test
    public void testItAcceptsHttpsForShareLinkContent() {
        Uri imageUri = Uri.parse("https://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForApiShare(linkContent);
        ShareContentValidation.validateForMessage(linkContent);
        ShareContentValidation.validateForNativeShare(linkContent);
        ShareContentValidation.validateForWebShare(linkContent);
    }

    @Test
    public void testItAcceptsShareOpenGraphContent() {
        String actionKey = "foo";
        String actionValue = "fooValue";
        ShareOpenGraphAction shareOpenGraphAction =
                new ShareOpenGraphAction.Builder()
                        .putString(actionKey, actionValue)
                        .setActionType(actionKey)
                        .build();

        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder()
                        .setPreviewPropertyName(actionKey)
                        .setAction(shareOpenGraphAction).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
        ShareContentValidation.validateForNativeShare(shareOpenGraphContent);
        ShareContentValidation.validateForApiShare(shareOpenGraphContent);
        ShareContentValidation.validateForWebShare(shareOpenGraphContent);
    }

    private ShareLinkContent buildShareLinkContent(Uri imageLink) {
        ShareLinkContent.Builder builder = new ShareLinkContent.Builder();
        return builder.setImageUrl(imageLink)
                .setContentDescription("Some description")
                .setContentTitle("some title").build();
    }

    private SharePhoto buildSharePhoto(String url) {
        return new SharePhoto.Builder()
                .setImageUrl(Uri.parse(url))
                .build();
    }

    private Bitmap createStubBitmap() {
        return Bitmap.createBitmap(10,10, Bitmap.Config.ARGB_8888);
    }
}
