/**
 * Copyright 2012 Facebook
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
package com.facebook;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.RejectedExecutionException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.android.R;

/**
 * A Fragment that displays a Login/Logout button as well as the user's
 * profile picture and name when logged in. Be sure to call this Fragment's
 * {@link #onActivityResult(int, int, android.content.Intent)} method from
 * the parent Activity.
 */
public class LoginFragment extends FacebookFragment {

    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String PICTURE = "picture";
    private static final String FIELDS = "fields";
    
    private static final String PICTURE_URL = "https://graph.facebook.com/%s/picture?width=%d&height=%d";
    
    private static final String REQUEST_FIELDS = TextUtils.join(",", new String[] {ID, NAME, PICTURE});

    private LoginView loginButton;
    private TextView connectedStateLabel;
    private volatile GraphUser user;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_login_fragment, container, false);
        loginButton = (LoginView) view.findViewById(R.id.login_button);
        connectedStateLabel = (TextView) view.findViewById(R.id.profile_name);
        
        // if no background is set for some reason, then default to Facebook blue
        if (view.getBackground() == null) {
            view.setBackgroundColor(getResources().getColor(R.color.Facebook_blue));
        } else {
            view.getBackground().setDither(true);
        }
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateUI();
    }
    
    @Override
    public void setSession(Session newSession) {
        super.setSession(newSession);
        loginButton.setSession(newSession);
        user = null;
        fetchUserInfo();
        updateUI();
    }
    
    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        fetchUserInfo();
        updateUI();
    }
    
    private void fetchUserInfo() {
        final Session currentSession = getSession();
        if (currentSession != null && currentSession.getIsOpened()) {
            Request request = Request.newMeRequest(currentSession, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    if (currentSession == getSession()) {
                        user = response.getGraphObjectAs(GraphUser.class);
                        updateUI();
                    }
                }
            });
            Bundle parameters = new Bundle();
            parameters.putString(FIELDS, REQUEST_FIELDS);
            request.setParameters(parameters);
            Request.executeBatchAsync(request);
        } else {
            user = null;
        }
    }
    
    private void updateUI() {
        if (isSessionOpen()) {
            connectedStateLabel.setTextColor(getResources().getColor(R.color.LoginFragment_connectedStateTextColor));
            connectedStateLabel.setShadowLayer(1f, 0f, -1f,
                    getResources().getColor(R.color.LoginFragment_connectedStateShadowColor));
            
            if (user != null) {
                URL pictureURL = getPictureUrlOfUser();
                // Do we already have the right picture? If so, leave it alone.
                if (pictureURL != null && !pictureURL.equals(connectedStateLabel.getTag())) {
                    try {
                        ProfilePictureDownloadTask task = new ProfilePictureDownloadTask();
                        task.execute(pictureURL);
                    } catch (RejectedExecutionException exception) {
                        // TODO retry?
                    }
                }
                connectedStateLabel.setText(user.getName());
            } else {
                connectedStateLabel.setText(getResources().getString(R.string.LoginFragment_LoggedIn));
                
                // TODO: confirm the size of the profile pic (currently 64x64dp), and create a
                //       default profile pic of that size (currently it's 50x50px).
                Drawable noProfilePic = getResources().getDrawable(R.drawable.profile_default_icon);
                noProfilePic.setBounds(0, 0, 
                        getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicWidth), 
                        getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicHeight));
                connectedStateLabel.setCompoundDrawables(null, noProfilePic, null, null);
            }
        } else {
            int textColor = getResources().getColor(R.color.LoginFragment_notConnectedStateTextColor);
            connectedStateLabel.setTextColor(textColor);
            connectedStateLabel.setShadowLayer(0f, 0f, 0f, textColor);
            connectedStateLabel.setText(getResources().getString(R.string.LoginFragment_NotLoggedIn));
            connectedStateLabel.setCompoundDrawables(null, null, null, null);
            connectedStateLabel.setTag(null);
        }
    }

    
    private URL getPictureUrlOfUser() {
        try {
            return new URL(String.format(PICTURE_URL, user.getId(), 
                    getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicWidth), 
                    getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicHeight)));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    private class ProfilePictureDownloadTask extends AsyncTask<URL, Void, Bitmap> {
        private URL tag;
        
        @Override
        protected Bitmap doInBackground(URL... params) {
            try {
                tag = params[0];
                URLConnection connection = tag.openConnection();
                InputStream stream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                return bitmap;
                // TODO cache
            } catch (IOException e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            BitmapDrawable drawable = new BitmapDrawable(LoginFragment.this.getResources(), bitmap);
            drawable.setBounds(0, 0, 
                    getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicWidth), 
                    getResources().getDimensionPixelSize(R.dimen.LoginFragment_profilePicHeight));
            connectedStateLabel.setCompoundDrawables(null, drawable, null, null);
            connectedStateLabel.setTag(tag);
        }
    }
}
