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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
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

import androidx.core.view.NestedScrollingChild;
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

import static com.facebook.appevents.internal.ViewHierarchyConstants.*;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ViewHierarchy {
    private static final String TAG = ViewHierarchy.class.getCanonicalName();

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

    public static void updateBasicInfoOfView(View view, JSONObject json) {
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
    }

    public static void updateAppearanceOfView(View view, JSONObject json, float displayDensity) {
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
    }

    public static JSONObject getDictionaryOfView(View view) {
        if (view.getClass().getName().equals(CLASS_RCTROOTVIEW)) {
            RCTRootViewReference = new WeakReference<>(view);
        }

        JSONObject json = new JSONObject();

        try {
            updateBasicInfoOfView(view, json);

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

    public static int getClassTypeBitmask(View view) {
        int bitmask = 0;
        if (view instanceof ImageView) {
            bitmask |= (1 << IMAGEVIEW_BITMASK);
        }

        if (view.isClickable()) {
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
    public static View.OnClickListener getExistingOnClickListener(View view) {
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

            View.OnClickListener listener = null;
            Field listenerField = Class.forName("android.view.View$ListenerInfo")
                    .getDeclaredField("mOnClickListener");
            if (listenerField != null) {
                listenerField.setAccessible(true);
                listener = (View.OnClickListener) listenerField.get(listenerObj);
            }

            return listener;
        } catch (NoSuchFieldException e) { /* no op */
        } catch (ClassNotFoundException e) { /* no op */
        } catch (IllegalAccessException e) { /* no op */
        }
        return null;
    }

    public static void setOnClickListener(View view, View.OnClickListener newListener) {
        try {
            Field listenerInfoField = null;
            Field listenerField = null;
            try {
                listenerInfoField = Class.forName("android.view.View")
                        .getDeclaredField("mListenerInfo");
                listenerField = Class.forName("android.view.View$ListenerInfo")
                        .getDeclaredField("mOnClickListener");
            } catch (ClassNotFoundException e) {  /* no op */
            } catch (NoSuchFieldException e) {  /* no op */
            }

            if (listenerInfoField == null || listenerField == null) {
                view.setOnClickListener(newListener);
                return;
            }

            listenerInfoField.setAccessible(true);
            listenerField.setAccessible(true);

            Object listenerObj = null;
            try {
                listenerInfoField.setAccessible(true);
                listenerObj = listenerInfoField.get(view);
            } catch (IllegalAccessException e) { /* no op */
            }

            if (listenerObj == null) {
                view.setOnClickListener(newListener);
                return;
            }

            listenerField.set(listenerObj, newListener);
        } catch (Exception e) { /* no op */
        }
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
