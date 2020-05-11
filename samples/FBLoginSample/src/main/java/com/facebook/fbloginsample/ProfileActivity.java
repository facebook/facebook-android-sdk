/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
