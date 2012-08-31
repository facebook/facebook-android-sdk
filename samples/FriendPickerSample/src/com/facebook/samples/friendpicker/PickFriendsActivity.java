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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.FacebookException;
import com.facebook.FriendPickerFragment;
import com.facebook.GraphUser;
import com.facebook.PickerFragment;

// This class provides an example of an Activity that uses FriendPickerFragment to display a list of
// the user's friends. It takes a programmatic approach to creating the FriendPickerFragment with the
// desired parameters -- see PickPlaceActivity in the PlacePickerSample project for an example of an
// Activity creating a fragment (in this case a PlacePickerFragment) via XML layout rather than
// programmatically.
public class PickFriendsActivity extends FragmentActivity {
    FriendPickerFragment friendPickerFragment;
    Button doneButton;
    TextView titleView;

    // A helper to simplify life for callers who want to populate a Bundle with the necessary
    // parameters. A more sophisticated Activity might define its own set of parameters; our needs
    // are simple, so we just populate what we want to pass to the FriendPickerFragment.
    public static void populateParameters(Intent intent, String userId, boolean multiSelect) {
        intent.putExtra(FriendPickerFragment.USER_ID_BUNDLE_KEY, userId);
        intent.putExtra(FriendPickerFragment.MULTI_SELECT_BUNDLE_KEY, multiSelect);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_friends_activity);

        FragmentManager fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            // First time through, we create our fragment programmatically.
            final Bundle args = getIntent().getExtras();
            friendPickerFragment = new FriendPickerFragment(args);
            fm.beginTransaction()
                    .add(R.id.friend_picker_fragment, friendPickerFragment)
                    .commit();
        } else {
            // Subsequent times, our fragment is recreated by the framework and already has saved and
            // restored its state, so we don't need to specify args again. (In fact, this might be
            // incorrect if the fragment was modified programmatically since it was created.)
            friendPickerFragment = (FriendPickerFragment) fm.findFragmentById(R.id.friend_picker_fragment);
        }

        friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                PickFriendsActivity.this.onError(error);
            }
        });

        doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // We just store our selection in the Application for other activities to look at.
                FriendPickerApplication application = (FriendPickerApplication) getApplication();
                application.setSelectedUsers(friendPickerFragment.getSelection());

                setResult(RESULT_OK, null);
                finish();
            }
        });

        titleView = (TextView) findViewById(R.id.title);
    }

    private void onError(Exception error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error").setMessage(error.getMessage()).setPositiveButton("OK", null);
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            // Load data, unless a query has already taken place.
            friendPickerFragment.loadData(false);
        } catch (Exception ex) {
            onError(ex);
        }
    }
}
