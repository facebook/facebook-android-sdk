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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.accountkit.ui.LoginType;
import com.facebook.samples.loginsample.R;

public class ReverbFooterFragment extends Fragment {
    private static final String LOGIN_TYPE_KEY = "loginType";
    private static final String PROGRESS_KEY = "progress";
    private static final String PROGRESS_TYPE_KEY = "progressType";

    public enum ProgressType {
        BAR,
        DOTS,
    }

    public interface OnSwitchLoginTypeListener {
        void onSwitchLoginType();
    }

    private LoginType loginType;
    private OnSwitchLoginTypeListener onSwitchLoginTypeListener;
    private int progress = 0;
    private ProgressType progressType = ProgressType.BAR;

    public void setLoginType(final LoginType loginType) {
        if (loginType == null) {
            return;
        }
        this.loginType = loginType;
        updateButtonText(getView());
    }

    public void setOnSwitchLoginTypeListener(
            final OnSwitchLoginTypeListener onSwitchLoginTypeListener) {
        this.onSwitchLoginTypeListener = onSwitchLoginTypeListener;
        updateSwitchLoginTypeListener(getView());
    }

    public void setProgress(final int progress) {
        this.progress = progress;
        updateProgress(getView());
    }

    public void setProgressType(final ProgressType progressType) {
        if (progressType == null) {
            return;
        }
        this.progressType = progressType;
    }

    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            final String loginTypeString = savedInstanceState.getString(LOGIN_TYPE_KEY);
            loginType = loginTypeString == null ? loginType : LoginType.valueOf(loginTypeString);
            progress = savedInstanceState.getInt(PROGRESS_KEY, progress);
            final String progressTypeString = savedInstanceState.getString(PROGRESS_TYPE_KEY);
            progressType = progressTypeString == null
                    ? progressType
                    : ProgressType.valueOf(progressTypeString);
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            final int layoutResourceId;
            switch (progressType) {
                case DOTS:
                    layoutResourceId = R.layout.fragment_reverb_footer_dots;
                    break;
                case BAR:
                default:
                    layoutResourceId = R.layout.fragment_reverb_footer_bar;
                    break;
            }
            view = inflater.inflate(layoutResourceId, container, false);
        }
        updateButtonText(view);
        updateProgress(view);
        updateSwitchLoginTypeListener(view);
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(LOGIN_TYPE_KEY, loginType == null ? null : loginType.name());
        outState.putInt(PROGRESS_KEY, progress);
        outState.putString(PROGRESS_TYPE_KEY, progressType.name());
    }

    private void updateButtonText(@Nullable final View view) {
        if (view == null) {
            return;
        }
        final TextView switchLoginTypeButton
                = (TextView) view.findViewById(R.id.switch_login_type_button);
        if (switchLoginTypeButton == null) {
            return;
        }
        if (loginType == null) {
            switchLoginTypeButton.setVisibility(View.GONE);
            return;
        }
        switch (loginType) {
            case EMAIL:
                switchLoginTypeButton.setText(R.string.reverb_switch_login_type_email);
                break;
            case PHONE:
                switchLoginTypeButton.setText(R.string.reverb_switch_login_type_phone);
                break;
        }
        switchLoginTypeButton.setVisibility(View.VISIBLE);
    }

    private void updateProgress(@Nullable final View view) {
        if (view == null) {
            return;
        }

        final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.reverb_progress_bar);
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    private void updateSwitchLoginTypeListener(@Nullable final View view) {
        if (view == null || onSwitchLoginTypeListener == null) {
            return;
        }
        final View switchLoginTypeButton = view.findViewById(R.id.switch_login_type_button);
        if (switchLoginTypeButton == null) {
            return;
        }
        switchLoginTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (onSwitchLoginTypeListener != null) {
                    onSwitchLoginTypeListener.onSwitchLoginType();
                }
            }
        });
    }
}
