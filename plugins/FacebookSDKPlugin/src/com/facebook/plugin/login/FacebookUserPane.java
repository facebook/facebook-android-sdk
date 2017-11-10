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

package com.facebook.plugin.login;

import com.facebook.plugin.ui.ClickableMouseAdapter;
import com.google.gson.JsonObject;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.Image;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;

public class FacebookUserPane {
    private JPanel displayPanel;
    private JLabel profileImage;
    private JLabel descriptionLabel;
    private JButton signInOutButton;
    private JLabel credentialsPanel;
    private JPanel contentPanel;

    private JBPopup popup;

    private FacebookLogin fbLogin = FacebookLogin.getInstance();

    FacebookUserPane() {
        this.signInOutButton.addMouseListener(new ClickableMouseAdapter());
    }

    public static void popup(final JComponent source) {
        final JBPopup popup = new FacebookUserPane().getPopup();

        // Figure out the starting point for the popup
        final JComponent component = popup.getContent();
        final int startingPoint = (int)(source.getWidth() -
                component.getPreferredSize().getWidth());

        popup.show(new RelativePoint(source, new Point(startingPoint, source.getHeight() - 1)));
    }

    private JBPopup getPopup() {
        final JsonObject profile = fbLogin.getProfile();

        boolean loadDefault = true;

        if (profile != null) {
            try {
                final String description = new Formatter().format(
                        FacebookAssistantConstants.LOGGED_IN_DESCRIPTION,
                        profile.get(FacebookAssistantConstants.FIELD_NAME).getAsString(),
                        profile.get(FacebookAssistantConstants.FIELD_EMAIL).getAsString())
                        .toString();

                final String pictureUrl = profile
                        .get(FacebookAssistantConstants.FIELD_PICTURE)
                        .getAsJsonObject()
                        .get(FacebookAssistantConstants.FIELD_DATA)
                        .getAsJsonObject()
                        .get(FacebookAssistantConstants.FIELD_URL)
                        .getAsString();
                final Image image = ImageIO.read(new URL(pictureUrl));
                final Image scaledImage = image.getScaledInstance(72, 72, Image.SCALE_SMOOTH);

                credentialsPanel.setText(description);
                profileImage.setIcon(new ImageIcon(scaledImage));


                signInOutButton.setText(FacebookAssistantConstants.BUTTON_LOGOUT);
                signInOutButton.addActionListener(e -> logout());

                loadDefault = false;

            } catch (IOException | RuntimeException e) {
                loadDefault = true;
            }
        }

        descriptionLabel.setVisible(loadDefault);

        if (loadDefault) {
            signInOutButton.setText(FacebookAssistantConstants.BUTTON_LOGIN);
            signInOutButton.addActionListener(e -> login());
        } else {
            ((CardLayout)contentPanel.getLayout()).last(contentPanel);
        }

        popup = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(displayPanel, null)
                .createPopup();
        return popup;
    }

    private void login() {
        fbLogin.logIn(null);
        popup.cancel();
    }

    private void logout() {
        fbLogin.logOut();
        popup.cancel();
    }
}
