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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.GraphUser;
import com.facebook.Session;

import java.util.ArrayList;
import java.util.Collection;

public class FriendPickerSampleActivity extends FragmentActivity {
    private final int PICK_FRIENDS_ACTIVITY = 1;
    private final String APP_ID = "370546396320150";
    private Button pickFriendsButton;
    private TextView resultsTextView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        resultsTextView = (TextView) findViewById(R.id.resultsTextView);
        pickFriendsButton = (Button) findViewById(R.id.pickFriendsButton);
        pickFriendsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickPickFriends();
            }
        });

        Session.sessionOpen(this, APP_ID);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_FRIENDS_ACTIVITY:
                String results = "";
                if (resultCode == RESULT_OK) {
                    FriendPickerApplication application = (FriendPickerApplication)getApplication();
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
                resultsTextView.setText(results);
                break;
            default:
                Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
                break;
        }
    }

    private void onClickPickFriends() {
        Intent intent = new Intent(this, PickFriendsActivity.class);
        // Note: The following line is optional, as multi-select behavior is the default for
        // FriendPickerFragment. It is here to demonstrate how parameters could be passed to the
        // friend picker if single-select functionality was desired, or if a different user ID was
        // desired (for instance, to see friends of a friend).
        PickFriendsActivity.populateParameters(intent, null, true);
        startActivityForResult(intent, PICK_FRIENDS_ACTIVITY);
    }
}
