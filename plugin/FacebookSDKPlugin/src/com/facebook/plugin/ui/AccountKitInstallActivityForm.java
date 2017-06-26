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

import com.facebook.plugin.config.AccountKitActivityConfigurator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AccountKitInstallActivityForm implements WizardStep {
    private final Project project;

    private JPanel mainPanel;
    private JComboBox activitySelector;
    private JTextField requestCodeField;

    AccountKitInstallActivityForm(final Project project) {
        this.project = project;
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    @Override
    public void fillForm() {
        activitySelector.setModel(new DefaultComboBoxModel<>(
                AccountKitActivityConfigurator.getActivityClasses(project)));
    }

    @Override
    public boolean commitForm() {
        AccountKitActivityConfigurator.AvailableActivity activity =
                (AccountKitActivityConfigurator.AvailableActivity
                        ) activitySelector.getSelectedItem();

        if ((activity == null) || (activity.getPsiClass() == null)) {
            Messages.showErrorDialog(
                project,
                "Please select a valid activity to continue.",
                "Install Failed");
            return false;
        }

        if (StringUtils.isBlank(requestCodeField.getText())) {
            Messages.showErrorDialog(
                project,
                "Request Code cannot be blank.",
                "Install Failed");
            return false;
        }

        AccountKitActivityConfigurator activityConfigurator;
        try {
            activityConfigurator = new AccountKitActivityConfigurator(requestCodeField.getText());
        } catch (Exception e) {
            Messages.showErrorDialog(
                project,
                "Failed to load the activity configurator code.",
                "Install Failed");
            return false;
        }

        if (!activityConfigurator.isConfigured(activity.getPsiClass())) {
            activityConfigurator.doConfigure(activity.getPsiClass());
        }

        return true;
    }
}
