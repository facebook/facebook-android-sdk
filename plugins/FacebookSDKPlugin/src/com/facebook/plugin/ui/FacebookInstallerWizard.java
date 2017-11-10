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

import com.facebook.plugin.config.FacebookModuleConfigurator;
import com.intellij.openapi.project.Project;

import javax.annotation.Nonnull;

class FacebookInstallerWizard extends WizardDialog {

    private static final String COMPLETE_STRING_KEY = "fbCompletedText";
    private static final String REFERENCE_LINK_URL =
            "https://developers.facebook.com/docs/facebook-login/android";

    FacebookInstallerWizard(
            @Nonnull final Project project,
            final FacebookModuleConfigurator configurator) {
        super(
                project,
                "Integrate Facebook SDK",
                new WizardStep[] {
                        new FacebookPrerequisitesForm(),
                        new FacebookInstallDependenciesForm(
                                project,
                                configurator),
                        new CompletedForm(COMPLETE_STRING_KEY, REFERENCE_LINK_URL)
                });
    }
}
