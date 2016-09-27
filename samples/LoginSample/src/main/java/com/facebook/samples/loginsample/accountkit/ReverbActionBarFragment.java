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
import android.widget.TextView;

import com.facebook.accountkit.ui.LoginFlowState;
import com.facebook.samples.loginsample.R;

public class ReverbActionBarFragment extends Fragment {
    private static final String STATE_KEY = "state";

    private LoginFlowState state = LoginFlowState.NONE;

    public void setState(final LoginFlowState state) {
        if (state == null) {
            return;
        }
        this.state = state;
        updateTitleView(getView());
    }

    @Nullable
    @Override
    public View onCreateView(
            final LayoutInflater inflater,
            final ViewGroup container,
            final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            final String stateString = savedInstanceState.getString(STATE_KEY);
            state = stateString == null ? state : LoginFlowState.valueOf(stateString);
        }

        View view = super.onCreateView(inflater, container, savedInstanceState);
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_reverb_action_bar, container, false);
        }
        if (view == null) {
            return null;
        }
        updateTitleView(view);
        return view;
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(STATE_KEY, state.name());
    }

    private void updateTitleView(@Nullable final View view) {
        if (view == null || state == null) {
            return;
        }
        final TextView titleView = (TextView) view.findViewById(R.id.title_view);
        if (titleView != null) {
            switch (state) {
                case PHONE_NUMBER_INPUT:
                    titleView.setText(R.string.reverb_title_phone_number_input);
                    break;
                case EMAIL_INPUT:
                    titleView.setText(R.string.reverb_title_email_input);
                    break;
                case SENDING_CODE:
                    titleView.setText(R.string.reverb_title_sending_code);
                    break;
                case SENT_CODE:
                    titleView.setText(R.string.reverb_title_sent_code);
                    break;
                case CODE_INPUT:
                    titleView.setText(R.string.reverb_title_code_input);
                    break;
                case EMAIL_VERIFY:
                    titleView.setText(R.string.reverb_title_email_verify);
                    break;
                case VERIFYING_CODE:
                    titleView.setText(R.string.reverb_title_verifying_code);
                    break;
                case VERIFIED:
                    titleView.setText(R.string.reverb_title_verified);
                    break;
                case RESEND:
                    titleView.setText(R.string.reverb_title_resend);
                    break;
                case ERROR:
                    titleView.setText(R.string.reverb_title_error);
                    break;
            }
        }
    }
}
