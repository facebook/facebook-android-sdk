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
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.android.facet.AndroidFacet;

import java.util.ArrayList;
import java.util.List;

public interface Configurator extends Configurable {

    /**
     * @return the {@link Configurable}s that comprise this Configurator
     */
    Configurable[] getConfigurables();

    /**
     * @param project the {@link Project} to query Modules from.
     * @return the {@link AvailableModule.InstallationStatus} for each {@link AvailableModule}
     */
    default AvailableModule[] getAvailableModules(final Project project) {
        Configurable[] configurables = getConfigurables();
        Module[] allModules = ModuleManager.getInstance(project).getModules();

        List<AvailableModule> res = new ArrayList<>();
        for (Module module : allModules) {
            if (AndroidFacet.getInstance(module) != null) {
                res.add(new AvailableModule(module, configurables));
            }
        }
        return res.toArray(new AvailableModule[res.size()]);
    }

    default String getConfiguredValue(final Module module, final String resourceKey) {
        String val = "";
        for (Configurable configurable : getConfigurables()) {
            if (configurable instanceof AndroidResourcesConfigurator) {
                val = ((AndroidResourcesConfigurator) configurable)
                        .getResourceString(module, resourceKey);
            }
        }
        return val;
    }
}
