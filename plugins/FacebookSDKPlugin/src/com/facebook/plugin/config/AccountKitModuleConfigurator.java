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

public class AccountKitModuleConfigurator implements Configurator {
    private final Configurable[] configurators;

    public AccountKitModuleConfigurator() {
        this.configurators = createConfigurables();
    }

    /**
     * Updates the Configurator with the provided values
     * @param appName the String {@link AndroidResourcesConfigurator#FB_APP_ID} to update
     * @param appId the String {@link AndroidResourcesConfigurator#FB_APP_NAME} to update
     * @param clientToken the String {@link AndroidResourcesConfigurator#AK_CLIENT_TOKEN} to update
     */
    public void set(final String appName,
             final String appId,
             final String clientToken) {
        for (Configurable configurable : configurators) {
            if (configurable instanceof AndroidResourcesConfigurator) {
                ((AndroidResourcesConfigurator) configurable)
                        .addResourceString(AndroidResourcesConfigurator.FB_APP_NAME, appName)
                        .addResourceString(AndroidResourcesConfigurator.FB_APP_ID, appId)
                        .addResourceString(
                                AndroidResourcesConfigurator.AK_CLIENT_TOKEN,
                                clientToken)
                        .addResourceString(
                                AndroidResourcesConfigurator.AK_LOGIN_PROTOCOL_SCHEME,
                                "ak" + appId);
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

    private static Configurable[] createConfigurables() {
        // TODO: (T22294124) Clean these up
        return new Configurable[] {
                new GradleBuildConfigurator(
                        "mavenCentral",
                        "com.facebook.android:account-kit-sdk",
                        "4.+"),
                new AndroidResourcesConfigurator(
                        new String[] {
                                AndroidResourcesConfigurator.FB_APP_ID,
                                AndroidResourcesConfigurator.FB_APP_NAME,
                                AndroidResourcesConfigurator.AK_CLIENT_TOKEN,
                                AndroidResourcesConfigurator.AK_LOGIN_PROTOCOL_SCHEME }),
                new AndroidManifestConfigurator()
                        .addPermission("android.permission.INTERNET")
                        .addPermission("android.permission.READ_PHONE_STATE")
                        .addPermission("android.permission.RECEIVE_SMS")
                        .addPermission("android.permission.GET_ACCOUNTS")
                        .addMetadata(
                                "com.facebook.accountkit.ApplicationName",
                                "@string/fb_app_name")
                        .addMetadata(
                                "com.facebook.sdk.ApplicationId",
                                "@string/fb_app_id")
                        .addMetadata(
                                "com.facebook.accountkit.ClientToken",
                                "@string/ak_client_token")
                        .addActivity(
                                new AndroidActivity(
                                        "com.facebook.accountkit.ui.AccountKitActivity",
                                        false,
                                        null,
                                        null,
                                        null,
                                        false))
                        .addActivity(
                                new AndroidActivity(
                                        "com.facebook.accountkit.ui.AccountKitEmailRedirectActivity",
                                        false,
                                        null,
                                        null,
                                        AndroidActivity.AK_SCHEME_VALUE,
                                        false))
        };
    }
}
