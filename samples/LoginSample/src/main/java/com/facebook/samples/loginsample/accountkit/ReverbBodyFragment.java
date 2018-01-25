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

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.facebook.samples.loginsample.R;

public class ReverbBodyFragment extends Fragment {

    private static final String ICON_RESOURCE_ID_KEY = "iconResourceId";
    private static final String ICON_TINT_RESOURCE_ID_KEY = "iconTintResourceId";
    private static final String SHOW_PROGRESS_SPINNER_KEY = "showProgressSpinner";

    public static ReverbBodyFragment newInstance(
            int iconResourceId,
            int iconTintResourceId,
            boolean showProgressSpinner) {

        Bundle args = new Bundle();
        args.putInt(ICON_RESOURCE_ID_KEY, iconResourceId);
        args.putInt(ICON_TINT_RESOURCE_ID_KEY, iconTintResourceId);
        args.putBoolean(SHOW_PROGRESS_SPINNER_KEY, showProgressSpinner);

        ReverbBodyFragment fragment = new ReverbBodyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private int iconResourceId;
    private int iconTintResourceId;
    private boolean showProgressSpinner;

    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reverb_body, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        iconResourceId = getArguments().getInt(ICON_RESOURCE_ID_KEY);
        iconTintResourceId = getArguments().getInt(ICON_TINT_RESOURCE_ID_KEY);
        showProgressSpinner = getArguments().getBoolean(SHOW_PROGRESS_SPINNER_KEY);
        updateIcon(view);
    }

    private void updateIcon(final View view) {

        final View progressSpinner = view.findViewById(R.id.reverb_progress_spinner);
        if (progressSpinner != null) {
            progressSpinner.setVisibility(showProgressSpinner ? View.VISIBLE : View.GONE);
        }

        final ImageView iconView = (ImageView) view.findViewById(R.id.reverb_icon);
        if (iconView != null) {
            if (iconResourceId > 0) {

                /*
                 * Api levels lower than 21 do not support android:tint in xml drawables. Tint can
                 * be applied with DrawableCompat
                 */
                Drawable icon = getResources().getDrawable(iconResourceId);
                if(iconTintResourceId > 0) {
                    icon = DrawableCompat.wrap(icon);
                    DrawableCompat.setTint(
                            icon,
                            ContextCompat.getColor(view.getContext(), R.color.reverb_dark));
                }

                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
    }

}
