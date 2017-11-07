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

public class FacebookLoginCodeForm extends CodeFormContainer {

    private JPanel mainPanel;
    private JPanel loginButtonPanel;
    private JPanel customizeButtonPanel;
    private JPanel onCreatePanel;
    private JPanel registerCallbackCode;
    private JPanel onActivityResultCode;
    private JPanel checkLoginStatusCode;
    private JPanel performLoginCode;

    FacebookLoginCodeForm() {
        setCodeForm("\n" +
                "<com.facebook.login.widget.LoginButton\n" +
                "    android:id=\"@+id/login_button\"\n" +
                "    android:layout_width=\"wrap_content\"\n" +
                "    android:layout_height=\"wrap_content\"\n" +
                "    android:layout_gravity=\"center_horizontal\"\n" +
                "    android:layout_marginTop=\"30dp\"\n" +
                "    android:layout_marginBottom=\"30dp\" /> ", loginButtonPanel);

        setCodeForm("\n" +
                "@Override\n" +
                "public View onCreateView(\n" +
                "        LayoutInflater inflater,\n" +
                "        ViewGroup container,\n" +
                "        Bundle savedInstanceState) {\n" +
                "    View view = inflater.inflate(R.layout.splash, container, false);\n" +
                "\n" +
                "    loginButton = (LoginButton) view.findViewById(R.id.login_button);\n" +
                "    loginButton.setReadPermissions(\"email\");\n" +
                "    // If using in a fragment\n" +
                "    loginButton.setFragment(this);    \n" +
                "    // Other app specific specialization\n" +
                "\n" +
                "    // Callback registration\n" +
                "    loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {\n" +
                "        @Override\n" +
                "        public void onSuccess(LoginResult loginResult) {\n" +
                "            // App code\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void onCancel() {\n" +
                "            // App code\n" +
                "        }\n" +
                "\n" +
                "        @Override\n" +
                "        public void onError(FacebookException exception) {\n" +
                "            // App code\n" +
                "        }\n" +
                "    });    \n" +
                "}", customizeButtonPanel);

        setCodeForm("\n" +
                "public class MainActivity extends FragmentActivity {\n" +
                "    CallbackManager callbackManager;\n" +
                "    @Override\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        callbackManager = CallbackManager.Factory.create();\n" +
                "        LoginButton loginButton = (LoginButton) " +
                "view.findViewById(R.id.usersettings_fragment_login_button);\n" +
                "        loginButton.registerCallback(callbackManager, " +
                "new FacebookCallback<LoginResult>() { ... });\n" +
                "    }\n" +
                "}", onCreatePanel);

        setCodeForm("\n" +
                "@Override\n" +
                "public void onCreate(Bundle savedInstanceState) {\n" +
                "    super.onCreate(savedInstanceState);\n" +
                "\n" +
                "    callbackManager = CallbackManager.Factory.create();\n" +
                "\n" +
                "    LoginManager.getInstance().registerCallback(callbackManager,\n" +
                "            new FacebookCallback<LoginResult>() {\n" +
                "                @Override\n" +
                "                public void onSuccess(LoginResult loginResult) {\n" +
                "                    // App code\n" +
                "                }\n" +
                "\n" +
                "                @Override\n" +
                "                public void onCancel() {\n" +
                "                     // App code\n" +
                "                }\n" +
                "\n" +
                "                @Override\n" +
                "                public void onError(FacebookException exception) {\n" +
                "                     // App code   \n" +
                "                }\n" +
                "    });\n" +
                "}", registerCallbackCode);

        setCodeForm("\n" +
                "@Override\n" +
                "protected void onActivityResult(int requestCode, int resultCode, Intent data) {\n" +
                "    super.onActivityResult(requestCode, resultCode, data);\n" +
                "    callbackManager.onActivityResult(requestCode, resultCode, data);\n" +
                "}", onActivityResultCode);

        setCodeForm("\n" +
                "public class MainActivity extends FragmentActivity {\n" +
                "    CallbackManager callbackManager;\n" +
                "    @Override\n" +
                "    public void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        callbackManager = CallbackManager.Factory.create();\n" +
                "        LoginManager.getInstance().registerCallback(callbackManager, " +
                "new FacebookCallback<LoginResult>() {...});\n" +
                "    }\n" +
                "}", checkLoginStatusCode);

        setCodeForm("\n" +
                "LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList(\"public_profile\"));",
                performLoginCode);

    }

    @Override
    public JComponent getComponent() {
        return this.mainPanel;
    }
}
