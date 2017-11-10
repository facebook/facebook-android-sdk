// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.plugin.ui;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.JBColor;

public enum Icon {
    one("/icons/1.png", null),
    two("/icons/2.png",  null),
    three("/icons/3.png",  null),
    four("/icons/4.png",  null),
    five("/icons/5.png",  null),
    six("/icons/6.png",  null),
    seven("/icons/7.png",  null),
    eight("/icons/8.png",  null),
    nine("/icons/9.png",  null),
    triangle_down("/icons/glyph-triangle-down.png",  null),
    triangle_right("/icons/glyph-triangle-right.png",  null),
    key("/icons/glyph-key.png",  null),
    play("/icons/play.png", null),
    places("/icons/glyph-nearby-places.png", null),
    arrow_left("/icons/glyph-nav-arrow-left.png", null),
    line_chart("/icons/glyph-line-chart.png", null),
    friend_share("/icons/glyph-friend-share.png", null),
    chevron_right("/icons/glyph-chevron-right.png", null),
    messenger("/icons/glyph-app-messenger.png", null),
    facebook_hollow("/icons/glyph-app-facebook-hollow.png", null),
    facebook_icon("/icons/glyph-app-facebook.png", null),
    facebook_banner("/icons/facebook_banner.png",null),
    facebook_icon_large("/icons/facebook-icon.png", null);

    private javax.swing.Icon light;
    private javax.swing.Icon dark;

    Icon(final String darkPath, final String lightPath) {
        this.dark = IconLoader.getIcon(darkPath);
        if (lightPath != null) {
            this.light = IconLoader.getIcon(lightPath);
        }
    }

    /**
     * @return The correct {@link javax.swing.Icon} based on whether the IDE is requesting
     * a light or a dark compatible image.
     */
    public javax.swing.Icon get() {
        return (JBColor.isBright() && (light != null)) ? light : dark;
    }
}
