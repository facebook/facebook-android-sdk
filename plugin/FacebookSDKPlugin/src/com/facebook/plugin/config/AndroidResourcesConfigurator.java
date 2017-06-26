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

import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceType;
import com.android.tools.idea.res.LocalResourceRepository;
import com.android.tools.idea.res.ModuleResourceRepository;
import com.google.common.collect.ListMultimap;
import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AndroidResourcesConfigurator implements Configurator {

    private static final String STRING_TAG = "<string/>";
    private static final String NAME_ATTR = "name";

    private final Map<String, String> resourceStrings = new LinkedHashMap<>();

    AndroidResourcesConfigurator addResourceString(final String name, final String value) {
        if (!StringUtils.isBlank(name) && !StringUtils.isBlank(value)) {
            resourceStrings.put(name, value);
        }
        return this;
    }

    @Override
    public boolean isConfigured(final Module module) {
        LocalResourceRepository resourceRepository =
                ModuleResourceRepository.getModuleResources(module, false);
        if (resourceRepository == null) {
            return false;
        }

        for (String resourceName : resourceStrings.keySet()) {
            List<ResourceItem> resourceItems =
                    resourceRepository.getResourceItem(ResourceType.STRING, resourceName);
            if (resourceItems.size() == 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void doConfigure(final Module module) {
        LocalResourceRepository resourceRepository =
                ModuleResourceRepository.getModuleResources(module, true);
        if (resourceRepository == null) {
            return;
        }

        ListMultimap<String, ResourceItem> stringResources =
                resourceRepository.getItems().get(ResourceType.STRING);
        if (stringResources.size() == 0) {
            return;
        }

        ResourceItem firstStringResource = stringResources.values().iterator().next();
        XmlTag resourceTag = ModuleResourceRepository.getItemTag(
                module.getProject(),
                firstStringResource);
        PsiFile resourceFile = ModuleResourceRepository.getItemPsiFile(
                module.getProject(),
                firstStringResource);
        final XmlElementFactory elementFactory =
                XmlElementFactory.getInstance(module.getProject());

        new WriteCommandAction(resourceFile.getProject()) {
            @Override
            protected void run(@NotNull Result result) {

                for (String resourceName : resourceStrings.keySet()) {
                    List<ResourceItem> resourceItems =
                            resourceRepository.getResourceItem(ResourceType.STRING, resourceName);
                    if (resourceItems.size() == 0) {
                        XmlTag newResourceTag = elementFactory.createTagFromText(STRING_TAG);
                        newResourceTag.setAttribute(NAME_ATTR, resourceName);
                        newResourceTag.add(elementFactory.createDisplayText(
                                resourceStrings.get(resourceName)));

                        resourceFile.addAfter(newResourceTag, resourceTag);
                    }
                }

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(resourceFile);
                resourceRepository.invalidateResourceDirs();
                resourceRepository.sync();
            }
        }.execute();

        OpenFileAction.openFile(resourceFile.getVirtualFile().getPath(), module.getProject());
    }
}
