/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
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
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.appevents.internal;

import android.support.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ViewHierarchyConstants {

    public static final String ID_KEY = "id";
    public static final String CLASS_NAME_KEY = "classname";
    public static final String CLASS_TYPE_BITMASK_KEY = "classtypebitmask";
    public static final String TEXT_KEY = "text";
    public static final String DESC_KEY = "description";
    public static final String DIMENSION_KEY = "dimension";
    public static final String IS_USER_INPUT_KEY = "is_user_input";
    public static final String TAG_KEY = "tag";
    public static final String CHILDREN_VIEW_KEY = "childviews";
    public static final String HINT_KEY = "hint";
    public static final String DIMENSION_TOP_KEY = "top";
    public static final String DIMENSION_LEFT_KEY = "left";
    public static final String DIMENSION_WIDTH_KEY = "width";
    public static final String DIMENSION_HEIGHT_KEY = "height";
    public static final String DIMENSION_SCROLL_X_KEY = "scrollx";
    public static final String DIMENSION_SCROLL_Y_KEY = "scrolly";
    public static final String DIMENSION_VISIBILITY_KEY = "visibility";
    public static final String TEXT_SIZE = "font_size";
    public static final String TEXT_IS_BOLD = "is_bold";
    public static final String TEXT_IS_ITALIC = "is_italic";
    public static final String TEXT_STYLE = "text_style";
    public static final String ICON_BITMAP = "icon_image";
    public static final String INPUT_TYPE_KEY = "inputtype";
    public static final String IS_INTERACTED_KEY = "is_interacted";
    public static final String SCREEN_NAME_KEY = "screenname";
    public static final String VIEW_KEY = "view";

    public static final String ENGLISH = "ENGLISH";
    public static final String GERMAN = "GERMAN";
    public static final String SPANISH = "SPANISH";
    public static final String JAPANESE = "JAPANESE";

    public static final String VIEW_CONTENT = "VIEW_CONTENT";
    public static final String SEARCH = "SEARCH";
    public static final String ADD_TO_CART = "ADD_TO_CART";
    public static final String ADD_TO_WISHLIST = "ADD_TO_WISHLIST";
    public static final String INITIATE_CHECKOUT = "INITIATE_CHECKOUT";
    public static final String ADD_PAYMENT_INFO = "ADD_PAYMENT_INFO";
    public static final String PURCHASE = "PURCHASE";
    public static final String LEAD = "LEAD";
    public static final String COMPLETE_REGISTRATION = "COMPLETE_REGISTRATION";

    public static final String BUTTON_TEXT = "BUTTON_TEXT";
    public static final String PAGE_TITLE = "PAGE_TITLE";
    public static final String RESOLVED_DOCUMENT_LINK = "RESOLVED_DOCUMENT_LINK";
    public static final String BUTTON_ID = "BUTTON_ID";

    public static final int TEXTVIEW_BITMASK = 0;
    public static final int IMAGEVIEW_BITMASK = 1;
    public static final int BUTTON_BITMASK = 2;
    public static final int CLICKABLE_VIEW_BITMASK = 5;
    public static final int REACT_NATIVE_BUTTON_BITMASK = 6;
    public static final int ADAPTER_VIEW_ITEM_BITMASK = 9;
    public static final int LABEL_BITMASK = 10;
    public static final int INPUT_BITMASK = 11;
    public static final int PICKER_BITMASK = 12;
    public static final int SWITCH_BITMASK = 13;
    public static final int RADIO_GROUP_BITMASK = 14;
    public static final int CHECKBOX_BITMASK = 15;
    public static final int RATINGBAR_BITMASK = 16;
}
