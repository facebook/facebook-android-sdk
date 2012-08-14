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

package com.facebook.samples.friendpicker;

import android.app.AlertDialog;
import android.support.v4.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import com.facebook.FacebookException;
import com.facebook.FriendPickerFragment;
import com.facebook.Session;

public class FriendPickerSampleActivity extends FragmentActivity {
    FriendPickerFragment friendPickerFragment;

    Button pickFriendsButton;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        friendPickerFragment = (FriendPickerFragment) fm.findFragmentById(R.id.friend_picker_fragment);
        friendPickerFragment.setOnErrorListener(new FriendPickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                AlertDialog.Builder builder = new AlertDialog.Builder(FriendPickerSampleActivity.this);
                builder.setTitle("Error").setMessage(error.getMessage()).setPositiveButton("OK", null);
                builder.show();
            }
        });

        Session.sessionOpen(this, "370546396320150");

        // TODO port: move this to another activity, have a button launch it
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private void onClickPickFriends() {
        friendPickerFragment.loadData();
    }

}
