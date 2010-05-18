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
import com.facebook.android.Facebook.AuthListener;
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
    private Facebook mFb;
    private FbButton mLoginButton;
    private Button mRequestButton;
    private Button mFeedButton;
    private TextView mText;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mLoginButton = (FbButton) findViewById(R.id.login);
        mRequestButton = (Button) findViewById(R.id.requestButton);
        mFeedButton = (Button) findViewById(R.id.feedButton);
        mText = (TextView) Example.this.findViewById(R.id.txt);
        
        mFb = new Facebook();
        FbUtil.restoreSession(mFb, this);
        mFb.addAuthListener(new SampleAuthListener());
        mFb.addLogoutListener(new SampleLogoutListener());
        mLoginButton.init(mFb, APP_ID, PERMISSIONS);
        
        mRequestButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mFb.asyncRequest("me", new SampleRequestListener());                
            }
        });
        mRequestButton.setVisibility(mFb.isSessionValid()? View.VISIBLE : 
            View.INVISIBLE);
        
        mFeedButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mFb.dialog(Example.this,
                          "stream.publish", 
                          new SampleDialogListener());          
            }
        });
        mFeedButton.setVisibility(mFb.isSessionValid()? View.VISIBLE : 
            View.INVISIBLE);
    }
    
    public class SampleAuthListener implements AuthListener {
        
        public void onAuthSucceed() {
            mText.setText("You have logged in! ");
            mRequestButton.setVisibility(View.VISIBLE);
            mFeedButton.setVisibility(View.VISIBLE);
        }

        public void onAuthFail(String error) {
            mText.setText("Login Failed: " + error);
        }
    }
    
    public class SampleLogoutListener implements LogoutListener {
        public void onLogoutBegin() {
            mText.setText("Logging out...");
        }
        
        public void onLogoutFinish() {
            mText.setText("You have logged out! ");
            mRequestButton.setVisibility(View.INVISIBLE);
            mFeedButton.setVisibility(View.INVISIBLE);
        }
    }
    
    public class SampleRequestListener implements RequestListener {

        public void onSuccess(final String response) {
            // process the response here: executed in background thread
            Log.d("Facebook-Example", "Success! " + response.toString());            
            
            // then post the processed result back to the UI thread
            // if we do not do this, an runtime exception will be generated
            // e.g. "CalledFromWrongThreadException: Only the original thread 
            // that created a view hierarchy can touch its views."
            Example.this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        JSONObject json = Util.parseJson(response);
                        mText.setText("Hello, " + json.getString("name"));
                    } catch (JSONException e) {
                        Log.w("Facebook-Example", "JSON Error in response");
                    }
                    
                }
            });
        }

        public void onError(String error) {
            Log.d("Facebook-Example", "Request failed: " + error.toString());
        }
    }
    
    public class WallPostRequestListener implements RequestListener {
        
        public void onSuccess(final String response) {
            Log.d("Facebook-Example", "Success! " + response.toString());
            
            Example.this.runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        JSONObject json = Util.parseJson(response);
                        String message = json.getString("message");
                        mText.setText("Your Wall Post: " + message);
                    } catch (JSONException e) {
                        Log.w("Facebook-Example", "JSON Error in response");
                    }
                    
                }
            });
        }
        
        public void onError(String error) {
            Log.d("Facebook-Example", "Request failed: " + error.toString());                    
        }
    }
    
    public class SampleDialogListener implements DialogListener {

        public void onCancel() { 
            Log.d("Facebook-Example", "Dialog Canceled");
        }

        public void onError(String error) {
            Log.d("Facebook-Example", "Dialog error: " + error);
        }

        public void onSuccess(Bundle values) {
            String postId = values.getString("post_id");
            Log.d("Facebook-Example", "Dialog Success! post_id is " + postId);
            mFb.asyncRequest(postId, new WallPostRequestListener());
        }
    }
    
}