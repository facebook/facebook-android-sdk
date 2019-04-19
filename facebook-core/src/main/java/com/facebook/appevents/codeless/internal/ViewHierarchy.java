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

package com.facebook.appevents.codeless.internal;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingChild;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import com.facebook.internal.Utility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ViewHierarchy {
    private static final String TAG = ViewHierarchy.class.getCanonicalName();
    private static final String GET_ACCESSIBILITY_METHOD = "getAccessibilityDelegate";

    private static final String ID_KEY = "id";
    private static final String CLASS_NAME_KEY = "classname";
    private static final String CLASS_TYPE_BITMASK_KEY = "classtypebitmask";
    private static final String TEXT_KEY = "text";
    private static final String DESC_KEY = "description";
    private static final String DIMENSION_KEY = "dimension";
    private static final String IS_USER_INPUT_KEY = "is_user_input";
    private static final String TAG_KEY = "tag";
    private static final String CHILDREN_VIEW_KEY = "childviews";
    private static final String HINT_KEY = "hint";
    private static final String DIMENSION_TOP_KEY = "top";
    private static final String DIMENSION_LEFT_KEY = "left";
    private static final String DIMENSION_WIDTH_KEY = "width";
    private static final String DIMENSION_HEIGHT_KEY = "height";
    private static final String DIMENSION_SCROLL_X_KEY = "scrollx";
    private static final String DIMENSION_SCROLL_Y_KEY = "scrolly";
    private static final String DIMENSION_VISIBILITY_KEY = "visibility";
    private static final String TEXT_SIZE = "font_size";
    private static final String TEXT_IS_BOLD = "is_bold";
    private static final String TEXT_IS_ITALIC = "is_italic";
    private static final String TEXT_STYLE = "text_style";
    private static final String ICON_BITMAP = "icon_image";

    // React Native class names
    private static final String CLASS_RCTROOTVIEW =
            "com.facebook.react.ReactRootView";
    private static final String CLASS_RCTTEXTVIEW =
            "com.facebook.react.views.view.ReactTextView";
    private static final String CLASS_RCTVIEWGROUP =
            "com.facebook.react.views.view.ReactViewGroup";
    private static final String CLASS_TOUCHTARGETHELPER =
            "com.facebook.react.uimanager.TouchTargetHelper";

    // TouchTargetHelper method names
    private static final String METHOD_FIND_TOUCHTARGET_VIEW = "findTouchTargetView";

    private static final int TEXTVIEW_BITMASK = 0;
    private static final int IMAGEVIEW_BITMASK = 1;
    private static final int BUTTON_BITMASK = 2;
    private static final int CLICKABLE_VIEW_BITMASK = 5;
    private static final int REACT_NATIVE_BUTTON_BITMASK = 6;
    private static final int ADAPTER_VIEW_ITEM_BITMASK = 9;
    private static final int LABEL_BITMASK = 10;
    private static final int INPUT_BITMASK = 11;
    private static final int PICKER_BITMASK = 12;
    private static final int SWITCH_BITMASK = 13;
    private static final int RADIO_GROUP_BITMASK = 14;
    private static final int CHECKBOX_BITMASK = 15;
    private static final int RATINGBAR_BITMASK = 16;

    private static final int ICON_MAX_EDGE_LENGTH = 44;

    private static WeakReference<View> RCTRootViewReference = new WeakReference<>(null);
    private static @Nullable Method methodFindTouchTargetView = null;

    @Nullable
    public static ViewGroup getParentOfView(View view) {
        if (null == view) {
            return null;
        }

        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            return (ViewGroup)parent;
        }

        return null;
    }

    public static List<View> getChildrenOfView(View view) {
        ArrayList<View> children = new ArrayList<>();

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup)view;
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++) {
                children.add(viewGroup.getChildAt(i));
            }
        }

        return children;
    }

    public static JSONObject setBasicInfoOfView(View view, JSONObject json) {
        try {
            String text = getTextOfView(view);
            String hint = getHintOfView(view);
            Object tag = view.getTag();
            CharSequence description = view.getContentDescription();

            json.put(CLASS_NAME_KEY, view.getClass().getCanonicalName());
            json.put(CLASS_TYPE_BITMASK_KEY, getClassTypeBitmask(view));
            json.put(ID_KEY, view.getId());
            if (!SensitiveUserDataUtils.isSensitiveUserData(view)) {
                json.put(TEXT_KEY,
                        Utility.coerceValueIfNullOrEmpty(Utility.sha256hash(text), ""));
            } else {
                json.put(TEXT_KEY, "");
                json.put(IS_USER_INPUT_KEY, true);
            }
            json.put(HINT_KEY,
                    Utility.coerceValueIfNullOrEmpty(Utility.sha256hash(hint), ""));
            if (tag != null) {
                json.put(TAG_KEY,
                        Utility.coerceValueIfNullOrEmpty(Utility.sha256hash(tag.toString()), ""));
            }
            if (description != null) {
                json.put(DESC_KEY,
                        Utility.coerceValueIfNullOrEmpty(Utility.sha256hash(description.toString()), ""));
            }
            JSONObject dimension = getDimensionOfView(view);
            json.put(DIMENSION_KEY, dimension);
        } catch (JSONException e) {
            Utility.logd(TAG, e);
        }

        return json;
    }

    public static JSONObject setAppearanceOfView(View view, JSONObject json, float displayDensity) {
        try {
            JSONObject textStyle = new JSONObject();
            if (view instanceof TextView) {
                TextView textView = (TextView)view;
                Typeface typeface = textView.getTypeface();
                if (typeface != null) {
                    textStyle.put(TEXT_SIZE, textView.getTextSize());
                    textStyle.put(TEXT_IS_BOLD, typeface.isBold());
                    textStyle.put(TEXT_IS_ITALIC, typeface.isItalic());
                    json.put(TEXT_STYLE, textStyle);
                }
            }
            if (view instanceof ImageView) {
                Drawable drawable = ((ImageView) view).getDrawable();
                if (drawable instanceof BitmapDrawable) {
                    if (view.getHeight() / displayDensity <= ICON_MAX_EDGE_LENGTH &&
                            view.getWidth() / displayDensity <= ICON_MAX_EDGE_LENGTH) {
                        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                        if (bitmap != null) {
                            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
                            byte[] byteArray = byteArrayOutputStream.toByteArray();
                            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
                            json.put(ICON_BITMAP, encoded);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Utility.logd(TAG, e);
        }

        return json;
    }



    public static JSONObject getDictionaryOfView(View view) {
        if (view.getClass().getName().equals(CLASS_RCTROOTVIEW)) {
            RCTRootViewReference = new WeakReference<>(view);
        }

        JSONObject json = new JSONObject();

        try {
            json = setBasicInfoOfView(view, json);

            JSONArray childViews = new JSONArray();
            List<View> children = getChildrenOfView(view);
            for (int i = 0; i < children.size(); i++) {
                View child = children.get(i);
                JSONObject childInfo = getDictionaryOfView(child);
                childViews.put(childInfo);
            }
            json.put(CHILDREN_VIEW_KEY, childViews);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSONObject for view.", e);
        }

        return json;
    }

    private static int getClassTypeBitmask(View view) {
        int bitmask = 0;
        if (view instanceof ImageView) {
            bitmask |= (1 << IMAGEVIEW_BITMASK);
        }

        if (isClickableView(view)) {
            bitmask |= (1 << CLICKABLE_VIEW_BITMASK);
        }
        if (isAdapterViewItem(view)) {
            bitmask |= (1 << ADAPTER_VIEW_ITEM_BITMASK);
        }

        if (view instanceof TextView) {
            bitmask |= (1 << LABEL_BITMASK);
            bitmask |= (1 << TEXTVIEW_BITMASK);

            if (view instanceof Button) {
                bitmask |= (1 << BUTTON_BITMASK);

                if (view instanceof Switch) {
                    bitmask |= (1 << SWITCH_BITMASK);
                } else if (view instanceof CheckBox) {
                    bitmask |= (1 << CHECKBOX_BITMASK);
                }
            }

            if (view instanceof EditText) {
                bitmask |= (1 << INPUT_BITMASK);
            }
        } else if (view instanceof Spinner
                || view instanceof DatePicker) {
            bitmask |= (1 << PICKER_BITMASK);
        } else if (view instanceof RatingBar) {
            bitmask |= (1 << RATINGBAR_BITMASK);
        } else if (view instanceof RadioGroup) {
            bitmask |= (1 << RADIO_GROUP_BITMASK);
        } else if (view instanceof ViewGroup
                && isRCTButton(view, RCTRootViewReference.get())) {
            bitmask |= (1 << REACT_NATIVE_BUTTON_BITMASK);
        }

        return bitmask;
    }

    public static boolean isClickableView(View view) {
        try {
            Field listenerInfoField = Class.forName("android.view.View")
                    .getDeclaredField("mListenerInfo");
            if (listenerInfoField != null) {
                listenerInfoField.setAccessible(true);
                Object listenerObj = listenerInfoField.get(view);
                if (listenerObj == null) {
                    return false;
                }
                Field listenerField = Class.forName("android.view.View$ListenerInfo")
                        .getDeclaredField("mOnClickListener");

                if (listenerField != null) {
                    View.OnClickListener listener =
                            (View.OnClickListener) listenerField.get(listenerObj);
                    return listener != null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check if the view is clickable.", e);
        }

        return false;
    }

    private static boolean isAdapterViewItem(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof AdapterView ||
                        parent instanceof NestedScrollingChild;
    }

    public static String getTextOfView(View view) {
        Object textObj = null;
        if (view instanceof TextView) {
            textObj = ((TextView) view).getText();

            if (view instanceof Switch) {
                boolean isOn = ((Switch)view).isChecked();
                textObj = isOn ? "1" : "0";
            }
        } else if (view instanceof Spinner) {
            Spinner spinner = (Spinner)view;
            if (spinner.getCount() > 0) {
                Object selectedItem = ((Spinner) view).getSelectedItem();
                if (selectedItem != null) {
                    textObj = selectedItem.toString();
                }
            }
        } else if (view instanceof DatePicker) {
            DatePicker picker = (DatePicker) view;
            int y = picker.getYear();
            int m = picker.getMonth();
            int d = picker.getDayOfMonth();
            textObj = String.format("%04d-%02d-%02d", y, m, d);
        } else if (view instanceof TimePicker) {
            TimePicker picker = (TimePicker) view;
            int h = picker.getCurrentHour();
            int m = picker.getCurrentMinute();
            textObj = String.format("%02d:%02d", h, m);
        } else if (view instanceof RadioGroup) {
            RadioGroup radioGroup = (RadioGroup)view;
            int checkedId = radioGroup.getCheckedRadioButtonId();
            int childCount = radioGroup.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = radioGroup.getChildAt(i);
                if (child.getId() == checkedId && child instanceof RadioButton) {
                    textObj = ((RadioButton)child).getText();
                    break;
                }
            }
        } else if (view instanceof RatingBar) {
            RatingBar bar = (RatingBar)view;
            float rating = bar.getRating();
            textObj = String.valueOf(rating);
        }

        return textObj == null ? "" : textObj.toString();
    }

    public static String getHintOfView(View view) {
        CharSequence hintObj = null;
        if (view instanceof EditText) {
            hintObj = ((EditText) view).getHint();
        } else if (view instanceof TextView) {
            hintObj = ((TextView) view).getHint();
        }

        return hintObj == null ? "" : hintObj.toString();
    }

    private static JSONObject getDimensionOfView(View view) {
        JSONObject dimension = new JSONObject();

        try {
            dimension.put(DIMENSION_TOP_KEY, view.getTop());
            dimension.put(DIMENSION_LEFT_KEY, view.getLeft());
            dimension.put(DIMENSION_WIDTH_KEY, view.getWidth());
            dimension.put(DIMENSION_HEIGHT_KEY, view.getHeight());
            dimension.put(DIMENSION_SCROLL_X_KEY, view.getScrollX());
            dimension.put(DIMENSION_SCROLL_Y_KEY, view.getScrollY());
            dimension.put(DIMENSION_VISIBILITY_KEY, view.getVisibility());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSONObject for dimension.", e);
        }

        return dimension;
    }

    @Nullable
    public static View.AccessibilityDelegate getExistingDelegate(View view) {
        try {
            Class<?> viewClass = view.getClass();
            Method getAccessibilityDelegateMethod =
                    viewClass.getMethod(GET_ACCESSIBILITY_METHOD);
            return (View.AccessibilityDelegate)
                    getAccessibilityDelegateMethod.invoke(view);
        } catch (NoSuchMethodException e) { /* no op */
        } catch (NullPointerException e) { /* no op */
        } catch (SecurityException e) { /* no op */
        } catch (IllegalAccessException e) { /* no op */
        } catch (InvocationTargetException e) { /* no op */
        }
        return null;
    }

    @Nullable
    public static View.OnTouchListener getExistingOnTouchListener(View view) {
        try {
            Field listenerInfoField = Class.forName("android.view.View")
                    .getDeclaredField("mListenerInfo");
            if (listenerInfoField != null) {
                listenerInfoField.setAccessible(true);
            }

            Object listenerObj = listenerInfoField.get(view);
            if (listenerObj == null) {
                return null;
            }

            View.OnTouchListener listener = null;
            Field listenerField = Class.forName("android.view.View$ListenerInfo")
                    .getDeclaredField("mOnTouchListener");
            if (listenerField != null) {
                listenerField.setAccessible(true);
                listener = (View.OnTouchListener) listenerField.get(listenerObj);
            }

            return listener;
        } catch (NoSuchFieldException e) {
            Utility.logd(TAG, e);
        } catch (ClassNotFoundException e) {
            Utility.logd(TAG, e);
        } catch (IllegalAccessException e) {
            Utility.logd(TAG, e);
        }
        return null;
    }

    @Nullable
    public static View getTouchReactView(float[] location, @Nullable View RCTRootView) {
        initTouchTargetHelperMethods();
        if (null == methodFindTouchTargetView || null == RCTRootView) {
            return null;
        }

        try {
            View nativeTargetView = (View)methodFindTouchTargetView
                    .invoke(null, location, RCTRootView);
            if (nativeTargetView != null && nativeTargetView.getId() > 0) {
                View reactTargetView = (View)nativeTargetView.getParent();
                return reactTargetView != null ? reactTargetView : null;
            }
        } catch (IllegalAccessException e) {
            Utility.logd(TAG, e);
        } catch (InvocationTargetException e) {
            Utility.logd(TAG, e);
        }

        return null;
    }

    public static boolean isRCTButton(View view, @Nullable View RCTRootView) {
        // React Native Button and Touchable components are all ReactViewGroup
        String className = view.getClass().getName();
        if (className.equals(CLASS_RCTVIEWGROUP)) {
            float[] location = getViewLocationOnScreen(view);
            View touchTargetView = getTouchReactView(location, RCTRootView);
            return touchTargetView != null && touchTargetView.getId() == view.getId();
        }

        return false;
    }

    public static boolean isRCTRootView(View view) {
        return view.getClass().getName().equals(CLASS_RCTROOTVIEW);
    }

    public static boolean isRCTTextView(View view) {
        return view.getClass().getName().equals(CLASS_RCTTEXTVIEW);
    }

    public static boolean isRCTViewGroup(View view) {
        return view.getClass().getName().equals(CLASS_RCTVIEWGROUP);
    }

    @Nullable
    public static View findRCTRootView(View view) {
        while (null != view) {
            if (isRCTRootView(view)) {
                return view;
            }
            ViewParent viewParent = view.getParent();
            if (viewParent instanceof View) {
                view = (View)viewParent;
            } else {
                break;
            }
        }
        return null;
    }

    private static float[] getViewLocationOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        float[] result = new float[2];
        result[0] = location[0];
        result[1] = location[1];
        return result;
    }

    private static void initTouchTargetHelperMethods() {
        if (null != methodFindTouchTargetView) {
            return;
        }

        try {
            Class<?> RCTTouchTargetHelper = Class.forName(CLASS_TOUCHTARGETHELPER);
            methodFindTouchTargetView = RCTTouchTargetHelper.getDeclaredMethod(
                    METHOD_FIND_TOUCHTARGET_VIEW, float[].class, ViewGroup.class);
            methodFindTouchTargetView.setAccessible(true);
        } catch (ClassNotFoundException e) {
            Utility.logd(TAG, e);
        } catch (NoSuchMethodException e) {
            Utility.logd(TAG, e);
        }
    }
}
