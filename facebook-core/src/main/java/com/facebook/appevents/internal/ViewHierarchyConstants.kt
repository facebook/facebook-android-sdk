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
package com.facebook.appevents.internal

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object ViewHierarchyConstants {
  const val ID_KEY = "id"
  const val CLASS_NAME_KEY = "classname"
  const val CLASS_TYPE_BITMASK_KEY = "classtypebitmask"
  const val TEXT_KEY = "text"
  const val DESC_KEY = "description"
  const val DIMENSION_KEY = "dimension"
  const val IS_USER_INPUT_KEY = "is_user_input"
  const val TAG_KEY = "tag"
  const val CHILDREN_VIEW_KEY = "childviews"
  const val HINT_KEY = "hint"
  const val DIMENSION_TOP_KEY = "top"
  const val DIMENSION_LEFT_KEY = "left"
  const val DIMENSION_WIDTH_KEY = "width"
  const val DIMENSION_HEIGHT_KEY = "height"
  const val DIMENSION_SCROLL_X_KEY = "scrollx"
  const val DIMENSION_SCROLL_Y_KEY = "scrolly"
  const val DIMENSION_VISIBILITY_KEY = "visibility"
  const val TEXT_SIZE = "font_size"
  const val TEXT_IS_BOLD = "is_bold"
  const val TEXT_IS_ITALIC = "is_italic"
  const val TEXT_STYLE = "text_style"
  const val ICON_BITMAP = "icon_image"
  const val INPUT_TYPE_KEY = "inputtype"
  const val IS_INTERACTED_KEY = "is_interacted"
  const val SCREEN_NAME_KEY = "screenname"
  const val VIEW_KEY = "view"
  const val ENGLISH = "ENGLISH"
  const val GERMAN = "GERMAN"
  const val SPANISH = "SPANISH"
  const val JAPANESE = "JAPANESE"
  const val VIEW_CONTENT = "VIEW_CONTENT"
  const val SEARCH = "SEARCH"
  const val ADD_TO_CART = "ADD_TO_CART"
  const val ADD_TO_WISHLIST = "ADD_TO_WISHLIST"
  const val INITIATE_CHECKOUT = "INITIATE_CHECKOUT"
  const val ADD_PAYMENT_INFO = "ADD_PAYMENT_INFO"
  const val PURCHASE = "PURCHASE"
  const val LEAD = "LEAD"
  const val COMPLETE_REGISTRATION = "COMPLETE_REGISTRATION"
  const val BUTTON_TEXT = "BUTTON_TEXT"
  const val PAGE_TITLE = "PAGE_TITLE"
  const val RESOLVED_DOCUMENT_LINK = "RESOLVED_DOCUMENT_LINK"
  const val BUTTON_ID = "BUTTON_ID"
  const val TEXTVIEW_BITMASK = 0
  const val IMAGEVIEW_BITMASK = 1
  const val BUTTON_BITMASK = 2
  const val CLICKABLE_VIEW_BITMASK = 5
  const val REACT_NATIVE_BUTTON_BITMASK = 6
  const val ADAPTER_VIEW_ITEM_BITMASK = 9
  const val LABEL_BITMASK = 10
  const val INPUT_BITMASK = 11
  const val PICKER_BITMASK = 12
  const val SWITCH_BITMASK = 13
  const val RADIO_GROUP_BITMASK = 14
  const val CHECKBOX_BITMASK = 15
  const val RATINGBAR_BITMASK = 16
}
