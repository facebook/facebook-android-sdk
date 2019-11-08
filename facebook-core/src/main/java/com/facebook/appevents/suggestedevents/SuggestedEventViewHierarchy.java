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

package com.facebook.appevents.suggestedevents;

import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;

import com.facebook.appevents.codeless.internal.ViewHierarchy;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.facebook.appevents.internal.ViewHierarchyConstants.*;

class SuggestedEventViewHierarchy {
    final static String TAG = SuggestedEventViewHierarchy.class.getCanonicalName();
    final private static List<Class< ?extends View>> blacklistedViews =
            new ArrayList<>(Arrays.asList(
                    Switch.class,
                    Spinner.class,
                    DatePicker.class,
                    TimePicker.class,
                    RadioGroup.class,
                    RatingBar.class));

    static JSONObject getDictionaryOfView(View view, View clickedView) {
        JSONObject json = new JSONObject();
        try {
            if (view == clickedView) {
                json.put(IS_INTERACTED_KEY, true);
            }
            updateBasicInfo(view, json);

            JSONArray childViews = new JSONArray();
            List<View> children = ViewHierarchy.getChildrenOfView(view);
            for (int i = 0; i < children.size(); i++) {
                View child = children.get(i);
                JSONObject childInfo = getDictionaryOfView(child, clickedView);
                childViews.put(childInfo);
            }
            json.put(CHILDREN_VIEW_KEY, childViews);
        } catch (JSONException e) {
            // swallow
        }

        return json;
    }

    static void updateBasicInfo(View view, JSONObject json) {
        try {
            String text = ViewHierarchy.getTextOfView(view);
            String hint = ViewHierarchy.getHintOfView(view);
            Object tag = view.getTag();
            CharSequence description = view.getContentDescription();

            json.put(CLASS_NAME_KEY, view.getClass().getSimpleName());
            // TODO(T54293420): use id name rather than id
            json.put(ID_KEY, view.getId());
            json.put(CLASS_TYPE_BITMASK_KEY, ViewHierarchy.getClassTypeBitmask(view));
            if (!text.isEmpty()) {
                json.put(TEXT_KEY, text);
            }
            if (!hint.isEmpty()) {
                json.put(HINT_KEY, hint);
            }
            if (tag != null) {
                json.put(TAG_KEY, tag.toString());
            }
            if (description != null) {
                json.put(DESC_KEY, description.toString());
            }
            if (view instanceof EditText) {
                json.put(INPUT_TYPE_KEY, ((EditText) view).getInputType());
            }
        } catch (JSONException e) {
            // swallow
        }
    }

    static List<View> getAllClickableViews(View view) {
        List<View> clickableViews = new ArrayList<>();

        for (Class<? extends View> viewClass : blacklistedViews) {
            if (viewClass.isInstance(view)) {
                return clickableViews;
            }
        }

        if (view.isClickable()) {
            clickableViews.add(view);
        }

        List<View> children = ViewHierarchy.getChildrenOfView(view);
        for (View child : children) {
            clickableViews.addAll(getAllClickableViews(child));
        }
        return clickableViews;
    }
}