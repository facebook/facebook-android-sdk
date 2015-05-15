/**
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.switchuser;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.*;
import android.widget.*;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.ProfilePictureView;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsFragment extends ListFragment {

    public static final String TAG = "SettingsFragment";
    private static final String CURRENT_SLOT_KEY = "CurrentSlot";

    private SlotManager slotManager;
    private Menu optionsMenu;
    private CallbackManager callbackManager;
    private ProfileTracker profileTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        slotManager = new SlotManager();
        slotManager.restore(
                getActivity(),
                savedInstanceState != null ?
                        savedInstanceState.getInt(CURRENT_SLOT_KEY, SlotManager.NO_SLOT) :
                        SlotManager.NO_SLOT);
        ArrayList<Slot> slotList = new ArrayList<Slot>(
                Arrays.asList(slotManager.getAllSlots()));

        Slot currentSlot = slotManager.getSelectedSlot();
        if (currentSlot != null && currentSlot.getAccessToken() != null) {
            AccessToken.setCurrentAccessToken(currentSlot.getAccessToken());
        }

        setListAdapter(new SlotAdapter(slotList));
        setHasOptionsMenu(true);
        setUpCallbacks();
        currentUserChanged();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.context_settings, menu);
        optionsMenu = menu;
        updateMenuVisibility();
    }

    private void setUpCallbacks() {
        callbackManager = CallbackManager.Factory.create();
        LoginManager manager = LoginManager.getInstance();
        manager.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Profile.fetchProfileForCurrentAccessToken();
            }

            @Override
            public void onError(FacebookException exception) {
                AccessToken.setCurrentAccessToken(null);
                currentUserChanged();
            }

            @Override
            public void onCancel() {
                AccessToken.setCurrentAccessToken(null);
                currentUserChanged();
            }
        });

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                Slot currentSlot = slotManager.getSelectedSlot();
                AccessToken currentAccessToken = AccessToken.getCurrentAccessToken();
                if(currentSlot != null && currentAccessToken != null && currentProfile != null) {
                    currentSlot.setUserInfo(
                            new UserInfo(currentProfile.getName(), currentAccessToken));
                    currentUserChanged();
                }
            }
        };
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        slotManager.setCurrentUserSlot(position);
        Slot newSlot = slotManager.getSelectedSlot();
        if (newSlot.getAccessToken() == null) {
            final LoginManager manager = LoginManager.getInstance();
            manager.setLoginBehavior(newSlot.getLoginBehavior());
            manager.logInWithReadPermissions(this, null);
        } else {
            AccessToken.setCurrentAccessToken(newSlot.getAccessToken());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Slot slot = slotManager.getSelectedSlot();

        switch (item.getItemId()) {
            case R.id.menu_item_clear_slot:
                if (slot.getUserId() != null) {
                    // Clear out data that this app stored in the cache
                    // Not calling Session.closeAndClearTokenInformation() because we have
                    // additional data stored in the cache.
                    slot.clear();
                    if (slot == slotManager.getSelectedSlot()) {
                        slotManager.setCurrentUserSlot(SlotManager.NO_SLOT);
                    }

                    currentUserChanged();
                }
                return true;
        }

        return super.onContextItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(CURRENT_SLOT_KEY, slotManager.getSelectedSlotNumber());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        profileTracker.stopTracking();
    }

    private void updateMenuVisibility() {
        if (optionsMenu != null) {
            if (slotManager.getSelectedSlot() == null) {
                optionsMenu.setGroupVisible(0, false);
            } else if (optionsMenu != null) {
                optionsMenu.setGroupVisible(0, true);
            }
        }
    }

    private void currentUserChanged() {
        if (slotManager == null) {
            // Fragment has not had onCreate called yet.
            return;
        }

        updateMenuVisibility();
        updateListView();
        Slot currentSlot = slotManager.getSelectedSlot();
        AccessToken currentToken = (currentSlot != null) ? currentSlot.getAccessToken() : null;
        AccessToken.setCurrentAccessToken(currentToken);
    }

    private void updateListView() {
        SlotAdapter adapter = (SlotAdapter) getListAdapter();

        // Adapter will be null if the list is not shown
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private class SlotManager {
        static final int NO_SLOT = -1;

        private final static int MAX_SLOTS = 4;

        private static final String SETTINGS_CURRENT_SLOT_KEY = "CurrentSlot";
        private static final String SETTINGS_NAME = "UserManagerSettings";

        private SharedPreferences settings;
        private int selectedSlotNumber = NO_SLOT;

        private Slot[] slots;

        public void restore(Context context, int oldSelectedSlot) {
            if (context == null) {
                throw new IllegalArgumentException("context cannot be null");
            }

            slots = new Slot[MAX_SLOTS];
            for (int i = 0; i < MAX_SLOTS; i++) {
                LoginBehavior loginBehavior = (i == 0) ?
                        LoginBehavior.SSO_WITH_FALLBACK :
                        LoginBehavior.SUPPRESS_SSO;
                slots[i] = new Slot(i, loginBehavior);
            }

            // Restore the last known state from when the app ran last.
            settings = FacebookSdk.getApplicationContext().getSharedPreferences(
                    SETTINGS_NAME, Context.MODE_PRIVATE);
            int savedSlotNumber = settings.getInt(SETTINGS_CURRENT_SLOT_KEY, NO_SLOT);
            if (savedSlotNumber != NO_SLOT && savedSlotNumber != oldSelectedSlot) {
                // This will trigger the full flow of login
                setCurrentUserSlot(savedSlotNumber);
            } else {
                // We already knew which slot was selected. So don't notify that a new slot was
                // selected since that will log out and start login process. And
                // doing so will have the effect of clearing out state like the profile pic.
                setCurrentUserSlot(savedSlotNumber);
            }
        }

        public Slot getSelectedSlot() {
            if (selectedSlotNumber == NO_SLOT) {
                return null;
            } else {
                return getSlot(selectedSlotNumber);
            }
        }

        public int getSelectedSlotNumber() {
            return selectedSlotNumber;
        }

        public void setCurrentUserSlot(int slot) {
            if (slot != selectedSlotNumber) {
                // Store the selected slot number for when the app is closed and restarted
                settings.edit().putInt(SETTINGS_CURRENT_SLOT_KEY, slot).apply();
                selectedSlotNumber = slot;
                currentUserChanged();
            }
        }

        private Slot[] getAllSlots() {
            return slots;
        }

        private Slot getSlot(int slot) {
            validateSlot(slot);
            return slots[slot];
        }

        private void validateSlot(int slot) {
            if (slot <= NO_SLOT || slot >= MAX_SLOTS) {
                throw new IllegalArgumentException(
                        String.format("Choose a slot between 0 and %d inclusively", MAX_SLOTS - 1));
            }
        }
    }

    private class SlotAdapter extends ArrayAdapter<Slot> {

        public SlotAdapter(ArrayList<Slot> slots) {
            super(getActivity(), android.R.layout.simple_list_item_1, slots);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (null == convertView) {
                convertView = getActivity().getLayoutInflater()
                        .inflate(R.layout.list_item_user, parent, false);
            }

            Slot slot = getItem(position);
            if (slot.getLoginBehavior() != LoginBehavior.SUPPRESS_SSO) {
                convertView.setBackgroundColor(Color.argb(50, 255, 255, 255));
            }

            String userName = slot.getUserName();
            if (userName == null) {
                userName = getString(R.string.empty_slot);
            }

            String userId = slot.getUserId();
            ProfilePictureView profilePic = (ProfilePictureView) convertView.findViewById(
                    R.id.slotPic);
            if (userId != null) {
                profilePic.setProfileId(userId);
            } else {
                profilePic.setProfileId(null);
            }

            TextView userNameTextView = (TextView) convertView.findViewById(
                    R.id.slotUserName);
            userNameTextView.setText(userName);

            final CheckBox currentUserCheckBox = (CheckBox) convertView.findViewById(
                    R.id.currentUserIndicator);
            currentUserCheckBox.setChecked(
                    slotManager.getSelectedSlot() == slot
                            && slot.getUserInfo() != null);
            currentUserCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentUserCheckBox.isChecked()) {
                        slotManager.setCurrentUserSlot(position);
                    } else {
                        slotManager.setCurrentUserSlot(SlotManager.NO_SLOT);
                    }
                    SlotAdapter adapter = (SlotAdapter) getListAdapter();
                    adapter.notifyDataSetChanged();
                }
            });

            currentUserCheckBox.setEnabled(slot.getAccessToken() != null);

            return convertView;
        }

    }
}
