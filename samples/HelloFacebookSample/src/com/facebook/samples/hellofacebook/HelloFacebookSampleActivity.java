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

package com.facebook.samples.hellofacebook;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class HelloFacebookSampleActivity extends FacebookActivity {
    private final int PICK_FRIENDS_ACTIVITY = 1;
    private final int PICK_PLACE_ACTIVITY = 2;
    private final int REAUTHORIZE_ACTIVITY = 3;
    private final String APP_ID = "355198514515820";
    private final String PENDING_ACTION_BUNDLE_KEY = "com.facebook.samples.hellofacebook:PendingAction";

    private Button postStatusUpdateButton;
    private Button postPhotoButton;
    private Button pickFriendsButton;
    private Button pickPlaceButton;
    private LoginButton loginButton;
    private ProfilePictureView profilePictureView;
    private TextView greeting;
    private PendingActionAfterReauthorize pendingActionAfterReauthorize = PendingActionAfterReauthorize.NONE;
    private final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };
    private GraphUser user;

    private enum PendingActionAfterReauthorize {
        NONE,
        POST_PHOTO,
        POST_STATUS_UPDATE
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        loginButton = (LoginButton) findViewById(R.id.login_button);
        loginButton.setApplicationId(APP_ID);
        loginButton.setUserInfoChangedCallback(new LoginButton.UserInfoChangedCallback() {
            @Override
            public void onUserInfoFetched(GraphUser user) {
                HelloFacebookSampleActivity.this.user = user;
                updateUI();
            }
        });

        profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
        greeting = (TextView) findViewById(R.id.greeting);

        postStatusUpdateButton = (Button) findViewById(R.id.postStatusUpdateButton);
        postStatusUpdateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostStatusUpdate();
            }
        });

        postPhotoButton = (Button) findViewById(R.id.postPhotoButton);
        postPhotoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPostPhoto();
            }
        });

        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        pickPlaceButton = (Button) findViewById(R.id.pickPlaceButton);
        pickPlaceButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickPlace();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        updateUI();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Session.ACTION_ACTIVE_SESSION_OPENED);
        filter.addAction(Session.ACTION_ACTIVE_SESSION_CLOSED);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(PENDING_ACTION_BUNDLE_KEY, pendingActionAfterReauthorize.ordinal());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int ordinal = savedInstanceState.getInt(PENDING_ACTION_BUNDLE_KEY, 0);
        pendingActionAfterReauthorize = PendingActionAfterReauthorize.values()[ordinal];
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case PICK_FRIENDS_ACTIVITY:
                String results = "";
                if (resultCode == RESULT_OK) {
                    HelloFacebookApplication application = (HelloFacebookApplication) getApplication();
                    Collection<GraphUser> selection = application.getSelectedUsers();
                    if (selection != null && selection.size() > 0) {
                        ArrayList<String> names = new ArrayList<String>();
                        for (GraphUser user : selection) {
                            names.add(user.getName());
                        }
                        results = TextUtils.join(", ", names);
                    } else {
                        results = "<No friends selected>";
                    }
                } else {
                    results = "<Cancelled>";
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("You picked:").setMessage(results).setPositiveButton("OK", null);
                builder.show();

                break;

            case PICK_PLACE_ACTIVITY:
                results = "";
                if (resultCode == RESULT_OK) {
                    HelloFacebookApplication application = (HelloFacebookApplication) getApplication();
                    GraphPlace selection = application.getSelectedPlace();
                    if (selection != null) {
                        results = selection.getName();
                    } else {
                        results = "<No place selected>";
                    }
                } else {
                    results = "<Cancelled>";
                }

                builder = new AlertDialog.Builder(this);
                builder.setTitle("You picked:").setMessage(results).setPositiveButton("OK", null);
                builder.show();

                break;
        }
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        super.onSessionStateChange(state, exception);
        if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingActionAfterReauthorize();
        }
    }

    private void updateUI() {
        boolean enableButtons = Session.getActiveSession() != null &&
                Session.getActiveSession().getState().isOpened();

        postStatusUpdateButton.setEnabled(enableButtons);
        postPhotoButton.setEnabled(enableButtons);
        pickFriendsButton.setEnabled(enableButtons);
        pickPlaceButton.setEnabled(enableButtons);

        if (enableButtons && user != null) {
            profilePictureView.setUserId(user.getId());
            greeting.setText(String.format("Hello %s!", user.getFirstName()));
        } else {
            profilePictureView.setUserId(null);
            greeting.setText(null);
        }
    }

    private void handlePendingActionAfterReauthorize() {
        switch (pendingActionAfterReauthorize) {
            case POST_PHOTO:
                postPhoto();
                break;
            case POST_STATUS_UPDATE:
                postStatusUpdate();
                break;
        }
        pendingActionAfterReauthorize = PendingActionAfterReauthorize.NONE;
    }

    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }

    private void showAlert(String message, GraphObject result, Exception exception) {
        String title = null;
        String alertMessage = null;
        if (exception == null) {
            title = "Success";
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = String.format("Successfully posted '%s'.\nPost ID: %s", message, id);
        } else {
            title = "Error";
            alertMessage = exception.getMessage();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(alertMessage).setPositiveButton("OK", null);
        builder.show();
    }

    private void onClickPostStatusUpdate() {
        performPublish(PendingActionAfterReauthorize.POST_STATUS_UPDATE);
    }

    private void postStatusUpdate() {
        final String message = String
                .format("Updating status for %s at %s", user.getFirstName(), (new Date().toString()));
        Request request = Request.newStatusUpdateRequest(Session.getActiveSession(), message, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                showAlert(message, response.getGraphObject(), response.getError());
            }
        });
        Request.executeBatchAsync(request);
    }

    private void onClickPostPhoto() {
        performPublish(PendingActionAfterReauthorize.POST_PHOTO);
    }

    private void postPhoto() {
        Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.icon);
        Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Request.Callback() {
            @Override
            public void onCompleted(Response response) {
                showAlert("Photo Post", response.getGraphObject(), response.getError());
            }
        });
        Request.executeBatchAsync(request);
    }

    private void onClickPickFriends() {
        Intent intent = new Intent(this, PickFriendsActivity.class);
        startActivityForResult(intent, PICK_FRIENDS_ACTIVITY);
    }

    private void onClickPickPlace() {
        Intent intent = new Intent(this, PickPlaceActivity.class);
        PickPlaceActivity.populateParameters(intent, SEATTLE_LOCATION, null);
        startActivityForResult(intent, PICK_PLACE_ACTIVITY);
    }

    private interface PublishAction {
        void publish();
    }

    private void performPublish(PendingActionAfterReauthorize action) {
        Session session = Session.getActiveSession();
        if (session != null) {
            pendingActionAfterReauthorize = action;
            if (session.getPermissions().contains("publish_actions")) {
                // We can do the action right away.
                handlePendingActionAfterReauthorize();
            } else {
                // We need to reauthorize, then complete the action when we get called back.
                session.reauthorize(this, SessionLoginBehavior.SSO_WITH_FALLBACK,
                        Arrays.asList(new String[]{"publish_actions"}), REAUTHORIZE_ACTIVITY);
            }
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI();
        }
    };


}
