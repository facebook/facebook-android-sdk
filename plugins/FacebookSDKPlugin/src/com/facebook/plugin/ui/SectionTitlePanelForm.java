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

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

public class SectionTitlePanelForm implements Viewable, LinkClickable {

    private JPanel mainPanel;
    private JLabel linkLabel;
    private JLabel descriptionLabel;
    private JLabel titleLabel;

    SectionTitlePanelForm(
            final String titleKey,
            final String descriptionKey,
            @Nonnull final String link) {
        this.titleLabel.setText(ResourceBundle.getBundle("values/strings").getString(titleKey));
        this.descriptionLabel.setText(ResourceBundle.getBundle("values/strings").getString(descriptionKey));

        this.linkLabel.addMouseListener(new ClickableMouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                clickLink(link);
            }
        });
    }

    @Override
    public JComponent getComponent() {
        return mainPanel;
    }
}
