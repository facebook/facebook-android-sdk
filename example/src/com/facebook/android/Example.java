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

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.Facebook.LogoutListener;
import com.facebook.android.Facebook.RequestListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Example extends Activity {
    
    private static final String APP_ID = "110862205611506";
    private static final String[] PERMISSIONS =
        new String[] {"publish_stream","user_photos","user_videos"};
    private FbButton mLoginButton;
    private Button requestButton;
    private TextView mText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLoginButton = (FbButton) findViewById(R.id.login);
        requestButton = (Button) findViewById(R.id.requestButton);
        mText = (TextView) Example.this.findViewById(R.id.txt);
        
        final Facebook fb = new Facebook();
        FbUtil.restoreSession(fb, this);
        mLoginButton.init(fb, APP_ID, PERMISSIONS,
                          new SampleLoginListener(),
                          new SampleLogoutListener());
        requestButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fb.request("me", new SampleRequestListener());                
            }
        });
        requestButton.setVisibility(fb.isSessionValid()? View.VISIBLE : 
                                    View.INVISIBLE);
    }
    
    public class SampleLoginListener extends DialogListener {

        @Override
        public void onDialogSucceed(Bundle values) {
            mText.setText("You have logged in! ");
            requestButton.setVisibility(View.VISIBLE);
        }
        
        @Override
        public void onDialogCancel() {
            mText.setText("Login cancelled - try again!");
        }

        @Override
        public void onDialogFail(String error) {
            mText.setText("Login Failed: " + error);
        }
    }
    
    public class SampleLogoutListener extends LogoutListener {
        
        @Override
        public void onLogoutFinish() {
            mText.setText("You have logged out! ");
            requestButton.setVisibility(View.INVISIBLE);
        }
    }
    
    public class SampleRequestListener extends RequestListener {

        @Override
        public void onRequestSucceed(final JSONObject response) {
            // process the response here: executed in background thread
            Log.d("Facebook-Example", "Success! " + response.toString());
            
            // then post the processed result back to the UI thread
            Example.this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        mText.setText("Hello, " + response.getString("name"));
                    } catch (JSONException e) {
                        Log.w("Facebook-Example", "JSON Error in response");
                    }
                    
                }
            });
        }

        @Override
        public void onRequestFail(String error) {
            Log.d("Facebook-Example", "Request failed: " + error.toString());
        }
    }
}