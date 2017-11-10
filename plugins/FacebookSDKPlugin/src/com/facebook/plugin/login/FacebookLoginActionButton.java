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

import com.facebook.plugin.ui.Icon;
import com.google.gson.JsonObject;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionButton;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;

public final class FacebookLoginActionButton
        extends ActionButton
        implements FacebookLoginEventListener {

    public FacebookLoginActionButton(
            final AnAction action,
            final Presentation presentation,
            final String place,
            final Dimension minimumSize) {

        super(action, presentation, place, minimumSize);
        FacebookLogin.getInstance().addEventListener(this);
        update();
    }

    private void update() {
        boolean loadDefault;

        try {
            loadDefault = !loadProfileInfo();
        } catch (IOException | RuntimeException e) {
            loadDefault = true;
        }

        if (loadDefault) {
            setToolTipText(FacebookAssistantConstants.LOG_IN_TEXT);
            myPresentation.setDescription(FacebookAssistantConstants.LOG_IN_TEXT);
            myPresentation.setIcon(Icon.facebook_icon.get());
        }
    }

    private boolean loadProfileInfo()
            throws IOException {

        final JsonObject profile = FacebookLogin.getInstance().getProfile();
        if (profile == null) {
            return false;
        }

        final String toolTip = new Formatter().format(
                FacebookAssistantConstants.LOGGED_IN_TEXT,
                profile.get(FacebookAssistantConstants.FIELD_NAME).getAsString())
                .toString();

        final String pictureUrl = profile
                .get(FacebookAssistantConstants.FIELD_PICTURE)
                .getAsJsonObject()
                .get(FacebookAssistantConstants.FIELD_DATA)
                .getAsJsonObject()
                .get(FacebookAssistantConstants.FIELD_URL)
                .getAsString();
        final Image image = ImageIO.read(new URL(pictureUrl));
        final Image scaledImage = image.getScaledInstance(16, 16, Image.SCALE_SMOOTH);

        setToolTipText(toolTip);
        myPresentation.setDescription(toolTip);
        myPresentation.setIcon(new ImageIcon(scaledImage));

        return true;
    }

    @Override
    public void handleEvent(final FacebookLoginEventType eventType) {
        if ((eventType == FacebookLoginEventType.LoggedIn) ||
                (eventType == FacebookLoginEventType.LoginExpired) ||
                (eventType == FacebookLoginEventType.LoggedOut)) {
            update();
        }
    }
}
