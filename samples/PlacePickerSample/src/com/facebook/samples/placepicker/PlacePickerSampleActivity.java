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
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.GraphLocation;
import com.facebook.GraphPlace;
import com.facebook.PlacePickerFragment;
import com.facebook.Session;

public class PlacePickerSampleActivity extends FragmentActivity implements LocationListener {
    private final int PLACE_ACTIVITY = 1;
    private final String APP_ID = "378281678861098";
    private final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };
    private final Location SAN_FRANCISCO_LOCATION = new Location("") {
        {
            setLatitude(37.7750);
            setLongitude(-122.4183);
        }
    };
    private final Location PARIS_LOCATION = new Location("") {
        {
            setLatitude(48.857875);
            setLongitude(2.294635);
        }
    };

    private PlacePickerFragment placePickerFragment;
    private TextView resultsTextView;
    private LocationManager locationManager;
    private Location lastKnownLocation;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        resultsTextView = (TextView) findViewById(R.id.resultsTextView);
        Button button = (Button) findViewById(R.id.seattleButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickSeattle();
            }
        });

        button = (Button) findViewById(R.id.sanFranciscoButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickSanFrancisco();
            }
        });

        button = (Button) findViewById(R.id.gpsButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                onClickGPS();
            }
        });

        Session.sessionOpen(this, APP_ID);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private void onError(Exception exception) {
        AlertDialog.Builder builder = new AlertDialog.Builder(PlacePickerSampleActivity.this);
        builder.setTitle("Error").setMessage(exception.getMessage()).setPositiveButton("OK", null);
        builder.show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PLACE_ACTIVITY:
                String results = "";
                if (resultCode == RESULT_OK) {
                    PlacePickerApplication application = (PlacePickerApplication) getApplication();
                    GraphPlace selection = application.getSelectedPlace();
                    if (selection != null) {
                        GraphLocation location = selection.getLocation();

                        results = String.format("Name: %s\nCategory: %s\nLocation: (%f,%f)\nStreet: %s, %s, %s, %s, %s",
                                selection.getName(), selection.getCategory(),
                                location.getLatitude(), location.getLongitude(),
                                location.getStreet(), location.getCity(), location.getState(), location.getZip(),
                                location.getCountry());
                    } else {
                        results = "<No place selected>";
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

    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    private void onClickSeattle() {
        try {
            Intent intent = new Intent(this, PickPlaceActivity.class);
            PickPlaceActivity.populateParameters(intent, SEATTLE_LOCATION, null);

            startActivityForResult(intent, PLACE_ACTIVITY);
        } catch (Exception ex) {
            onError(ex);
        }
    }

    private void onClickSanFrancisco() {
        try {
            Intent intent = new Intent(this, PickPlaceActivity.class);
            PickPlaceActivity.populateParameters(intent, SAN_FRANCISCO_LOCATION, null);

            startActivityForResult(intent, PLACE_ACTIVITY);
        } catch (Exception ex) {
            onError(ex);
        }
    }

    private void onClickGPS() {
        try {
            if (lastKnownLocation == null) {
                Criteria criteria = new Criteria();
                String bestProvider = locationManager.getBestProvider(criteria, false);
                if (bestProvider != null) {
                    lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
                }
            }
            if (lastKnownLocation == null) {
                String model = android.os.Build.MODEL;
                if (model.equals("sdk") || model.equals("google_sdk") || model.contains("x86")) {
                    // Looks like they are on an emulator, pretend we're in Paris if we don't have a
                    // location set.
                    lastKnownLocation = PARIS_LOCATION;
                } else {
                    String text = "Could not obtain your current location.";
                    AlertDialog.Builder builder = new AlertDialog.Builder(PlacePickerSampleActivity.this);
                    builder.setTitle("Error").setMessage(text).setPositiveButton("OK", null);
                    builder.show();
                    return;
                }
            }

            Intent intent = new Intent(this, PickPlaceActivity.class);
            PickPlaceActivity.populateParameters(intent, lastKnownLocation, null);

            startActivityForResult(intent, PLACE_ACTIVITY);
        } catch (Exception ex) {
            onError(ex);
        }
    }

}
