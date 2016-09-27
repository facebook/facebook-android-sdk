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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.facebook.samples.loginsample.R;

public class ReverbBodyFragment extends Fragment {
    private static final String ICON_RESOURCE_ID_KEY = "iconResourceId";
    private static final String SHOW_PROGRESS_SPINNER_KEY = "showProgressSpinner";

    private int iconResourceId = 0;
    private boolean showProgressSpinner;

    public void setIconResourceId(final int iconResourceId) {
        this.iconResourceId = iconResourceId;
        updateIcon(getView());
    }

    public void setShowProgressSpinner(final boolean showProgressSpinner) {
        this.showProgressSpinner = showProgressSpinner;
        updateIcon(getView());
    }

    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            iconResourceId = savedInstanceState.getInt(ICON_RESOURCE_ID_KEY, iconResourceId);
            showProgressSpinner = savedInstanceState.getBoolean(
                    SHOW_PROGRESS_SPINNER_KEY,
                    showProgressSpinner);
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_reverb_body, container, false);
        }
        updateIcon(view);
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(ICON_RESOURCE_ID_KEY, iconResourceId);
        outState.putBoolean(SHOW_PROGRESS_SPINNER_KEY, showProgressSpinner);
    }

    private void updateIcon(@Nullable final View view) {
        if (view == null) {
            return;
        }

        final View progressSpinner = view.findViewById(R.id.reverb_progress_spinner);
        if (progressSpinner != null) {
            progressSpinner.setVisibility(showProgressSpinner ? View.VISIBLE : View.GONE);
        }

        final ImageView iconView = (ImageView) view.findViewById(R.id.reverb_icon);
        if (iconView != null) {
            if (iconResourceId > 0) {
                iconView.setImageResource(iconResourceId);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }
        }
    }
}
