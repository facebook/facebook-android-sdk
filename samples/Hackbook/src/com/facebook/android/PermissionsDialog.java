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

package com.facebook.android;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TabHost.TabSpec;
import com.facebook.android.Facebook.DialogListener;

import java.util.Vector;

@SuppressWarnings("deprecation")
public class PermissionsDialog extends Dialog {

    private final static int TAB_HEIGHT = 50;

    private Button mGetPermissions;
    private TextView mPermissionDetails;

    private Activity activity;

    private ListView userPermissionsList, friendPermissionsList, extendedPermissionsList;

    private BaseAdapter userPermissionsAdapter, friendPermissionsAdapter,
            extendedPermissionAdapter;

    protected Vector<String> reqPermVector;

    String[] user_permissions = { "user_about_me", "user_activities", "user_birthday",
            "user_checkins", "user_education_history", "user_events", "user_groups",
            "user_hometown", "user_interests", "user_likes", "user_location", "user_notes",
            "user_online_presence", "user_photos", "user_photo_video_tags", "user_relationships",
            "user_relationship_details", "user_religion_politics", "user_status", "user_videos",
            "user_website", "user_work_history" };

    String[] friend_permissions = { "friends_about_me", "friends_activities", "friends_birthday",
            "friends_checkins", "friends_education_history", "friends_events", "friends_groups",
            "friends_hometown", "friends_interests", "friends_likes", "friends_location",
            "friends_notes", "friends_online_presence", "friends_photos",
            "friends_photo_video_tags", "friends_relationships", "friends_relationship_details",
            "friends_religion_politics", "friends_status", "friends_videos", "friends_website",
            "friends_work_history" };

    String[] extended_permissions = { "ads_management", "create_event", "create_note", "email",
            "export_stream", "manage_friendlists", "manage_groups", "manage_pages",
            "offline_access", "publish_actions", "photo_upload", "publish_checkins",
            "publish_stream", "read_friendlists", "read_insights", "read_mailbox", "read_requests",
            "read_stream", "rsvp_event", "share_item", "status_update", "sms", "video_upload",
            "xmpp_login" };

    public PermissionsDialog(Activity activity) {
        super(activity);
        this.activity = activity;
        setTitle(activity.getString(R.string.permissions_request));
        reqPermVector = new Vector<String>();
    }

