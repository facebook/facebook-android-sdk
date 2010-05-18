/*
 * Copyright 2010 Facebook, Inc.
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

import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.Facebook.LogoutListener;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;

public class FbButton extends ImageButton {
    
    public FbButton(Context context) {
        super(context);
    }
    
    public FbButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public FbButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public void init(final Facebook fb,
                     final String applicationID,
                     final String[] permissions,
                     final DialogListener loginCallback,
                     final LogoutListener logoutCallback) {
        setBackgroundColor(Color.TRANSPARENT);
        setAdjustViewBounds(true);
        setImageResource(fb.isSessionValid() ? R.drawable.logout_button : 
                         R.drawable.login_button);
        drawableStateChanged();
        fb.addLogoutListener(logoutCallback);
        setOnClickListener(new OnClickListener() {
            public void onClick(View arg0) {
                if (fb.isSessionValid()) {
                    fb.logout(getContext());
                    FbUtil.clearSavedSession(getContext());
                    setImageResource(R.drawable.login_button);
                } else {
                    fb.authorize(getContext(), applicationID, permissions, 
                            new DialogListener() {

                        @Override
                        public void onDialogSucceed(Bundle values) {
                            setImageResource(R.drawable.logout_button);
                            FbUtil.saveSession(fb, getContext());
                            loginCallback.onDialogSucceed(values);
                        }

                        @Override
                        public void onDialogCancel() {
                            loginCallback.onDialogCancel();
                        }
                        
                        @Override
                        public void onDialogFail(String error) {
                            loginCallback.onDialogFail(error);
                        }
                    });
                }
            }
        });
    }
    
}
