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

package com.facebook.plugin.config;

import com.facebook.plugin.utils.PsiHelper;
import com.intellij.openapi.module.Module;
import org.jetbrains.plugins.gradle.util.GradleConstants;

public final class AvailableModule {

    private enum InstallationStatus {
        UNAVAILABLE,
        INSTALLED,
        AVAILABLE
    }

    private final Module module;
    private final InstallationStatus installationStatus;

    AvailableModule(final Module module, final Configurable[] configurables) {
        AvailableModule.InstallationStatus moduleType =
                AvailableModule.InstallationStatus.UNAVAILABLE;

        if (PsiHelper.findAllFiles(module, GradleConstants.DEFAULT_SCRIPT_NAME).size() > 0) {
            moduleType = AvailableModule.InstallationStatus.INSTALLED;

            for (Configurable configurable : configurables) {
                if (!configurable.isConfigured(module)) {
                    moduleType = AvailableModule.InstallationStatus.AVAILABLE;
                    break;
                }
            }
        }

        this.module = module;
        this.installationStatus = moduleType;
    }

    public Module getModule() {
        return module;
    }

    public boolean isInstalled() {
        return installationStatus == InstallationStatus.INSTALLED;
    }

    public boolean isUnavailable() {
        return installationStatus == InstallationStatus.UNAVAILABLE;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(module.getName());
        if (installationStatus == InstallationStatus.UNAVAILABLE) {
            builder.append(" (unavailable)");
        } else if (installationStatus == InstallationStatus.INSTALLED) {
            builder.append(" (installed)");
        }
        return builder.toString();
    }
}
