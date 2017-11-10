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

import com.facebook.plugin.config.AccountKitModuleConfigurator;
import com.facebook.plugin.config.AndroidResourcesConfigurator;
import com.facebook.plugin.config.AvailableModule;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AccountKitInstallDependenciesForm implements WizardStep {

    private static final String AVAILABLE_MODULE_CHANGED_ACTION = "availableModuleChangedAction";

    private final Project project;
    private final AccountKitModuleConfigurator moduleConfigurator;

    private JPanel mainPanel;
    private JComboBox<AvailableModule> moduleSelector;
    private JTextField appNameField;
    private JTextField appIdField;
    private JTextField clientTokenField;

    AccountKitInstallDependenciesForm(
            final Project project,
            final AccountKitModuleConfigurator configurator) {
        this.project = project;
        this.moduleConfigurator = configurator;
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void fillForm() {
        moduleSelector.setModel(new DefaultComboBoxModel<>(
                moduleConfigurator.getAvailableModules(project)));

        moduleSelector.setActionCommand(AVAILABLE_MODULE_CHANGED_ACTION);

        moduleSelector.addActionListener(e -> {
            if (AVAILABLE_MODULE_CHANGED_ACTION.contentEquals(e.getActionCommand())) {
                Module module = ((AvailableModule)moduleSelector.getSelectedItem())
                        .getModule();
                appIdField.setText(
                        moduleConfigurator.getConfiguredValue(
                                module,
                                AndroidResourcesConfigurator.FB_APP_ID));

                appNameField.setText(
                        moduleConfigurator.getConfiguredValue(
                                module,
                                AndroidResourcesConfigurator.FB_APP_NAME));

                clientTokenField.setText(
                        moduleConfigurator.getConfiguredValue(
                                module,
                                AndroidResourcesConfigurator.AK_CLIENT_TOKEN));
            }
        });
        moduleSelector.setSelectedIndex(0);
    }

    @Override
    public boolean commitForm() {
        AvailableModule selectedModule =
                (AvailableModule) moduleSelector.getSelectedItem();

        if (selectedModule == null) {
            Messages.showErrorDialog(
                project,
                "Please select a module to continue.",
                "Install Failed");
            return false;
        }

        if (selectedModule.isUnavailable()) {
            Messages.showErrorDialog(
                project,
                "Cannot install AccountKit on module " +
                  selectedModule.getModule().getName() +
                  " because it either is not an Android module " +
                  "or is not built using Gradle.",
                "Install Failed");
            return false;
        }

        if (StringUtils.isBlank(appNameField.getText())) {
            Messages.showErrorDialog(
                project,
                "App Name cannot be blank.",
                "Install Failed");
            return false;
        }

        if (StringUtils.isBlank(appIdField.getText()) ||
                !StringUtils.isNumeric(appIdField.getText())) {
            Messages.showErrorDialog(
                project,
                "App Id must be a valid number.",
                "Install Failed");
            return false;
        }

        if (StringUtils.isBlank(clientTokenField.getText())) {
            Messages.showErrorDialog(
                project,
                "Client Token cannot be blank.",
                "Install Failed");
            return false;
        }

        moduleConfigurator.set(
                        appNameField.getText(),
                        appIdField.getText(),
                        clientTokenField.getText());

        try {
            moduleConfigurator.doConfigure(selectedModule.getModule());
        } catch (Exception e) {
            Messages.showErrorDialog(
                    project,
                    "Something went wrong while installing module " +
                            selectedModule.getModule().getName() +
                            ": \n" + e.getMessage(),
                    "Install Failed");
        }

        return true;
    }
}
