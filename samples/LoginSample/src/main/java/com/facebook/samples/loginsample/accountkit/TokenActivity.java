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

package com.facebook.samples.loginsample.accountkit;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.PhoneNumber;
import com.facebook.samples.loginsample.R;

public class TokenActivity extends Activity {
    static final String HELLO_TOKEN_ACTIVITY_FINAL_STATE_EXTRA =
            "HELLO_TOKEN_ACTIVITY_FINAL_STATE_EXTRA";
    static final String HELLO_TOKEN_ACTIVITY_INITIAL_STATE_EXTRA =
            "HELLO_TOKEN_ACTIVITY_INITIAL_STATE_EXTRA";

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hello_token);

        final Button signOut = (Button) findViewById(R.id.log_out_button);
        signOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccountKit.logOut();
                finish();
            }
        });

        final String finalState =
                getIntent().getStringExtra(HELLO_TOKEN_ACTIVITY_FINAL_STATE_EXTRA);
        final String initialState =
                getIntent().getStringExtra(HELLO_TOKEN_ACTIVITY_INITIAL_STATE_EXTRA);

        if (initialState != null) {
            final TextView initialStateView = (TextView) findViewById(R.id.initial_state);
            final TextView initialStateLabelView =
                    (TextView) findViewById(R.id.initial_state_label);
            if (initialStateView != null && initialStateLabelView != null) {
                initialStateView.setText(initialState);
                initialStateView.setVisibility(View.VISIBLE);
                initialStateLabelView.setVisibility(View.VISIBLE);
            }
        }

        if (finalState != null) {
            final TextView finalStateView = (TextView) findViewById(R.id.final_state);
            final TextView finalStateLabelView =
                    (TextView) findViewById(R.id.final_state_label);
            if (finalStateView != null && finalStateLabelView != null) {
                finalStateView.setText(finalState);
                finalStateView.setVisibility(View.VISIBLE);
                finalStateLabelView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(final Account account) {
                final TextView userId = (TextView) findViewById(R.id.user_id);
                userId.setText(account.getId());

                final TextView phoneNumber = (TextView) findViewById(R.id.user_phone);
                final PhoneNumber number = account.getPhoneNumber();
                phoneNumber.setText(number == null ? null : number.toString());

                final TextView email = (TextView) findViewById(R.id.user_email);
                email.setText(account.getEmail());
            }

            @Override
            public void onError(final AccountKitError error) {
            }
        });
    }
}
