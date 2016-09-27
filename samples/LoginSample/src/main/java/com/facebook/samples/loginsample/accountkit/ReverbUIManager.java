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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.ui.AdvancedUIManager;
import com.facebook.accountkit.ui.ButtonType;
import com.facebook.accountkit.ui.LoginFlowState;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.accountkit.ui.TextPosition;
import com.facebook.samples.loginsample.R;

public class ReverbUIManager implements AdvancedUIManager, Parcelable {
    public static final String LOGIN_TYPE_EXTRA = "loginType";
    public static final String SWITCH_LOGIN_TYPE_EVENT = "switch-login-type";

    private final ButtonType confirmButton;
    private final ButtonType entryButton;
    private AdvancedUIManagerListener listener;
    private final LoginType loginType;
    private final TextPosition textPosition;
    private final int themeResourceId;

    public ReverbUIManager(
            final ButtonType confirmButton,
            final ButtonType entryButton,
            final LoginType loginType,
            final TextPosition textPosition,
            final int themeResourceId) {
        this.confirmButton = confirmButton;
        this.entryButton = entryButton;
        this.loginType = loginType;
        this.textPosition = textPosition;
        this.themeResourceId = themeResourceId;
    }

    @Override
    @Nullable
    public  Fragment getActionBarFragment(final LoginFlowState state) {
        final ReverbActionBarFragment fragment = new ReverbActionBarFragment();
        fragment.setState(state);
        return fragment;
    }

    @Override
    @Nullable
    public Fragment getBodyFragment(final LoginFlowState state) {
        int iconResourceId = 0;
        boolean showProgressSpinner = false;
        switch (state) {
            case SENDING_CODE:
                showProgressSpinner = true;
                break;
            case SENT_CODE:
                switch (loginType) {
                    case EMAIL:
                        iconResourceId = R.drawable.reverb_email;
                        break;
                    case PHONE:
                        iconResourceId = R.drawable.reverb_progress_complete;
                        break;
                }
                break;
            case EMAIL_VERIFY:
                iconResourceId = R.drawable.reverb_email_sent;
                break;
            case VERIFYING_CODE:
                showProgressSpinner = true;
                break;
            case VERIFIED:
                iconResourceId = R.drawable.reverb_progress_complete;
                break;
            case ERROR:
                iconResourceId = R.drawable.reverb_error;
                break;
            case PHONE_NUMBER_INPUT:
            case EMAIL_INPUT:
            case CODE_INPUT:
            case RESEND:
            case NONE:
            default:
                return null;
        }
        final ReverbBodyFragment fragment = new ReverbBodyFragment();
        fragment.setIconResourceId(iconResourceId);
        fragment.setShowProgressSpinner(showProgressSpinner);
        return fragment;
    }

    @Override
    @Nullable
    public ButtonType getButtonType(final LoginFlowState state) {
        switch (state) {
            case PHONE_NUMBER_INPUT:
            case EMAIL_INPUT:
                return entryButton;
            case CODE_INPUT:
                return confirmButton;
            default:
                return null;
        }
    }

    @Override
    @Nullable
    public Fragment getFooterFragment(final LoginFlowState state) {
        final int progress;
        switch (state) {
            case PHONE_NUMBER_INPUT:
            case EMAIL_INPUT:
                progress = 1;
                break;
            case SENDING_CODE:
            case SENT_CODE:
                progress = 2;
                break;
            case CODE_INPUT:
            case EMAIL_VERIFY:
                progress = 3;
                break;
            case VERIFYING_CODE:
                progress = 4;
                break;
            case VERIFIED:
                progress = 5;
                break;
            case RESEND:
            case ERROR:
            case NONE:
            default:
                return null;
        }
        final ReverbFooterFragment fragment = new ReverbFooterFragment();
        if (progress == 1) {
            fragment.setLoginType(loginType);
            fragment.setOnSwitchLoginTypeListener(
                    new ReverbFooterFragment.OnSwitchLoginTypeListener() {
                        @Override
                        public void onSwitchLoginType() {
                            if (listener == null) {
                                return;
                            }

                            listener.onCancel();

                            final Activity activity = fragment.getActivity();
                            if (activity == null) {
                                return;
                            }
                            final Context applicationContext = activity.getApplicationContext();
                            final LoginType newLoginType;
                            switch (loginType) {
                                case EMAIL:
                                    newLoginType = LoginType.PHONE;
                                    break;
                                case PHONE:
                                    newLoginType = LoginType.EMAIL;
                                    break;
                                default:
                                    return;
                            }
                            LocalBroadcastManager
                                    .getInstance(applicationContext)
                                    .sendBroadcast(new Intent(SWITCH_LOGIN_TYPE_EVENT)
                                            .putExtra(LOGIN_TYPE_EXTRA, newLoginType.name()));
                        }
                    });
        }
        if (themeResourceId == R.style.AppLoginTheme_Reverb_A) {
            fragment.setProgressType(ReverbFooterFragment.ProgressType.BAR);
        } else if (themeResourceId == R.style.AppLoginTheme_Reverb_B
                || themeResourceId == R.style.AppLoginTheme_Reverb_C) {
            fragment.setProgressType(ReverbFooterFragment.ProgressType.DOTS);
        }
        fragment.setProgress(progress);
        return fragment;
    }

    @Override
    @Nullable
    public Fragment getHeaderFragment(final LoginFlowState state) {
        if (state == LoginFlowState.ERROR) {
            return null;
        } else {
            return SpaceFragment.create(
                    R.styleable.Theme_AccountKitSample_Style_reverb_content_margin_top);
        }
    }

    @Override
    @Nullable
    public TextPosition getTextPosition(final LoginFlowState state) {
        return textPosition == null ? TextPosition.ABOVE_BODY : textPosition;
    }

    @Override
    public void setAdvancedUIManagerListener(final AdvancedUIManagerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onError(final AccountKitError error) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(confirmButton != null ? confirmButton.name() : null);
        dest.writeString(entryButton != null ? entryButton.name() : null);
        dest.writeString(loginType != null ? loginType.name() : null);
        dest.writeString(textPosition != null ? textPosition.name() : null);
        dest.writeInt(themeResourceId);
    }

    public static final Creator<ReverbUIManager> CREATOR
            = new Creator<ReverbUIManager>() {
        @Override
        public ReverbUIManager createFromParcel(final Parcel source) {
            String s = source.readString();
            final ButtonType confirmButton = s == null ? null : ButtonType.valueOf(s);
            s = source.readString();
            final ButtonType entryButton = s == null ? null : ButtonType.valueOf(s);
            s = source.readString();
            final LoginType loginType = s == null ? null : LoginType.valueOf(s);
            s = source.readString();
            final TextPosition textPosition = s == null ? null : TextPosition.valueOf(s);
            final int themeResourceId = source.readInt();
            return new ReverbUIManager(
                    confirmButton,
                    entryButton,
                    loginType,
                    textPosition,
                    themeResourceId);
        }

        @Override
        public ReverbUIManager[] newArray(final int size) {
            return new ReverbUIManager[size];
        }
    };
}
