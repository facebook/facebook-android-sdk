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

import com.intellij.openapi.module.Module;

import java.util.ArrayList;
import java.util.List;

public class FacebookModuleConfigurator implements Configurator {
    private final Configurable[] configurators;
    private final List<String> stringResources;

    public FacebookModuleConfigurator(
            final String library,
            final List<String> stringResources,
            final List<AndroidActivity> activities) {
        this.stringResources = new ArrayList<>(stringResources);
        this.configurators = createConfigurables(library, stringResources, activities);
    }

    /**
     * Updates the Configurator with the provided values
     * @param appId the String {@link AndroidResourcesConfigurator#FB_APP_NAME} to update
     */
    public void setAppId(final String appId) {
        for (Configurable configurable : configurators) {
            if (configurable instanceof AndroidResourcesConfigurator) {
                AndroidResourcesConfigurator androidResourcesConfigurator =
                        (AndroidResourcesConfigurator) configurable;
                if (this.stringResources.contains(AndroidResourcesConfigurator.FB_APP_ID)) {
                    androidResourcesConfigurator.addResourceString(
                            AndroidResourcesConfigurator.FB_APP_ID, appId);
                }
                if (this.stringResources.contains(AndroidResourcesConfigurator.FB_LOGIN_PROTOCOL_SCHEME)) {
                    androidResourcesConfigurator.addResourceString(
                            AndroidResourcesConfigurator.FB_LOGIN_PROTOCOL_SCHEME, "fb" + appId);
                }

            } else if (configurable instanceof AndroidManifestConfigurator) {
                ((AndroidManifestConfigurator) configurable).addAppId(appId);
            }
        }
    }

    @Override
    public boolean isConfigured(final Module module) {
        for (Configurable configurable : configurators) {
            if (!configurable.isConfigured(module)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void doConfigure(final Module module) {
        for (Configurable configurator : configurators) {
            configurator.doConfigure(module);
        }
    }

    @Override
    public Configurable[] getConfigurables() {
        return configurators;
    }

    private static Configurable[] createConfigurables(
            final String library,
            final List<String> stringResources,
            final List<AndroidActivity> activities) {
        return new Configurable[]{
                new GradleBuildConfigurator(
                        "mavenCentral",
                        library,
                        "4.+"),
                new AndroidResourcesConfigurator(
                        stringResources.toArray(
                                new String[stringResources.size()])),
                new AndroidManifestConfigurator()
                        .addMetadata(
                                "com.facebook.sdk.ApplicationId",
                                "@string/fb_app_id")
                        .addPermission("android.permission.INTERNET")
                        .addActivities(activities),
        };
    }
}
