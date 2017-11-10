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

public class AccountKitCodeForm extends CodeFormContainer {
    private JPanel mainPanel;
    private JPanel checkSessionsCode;
    private JPanel smsCode;
    private JPanel emailCode;
    private JPanel handleResultCode;
    private JPanel logoutCode;
    private JPanel accessInfoCode;

    AccountKitCodeForm() {
        setCodeForm("\n" +
                "import com.facebook.accountkit.AccountKit;\n" +
                "import com.facebook.accountkit.AccessToken;\n" +
                "\n" +
                "AccessToken accessToken = AccountKit.getCurrentAccessToken();\n" +
                "\n" +
                "if (accessToken != null) {\n" +
                "  //Handle Returning User\n" +
                "} else {\n" +
                "  //Handle new or logged out user\n" +
                "}", checkSessionsCode);

        setCodeForm("\n" +
                "import com.facebook.accountkit.AccountKit;\n" +
                "\n" +
                "public static int APP_REQUEST_CODE = 99;\n" +
                "\n" +
                "public void phoneLogin(final View view) {\n" +
                "  final Intent intent = new Intent(getActivity(), AccountKitActivity.class);\n" +
                "  AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =\n" +
                "    new AccountKitConfiguration.AccountKitConfigurationBuilder(\n" +
                "      LoginType.PHONE,\n" +
                "      AccountKitActivity.ResponseType.CODE); // or .ResponseType.TOKEN\n" +
                "  // ... perform additional configuration ...\n" +
                "  intent.putExtra(\n" +
                "    AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,\n" +
                "    configurationBuilder.build());\n" +
                "  startActivityForResult(intent, APP_REQUEST_CODE);\n" +
                "}\n", smsCode);

        setCodeForm("\n" +
                "import com.facebook.accountkit.AccountKit.\n" +
                "\n" +
                "public static int APP_REQUEST_CODE = 99;\n" +
                "\n" +
                "public void emailLogin(final View view) {\n" +
                "  final Intent intent = new Intent(getActivity(), AccountKitActivity.class);\n" +
                "  AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =\n" +
                "    new AccountKitConfiguration.AccountKitConfigurationBuilder(\n" +
                "      LoginType.EMAIL,\n" +
                "      AccountKitActivity.ResponseType.CODE); // or .ResponseType.TOKEN\n" +
                "  // ... perform additional configuration ...\n" +
                "  intent.putExtra(\n" +
                "    AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,\n" +
                "    configurationBuilder.build());\n" +
                "  startActivityForResult(intent, APP_REQUEST_CODE);\n" +
                "}\n", emailCode);

        setCodeForm("\n" +
                "    @Override\n" +
                "    protected void onActivityResult(\n" +
                "            final int requestCode,\n" +
                "            final int resultCode,\n" +
                "            final Intent data) {\n" +
                "        super.onActivityResult(requestCode, resultCode, data);\n" +
                "        if (requestCode == APP_REQUEST_CODE) { // confirm that this response matches your request\n" +
                "            AccountKitLoginResult loginResult = " +
                "data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);\n" +
                "            String toastMessage;\n" +
                "            if (loginResult.getError() != null) {\n" +
                "                toastMessage = loginResult.getError().getErrorType().getMessage();\n" +
                "                showErrorActivity(loginResult.getError());\n" +
                "            } else if (loginResult.wasCancelled()) {\n" +
                "                toastMessage = \"Login Cancelled\";\n" +
                "            } else {\n" +
                "                if (loginResult.getAccessToken() != null) {\n" +
                "                    toastMessage = \"Success:\" + loginResult.getAccessToken().getAccountId();\n" +
                "                } else {\n" +
                "                    toastMessage = String.format(\n" +
                "                            \"Success:%s...\",\n" +
                "                            loginResult.getAuthorizationCode().substring(0,10));\n" +
                "                }\n" +
                "\n" +
                "                // If you have an authorization code, retrieve it from\n" +
                "                // loginResult.getAuthorizationCode()\n" +
                "                // and pass it to your server and exchange it for an access token.\n" +
                "\n" +
                "                // Success! Start your next activity...\n" +
                "                goToMyLoggedInActivity();\n" +
                "            }\n" +
                "\n" +
                "            // Surface the result to your user in an appropriate way.\n" +
                "            Toast.makeText(\n" +
                "                    this,\n" +
                "                    toastMessage,\n" +
                "                    Toast.LENGTH_LONG)\n" +
                "                    .show();\n" +
                "        }\n" +
                "    }\n" +
                "\n", handleResultCode);

        setCodeForm("\n" +
                "\n" +
                "import com.facebook.accountkit.AccountKit;\n" +
                "\n" +
                "AccountKit.logOut();\n" +
                "\n", logoutCode);

        setCodeForm("\n" +
                "\n" +
                "AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {\n" +
                "  @Override\n" +
                "  public void onSuccess(final Account account) {\n" +
                "    // Get Account Kit ID\n" +
                "    String accountKitId = account.getId();\n" +
                "\n" +
                "    // Get phone number\n" +
                "    PhoneNumber phoneNumber = account.getPhoneNumber();\n" +
                "    String phoneNumberString = phoneNumber.toString();\n" +
                "\n" +
                "    // Get email\n" +
                "    String email = account.getEmail();\n" +
                "  }\n" +
                "  \n" +
                "  @Override\n" +
                "  public void onError(final AccountKitError error) {\n" +
                "    // Handle Error\n" +
                "  }\n" +
                "});\n", accessInfoCode);
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }
}
