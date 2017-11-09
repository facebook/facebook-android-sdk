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

import com.facebook.plugin.config.AvailableModule;
import com.facebook.plugin.config.Configurator;
import com.facebook.plugin.login.FacebookLogin;
import com.facebook.plugin.login.FacebookLoginEventListener;
import com.facebook.plugin.login.FacebookLoginEventType;
import com.intellij.openapi.project.Project;


abstract class IntegrationButtonForm<T extends Configurator>
        extends TitleButtonForm implements FacebookLoginEventListener {

    private static final FacebookLogin fbLogin = FacebookLogin.getInstance();
    private final boolean requiresFBLogin;
    private boolean isConnected = false;
    private final Project project;
    private T configurator;

    // TODO: (T22774398) update this signature when developer.facebook.com API for getting apps is completed
        IntegrationButtonForm(
                final Project project,
                final T configurator,
                final boolean requiresFBLogin,
                final String titleKey,
                final String buttonKey) {
        super(
                titleKey,
                Icon.one,
                buttonKey,
                "completed");

        this.configurator = configurator;

        this.requiresFBLogin = requiresFBLogin;
        if (requiresFBLogin) {
            this.isConnected = fbLogin.getAccessToken() != null;
            fbLogin.addEventListener(this);
        }
        this.project = project;

        this.button.addActionListener(e -> {

            if (requiresFBLogin && !isConnected) {
                fbLogin.logIn(this);
            } else {
                launchDialog();
            }
        });

        updateUI();
    }

    @Override
    public void handleEvent(FacebookLoginEventType eventType) {
        if (eventType == FacebookLoginEventType.LoggedIn) {
            launchDialog();
        }
        this.isConnected = fbLogin.getAccessToken() != null;
        updateUI();
    }

    private void launchDialog() {
        updateUI();
        onButtonClick(project);
    }

    private boolean icConnected() {
        return !requiresFBLogin || isConnected;
    }

    T getConfigurator() {
            return this.configurator;
    }

    @Override
    boolean isCompletelyUnavailable() {
        if (!icConnected() || configurator == null) return false;

        boolean hasAvailableModules = false;
        boolean hasCompletedModules = false;
        AvailableModule[] availableModules =
                configurator.getAvailableModules(project);
        for (AvailableModule availableModule : availableModules) {
            if (!hasAvailableModules) {
                if (availableModule.isInstalled()) {
                    hasCompletedModules = true;
                } else if (!availableModule.isUnavailable()) {
                    hasAvailableModules = true;
                }
            }
        }
        return !hasCompletedModules && !hasAvailableModules;
    }

    @Override
    boolean isCompletelyCompleted() {
        if (!icConnected() || configurator == null) return false;

        boolean hasAvailableModules = false;
        boolean hasCompletedModules = false;
        AvailableModule[] availableModules =
                configurator.getAvailableModules(project);
        for (AvailableModule availableModule : availableModules) {
            if (!hasAvailableModules) {
                if (availableModule.isInstalled()) {
                    hasCompletedModules = true;
                } else if (!availableModule.isUnavailable()) {
                    hasAvailableModules = true;
                }
            }
        }
        return hasCompletedModules && !hasAvailableModules;
    }

    abstract void onButtonClick(final Project project);
}
