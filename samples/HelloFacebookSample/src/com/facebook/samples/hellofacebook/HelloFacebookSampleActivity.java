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
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class HelloFacebookSampleActivity extends FacebookActivity {
    @SuppressWarnings("serial")
    private static final List<String> PERMISSIONS = new ArrayList<String>() {{
        add("publish_actions");
    }};

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
    private PendingAction pendingAction = PendingAction.NONE;
    private final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };
    private GraphUser user;

    private enum PendingAction {
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
                // It's possible that we were waiting for this.user to be populated in order to post a
                // status update.
                handlePendingAction();
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

        outState.putInt(PENDING_ACTION_BUNDLE_KEY, pendingAction.ordinal());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int ordinal = savedInstanceState.getInt(PENDING_ACTION_BUNDLE_KEY, 0);
        pendingAction = PendingAction.values()[ordinal];
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
                        results = getString(R.string.no_friends_selected);
                    }
                } else {
                    results = getString(R.string.cancelled_brackets);
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.you_picked)).setMessage(results)
                        .setPositiveButton(getString(R.string.ok), null);
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
                        results = getString(R.string.no_place_selected);
                    }
                } else {
                    results = getString(R.string.cancelled_brackets);
                }

                builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.you_picked)).setMessage(results)
                        .setPositiveButton(getString(R.string.ok), null);
                builder.show();

                break;
        }
    }

    @Override
    protected void onSessionStateChange(SessionState state, Exception exception) {
        super.onSessionStateChange(state, exception);
        if (pendingAction != PendingAction.NONE &&
                exception instanceof FacebookOperationCanceledException) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.cancelled))
                    .setMessage(getString(R.string.permission_not_granted))
                    .setPositiveButton(getString(R.string.ok), null)
                    .show();
            pendingAction = PendingAction.NONE;
        } else if (state == SessionState.OPENED_TOKEN_UPDATED) {
            handlePendingAction();
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
            greeting.setText(getString(R.string.hello_user, user.getFirstName()));
        } else {
            profilePictureView.setUserId(null);
            greeting.setText(null);
        }
    }

    @SuppressWarnings("incomplete-switch")
    private void handlePendingAction() {
        PendingAction previouslyPendingAction = pendingAction;
        // These actions may re-set pendingAction if they are still pending, but we assume they
        // will succeed.
        pendingAction = PendingAction.NONE;

        switch (previouslyPendingAction) {
            case POST_PHOTO:
                postPhoto();
                break;
            case POST_STATUS_UPDATE:
                postStatusUpdate();
                break;
        }
    }

    private interface GraphObjectWithId extends GraphObject {
        String getId();
    }

    private void showAlert(String message, GraphObject result, FacebookRequestError error) {
        String title = null;
        String alertMessage = null;
        if (error == null) {
            title = getString(R.string.success);
            String id = result.cast(GraphObjectWithId.class).getId();
            alertMessage = getString(R.string.successfully_posted_post, message, id);
        } else {
            title = getString(R.string.error);
            alertMessage = error.getErrorMessage();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setMessage(alertMessage).setPositiveButton(getString(R.string.ok), null);
        builder.show();
    }

    private void onClickPostStatusUpdate() {
        performPublish(PendingAction.POST_STATUS_UPDATE);
    }

    private void postStatusUpdate() {
        if (user != null && hasPublishPermission()) {
            final String message = getString(R.string.status_update, user.getFirstName(), (new Date().toString()));
            Request request = Request
                    .newStatusUpdateRequest(Session.getActiveSession(), message, new Request.Callback() {
                        @Override
                        public void onCompleted(Response response) {
                            showAlert(message, response.getGraphObject(), response.getError());
                        }
                    });
            Request.executeBatchAsync(request);
        } else {
            pendingAction = PendingAction.POST_STATUS_UPDATE;
        }
    }

    private void onClickPostPhoto() {
        performPublish(PendingAction.POST_PHOTO);
    }

    private void postPhoto() {
        if (hasPublishPermission()) {
            Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.icon);
            Request request = Request.newUploadPhotoRequest(Session.getActiveSession(), image, new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    showAlert(getString(R.string.photo_post), response.getGraphObject(), response.getError());
                }
            });
            Request.executeBatchAsync(request);
        } else {
            pendingAction = PendingAction.POST_PHOTO;
        }
    }

    private void onClickPickFriends() {
        Intent intent = new Intent(this, PickFriendsActivity.class);
        startActivityForResult(intent, PICK_FRIENDS_ACTIVITY);
    }

    private void onClickPickPlace() {
        Intent intent = new Intent(this, PickPlaceActivity.class);
        PickPlaceActivity.populateParameters(intent, SEATTLE_LOCATION, null, getString(R.string.pick_seattle_place));
        startActivityForResult(intent, PICK_PLACE_ACTIVITY);
    }

    private boolean hasPublishPermission() {
        Session session = Session.getActiveSession();
        return session != null && session.getPermissions().contains("publish_actions");
    }

    private void performPublish(PendingAction action) {
        Session session = Session.getActiveSession();
        if (session != null) {
            pendingAction = action;
            if (hasPublishPermission()) {
                // We can do the action right away.
                handlePendingAction();
            } else {
                // We need to reauthorize, then complete the action when we get called back.
                Session.ReauthorizeRequest reauthRequest = new Session.ReauthorizeRequest(this, PERMISSIONS).
                        setRequestCode(REAUTHORIZE_ACTIVITY).
                        setLoginBehavior(SessionLoginBehavior.SSO_WITH_FALLBACK);
                session.reauthorizeForPublish(reauthRequest);
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
