/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fbloginsample.callbacks.GetUserCallback;
import com.facebook.fbloginsample.entities.User;
import com.facebook.fbloginsample.requests.UserRequest;

public class ProfileActivity extends Activity implements GetUserCallback.IGetUserResponse {
  private SimpleDraweeView mProfilePhotoView;
  private TextView mName;
  private TextView mId;
  private TextView mEmail;
  private TextView mPermissions;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_profile);

    mProfilePhotoView = findViewById(R.id.profile_photo);
    mName = findViewById(R.id.name);
    mId = findViewById(R.id.id);
    mEmail = findViewById(R.id.email);
    mPermissions = findViewById(R.id.permissions);
  }

  @Override
  protected void onResume() {
    super.onResume();
    UserRequest.makeUserRequest(new GetUserCallback(ProfileActivity.this).getCallback());
  }

  @Override
  public void onCompleted(User user) {
    mProfilePhotoView.setImageURI(user.getPicture());
    mName.setText(user.getName());
    mId.setText(user.getId());
    if (user.getEmail() == null) {
      mEmail.setText(R.string.no_email_perm);
      mEmail.setTextColor(Color.RED);
    } else {
      mEmail.setText(user.getEmail());
      mEmail.setTextColor(Color.BLACK);
    }
    mPermissions.setText(user.getPermissions());
  }
}
