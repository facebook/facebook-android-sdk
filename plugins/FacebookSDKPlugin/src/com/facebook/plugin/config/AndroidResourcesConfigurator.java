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
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AndroidResourcesConfigurator implements Configurable {

    private static final String STRING_TAG = "<string/>";
    private static final String NAME_ATTR = "name";

    private static final String EMPTY_VALUE = "EMPTY";

    public static final String FB_APP_ID = "fb_app_id";
    public static final String FB_APP_NAME = "fb_app_name";
    public static final String FB_LOGIN_PROTOCOL_SCHEME = "fb_login_protocol_scheme";
    public static final String AK_CLIENT_TOKEN = "ak_client_token";
    public static final String AK_LOGIN_PROTOCOL_SCHEME = "ak_login_protocol_scheme";

    private final Map<String, String> resourceStrings = new LinkedHashMap<>();

    /**
     * @param stringResources an array of String resource IDs that you plan on configuring.
     *        I.E. {@link #FB_APP_ID}, {@link #FB_APP_NAME}, or {@link #AK_CLIENT_TOKEN}
     */
    AndroidResourcesConfigurator(String[] stringResources) {
        for (String resource: stringResources) {
            addResourceString(resource, EMPTY_VALUE);
        }
    }

    AndroidResourcesConfigurator addResourceString(final String name, final String value) {
        if (!StringUtils.isBlank(name) && !StringUtils.isBlank(value)) {
            resourceStrings.put(name, value);
        }
        return this;
    }

    String getResourceString(final Module module, final String name) {
        ResourceItem item = getResourceItem(module, name);
        return item == null ? "" : item.getResourceValue(false).getValue();
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

    private ResourceItem getResourceItem(final Module module, final String resourceName) {
        LocalResourceRepository resourceRepository =
                ModuleResourceRepository.getModuleResources(module, false);
        if (resourceRepository == null) {
            return null;
        }

        List<ResourceItem> resourceItems =
                resourceRepository.getResourceItem(ResourceType.STRING, resourceName);
        return resourceItems.size() != 1 ? null : resourceItems.get(0);
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
        XmlFile resourceFile = (XmlFile) ModuleResourceRepository.getItemPsiFile(
                module.getProject(),
                firstStringResource);
        final XmlElementFactory elementFactory =
                XmlElementFactory.getInstance(module.getProject());

        new WriteCommandAction(resourceFile.getProject()) {
            @Override
            protected void run(@NotNull Result result) {

                Map<String, String> workingStringResources =
                    getValidStringResources(resourceStrings);
                if (workingStringResources.isEmpty()) {
                    return;
                }

                for (String resourceName : workingStringResources.keySet()) {
                    ResourceItem resourceItem = getResourceItem(module, resourceName);

                    XmlTag newResourceTag = elementFactory.createTagFromText(STRING_TAG);
                    newResourceTag.setAttribute(NAME_ATTR, resourceName);
                    newResourceTag.add(elementFactory.createDisplayText(
                            workingStringResources.get(resourceName)));

                    if (resourceItem == null) {
                        resourceFile.getRootTag().addSubTag(newResourceTag, true);
                    } else {
                        ModuleResourceRepository
                                .getItemTag(module.getProject(), resourceItem)
                                .replace(newResourceTag);
                    }
                }

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(resourceFile);
                resourceRepository.invalidateResourceDirs();
                resourceRepository.sync();
            }
        }.execute();

        OpenFileAction.openFile(resourceFile.getVirtualFile().getPath(), module.getProject());
    }

    /**
     * Because we use {@link #EMPTY_VALUE} as a placeholder to enable
     * checking if this is initially in a configured state this is used
     * to sanatize the working Map
     * @param stringResources the {@link Map<String, String>} to sanitize
     * @return a copy of the provided Map with any EMPTY_VALUEs removed.
     */
    private static Map<String, String> getValidStringResources(
        Map<String, String> stringResources) {
        Map<String, String> workingStrings = new HashMap<>(stringResources);
        for (String key : workingStrings.keySet()) {
            if (workingStrings.get(key).contentEquals(EMPTY_VALUE)) {
                workingStrings.remove(key);
            }
        }
        return workingStrings;
    }
}
