// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.plugin.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FacebookPrerequisitesForm implements WizardStep, LinkClickable {

    private static final String STEP_1_LINK = "https://developers.facebook.com/apps/";

    private JPanel mainPanel;
    private JLabel step1Label;

    FacebookPrerequisitesForm() {
        MouseAdapter mouseAdapter = new ClickableMouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Component component = e.getComponent();
                if (component == step1Label) {
                    clickLink(STEP_1_LINK);
                }
            }
        };

        this.step1Label.addMouseListener(mouseAdapter);
    }

    @Override
    public void fillForm() { /* no op */ }

    @Override
    public boolean commitForm() {
        return true;
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }
}
