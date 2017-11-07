/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.plugin.ui;

import javax.swing.*;

public class FacebookShareCodeForm extends CodeFormContainer {

    private JPanel mainPanel;
    private JPanel shareLinkCode;
    private JPanel sharePhotoCode;
    private JPanel shareVideoCode;
    private JPanel shareMultimediaCode;
    private JPanel likeButtonCode;
    private JPanel shareButtonCode;
    private JPanel sendButtonCode;
    private JPanel shareDialogCode;
    private JPanel shareDialogOnCreateCode;
    private JPanel shareDialogShowCode;
    private JPanel shareDialogOnActivityResultCode;
    private JPanel messageDialogCode;
    private JPanel hashtagsCode;
    private JPanel quoteSharingCode;

    FacebookShareCodeForm() {
        setCodeForm("\n" +
                "ShareLinkContent content = new ShareLinkContent.Builder()\n" +
                "        .setContentUrl(Uri.parse(\"https://developers.facebook.com\"))\n" +
                "        .build();", shareLinkCode);

        setCodeForm("\n" +
                "Bitmap image = ...\n" +
                "SharePhoto photo = new SharePhoto.Builder()\n" +
                "        .setBitmap(image)\n" +
                "        .build();\n" +
                "SharePhotoContent content = new SharePhotoContent.Builder()\n" +
                "        .addPhoto(photo)\n" +
                "        .build();", sharePhotoCode);

        setCodeForm("\n" +
                "Uri videoFileUri = ...\n" +
                "ShareVideo = new ShareVideo.Builder()\n" +
                "        .setLocalUrl(videoUrl)\n" +
                "        .build();\n" +
                "ShareVideoContent content = new ShareVideoContent.Builder()\n" +
                "        .setVideo(video)\n" +
                "        .build();", shareVideoCode);

        setCodeForm("\n" +
                "SharePhoto sharePhoto1 = new SharePhoto.Builder()\n" +
                "    .setBitmap(...)\n" +
                "    .build();\n" +
                "SharePhoto sharePhoto2 = new SharePhoto.Builder()\n" +
                "    .setBitmap(...)\n" +
                "    .build();\n" +
                "ShareVideo shareVideo1 = new ShareVideo.Builder()\n" +
                "    .setLocalUrl(...)\n" +
                "    .build();\n" +
                "ShareVideo shareVideo2 = new ShareVideo.Builder()\n" +
                "    .setLocalUrl(...)\n" +
                "    .build();\n" +
                "\n" +
                "ShareContent shareContent = new ShareMediaContent.Builder()\n" +
                "    .addMedium(sharePhoto1)\n" +
                "    .addMedium(sharePhoto2)\n" +
                "    .addMedium(shareVideo1)\n" +
                "    .addMedium(shareVideo2)\n" +
                "    .build();\n" +
                "\n" +
                "ShareDialog shareDialog = new ShareDialog(...);\n" +
                "shareDialog.show(shareContent, Mode.AUTOMATIC);", shareMultimediaCode);


        setCodeForm("\n" +
                "LikeView likeView = (LikeView) findViewById(R.id.like_view);\n" +
                "likeView.setObjectIdAndType(\n" +
                "    \"https://www.facebook.com/FacebookDevelopers\",\n" +
                "    LikeView.ObjectType.PAGE);", likeButtonCode);


        setCodeForm("\n" +
                "ShareButton shareButton = (ShareButton)findViewById(R.id.fb_share_button);\n" +
                "shareButton.setShareContent(content);", shareButtonCode);


        setCodeForm("\n" +
                "SendButton sendButton = (SendButton)findViewById(R.id.fb_send_button);\n" +
                "sendButton.setShareContent(shareContent);\n" +
                "sendButton.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() { ... });",
                sendButtonCode);


        setCodeForm("\n" +
                "ShareDialog.show(activityOrFragment, content);", shareDialogCode);

        setCodeForm("\n" +
                "public class MainActivity extends FragmentActivity {\n" +
                "    CallbackManager callbackManager;\n" +
                "    ShareDialog shareDialog;\n" +
                "    @Override\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        callbackManager = CallbackManager.Factory.create();\n" +
                "        shareDialog = new ShareDialog(this);\n" +
                "        // this part is optional\n" +
                "        shareDialog.registerCallback(callbackManager, " +
                "new FacebookCallback<Sharer.Result>() { ... });\n" +
                "    }", shareDialogOnCreateCode);


        setCodeForm("\n" +
                "if (ShareDialog.canShow(ShareLinkContent.class)) {\n" +
                "    ShareLinkContent linkContent = new ShareLinkContent.Builder()\n" +
                "            .setContentUrl(Uri.parse(\"http://developers.facebook.com/android\"))\n" +
                "            .build();\n" +
                "    shareDialog.show(linkContent);\n" +
                "}", shareDialogShowCode);


        setCodeForm("\n" +
                "@Override\n" +
                "protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {\n" +
                "    super.onActivityResult(requestCode, resultCode, data);\n" +
                "    callbackManager.onActivityResult(requestCode, resultCode, data);\n" +
                "}", shareDialogOnActivityResultCode);


        setCodeForm("\n" +
                "MessageDialog.show(activityOrFragment, content);", messageDialogCode);

        setCodeForm("\n" +
                "ShareLinkContent content = new ShareLinkContent.Builder()\n" +
                "        .setContentUrl(Uri.parse(\"https://developers.facebook.com\"))\n" +
                "        .setShareHashtag(new ShareHashtag.Builder()\n" +
                "                .setHashtag(\"#ConnectTheWorld\")\n" +
                "                .build());\n" +
                "        .build();", hashtagsCode);

        setCodeForm("\n" +
                "ShareLinkContent content = new ShareLinkContent.Builder()\n" +
                "        .setContentUrl(Uri.parse(\"https://developers.facebook.com\"))\n" +
                "        .setQuote(\"Connect on a global scale.\")\n" +
                "        .build();", quoteSharingCode);
    }

    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }
}
