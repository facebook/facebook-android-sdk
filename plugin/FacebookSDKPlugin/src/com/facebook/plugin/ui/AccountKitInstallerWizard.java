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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

public class AccountKitInstallerWizard extends DialogWrapper {

    private static final Dimension defaultMinSize = new Dimension(800, 400);
    private static final Dimension defaultMaxSize = new Dimension(800, 600);

    private JPanel mainPanel;

    private WizardStep[] wizardSteps;
    private WizardAction previousAction;
    private WizardAction nextAction;
    private int currentStep = 0;

    public AccountKitInstallerWizard(final Project project) {
        super(project);

        wizardSteps = new WizardStep[]{
                new AccountKitPrerequisitesForm(),
                new AccountKitInstallDependenciesForm(project),
                new AccountKitInstallActivityForm(project),
                new AccountKitCompletedForm()
        };

        setTitle("Install AccountKit SDK");
        init();
        setStep(0, false);
    }

    @NotNull
    @Override
    protected Action[] createLeftSideActions() {
        return new Action[]{
                new DialogWrapperExitAction("Close", CLOSE_EXIT_CODE)
        };
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        previousAction = new WizardAction("< Previous", -1);
        nextAction = new WizardAction("Next >", 1);
        return new Action[]{
                previousAction,
                nextAction
        };
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        mainPanel = new JPanel(true);
        mainPanel.setMinimumSize(defaultMinSize);
        mainPanel.setMaximumSize(defaultMaxSize);
        mainPanel.setLayout(new GridLayout(0, 1));
        return mainPanel;
    }

    private void setStep(final int newStep, final boolean shouldCommit) {
        if ((newStep < 0) || (newStep >= wizardSteps.length) || (mainPanel == null)) {
            return;
        }

        if (shouldCommit) {
            if (!wizardSteps[currentStep].commitForm()) {
                return;
            }
        }

        currentStep = newStep;
        wizardSteps[newStep].fillForm();

        JComponent centerPanel = wizardSteps[newStep].getComponent();
        centerPanel.setMinimumSize(defaultMinSize);
        centerPanel.setMaximumSize(defaultMaxSize);
        centerPanel.invalidate();
        centerPanel.updateUI();

        mainPanel.removeAll();
        mainPanel.add(centerPanel);
        mainPanel.invalidate();
        mainPanel.updateUI();

        previousAction.setEnabled(currentStep > 0);
        nextAction.setEnabled(currentStep < wizardSteps.length - 1);
    }

    protected final class WizardAction extends DialogWrapperAction {
        final int delta;

        private WizardAction(@NotNull final String name, final int delta) {
            super(name);
            this.delta = delta;
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            setStep(currentStep + delta, delta > 0);
        }
    }
}
