package com.facebook.android.example;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.Facebook.RequestListener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Example extends Activity {
    private static final String[] PERMISSIONS =
        new String[] {"publish_stream","user_photos","user_videos"};

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        final Facebook fb = new Facebook(this, "110862205611506");
        fb.authorize(PERMISSIONS, new DialogListener() {

            @Override
            public void onDialogSucceed(Bundle values) {
                fb.request("me", new SampleRequestListener());
            }

            @Override
            public void onDialogFail(String error) {
                Log.d("SDK-DEBUG", "Login failed: " + error.toString());
            }
        });
    }
    
    public class SampleRequestListener extends RequestListener {

        @Override
        public void onRequestSucceed(final JSONObject response) {
            // process the response here: executed in background thread
            Log.d("SDK-DEBUG", "Request succeeded: " + response.toString());
            
            // then post the processed result back to the UI
            // PROBLEM: this needs to be done in the UI thread, not the background thread
            Example.this.runOnUiThread(new Runnable() {
                public void run() {
                    TextView label = (TextView) Example.this.findViewById(R.id.label);
                    try {
                        label.setText("Hello there, " + response.getString("name"));
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onRequestFail(String error) {
            Log.d("SDK-DEBUG", "Request failed: " + error.toString());
        }
    }
}