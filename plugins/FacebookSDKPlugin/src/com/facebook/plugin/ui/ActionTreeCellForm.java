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

import com.google.common.base.Strings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.UIUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ResourceBundle;

public class ActionTreeCellForm implements Viewable, LinkClickable {

    // region static

    static ActionTreeCellForm[] getActions(Project project, FbToolWindowForm toolWindow) {
        return new ActionTreeCellForm[] {
                new ActionTreeCellForm(
                        "facebookCoreSDK",
                        "facebookCoreDescription",
                        "https://developers.facebook.com/docs/app-events/android",
                        Icon.line_chart,
                        getParentAction(new ActionTreeCellForm(
                                "integrateFacebookCoreSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "facebookCoreSDK",
                                        new SectionTitlePanelForm(
                                                "integrateFacebookCoreSDK",
                                                "facebookAnalyticsSectionDescription",
                                                "https://developers.facebook.com/docs/app-events/android"),
                                        new InstallerForm(
                                                new FacebookCoreIntegrationForm(project),
                                                new FacebookCoreCodeForm()))))),
                new ActionTreeCellForm(
                        "facebookLoginSDK",
                        "facebookLoginDescription",
                        "https://developers.facebook.com/docs/facebook-login/android",
                        Icon.facebook_hollow,
                        getParentAction(new ActionTreeCellForm(
                                "integrateFacebookLoginSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "facebookLoginSDK",
                                        new SectionTitlePanelForm(
                                                "facebookLoginSDK",
                                                "facebookLoginSectionDescription",
                                                "https://developers.facebook.com/docs/facebook-login/android"),
                                        new InstallerForm(
                                                new FacebookLoginIntegrationForm(project),
                                                new FacebookLoginCodeForm()))))),
                new ActionTreeCellForm(
                        "facebookShareSDK",
                        "facebookShareDescription",
                        "https://developers.facebook.com/docs/sharing/android",
                        Icon.friend_share,
                        getParentAction(new ActionTreeCellForm(
                                "integrateFacebookShareSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "facebookShareSDK",
                                        new SectionTitlePanelForm(
                                                "facebookShareSDK",
                                                "facebookShareSectionDescription",
                                                "https://developers.facebook.com/docs/sharing/android"),
                                        new InstallerForm(
                                                new FacebookShareIntegrationForm(project),
                                                new FacebookShareCodeForm()))))),
                new ActionTreeCellForm(
                        "facebookPlacesSDK",
                        "facebookPlacesDescription",
                        "https://developers.facebook.com/docs/places/android",
                        Icon.places,
                        getParentAction(new ActionTreeCellForm(
                                "integrateFacebookPlacesSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "facebookPlacesSDK",
                                        new SectionTitlePanelForm(
                                                "facebookPlacesSDK",
                                                "facebookPlacesSectionDescription",
                                                "https://developers.facebook.com/docs/places/android"),
                                        new InstallerForm(
                                                new FacebookPlacesIntegrationForm(project),
                                                new FacebookPlacesCodeForm()))))),
                new ActionTreeCellForm(
                        "facebookMessengerSDK",
                        "facebookMessengerDescription",
                        "https://developers.facebook.com/docs/messenger/android",
                        Icon.messenger,
                        getParentAction(new ActionTreeCellForm(
                                "integrateFacebookMessengerSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "facebookMessengerSDK",
                                        new SectionTitlePanelForm(
                                                "facebookMessengerSDK",
                                                "facebookMessengerSectionDescription",
                                                "https://developers.facebook.com/docs/messenger/android"),
                                        new InstallerForm(
                                                new FacebookMessengerIntegrationForm(project),
                                                new FacebookMessengerCodeForm()))))),
                new ActionTreeCellForm(
                        "accountKitSDK",
                        "accountKitDescription",
                        "https://developers.facebook.com/docs/accountkit/android",
                        Icon.key,
                        getParentAction(new ActionTreeCellForm(
                                "integrateAccountKitSDKLink",
                                null,
                                null,
                                Icon.play,
                                (self) -> toolWindow.addView(
                                        "accountKitSDK",
                                        new SectionTitlePanelForm(
                                                "accountKitSDK",
                                                "accountKitSectionDescription",
                                                "https://developers.facebook.com/docs/accountkit/android"),
                                        new InstallerForm(
                                                new AccountKitIntegrationForm(project),
                                                new AccountKitCodeForm())))))
        };
    }

    // endregion

    private JLabel title;
    private JLabel description;
    private JPanel panel;
    private JLabel descriptionLink;
    private JPanel childPanel;
    private JLabel arrowLabel;
    private boolean expanded;

    private interface IAction {
        void execute(ActionTreeCellForm self);
    }

    private interface IParentAction extends IAction {}
    private static IParentAction getParentAction(ActionTreeCellForm childAction) {
        return (self) -> {
            if (self.expanded) {
                self.contract();
            } else {
                self.expand(childAction);
            }
        };
    }

    private ActionTreeCellForm(
            @Nonnull String titleKey,
            @Nullable String descriptionKey,
            @Nullable String link,
            @Nullable Icon icon,
            @Nonnull IAction action) {
        if (Strings.isNullOrEmpty(titleKey)) {
            this.title.setVisible(false);
        } else {
            this.title.setText(ResourceBundle.getBundle("values/strings").getString(titleKey));
            this.title.setIcon(icon.get());
            this.title.setVisible(true);
        }

        if (Strings.isNullOrEmpty(descriptionKey)) {
            this.description.setVisible(false);
        } else {
            this.description.setText(ResourceBundle.getBundle("values/strings").getString(descriptionKey));
            this.description.setVisible(true);
        }

        if (Strings.isNullOrEmpty(link)) {
            this.descriptionLink.setVisible(false);
        } else {
            this.descriptionLink.setText(ResourceBundle.getBundle("values/strings").getString("moreInfo"));
            this.descriptionLink.setVisible(true);
        }

        this.expanded = false;

        getComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (action instanceof IParentAction) {
                    JPanel hovered = (JPanel) e.getComponent();
                    for (Component c : hovered.getComponents()) {
                        c.setBackground(JBColor.background().darker());
                    }
                    hovered.setBackground(JBColor.background().darker());
                } else {
                    e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                e.getComponent().setCursor(Cursor.getDefaultCursor());
                if (action instanceof IParentAction) {
                    JPanel unhovered = (JPanel) e.getComponent();
                    for (Component c : unhovered.getComponents()) {
                        c.setBackground(UIUtil.getLabelBackground());
                    }
                    unhovered.setBackground(UIUtil.getLabelBackground());
                } else {
                    e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                action.execute(ActionTreeCellForm.this);
            }
        });

        this.descriptionLink.addMouseListener(new ClickableMouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                clickLink(link);
            }
        });

        this.arrowLabel.setIcon(Icon.triangle_right.get());
        if (!(action instanceof IParentAction)) {
            this.arrowLabel.setVisible(false);
        }
    }

    /**
     * Expands (adds) the child view
     */
    private void expand(ActionTreeCellForm childAction) {
        final GridConstraints gridConstraints = new GridConstraints();
        gridConstraints.setRow(0);
        gridConstraints.setColumn(0);
        gridConstraints.setFill(GridConstraints.FILL_BOTH);
        gridConstraints.setHSizePolicy(GridConstraints.ALIGN_FILL);
        gridConstraints.setVSizePolicy(GridConstraints.ALIGN_FILL);
        this.arrowLabel.setIcon(Icon.triangle_down.get());
        childPanel.add(childAction.getComponent(), gridConstraints);
        childPanel.setVisible(true);
        childPanel.updateUI();
        expanded = true;
    }

    /**
     * Contracts (removes) the child view
     */
    private void contract() {
        this.arrowLabel.setIcon(Icon.triangle_right.get());
        childPanel.removeAll();
        childPanel.setVisible(false);
        childPanel.updateUI();
        expanded = false;
    }

    @Override
    public JComponent getComponent() {
        return this.panel;
    }
}
