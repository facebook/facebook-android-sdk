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

import android.app.Activity;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;

import com.facebook.FacebookContentProvider;
import com.facebook.FacebookException;
import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.FacebookTestCase;
import com.facebook.junittests.MainActivity;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideoContent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowContentResolver;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ShareContentValidation}
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 18, manifest = Config.NONE)
public class ShareContentValidationTest {

    // Share by Message
    @Test(expected = FacebookException.class)
    public void testValidatesNullForMessage() {
        ShareContentValidation.validateForMessage(null);
    }

    // -LinkContent
    @Test(expected = FacebookException.class)
    public void testValidatesNoHttpForShareLinkContentMessage() throws MalformedURLException, URISyntaxException {
        Uri imageUri = Uri.parse("ftp://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForMessage(linkContent);
    }

    // -PhotoContent
    @Test(expected = FacebookException.class)
    public void testValidatesNullImageForPhotoShareByMessage() throws MalformedURLException {
        SharePhotoContent.Builder spcBuilder = new SharePhotoContent.Builder();
        SharePhoto sharePhoto = new SharePhoto.Builder().setImageUrl(null).setBitmap(null)
                .build();
        SharePhotoContent sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build();

        ShareContentValidation.validateForMessage(sharePhotoContent);
    }

    @Test(expected = FacebookException.class)
    public void testValidatesEmptyListOfPhotoForPhotoShareByMessage() throws MalformedURLException {
        SharePhotoContent sharePhoto = new SharePhotoContent.Builder().build();

        ShareContentValidation.validateForMessage(sharePhoto);
    }

    @Test(expected = FacebookException.class)
    public void testValidatesMaxSizeOfPhotoShareByMessage() throws MalformedURLException {
        SharePhotoContent sharePhotoContent =
                new SharePhotoContent.Builder().addPhoto(buildSharePhoto("https://facebook.com/awesome-1.gif"))
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
    public void testValidatesEmptyPreviewPhotoForShareVideoContentByMessage() throws MalformedURLException {
        ShareVideoContent sharePhoto = new ShareVideoContent.Builder().setPreviewPhoto(null).build();

        ShareContentValidation.validateForMessage(sharePhoto);
    }

    // -ShareOpenGraphContent
    @Test(expected = FacebookException.class)
    public void testValidatesShareOpenGraphWithNoActionByMessage() {
        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder().setAction(null).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    @Test(expected = FacebookException.class)
    public void testValidateShareOpenGraphWithNoTypeByMessage() {
        ShareOpenGraphAction shareOpenGraphAction
                = new ShareOpenGraphAction.Builder().setActionType(null).build();

        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder()
                        .setAction(shareOpenGraphAction).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    @Test(expected = FacebookException.class)
    public void testValidatesShareOpenGraphWithPreviewPropertyNameByMessage() {
        ShareOpenGraphAction shareOpenGraphAction
                = new ShareOpenGraphAction.Builder().setActionType("foo").build();

        ShareOpenGraphContent shareOpenGraphContent =
                new ShareOpenGraphContent.Builder()
                        .setAction(shareOpenGraphAction).build();

        ShareContentValidation.validateForMessage(shareOpenGraphContent);
    }

    // Share by Native (Is the same as Message)
    @Test(expected = FacebookException.class)
    public void testValidatesNullContentForNativeShare() {
        ShareContentValidation.validateForNativeShare(null);
    }

    @Test(expected = FacebookException.class)
    public void testValidatesNotHttpForShareLinkContentByNative() throws MalformedURLException, URISyntaxException {
        Uri imageUri = Uri.parse("ftp://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForNativeShare(linkContent);
    }

    // Share by Web
    @Test(expected = FacebookException.class)
    public void testValidatesNullContentForWebShare() {
        ShareContentValidation.validateForWebShare(null);
    }

    @Test(expected = FacebookException.class)
    public void testDoesNotAcceptSharePhotoContentByWeb() {
        SharePhoto sharePhoto = buildSharePhoto("https://facebook.com/awesome.gif");
        SharePhotoContent sharePhotoContent =
                new SharePhotoContent.Builder().addPhoto(sharePhoto).build();

        ShareContentValidation.validateForWebShare(sharePhotoContent);
    }

    @Test(expected = FacebookException.class)
    public void testDoesNotAcceptShareVideoContentByWeb() {
        SharePhoto previewPhoto = buildSharePhoto("https://facebook.com/awesome.gif");
        ShareVideoContent shareVideoContent =
                new ShareVideoContent.Builder().setPreviewPhoto(previewPhoto).build();

        ShareContentValidation.validateForWebShare(shareVideoContent);
    }

    // Share by Api
    @Test(expected = FacebookException.class)
    public void testValidatesNullContentForApiShare() {
        ShareContentValidation.validateForApiShare(null);
    }

    @Test(expected = FacebookException.class)
    public void testDoesNotAcceptSharePhotoContentByApi() throws MalformedURLException {
        Uri imageUri = Uri.parse("https://facebook.com/awesome-content.gif");
        SharePhotoContent.Builder spcBuilder = new SharePhotoContent.Builder();
        SharePhoto sharePhoto = new SharePhoto.Builder().setImageUrl(imageUri)
                .build();
        SharePhotoContent sharePhotoContent = spcBuilder.addPhoto(sharePhoto).build();

        ShareContentValidation.validateForApiShare(sharePhotoContent);
    }

    @Test
    public void testAcceptNullImageForShareLinkContent() throws MalformedURLException, URISyntaxException {
        ShareLinkContent nullImageContent = buildShareLinkContent(null);

        ShareContentValidation.validateForMessage(nullImageContent);
        ShareContentValidation.validateForNativeShare(nullImageContent);
        ShareContentValidation.validateForWebShare(nullImageContent);
        ShareContentValidation.validateForApiShare(nullImageContent);
    }

    @Test
    public void testAcceptHttpForShareLinkContent() throws MalformedURLException, URISyntaxException {
        Uri imageUri = Uri.parse("http://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForMessage(linkContent);
        ShareContentValidation.validateForNativeShare(linkContent);
        ShareContentValidation.validateForWebShare(linkContent);
        ShareContentValidation.validateForApiShare(linkContent);
    }

    @Test
    public void testAcceptHttpsForShareLinkContent() throws MalformedURLException, URISyntaxException {
        Uri imageUri = Uri.parse("https://facebook.com/awesome-content.gif");
        ShareLinkContent linkContent = buildShareLinkContent(imageUri);

        ShareContentValidation.validateForMessage(linkContent);
        ShareContentValidation.validateForNativeShare(linkContent);
        ShareContentValidation.validateForWebShare(linkContent);
        ShareContentValidation.validateForApiShare(linkContent);
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