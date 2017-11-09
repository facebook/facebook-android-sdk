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

import com.facebook.plugin.config.AndroidActivity;
import com.facebook.plugin.config.AndroidResourcesConfigurator;
import com.facebook.plugin.config.FacebookModuleConfigurator;
import com.intellij.openapi.project.Project;

import java.util.Collections;

class FacebookShareIntegrationForm extends FacebookIntegrationForm {

    FacebookShareIntegrationForm(final Project project) {
        super(
                project,
                new FacebookModuleConfigurator(
                        "com.facebook.android:facebook-share",
                        Collections.singletonList(AndroidResourcesConfigurator.FB_APP_ID),
                        Collections.singletonList(
                                new AndroidActivity(
                                        "com.facebook.FacebookContentProvider",
                                        false,
                                        null,
                                        true,
                                        null,
                                        true))),
                true,
                "integrateFacebookShareSDKBold",
                "integrateFacebookShareSDK");
    }
}
