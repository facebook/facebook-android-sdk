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

import com.intellij.CommonBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

abstract class WizardDialog extends DialogWrapper implements Viewable {

    private static final Dimension defaultMinSize = new Dimension(800, 400);
    private static final Dimension defaultMaxSize = new Dimension(800, 600);

    private JPanel mainPanel;

    private int currentStep = 0;
    private WizardStep[] wizardSteps;
    private WizardAction[] actions;

    WizardDialog(@Nonnull final Project project, @Nonnull String title, WizardStep[] wizardSteps) {
        super(project);

        this.wizardSteps = wizardSteps;

        setTitle(title);
        init();
        setStep(0, true);
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
        ArrayList<WizardAction> actionsList = new ArrayList<>(1);
        actionsList.add(new WizardAction(true));
        if (wizardSteps.length > 1) {
            actionsList.add(0, new WizardAction(false));
        }

        this.actions = actionsList.toArray(new WizardAction[actionsList.size()]);
        return actions;
    }

    private void updateButtonUI() {
        for (WizardAction action : actions) {
            action.updateUI();
        }
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

    private void setStep(final int newStep, final boolean skipConfirmCheck) {
        if ((newStep < 0) || (newStep >= wizardSteps.length) || (mainPanel == null)) {
            return;
        }

        if (!skipConfirmCheck && !wizardSteps[currentStep].commitForm()) {
            return;
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

        updateButtonUI();
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }

    private class WizardAction extends DialogWrapperAction {

        private static final String PREVIOUS = "< Previous";
        private static final String NEXT = "Next >";

        private WizardAction(final boolean isPrimaryAction) {
            super(isPrimaryAction ? NEXT : PREVIOUS);
            putValue(DEFAULT_ACTION, isPrimaryAction);
        }

        @Override
        protected void doAction(ActionEvent actionEvent) {
            boolean isPrimaryAction = (boolean) getValue(DEFAULT_ACTION);
            if (isPrimaryAction && currentStep == wizardSteps.length - 1) {
                close(OK_EXIT_CODE);
            } else {
                int delta = isPrimaryAction ? 1 : -1;
                setStep(currentStep + delta, delta < 0);
            }
        }

        void updateUI() {
            boolean isPrimaryAction = (boolean) getValue(DEFAULT_ACTION);
            if (!isPrimaryAction) {
                setEnabled(currentStep > 0);
            } else if (currentStep == wizardSteps.length - 1) {
                putValue(NAME, CommonBundle.getOkButtonText());
            } else {
                putValue(NAME, NEXT);
            }
        }
    }
}
