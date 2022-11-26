/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.hellofacebook;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import com.facebook.*;
import com.facebook.AccessToken;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.ProfilePictureView;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import java.util.ArrayList;
import java.util.Arrays;

public class HelloFacebookSampleActivity extends FragmentActivity {

  private static final String PERMISSION = "publish_actions";
  private static final Location SEATTLE_LOCATION =
      new Location("") {
        {
          setLatitude(47.6097);
          setLongitude(-122.3331);
        }
      };

  private final String PENDING_ACTION_BUNDLE_KEY = "com.example.hellofacebook:PendingAction";

  private Button postStatusUpdateButton;
  private Button postPhotoButton;
  private ProfilePictureView profilePictureView;
  private TextView greeting;
  private PendingAction pendingAction = PendingAction.NONE;
  private boolean canPresentShareDialog;
  private boolean canPresentShareDialogWithPhotos;
  private CallbackManager callbackManager;
  private ProfileTracker profileTracker;
  private ShareDialog shareDialog;
  private FacebookCallback<Sharer.Result> shareCallback =
      new FacebookCallback<Sharer.Result>() {
        @Override
        public void onCancel() {
          Log.d("HelloFacebook", "Canceled");
        }

        @Override
        public void onError(@NonNull FacebookException error) {
          Log.d("HelloFacebook", String.format("Error: %s", error.toString()));
          String title = getString(R.string.error);
          String alertMessage = error.getMessage();
          showResult(title, alertMessage);
        }

        @Override
        public void onSuccess(@NonNull Sharer.Result result) {
          Log.d("HelloFacebook", "Success!");
          if (result.getPostId() != null) {
            String title = getString(R.string.success);
            String id = result.getPostId();
            String alertMessage = getString(R.string.successfully_posted_post, id);
            showResult(title, alertMessage);
          }
        }

        private void showResult(String title, String alertMessage) {
          new AlertDialog.Builder(HelloFacebookSampleActivity.this)
              .setTitle(title)
              .setMessage(alertMessage)
              .setPositiveButton(R.string.ok, null)
              .show();
        }
      };

  private enum PendingAction {
    NONE,
    POST_PHOTO,
    POST_STATUS_UPDATE
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    callbackManager = CallbackManager.Factory.create();

    LoginManager.getInstance()
        .registerCallback(
            callbackManager,
            new FacebookCallback<LoginResult>() {
              @Override
              public void onSuccess(@NonNull LoginResult loginResult) {
                handlePendingAction();
                updateUI();
              }

              @Override
              public void onCancel() {
                if (pendingAction != PendingAction.NONE) {
                  showAlert();
                  pendingAction = PendingAction.NONE;
                }
                updateUI();
              }

              @Override
              public void onError(@NonNull FacebookException exception) {
                if (pendingAction != PendingAction.NONE
                    && exception instanceof FacebookAuthorizationException) {
                  showAlert();
                  pendingAction = PendingAction.NONE;
                }
                updateUI();
              }

              private void showAlert() {
                new AlertDialog.Builder(HelloFacebookSampleActivity.this)
                    .setTitle(R.string.cancelled)
                    .setMessage(R.string.permission_not_granted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
              }
            });

    shareDialog = new ShareDialog(this);
    shareDialog.registerCallback(callbackManager, shareCallback);

    if (savedInstanceState != null) {
      String name = savedInstanceState.getString(PENDING_ACTION_BUNDLE_KEY);
      pendingAction = PendingAction.valueOf(name);
    }

    setContentView(R.layout.main);

    profileTracker =
        new ProfileTracker() {
          @Override
          protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
            updateUI();
            // It's possible that we were waiting for Profile to be populated in order to
            // post a status update.
            handlePendingAction();
          }
        };

    profilePictureView = (ProfilePictureView) findViewById(R.id.profilePicture);
    greeting = (TextView) findViewById(R.id.greeting);

