package com.facebook.android.tests;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class Tests extends Activity {
    
    // Your Facebook Application ID must be set before running this example
    // See http://www.facebook.com/developers/createapp.php
    public static final String APP_ID = "110862205611506";
    
    private static final String[] PERMISSIONS =
        new String[] {"publish_stream", "read_stream", "offline_access"};
    
    TextView publicTestsText;
    TextView publicErrorsText;
    Button loginButton;
    TextView authenticatedTestsText;
    TextView authenticatedErrorsText;
    Button postButton;
    TextView wallPostText;
    TextView deletedPostText;
    Button logoutButton;
    TextView logoutText;
    
    Facebook authenticatedFacebook = new Facebook();
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        publicTestsText = (TextView) findViewById(R.id.publicTests);
        publicErrorsText = (TextView) findViewById(R.id.publicErrors);
        loginButton = (Button) findViewById(R.id.login);
        authenticatedTestsText = (TextView) findViewById(
                R.id.authenticatedTests);
        authenticatedErrorsText = (TextView) findViewById(
                R.id.authenticatedErrors);
        postButton = (Button) findViewById(R.id.post);
        wallPostText = (TextView) findViewById(R.id.wallPost);
        deletedPostText = (TextView) findViewById(R.id.deletedPost);
        logoutButton = (Button) findViewById(R.id.logout);
        logoutText = (TextView) findViewById(R.id.logoutTest);
               
        // button to test UI Server login method
        loginButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                authenticatedFacebook.authorize(Tests.this, 
                        APP_ID, PERMISSIONS, new TestLoginListener());
            }
        });
        
        // button for testing UI server publish stream dialog
        postButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                authenticatedFacebook.dialog(Tests.this, "stream.publish", 
                        new TestUiServerListener());
            }
        });
        
        // enable logout test button
        logoutButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                runTestLogout();
            }
        });
        
        runTestPublicApi();
    }
    
    public void runTestPublicApi() {
        if (testPublicApi()) {
            publicTestsText.setText("Public API tests passed");
            publicTestsText.setTextColor(Color.GREEN);
        } else {
            publicTestsText.setText("Public API tests failed");
            publicTestsText.setTextColor(Color.RED);
        }
        
        if (testPublicErrors()) {
            publicErrorsText.setText("Public API errors passed");
            publicErrorsText.setTextColor(Color.GREEN);
        } else {
            publicErrorsText.setText("Public API errors failed");
            publicErrorsText.setTextColor(Color.RED);
        }
    }
    
    public boolean testPublicApi() {
        Facebook fb = new Facebook();
        try {
            Log.d("Tests", "Testing standard API call");
            JSONObject response = Util.parseJson(fb.request("4"));
            if (!response.getString("name").equals("Mark Zuckerberg")) {
                return false;
            }
            
            Log.d("Tests", "Testing an API call with a specific method");
            response = Util.parseJson(
                    fb.request("soneff", new Bundle(), "GET"));
            if (!response.getString("name").equals("Steven Soneff")) {
                return false;
            }
            
            Log.d("Tests", "Testing a public search query");
            Bundle params = new Bundle();
            params.putString("q", "facebook");
            response = Util.parseJson(fb.request("search", params));
            if (response.getJSONArray("data").length() == 0) return false;
            
            Log.d("Tests", "Public API Tests passed"); 
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean testPublicErrors() {
        Facebook fb = new Facebook();
        try {
            Bundle params = new Bundle();
            
            Log.d("Tests", "Testing illegal post");
            params.putString("message", "Hello World");
            try {
                Util.parseJson(fb.request("4", params, "POST"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("Unsupported post request.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing illegal delete");
            try {
                Util.parseJson(fb.request("4", new Bundle(), "DELETE"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("Unsupported delete request.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing illegal post to Zuck's feed");
            try {
                Util.parseJson(fb.request("4/feed", new Bundle(), "POST"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("(#200) The user hasn't " +
                		"authorized the application to perform this action")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing invalidly specified parameters");
            try {
                Util.parseJson(fb.request("bgolub?fields=id,name,picture"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().startsWith("Unknown fields: picture?")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing request for 'me' is rejected without " +
            		"access_token");
            try {
                Util.parseJson(fb.request("me"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals(
                        "An active access token must be used to " +
                        "query information about the current user.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing empty request");
            try {
                Util.parseJson(fb.request(""));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("Unsupported get request.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing an invalid path");
            try {
                Util.parseJson(fb.request("invalidinvalidinvalidinvalid"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals(
                        "Some of the aliases you requested do not exist: " +
                        "invalidinvalidinvalidinvalid")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing missing query parameter");
            try {
                Util.parseJson(fb.request("search", new Bundle(), "GET"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("No node specified")) return false;
            }
            
            Log.d("Tests", "Testing that API method is specified");
            try {
                fb.request(new Bundle());
                return false;
            } catch (IllegalArgumentException e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals(
                        "API method must be specified. " +
                        "(parameters must contain key \"method\" " +
                        "and value). See http://developers.facebook." +
                        "com/docs/reference/rest/")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing that old API cannot be made without " +
            		"access token");
            params.putString("method", "stream.publish");
            try {
                Util.parseJson(fb.request(params));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (e.getErrorCode() != 101 || 
                        !e.getMessage().equals("Invalid API key") ) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing invalid access token");
            try {
                fb.setAccessToken("invalid");
                Util.parseJson(fb.request("me", new Bundle(), "GET"));
                return false;
            } catch (FacebookError e) {
                Log.d("Tests", "*" + e.getMessage() + "*");
                if (!e.getMessage().equals("Invalid OAuth access token.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "Public API Error Tests passed"); 
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public class TestLoginListener implements DialogListener {

        public void onComplete(Bundle values) {
            if (testAuthenticatedApi()) {
                authenticatedTestsText.setText(
                        "Authenticated API tests passed");
                authenticatedTestsText.setTextColor(Color.GREEN);
            } else {
                authenticatedTestsText.setText(
                        "Authenticated API tests failed");
                authenticatedTestsText.setTextColor(Color.RED);
            }
            if (testAuthenticatedErrors()) {
                authenticatedErrorsText.setText(
                        "Authenticated API errors passed");
                authenticatedErrorsText.setTextColor(Color.GREEN);
            } else {
                authenticatedErrorsText.setText(
                        "Authenticated API errors failed");
                authenticatedErrorsText.setTextColor(Color.RED);
            }
        }

        public void onCancel() {
        }

        public void onError(DialogError e) {
            e.printStackTrace();
        }

        public void onFacebookError(FacebookError e) {
            e.printStackTrace();
        }
    }
    
    public boolean testAuthenticatedApi() {
        if (!authenticatedFacebook.isSessionValid()) return false;
        try {
            Log.d("Tests", "Testing request for 'me'");
            String response = authenticatedFacebook.request("me");
            JSONObject obj = Util.parseJson(response);
            if (obj.getString("name") == null || 
                    obj.getString("name").equals("")) {
                return false;
            }
            
            Log.d("Tests", "Testing graph API wall post");
            Bundle parameters = new Bundle();
            parameters.putString("message", URLEncoder.encode("hello world"));
            parameters.putString("description", 
                    URLEncoder.encode("test test test"));
            response = authenticatedFacebook.request("me/feed", parameters, 
                    "POST");
            Log.d("Tests", "got response: " + response);
            if (response == null || response.equals("") || 
                    response.equals("false")) {
                return false;
            }
            
            Log.d("Tests", "Testing graph API delete");
            response = response.replaceAll("\\{\"id\":\"", "");
            response = response.replaceAll("\"\\}", "");
            response = authenticatedFacebook.request(response, new Bundle(), 
                    "DELETE");
            if (!response.equals("true")) return false;
            
            Log.d("Tests", "Testing old API wall post");
            parameters = new Bundle();
            parameters.putString("method", "stream.publish");
            String attachments = 
                URLEncoder.encode("{\"name\":\"Name=Title\"," +
                		"\"href\":\"http://www.google.fr/\",\"" +
                		"caption\":\"Caption\",\"description\":\"Description" +
                		"\",\"media\":[{\"type\":\"image\",\"src\":" +
                		"\"http://www.kratiroff.com/logo-facebook.jpg\"," +
                		"\"href\":\"http://developers.facebook.com/\"}]," +
                		"\"properties\":{\"another link\":{\"text\":\"" +
                		"Facebook homepage\",\"href\":\"http://www.facebook." +
                		"com\"}}}");
            parameters.putString("attachment", attachments);
            response = authenticatedFacebook.request(parameters);
            Log.d("Tests", "got response: " + response);
            if (response == null || response.equals("") || 
                    response.equals("false")) {
                return false;
            }
            
            Log.d("Tests", "Testing wall post delete");
            response = response.replaceAll("\"", "");
            response = authenticatedFacebook.request(
                    response, new Bundle(), "DELETE");
            if (!response.equals("true")) return false;
            
            Log.d("Tests", "All Authenticated Tests Passed");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean testAuthenticatedErrors() {
        if (!authenticatedFacebook.isSessionValid()) return false;
        
        Log.d("Tests", "Testing that request for 'me/invalid' is rejected");
        try {
            Util.parseJson(authenticatedFacebook.request("me/invalid"));
            return false;
        } catch (Throwable e) {
            Log.d("Tests", "*" + e.getMessage() + "*");
            if (!e.getMessage().equals("Unknown path components: /invalid")) {
                return false;
            }
        }
        
        Log.d("Tests", "Testing that old API call with invalid method fails");
        Bundle params = new Bundle();
        params.putString("method", "something_invalid");
        try {
            Util.parseJson(authenticatedFacebook.request(params));
            return false;
        } catch (Throwable e) {
            Log.d("Tests", "*" + e.getMessage() + "*");
            if (!e.getMessage().equals("Unknown method") ) {
                return false;
            }
        }
        
        Log.d("Tests", "All Authenticated Error Tests Passed");
        return true;
    }
    
    public class TestUiServerListener implements DialogListener {

        public void onComplete(Bundle values) {
            final String postId = values.getString("post_id");
            if (postId != null) {
                Log.d("Facebook-Example", "Dialog Success! post_id=" + postId);
                new AsyncFacebookRunner(authenticatedFacebook).request(postId, 
                        new TestPostRequestListener());
            } else {
                Tests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        wallPostText.setText("Wall Post Failure");
                        wallPostText.setTextColor(Color.RED);
                    }
                });
            }
        }

        public void onCancel() { }

        public void onError(DialogError e) {
            e.printStackTrace();
        }

        public void onFacebookError(FacebookError e) {
            e.printStackTrace();
        }
    }
    
    public class TestPostRequestListener implements RequestListener {
        
        public void onComplete(final String response) {
            Log.d("Tests", "Got response: " + response);
            try {
                JSONObject json = Util.parseJson(response);
                //final String message = json.getString("message");
                String postId = json.getString("id");
                Tests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        wallPostText.setText("Wall Post Success");
                        wallPostText.setTextColor(Color.GREEN);
                    }
                });
                
                Log.d("Tests", "Testing wall post delete");
                if (testPostDelete(postId)) {
                    Tests.this.runOnUiThread(new Runnable() {
                        public void run() {
                            deletedPostText.setText("Deleted Post Success");
                            deletedPostText.setTextColor(Color.GREEN);
                        }
                    });
                } else {
                    Tests.this.runOnUiThread(new Runnable() {
                        public void run() {
                            deletedPostText.setText("Deleted Post Failure");
                            deletedPostText.setTextColor(Color.RED);
                        }
                    });
                }
            } catch (Throwable e) {
                e.printStackTrace();
                Tests.this.runOnUiThread(new Runnable() {
                    public void run() {
                        wallPostText.setText("Wall Post Failure");
                        wallPostText.setTextColor(Color.RED);
                    }
                });
            }
        }

        public void onFacebookError(FacebookError e) {
            e.printStackTrace();
        }

        public void onFileNotFoundException(FileNotFoundException e) {
            e.printStackTrace();
        }

        public void onIOException(IOException e) {
            e.printStackTrace();
        }

        public void onMalformedURLException(MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean testPostDelete(String postId) {
        try {
            String deleteResponse = 
                authenticatedFacebook.request(postId, new Bundle(), "DELETE");
            return deleteResponse.equals("true");
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void runTestLogout() {
        if (testLogout()) {
            logoutText.setText("Logout Tests Passed");
            logoutText.setTextColor(Color.GREEN);
        } else {
            logoutText.setText("Logout Tests Failed");
            logoutText.setTextColor(Color.RED);
        }
    }
    
    public boolean testLogout() {
        try {
            String oldAccessToken = authenticatedFacebook.getAccessToken();
            
            Log.d("Tests", "Testing logout");
            String response = authenticatedFacebook.logout(this);
            Log.d("Tests", "Got logout response: *" + response + "*");
            if (!response.equals("true")) {
                return false;
            }
            
            Log.d("Tests", "Testing logout on logged out facebook session");
            try {
                Util.parseJson(authenticatedFacebook.logout(this));
                return false;
            } catch (FacebookError e) {
                if (e.getErrorCode() != 101 || 
                        !e.getMessage().equals("Invalid API key") ) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing logout on unauthenticated object");
            try {
                Util.parseJson(new Facebook().logout(this));
                return false;
            } catch (FacebookError e) {
                if (e.getErrorCode() != 101 || 
                        !e.getMessage().equals("Invalid API key") ) {
                    return false;
                }
            }
            
            Log.d("Tests", "Testing that old access token no longer works");
            Facebook invalidFb = new Facebook();
            invalidFb.setAccessToken(oldAccessToken);
            try {
                Util.parseJson(invalidFb.request("me"));
                return false;
            } catch (FacebookError e) {
                if (!e.getMessage().equals("Error processing access token.")) {
                    return false;
                }
            }
            
            Log.d("Tests", "All Logout Tests Passed");
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // test bad UI server method?
    
    // test invalid permission? <-- UI server test
}