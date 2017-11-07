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

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Generic View that contains a numbers title
 * with a single Button as its child
 * and handles updating it based on completed/available
 * states.
 */
abstract class TitleButtonForm implements Viewable {

    private JPanel mainPanel;
    protected JButton button;
    protected JLabel completedLabel;
    protected JPanel contentPanel;
    protected JLabel titleLabel;
    private JLabel stepNumberLabel;
    private JLabel unavailableLabel;

    TitleButtonForm(String titleKey, Icon icon, String buttonKey, String completedKey) {
        this.titleLabel.setText(ResourceBundle.getBundle("/values/strings").getString(titleKey));
        this.stepNumberLabel.setIcon(icon.get());
        this.button.setText(ResourceBundle.getBundle("/values/strings").getString(buttonKey));
        this.completedLabel.setText(ResourceBundle.getBundle("/values/strings").getString(completedKey));

        this.button.addMouseListener(new ClickableMouseAdapter());

        this.updateUI();
    }

    abstract boolean isCompletelyCompleted();

    abstract boolean isCompletelyUnavailable();

    /**
     * Causes the view UI to be updated.
     */
    void updateUI() {
        if (isCompletelyCompleted()) {
            ((CardLayout) contentPanel.getLayout()).show(contentPanel, "connected");
        } else if (isCompletelyUnavailable()) {
            ((CardLayout) contentPanel.getLayout()).show(contentPanel, "unavailable");
        } else {
            ((CardLayout) contentPanel.getLayout()).show(contentPanel, "connect");
        }
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }
}