    postStatusUpdateButton = (Button) findViewById(R.id.postStatusUpdateButton);
    postStatusUpdateButton.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View view) {
            onClickPostStatusUpdate();
          }
        });

    postPhotoButton = (Button) findViewById(R.id.postPhotoButton);
    postPhotoButton.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View view) {
            onClickPostPhoto();
          }
        });

    // Can we present the share dialog for regular links?
    canPresentShareDialog = ShareDialog.canShow(ShareLinkContent.class);

    // Can we present the share dialog for photos?
    canPresentShareDialogWithPhotos = ShareDialog.canShow(SharePhotoContent.class);
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateUI();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putString(PENDING_ACTION_BUNDLE_KEY, pendingAction.name());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    callbackManager.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    profileTracker.stopTracking();
    LoginManager.getInstance().unregisterCallback(callbackManager);
  }

  private void updateUI() {
    boolean enableButtons = AccessToken.isCurrentAccessTokenActive();

    postStatusUpdateButton.setEnabled(enableButtons || canPresentShareDialog);
    postPhotoButton.setEnabled(enableButtons || canPresentShareDialogWithPhotos);

    Profile profile = Profile.getCurrentProfile();
    if (enableButtons && profile != null) {
      profilePictureView.setProfileId(profile.getId());
      greeting.setText(getString(R.string.hello_user, profile.getFirstName()));
    } else {
      profilePictureView.setProfileId(null);
      greeting.setText(null);
    }
  }

  private void handlePendingAction() {
    PendingAction previouslyPendingAction = pendingAction;
    // These actions may re-set pendingAction if they are still pending, but we assume they
    // will succeed.
    pendingAction = PendingAction.NONE;

    switch (previouslyPendingAction) {
      case NONE:
        break;
      case POST_PHOTO:
        postPhoto();
        break;
      case POST_STATUS_UPDATE:
        postStatusUpdate();
        break;
    }
  }

  private void onClickPostStatusUpdate() {
    performPublish(PendingAction.POST_STATUS_UPDATE, canPresentShareDialog);
  }

  private void postStatusUpdate() {
    Profile profile = Profile.getCurrentProfile();
    ShareLinkContent linkContent =
        new ShareLinkContent.Builder()
            .setContentUrl(Uri.parse("http://developers.facebook.com/docs/android"))
            .build();
    if (canPresentShareDialog) {
      shareDialog.show(linkContent);
    } else if (profile != null && hasPublishPermission()) {
      ShareApi.share(linkContent, shareCallback);
    } else {
      pendingAction = PendingAction.POST_STATUS_UPDATE;
    }
  }

  private void onClickPostPhoto() {
    performPublish(PendingAction.POST_PHOTO, canPresentShareDialogWithPhotos);
  }

  private void postPhoto() {
    Bitmap image = BitmapFactory.decodeResource(this.getResources(), R.drawable.icon);
    SharePhoto sharePhoto = new SharePhoto.Builder().setBitmap(image).build();
    ArrayList<SharePhoto> photos = new ArrayList<>();
    photos.add(sharePhoto);

    SharePhotoContent sharePhotoContent = new SharePhotoContent.Builder().setPhotos(photos).build();
    if (canPresentShareDialogWithPhotos) {
      shareDialog.show(sharePhotoContent);
    } else if (hasPublishPermission()) {
      ShareApi.share(sharePhotoContent, shareCallback);
    } else {
      pendingAction = PendingAction.POST_PHOTO;
      // We need to get new permissions, then complete the action when we get called back.
      LoginManager.getInstance().logInWithPublishPermissions(this, Arrays.asList(PERMISSION));
    }
  }

  private boolean hasPublishPermission() {
    return AccessToken.isCurrentAccessTokenActive()
        && AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions");
  }

  private void performPublish(PendingAction action, boolean allowNoToken) {
    if (AccessToken.isCurrentAccessTokenActive() || allowNoToken) {
      pendingAction = action;
      handlePendingAction();
    }
  }
}
