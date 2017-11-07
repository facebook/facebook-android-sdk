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

public class FacebookMessengerCodeForm extends CodeFormContainer {
    private JPanel mainPanel;
    private JPanel messengerUtilsCode;
    private JPanel additionalParametersCode;
    private JPanel messengerButtonsCode;
    private JPanel manifestCallbackCode;
    private JPanel pickingCode;
    private JPanel sendingContentBackCode;

    FacebookMessengerCodeForm() {
        setCodeForm("\n" +
                "String mimeType = \"image/jpeg\";\n" +
                "\n" +
                "// contentUri points to the content being shared to Messenger\n" +
                "ShareToMessengerParams shareToMessengerParams =\n" +
                "        ShareToMessengerParams.newBuilder(contentUri, mimeType)\n" +
                "                .build();\n" +
                "\n" +
                "// Sharing from an Activity\n" +
                "MessengerUtils.shareToMessenger(\n" +
                "       this,\n" +
                "       REQUEST_CODE_SHARE_TO_MESSENGER,\n" +
                "       shareToMessengerParams);", messengerUtilsCode);

        setCodeForm("\n" +
                "String metadata = \"{ \\\"image\\\" : \\\"trees\\\" }\";\n" +
                "ShareToMessengerParams shareToMessengerParams =\n" +
                "        ShareToMessengerParams.newBuilder(contentUri, \"image/jpeg\")\n" +
                "                .setMetaData(metadata)\n" +
                "                .build();", additionalParametersCode);

        setCodeForm("\n" +
                "<LinearLayout>\n" +
                "  <!-- Include this in your layout -->\n" +
                "  <include layout=\"@layout/messenger_button_send_blue_large\" />    \n" +
                "\n" +
                "</LinearLayout>", messengerButtonsCode);

        setCodeForm("\n" +
                "<activity ...>\n" +
                "\n" +
                "  <intent-filter>\n" +
                "      <action android:name=\"android.intent.action.PICK\"/>\n" +
                "      <category android:name=\"android.intent.category.DEFAULT\" />\n" +
                "      <category android:name=\"com.facebook.orca.category.PLATFORM_THREAD_20150314\" />\n" +
                "  </intent-filter>\n" +
                "\n" +
                "</activity>", manifestCallbackCode);

        setCodeForm("\n" +
                "// Are we in a PICK flow?\n" +
                "private boolean mPicking;\n" +
                "\n" +
                "Intent intent = getIntent();\n" +
                "\n" +
                "if (Intent.ACTION_PICK.equals(intent.getAction())) {\n" +
                "    mPicking = true;    \n" +
                "    MessengerThreadParams mThreadParams = MessengerUtils.getMessengerThreadParamsForIntent(intent);\n" +
                "    \n" +
                "    String metadata = mThreadParams.metadata;\n" +
                "    List<String> participantIds = mThreadParams.participants;\n" +
                "}", pickingCode);

        setCodeForm("\n" +
                "ShareToMessengerParams shareToMessengerParams =\n" +
                "    ShareToMessengerParams.newBuilder(contentUri, \"image/jpeg\")\n" +
                "        .setMetaData(\"{ \\\"image\\\" : \\\"trees\\\" }\")\n" +
                "        .build();\n" +
                "\n" +
                "if (mPicking) {\n" +
                "    MessengerUtils.finishShareToMessenger(this, shareToMessengerParams);\n" +
                "} else {  \n" +
                "    MessengerUtils.shareToMessenger(\n" +
                "        this,\n" +
                "        REQUEST_CODE_SHARE_TO_MESSENGER,\n" +
                "        shareToMessengerParams);\n" +
                "}", sendingContentBackCode);
    }

    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }
}
