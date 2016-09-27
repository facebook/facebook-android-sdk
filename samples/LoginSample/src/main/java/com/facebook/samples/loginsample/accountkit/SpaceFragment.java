/**
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

package com.facebook.samples.loginsample.accountkit;

import android.app.Activity;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.facebook.samples.loginsample.R;

public class SpaceFragment extends InflateFragment {

    private static final String HEIGHT_ATTRIBUTE_KEY = "heightAttribute";

    private AttributeSet attributes;

    public static SpaceFragment create(final int heightAttribute) {
        final SpaceFragment fragment = new SpaceFragment();
        final Bundle arguments = new Bundle();
        arguments.putInt(HEIGHT_ATTRIBUTE_KEY, heightAttribute);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_space, container, false);
        }
        updateHeight(view);
        return view;
    }

    @Override
    protected void handleAttributes(final AttributeSet attrs) {
        attributes = attrs;
        updateHeight(getView());
    }

    private void updateHeight(final View view) {
        if (view == null) {
            return;
        }
        final Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }

        final int heightAttribute = arguments.getInt(HEIGHT_ATTRIBUTE_KEY, -1);
        if (heightAttribute >= 0) {
            final TypedArray a = activity.obtainStyledAttributes(
                    attributes,
                    R.styleable.Theme_AccountKitSample_Style);
            final int heightAttributeValue = a.getDimensionPixelSize(heightAttribute, -1);
            a.recycle();
            if (heightAttributeValue >= 0) {
                view.getLayoutParams().height = heightAttributeValue;
            }
        }
    }
}
