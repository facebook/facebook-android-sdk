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

package com.facebook.samples.placepicker;

import android.app.AlertDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.facebook.FacebookException;
import com.facebook.PickerFragment;
import com.facebook.PlacePickerFragment;

// This class provides an example of an Activity that uses PlacePickerFragment to display a list of
// the places. It takes a layout-based approach to creating the PlacePickerFragment with the
// desired parameters -- see PickFriendActivity in the FriendPickerSample project for an example of an
// Activity creating a fragment (in this case a FriendPickerFragment) programmatically rather than
// via XML layout.
public class PickPlaceActivity extends FragmentActivity {
    PlacePickerFragment placePickerFragment;
    Button doneButton;
    TextView titleView;
    EditText searchBox;

    // A helper to simplify life for callers who want to populate a Bundle with the necessary
    // parameters. A more sophisticated Activity might define its own set of parameters; our needs
    // are simple, so we just populate what we want to pass to the PlacePickerFragment.
    public static void populateParameters(Intent intent, Location location, String searchText) {
        intent.putExtra(PlacePickerFragment.LOCATION_BUNDLE_KEY, location);
        intent.putExtra(PlacePickerFragment.SEARCH_TEXT_BUNDLE_KEY, searchText);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_place_activity);

        FragmentManager fm = getSupportFragmentManager();
        placePickerFragment = (PlacePickerFragment) fm.findFragmentById(R.id.place_picker_fragment);
        if (savedInstanceState == null) {
            // If this is the first time we have created the fragment, update its properties based on
            // any parameters we received via our Intent.
            placePickerFragment.setSettingsFromBundle(getIntent().getExtras());
        }

        placePickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
            @Override
            public void onError(FacebookException error) {
                PickPlaceActivity.this.onError(error);
            }
        });

        // We finish the activity when either the Done button is pressed or when a place is
        // selected (since only a single place can be selected).
        placePickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged() {
                if (placePickerFragment.getSelection() != null) {
                    finishActivity();
                }
            }
        });
        doneButton = (Button) findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishActivity();
            }
        });

        searchBox = (EditText) findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new SearchTextWatcher());

        titleView = (TextView) findViewById(R.id.title);
    }

    private void finishActivity() {
        // We just store our selection in the Application for other activities to look at.
        PlacePickerApplication application = (PlacePickerApplication) getApplication();
        application.setSelectedPlace(placePickerFragment.getSelection());

        setResult(RESULT_OK, null);
        finish();
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
            placePickerFragment.loadData(false);
        } catch (Exception ex) {
            onError(ex);
        }
    }

    private class SearchTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            placePickerFragment.setSearchTextAndReload(s.toString(), false);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}
