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

package com.facebook.plugin.config;

import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.android.dom.manifest.Activity;

import javax.annotation.Nullable;

public class AndroidActivity {

    static final String METADATA_TAG = "<meta-data/>";
    static final String PERMISSION_TAG = "<uses-permission/>";
    private static final String INTENT_FILTER_TAG = "<intent-filter/>";
    private static final String ACTION_TAG = "<action/>";
    private static final String CATEGORY_TAG = "<category/>";
    private static final String DATA_TAG = "<data/>";

    static final String NAME_ATTR = "android:name";
    static final String VALUE_ATTR = "android:value";
    private static final String CONFIG_CHANGES_ATTR = "android:configChanges";
    private static final String LABEL_ATTR = "android:label";
    private static final String EXPORTED_ATTR = "android:exported";
    private static final String SCHEME_ATTR = "android:scheme";
    private static final String AUTHORITIES_ATTR = "android:authorities";

    public static final String FB_SCHEME_VALUE = "@string/fb_login_protocol_scheme";
    public static final String AK_SCHEME_VALUE = "@string/ak_login_protocol_scheme";
    private static final String CONFIG_CHANGES_VALUE =
            "keyboard|keyboardHidden|screenLayout|screenSize|orientation";
    private static final String INTENT_ACTION_VIEW_VALUE = "android.intent.action.VIEW";
    private static final String CATEGORY_DEFAULT_VALUE = "android.intent.category.DEFAULT";
    private static final String CATEGORY_BROWSABLE_VALUE = "android.intent.category.BROWSABLE";
    private static final String AUTHORITIES_VALUE = "com.facebook.app.FacebookContentProvider%1$s";

    private final String name;
    private final boolean configChanges;
    private final @Nullable String label;
    private final @Nullable Boolean exported;
    private final @Nullable String intentFiltersScheme;
    private final boolean authorities;

    public AndroidActivity(
            final String name,
            final  boolean configChanges,
            final @Nullable  String label,
            final @Nullable Boolean exported,
            final @Nullable String intentFiltersScheme,
            final boolean authorities) {
        this.name = name;
        this.configChanges = configChanges;
        this.label = label;
        this.exported = exported;
        this.intentFiltersScheme = intentFiltersScheme;
        this.authorities = authorities;
    }

    void create(final XmlElementFactory elementFactory, final Activity activity, final String appId) {
        activity.getActivityClass().setStringValue(this.name);
        XmlTag newActivityTag = activity.getXmlTag();

        if (this.configChanges) {
            newActivityTag.setAttribute(CONFIG_CHANGES_ATTR, CONFIG_CHANGES_VALUE);
        }

        if (this.label != null) {
            newActivityTag.setAttribute(LABEL_ATTR, this.label);
        }

        if (this.authorities && appId != null) {
            newActivityTag.setAttribute(AUTHORITIES_ATTR, String.format(AUTHORITIES_VALUE, appId));
        }

        if (this.exported != null) {
            newActivityTag.setAttribute(EXPORTED_ATTR, this.exported.toString());
        }

        if (this.intentFiltersScheme != null) {
            XmlTag intentFilters = elementFactory.createTagFromText(INTENT_FILTER_TAG);

            XmlTag childTag = elementFactory.createTagFromText(ACTION_TAG);
            childTag.setAttribute(AndroidActivity.NAME_ATTR, INTENT_ACTION_VIEW_VALUE);
            intentFilters.addSubTag(childTag, false);

            childTag = elementFactory.createTagFromText(CATEGORY_TAG);
            childTag.setAttribute(AndroidActivity.NAME_ATTR, CATEGORY_DEFAULT_VALUE);
            intentFilters.addSubTag(childTag, false);

            childTag = elementFactory.createTagFromText(CATEGORY_TAG);
            childTag.setAttribute(AndroidActivity.NAME_ATTR, CATEGORY_BROWSABLE_VALUE);
            intentFilters.addSubTag(childTag, false);

            childTag = elementFactory.createTagFromText(DATA_TAG);
            childTag.setAttribute(SCHEME_ATTR, this.intentFiltersScheme);
            intentFilters.addSubTag(childTag, false);

            newActivityTag.addSubTag(intentFilters, false);
        }
    }

    String getName() {
        return this.name;
    }
}
