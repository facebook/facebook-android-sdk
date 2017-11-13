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

import com.facebook.plugin.ui.LinkClickable;
import com.google.common.base.Strings;
import com.intellij.openapi.wm.WindowManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FacebookAuthSpinner
        extends JDialog
        implements FacebookLoginEventListener, LinkClickable {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JLabel codeLabel;
    private JLabel fallbackLabel;

    private final FacebookLogin fbLogin = FacebookLogin.getInstance();

    public FacebookAuthSpinner() {
        setContentPane(contentPane);
        setModal(true);

        buttonCancel.addActionListener(e -> cancel());

        // call cancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                cancel();
            }
        });

        // call cancel() on ESCAPE
        contentPane.registerKeyboardAction(
                e -> cancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    public void popup() {
        if (fbLogin.getAccessToken() != null) {
            // Already logged in, no need to show spinner
            setVisible(false);
            return;
        }

        fbLogin.addEventListener(this);

        if (Strings.isNullOrEmpty(fbLogin.getUserCode()) ||
                LocalDateTime.now().isAfter(fbLogin.getExpiryTime())) {
            // No code was requested, or code is invalid, or expired
            fbLogin.startLoginAsync();
        }

        if (Strings.isNullOrEmpty(fbLogin.getUserCode())) {
            // Request failed
            cancel();
            return;
        }

        // Set the code's font size to be 10 points larger than the default font size in the IDE
        final Font labelFont = codeLabel.getFont();
        codeLabel.setFont(new Font(labelFont.getName(), Font.BOLD, labelFont.getSize() + 10));
        codeLabel.setText(fbLogin.getUserCode());

        fbLogin.waitForLoginAsync();

        // Wait a couple of seconds before trying to open the browser with the login URL
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    if (isVisible()) {
                        URI verificationUrl = FacebookLogin.getInstance().getVerificationUrl();
                        clickLink(verificationUrl);

                        MouseAdapter mouseAdapter = new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                                e.getComponent().setCursor(Cursor.getDefaultCursor());
                            }

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                clickLink(verificationUrl);
                            }
                        };

                        codeLabel.addMouseListener(mouseAdapter);
                        fallbackLabel.addMouseListener(mouseAdapter);
                    }
                },
                2,
                TimeUnit.SECONDS);

        pack();
        setLocationRelativeTo(WindowManager.getInstance().findVisibleFrame());
        setVisible(true);

        fbLogin.removeEventListener(this);
    }

    public void cancel() {
        fbLogin.removeEventListener(this);
        fbLogin.logOut();

        setVisible(false);
        dispose();
    }

    public void handleEvent(final FacebookLoginEventType eventType) {
        switch (eventType) {
            case LoginInitiated:
                popup();
                break;

            case LoginFailed:
            case LoginExpired:
                // TODO(T22379300): Show something about login failed on the dialog instead
                cancel();
                break;

            case LoggedOut:
                cancel();
                break;

            case LoggedIn:
                setVisible(false);
                break;
        }
    }
}
