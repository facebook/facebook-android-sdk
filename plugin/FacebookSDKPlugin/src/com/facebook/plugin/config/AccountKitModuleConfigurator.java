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
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.ArrayList;
import java.util.List;

public class AccountKitModuleConfigurator implements Configurator {
    private final Configurator[] configurators;

    public AccountKitModuleConfigurator(final String appName,
                                        final String appId,
                                        final String clientToken) {
        this.configurators = createConfigurators(appName, appId, clientToken);
    }

    @Override
    public boolean isConfigured(final Module module) {
        for (Configurator configurator : configurators) {
            if (!configurator.isConfigured(module)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void doConfigure(final Module module) {
        for (Configurator configurator : configurators) {
            configurator.doConfigure(module);
        }
    }

    private static Configurator[] createConfigurators(
            final String appName,
            final String appId,
            final String clientToken) {

        return new Configurator[]{
                new GradleBuildConfigurator(
                        "jcenter",
                        "com.facebook.android:account-kit-sdk",
                        "4.+"),
                new AndroidManifestConfigurator(
                        "com.facebook.accountkit.ui.AccountKitActivity")
                        .addMetadata(
                                "com.facebook.accountkit.ApplicationName",
                                "@string/fb_app_name")
                        .addMetadata(
                                "com.facebook.sdk.ApplicationId",
                                "@string/fb_app_id")
                        .addMetadata(
                                "com.facebook.accountkit.ClientToken",
                                "@string/ak_client_token")
                        .addPermission("android.permission.INTERNET")
                        .addPermission("android.permission.READ_PHONE_STATE")
                        .addPermission("android.permission.RECEIVE_SMS")
                        .addPermission("android.permission.GET_ACCOUNTS"),
                new AndroidResourcesConfigurator()
                        .addResourceString("fb_app_id", appId)
                        .addResourceString("fb_app_name", appName)
                        .addResourceString("ak_client_token", clientToken)
        };
    }

    public enum InstallationType {
        CannotInstall,
        AlreadyInstalled,
        Available
    }

    public static final class AvailableModule {
        private final Module module;
        private final InstallationType installationType;

        AvailableModule(final Module module, final InstallationType installationType) {
            this.module = module;
            this.installationType = installationType;
        }

        public Module getModule() {
            return module;
        }

        public InstallationType getInstallationType() {
            return installationType;
        }

        @Override
        public String toString() {
            String title = module.getName();
            if (installationType == InstallationType.CannotInstall) {
                title += " (unavailable)";
            } else if (installationType == InstallationType.AlreadyInstalled) {
                title += " (already installed)";
            }

            return title;
        }
    }

    public static AvailableModule[] getModuleAvailabilities(final Project project) {
        Configurator[] checkConfigurators = createConfigurators("", "", "");
        Module[] allModules = ModuleManager.getInstance(project).getModules();
        List<AvailableModule> res = new ArrayList<>();

        for (Module module : allModules) {
            InstallationType moduleType = InstallationType.CannotInstall;

            if (PsiHelper.findAllFiles(module, GradleConstants.DEFAULT_SCRIPT_NAME).size() > 0) {
                moduleType = InstallationType.AlreadyInstalled;

                for (Configurator configurator : checkConfigurators) {
                    if (!configurator.isConfigured(module)) {
                        moduleType = InstallationType.Available;
                        break;
                    }
                }
            }

            res.add(new AvailableModule(module, moduleType));
        }

        return res.toArray(new AvailableModule[res.size()]);
    }
}
