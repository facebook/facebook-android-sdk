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

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlElement;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.android.dom.manifest.Activity;
import org.jetbrains.android.dom.manifest.Application;
import org.jetbrains.android.dom.manifest.Manifest;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AndroidManifestConfigurator implements Configurable {


    private final Map<String, String> metadata;
    private final List<String> permissions;
    private final List<AndroidActivity> activities;
    private @Nullable String appId;

    AndroidManifestConfigurator() {
        this.metadata = new LinkedHashMap<>();
        this.permissions = new ArrayList<>();
        this.activities = new ArrayList<>();
    }

    AndroidManifestConfigurator addMetadata(final String name, final String value) {
        metadata.put(name, value);
        return this;
    }

    AndroidManifestConfigurator addPermission(final String permission) {
        permissions.add(permission);
        return this;
    }

    AndroidManifestConfigurator addActivity(final AndroidActivity activity) {
        activities.add(activity);
        return this;
    }

    AndroidManifestConfigurator addActivities(final List<AndroidActivity> activities) {
        this.activities.addAll(activities);
        return this;
    }

    AndroidManifestConfigurator addAppId(final String appId) {
        this.appId = appId;
        return this;
    }

    @Override
    public boolean isConfigured(final Module module) {
        final Manifest androidManifest = getAndroidManifest(module);
        if (androidManifest == null) {
            return false;
        }

        for (String permission : permissions) {
            if (!androidManifest.getXmlElement().getText().contains(permission)) {
                return false;
            }
        }

        for (String metadataName : metadata.keySet()) {
            if (!androidManifest.getXmlElement().getText().contains(metadataName)) {
                return false;
            }
        }

        for (AndroidActivity activity : activities) {
            if (!androidManifest.getApplication().getXmlElement().getText().contains(activity.getName())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void doConfigure(final Module module) {
        final Manifest androidManifest = getAndroidManifest(module);
        if ((androidManifest == null) ||
                (androidManifest.getApplication() == null) ||
                !WritingAccessProvider.isPotentiallyWritable(
                        androidManifest.getXmlElement().getContainingFile().getVirtualFile(),
                        null)) {
            Messages.showErrorDialog(
                module.getProject(),
                "Cannot find or modify Android manifest file for module " + module.getName(),
                "Facebook SDK Plugin");
            return;
        }

        final PsiFile manifestFile = androidManifest.getXmlElement().getContainingFile();
        final Application application = androidManifest.getApplication();
        final XmlElement applicationTag = application.getXmlElement();
        final XmlElementFactory elementFactory = XmlElementFactory.getInstance(module.getProject());

        new WriteCommandAction(manifestFile.getProject()) {
            @Override
            protected void run(@NotNull Result result) {

                for (String permission : permissions) {
                    if (!androidManifest.getXmlElement().getText().contains(permission)) {
                        XmlTag permTag = elementFactory
                                .createTagFromText(AndroidActivity.PERMISSION_TAG);
                        permTag.setAttribute(AndroidActivity.NAME_ATTR, permission);
                        androidManifest.getXmlElement().addBefore(permTag, applicationTag);
                    }
                }

                for (Map.Entry<String, String> metadata : metadata.entrySet()) {
                    if (!applicationTag.getText().contains(metadata.getKey())) {
                        XmlTag mdTag = elementFactory
                                .createTagFromText(AndroidActivity.METADATA_TAG);
                        mdTag.setAttribute(AndroidActivity.NAME_ATTR, metadata.getKey());
                        mdTag.setAttribute(AndroidActivity.VALUE_ATTR, metadata.getValue());
                        application.getXmlElement().add(mdTag);
                    }
                }

                for (AndroidActivity activity : activities) {
                    if (!applicationTag.getText().contains(activity.getName())) {
                        final Activity newActivity = application.addActivity();
                        activity.create(elementFactory, newActivity, appId);
                    }
                }

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(manifestFile);
            }
        }.execute();

        OpenFileAction.openFile(manifestFile.getVirtualFile().getPath(), module.getProject());
    }

    private static Manifest getAndroidManifest(final Module module) {
        final AndroidFacet facet = AndroidFacet.getInstance(module);
        if (facet == null) {
            return null;
        }

        final Manifest manifest = facet.getManifest();
        if (manifest == null) {
            return null;
        }

        return manifest;
    }
}
