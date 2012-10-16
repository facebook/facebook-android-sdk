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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

/**
 * A Fragment that displays a Login/Logout button as well as the user's
 * profile picture and name when logged in.
 * <p/>
 * This Fragment will create and use the active session upon construction
 * if it has the available data (if the app ID is specified in the manifest).
 * It will also open the active session if it does not require user interaction
 * (i.e. if the session is in the {@link SessionState#CREATED_TOKEN_LOADED} state.
 * Developers can override the use of the active session by calling
 * the {@link #setSession(Session)} method.
 */
public class LoginFragment extends FacebookFragment {

    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String PICTURE = "picture";
    private static final String FIELDS = "fields";
    
    private static final String PICTURE_URL = "https://graph.facebook.com/%s/picture?width=%d&height=%d";
    
    private static final String REQUEST_FIELDS = TextUtils.join(",", new String[] {ID, NAME, PICTURE});

    private LoginButton loginButton;
    private TextView connectedStateLabel;
    private GraphUser user;
    private Session userInfoSession; // the Session used to fetch the current user info
    private Drawable userProfilePic;
    private String userProfilePicID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.com_facebook_loginfragment, container, false);
        loginButton = (LoginButton) view.findViewById(R.id.com_facebook_loginfragment_login_button);
        loginButton.setFragment(this);
        connectedStateLabel = (TextView) view.findViewById(R.id.com_facebook_loginfragment_profile_name);
        
        // if no background is set for some reason, then default to Facebook blue
        if (view.getBackground() == null) {
            view.setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
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

    /**
     * @throws FacebookException if errors occur during the loading of user information
     */
    @Override
    public void onResume() {
        super.onResume();
        fetchUserInfo();
        updateUI();
    }

    /**
     * Set the Session object to use instead of the active Session. Since a Session
     * cannot be reused, if the user logs out from this Session, and tries to
     * log in again, a new Active Session will be used instead.
     * <p/>
     * If the passed in session is currently opened, this method will also attempt to
     * load some user information for display (if needed).
     *
     * @param newSession the Session object to use
     * @throws FacebookException if errors occur during the loading of user information
     */
    @Override
    public void setSession(Session newSession) {
        super.setSession(newSession);
        loginButton.setSession(newSession);
        fetchUserInfo();
        updateUI();
    }

    /**
     * Set the permissions to use when the session is opened. The permissions here
     * can only be read permissions. If any publish permissions are included, the login
     * attempt by the user will fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the session is open. If this is called
     * after the session is opened, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * Since the session can be automatically opened when the LoginFragment is constructed,
     * it's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by managing the session explicitly).
     *
     * @param permissions the read permissions to use
     *
     * @throws UnsupportedOperationException if setPublishPermissions has been called
     */
    public void setReadPermissions(List<String> permissions) {
        loginButton.setReadPermissions(permissions);
    }

    /**
     * Set the permissions to use when the session is opened. The permissions here
     * should only be publish permissions. If any read permissions are included, the login
     * attempt by the user may fail. The LoginButton can only be associated with either
     * read permissions or publish permissions, but not both. Calling both
     * setReadPermissions and setPublishPermissions on the same instance of LoginButton
     * will result in an exception being thrown unless clearPermissions is called in between.
     * <p/>
     * This method is only meaningful if called before the session is open. If this is called
     * after the session is opened, and the list of permissions passed in is not a subset
     * of the permissions granted during the authorization, it will log an error.
     * <p/>
     * Since the session can be automatically opened when the LoginButton is constructed,
     * it's important to always pass in a consistent set of permissions to this method, or
     * manage the setting of permissions outside of the LoginButton class altogether
     * (by managing the session explicitly).
     *
     * @param permissions the read permissions to use
     *
     * @throws UnsupportedOperationException if setReadPermissions has been called
     * @throws IllegalArgumentException if permissions is null or empty
     */
    public void setPublishPermissions(List<String> permissions) {
        loginButton.setPublishPermissions(permissions);
    }

    /**
     * Clears the permissions currently associated with this LoginButton.
     */
    public void clearPermissions() {
        loginButton.clearPermissions();
    }

    /**
     * Sets an OnErrorListener for this instance of LoginFragment to call into when
     * certain exceptions occur.
     *
     * @param onErrorListener The listener object to set
     */
    public void setOnErrorListener(LoginButton.OnErrorListener onErrorListener) {
        loginButton.setOnErrorListener(onErrorListener);
    }

    /**
     * Returns the current OnErrorListener for this instance of LoginFragment.
     *
     * @return The OnErrorListener
     */
    public LoginButton.OnErrorListener getOnErrorListener() {
        return loginButton.getOnErrorListener();
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        fetchUserInfo();
        updateUI();
    }
    
    private void fetchUserInfo() {
        final Session currentSession = getSession();
        if (currentSession != null && currentSession.isOpened()) {
            if (currentSession != userInfoSession) {
                Request request = Request.newMeRequest(currentSession, new Request.GraphUserCallback() {
                    @Override
                    public void onCompleted(GraphUser me, Response response) {
                        if (currentSession == getSession()) {
                            user = me;
                            updateUI();
                        }
                        if (response.getError() != null) {
                            loginButton.handleError(response.getError());
                        }
                    }
                });
                Bundle parameters = new Bundle();
                parameters.putString(FIELDS, REQUEST_FIELDS);
                request.setParameters(parameters);
                Request.executeBatchAsync(request);
                userInfoSession = currentSession;
            }
        } else {
            user = null;
        }
    }
    
    private void updateUI() {
        if (!isAdded()) {
            return;
        }
        if (isSessionOpen()) {
            connectedStateLabel.setTextColor(getResources().getColor(R.color.com_facebook_loginfragment_connected_text_color));
            connectedStateLabel.setShadowLayer(1f, 0f, -1f,
                    getResources().getColor(R.color.com_facebook_loginfragment_connected_shadow_color));
            
            if (user != null) {
                URL pictureURL = getPictureUrlOfUser();
                // Do we already have the right picture? If so, leave it alone.
                if (pictureURL != null && !pictureURL.equals(connectedStateLabel.getTag())) {
                    if (user.getId().equals(userProfilePicID)) {
                        connectedStateLabel.setCompoundDrawables(null, userProfilePic, null, null);
                        connectedStateLabel.setTag(pictureURL);
                    } else {
                        try {
                            ProfilePictureDownloadTask task = new ProfilePictureDownloadTask(user.getId());
                            task.execute(pictureURL);
                        } catch (RejectedExecutionException exception) {
                        }
                    }
                }
                connectedStateLabel.setText(user.getName());
            } else {
                connectedStateLabel.setText(getResources().getString(R.string.com_facebook_loginfragment_logged_in));
                Drawable noProfilePic = getResources().getDrawable(R.drawable.com_facebook_profile_default_icon);
                noProfilePic.setBounds(0, 0, 
                        getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_width),
                        getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_height));
                connectedStateLabel.setCompoundDrawables(null, noProfilePic, null, null);
            }
        } else {
            int textColor = getResources().getColor(R.color.com_facebook_loginfragment_not_connected_text_color);
            connectedStateLabel.setTextColor(textColor);
            connectedStateLabel.setShadowLayer(0f, 0f, 0f, textColor);
            connectedStateLabel.setText(getResources().getString(R.string.com_facebook_loginfragment_not_logged_in));
            connectedStateLabel.setCompoundDrawables(null, null, null, null);
            connectedStateLabel.setTag(null);
        }
    }

    private URL getPictureUrlOfUser() {
        try {
            return new URL(String.format(PICTURE_URL, user.getId(), 
                    getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_width),
                    getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_height)));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    private class ProfilePictureDownloadTask extends AsyncTask<URL, Void, Bitmap> {
        private URL tag;
        private String id;

        public ProfilePictureDownloadTask(String id) {
            this.id = id;
        }
        
        @Override
        protected Bitmap doInBackground(URL... params) {
            URLConnection connection = null;
            InputStream stream = null;
            try {
                tag = params[0];
                connection = tag.openConnection();
                stream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                return bitmap;
            } catch (IOException e) {
            } finally {
                Utility.closeQuietly(stream);
                Utility.disconnectQuietly(connection);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (LoginFragment.this.isVisible()) {
                BitmapDrawable drawable = new BitmapDrawable(LoginFragment.this.getResources(), bitmap);
                drawable.setBounds(0, 0,
                        getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_width),
                        getResources().getDimensionPixelSize(R.dimen.com_facebook_loginfragment_profile_picture_height));
                userProfilePic = drawable;
                userProfilePicID = id;
                connectedStateLabel.setCompoundDrawables(null, drawable, null, null);
                connectedStateLabel.setTag(tag);
            }
        }
    }
}