    /*
     * Layout the permission dialog
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.permissions_list);
        LayoutParams params = getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        params.height = LayoutParams.FILL_PARENT;
        getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        mPermissionDetails = (TextView) findViewById(R.id.permission_detail);
        mPermissionDetails.setMovementMethod(LinkMovementMethod.getInstance());

        userPermissionsList = (ListView) findViewById(R.id.user_permissions_list);
        friendPermissionsList = (ListView) findViewById(R.id.friend_permissions_list);
        extendedPermissionsList = (ListView) findViewById(R.id.extended_permissions_list);

        userPermissionsAdapter = new PermissionsListAdapter(user_permissions);
        userPermissionsList.setAdapter(userPermissionsAdapter);

        friendPermissionsAdapter = new PermissionsListAdapter(friend_permissions);
        friendPermissionsList.setAdapter(friendPermissionsAdapter);

        extendedPermissionAdapter = new PermissionsListAdapter(extended_permissions);
        extendedPermissionsList.setAdapter(extendedPermissionAdapter);

        TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        TabSpec spec1 = tabHost.newTabSpec("Tab 1");
        spec1.setIndicator(activity.getString(R.string.user));
        spec1.setContent(R.id.user_permissions_list);

        TabSpec spec2 = tabHost.newTabSpec("Tab 2");
        spec2.setIndicator(activity.getString(R.string.friend));
        spec2.setContent(R.id.friend_permissions_list);

        TabSpec spec3 = tabHost.newTabSpec("Tab 3");
        spec3.setIndicator(activity.getString(R.string.extended));
        spec3.setContent(R.id.extended_permissions_list);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);
        tabHost.setCurrentTab(0);
        tabHost.getTabWidget().getChildAt(0).getLayoutParams().height = TAB_HEIGHT;
        tabHost.getTabWidget().getChildAt(1).getLayoutParams().height = TAB_HEIGHT;
        tabHost.getTabWidget().getChildAt(2).getLayoutParams().height = TAB_HEIGHT;

        mGetPermissions = (Button) findViewById(R.id.get_permissions_button);

        mGetPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Source Tag: perms_tag Call authorize to get the new
                 * permissions
                 */
                if (reqPermVector.isEmpty() && Utility.mFacebook.isSessionValid()) {
                    Toast.makeText(activity.getBaseContext(), "No Permissions selected.",
                            Toast.LENGTH_SHORT).show();
                    PermissionsDialog.this.dismiss();
                } else {
                    String[] permissions = reqPermVector.toArray(new String[0]);
                    Utility.mFacebook.authorize(activity, permissions, new LoginDialogListener());
                }
            }
        });
    }

    /*
     * Callback when user has authorized the app with the new permissions
     */
    private final class LoginDialogListener implements DialogListener {
        @Override
        public void onComplete(Bundle values) {
            // Inform the parent loginlistener so it can update the user's
            // profile pic and name on the home screen.
            SessionEvents.onLoginSuccess();

            Toast.makeText(activity.getBaseContext(), "New Permissions granted.",
                    Toast.LENGTH_SHORT).show();
            PermissionsDialog.this.dismiss();
        }

        @Override
        public void onFacebookError(FacebookError error) {
            Toast.makeText(activity.getBaseContext(),
                    "Facebook Error! No new permissions granted.", Toast.LENGTH_SHORT).show();
            PermissionsDialog.this.dismiss();
        }

        @Override
        public void onError(DialogError error) {
            Toast.makeText(activity.getBaseContext(), "Error! No new permissions granted.",
                    Toast.LENGTH_SHORT).show();
            PermissionsDialog.this.dismiss();
        }

        @Override
        public void onCancel() {
            Toast.makeText(activity.getBaseContext(),
                    "Action cancelled, No new permissions granted.", Toast.LENGTH_SHORT).show();
            PermissionsDialog.this.dismiss();
        }
    }

    /**
     * Definition of the list adapter
     */
    public class PermissionsListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        String[] permissions;
        boolean[] isChecked;

        public PermissionsListAdapter(String[] permissions) {
            this.permissions = permissions;
            this.isChecked = new boolean[permissions.length];
            mInflater = LayoutInflater.from(activity.getBaseContext());
        }

        @Override
        public int getCount() {
            return permissions.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View hView = convertView;
            CheckBox checkbox;

            if (hView == null) {
                hView = mInflater.inflate(R.layout.permission_item, null);
                checkbox = (CheckBox) hView.findViewById(R.id.permission_checkbox);
                hView.setTag(checkbox);
            } else {
                checkbox = (CheckBox) hView.getTag();
            }
            checkbox.setText(this.permissions[position]);
            checkbox.setId(position);
            if (Utility.currentPermissions.containsKey(this.permissions[position])
                    && Utility.currentPermissions.get(this.permissions[position]).equals("1")) {
                checkbox.setTextColor(Color.GREEN);
                checkbox.setChecked(true);
                checkbox.setEnabled(false);
                checkbox.setOnCheckedChangeListener(null);
            } else {
                checkbox.setTextColor(Color.WHITE);
                checkbox.setChecked(this.isChecked[position]);
                checkbox.setEnabled(true);
                checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton button, boolean checked) {
                        isChecked[button.getId()] = checked;
                        if (checked) {
                            reqPermVector.add(button.getText().toString());
                        } else if (reqPermVector.contains(button.getText())) {
                            reqPermVector.remove(button.getText());
                        }
                    }
                });
            }
            return hView;
        }
    }
}
