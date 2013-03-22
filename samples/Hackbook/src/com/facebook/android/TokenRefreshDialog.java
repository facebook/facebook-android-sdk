/**
 * Copyright 2010-present Facebook.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.android;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class TokenRefreshDialog extends Dialog {

    private EditText tokenEdit, tokenExpiresEdit;
    private TextView mUsefulTip;
    private Button mRefreshButton;
    private Activity activity;

    public TokenRefreshDialog(Activity activity) {
        super(activity);
        this.activity = activity;
        setTitle(R.string.refresh_token_title);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.token_refresh);

        tokenEdit = (EditText) findViewById(R.id.tokenEdit);
        tokenEdit.setText(Utility.mFacebook.getAccessToken());

        tokenExpiresEdit = (EditText) findViewById(R.id.tokenExpiresEdit);
        setExpiresAt(Utility.mFacebook.getAccessExpires());

        mUsefulTip = (TextView) findViewById(R.id.usefulTip);
        mUsefulTip.setMovementMethod(LinkMovementMethod.getInstance());
        mRefreshButton = (Button) findViewById(R.id.refresh_button);

        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonState(false);
                RefreshTokenListener listener = new RefreshTokenListener();
                if (!Utility.mFacebook.extendAccessToken(activity, listener)) {
                    listener.onError(new Error(
                            activity.getString(R.string.refresh_token_binding_error)));
                }
            }
        });
    }

    private class RefreshTokenListener implements Facebook.ServiceListener {

        @Override
        public void onFacebookError(FacebookError e) {
            changeButtonState(true);
            String title = String.format(activity.getString(R.string.facebook_error) + "%d",
                    e.getErrorCode());
            Util.showAlert(activity, title, e.getMessage());
        }

        @Override
        public void onError(Error e) {
            changeButtonState(true);
            Util.showAlert(activity, activity.getString(R.string.error), e.getMessage());
        }

        @Override
        public void onComplete(Bundle values) {
            changeButtonState(true);

            // The access_token and expires_at values are automatically updated,
            // so they can be obtained by using:
            // - Facebook.getAccessToken()
            // - Facebook.getAccessExpires()
            // methods, but we can also get them from the 'values' bundle.
            tokenEdit.setText(values.getString(Facebook.TOKEN));
            setExpiresAt(values.getLong(Facebook.EXPIRES));
        }
    }

    private void changeButtonState(boolean enabled) {
        mRefreshButton.setEnabled(enabled);
        mRefreshButton.setText(enabled ? R.string.refresh_button : R.string.refresh_button_pending);
    }

    private void setExpiresAt(long time) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        tokenExpiresEdit.setText(dateFormat.format(new Date(time)));
    }
}
