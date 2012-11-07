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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("deprecation")
public class Places extends Activity implements OnItemClickListener {
    private Handler mHandler;
    private JSONObject location;

    protected ListView placesList;
    protected LocationManager lm;
    protected MyLocationListener locationListener;

    protected static JSONArray jsonArray;
    final static double TIMES_SQUARE_LAT = 40.756;
    final static double TIMES_SQUARE_LON = -73.987;

    protected ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
        location = new JSONObject();

        setContentView(R.layout.places_list);

        Bundle extras = getIntent().getExtras();
        String default_or_new = extras.getString("LOCATION");
        if (default_or_new.equals("times_square")) {
            try {
                location.put("latitude", new Double(TIMES_SQUARE_LAT));
                location.put("longitude", new Double(TIMES_SQUARE_LON));
            } catch (JSONException e) {
            }
            fetchPlaces();
        } else {
            getLocation();
        }
    }

    public void getLocation() {
        /*
         * launch a new Thread to get new location
         */
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                dialog = ProgressDialog.show(Places.this, "",
                        getString(R.string.fetching_location), false, true,
                        new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                showToast("No location fetched.");
                            }
                        });

                if (lm == null) {
                    lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                }

                if (locationListener == null) {
                    locationListener = new MyLocationListener();
                }

                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                String provider = lm.getBestProvider(criteria, true);
                if (provider != null && lm.isProviderEnabled(provider)) {
                    lm.requestLocationUpdates(provider, 1, 0, locationListener,
                            Looper.getMainLooper());
                } else {
                    /*
                     * GPS not enabled, prompt user to enable GPS in the
                     * Location menu
                     */
                    new AlertDialog.Builder(Places.this)
                            .setTitle(R.string.enable_gps_title)
                            .setMessage(getString(R.string.enable_gps))
                            .setPositiveButton(R.string.gps_settings,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivityForResult(
                                                    new Intent(
                                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                                                    0);
                                        }
                                    })
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            Places.this.finish();
                                        }
                                    }).show();
                }
                Looper.loop();
            }
        }.start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * User returning from the Location settings menu. try to fetch location
         * again.
         */
        dialog.dismiss();
        getLocation();
    }

    /*
     * Fetch nearby places by providing the search type as 'place' within 1000
     * mtrs of the provided lat & lon
     */
    private void fetchPlaces() {
        if (!isFinishing()) {
            dialog = ProgressDialog.show(Places.this, "", getString(R.string.nearby_places), true,
                    true, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            showToast("No places fetched.");
                        }
                    });
        }
        /*
         * Source tag: fetch_places_tag
         */
        Bundle params = new Bundle();
        params.putString("type", "place");
        try {
            params.putString("center",
                    location.getString("latitude") + "," + location.getString("longitude"));
        } catch (JSONException e) {
            showToast("No places fetched.");
            return;
        }
        params.putString("distance", "1000");
        Utility.mAsyncRunner.request("search", params, new placesRequestListener());
    }

    /*
     * Callback after places are fetched.
     */
    public class placesRequestListener extends BaseRequestListener {

        @Override
        public void onComplete(final String response, final Object state) {
            Log.d("Facebook-FbAPIs", "Got response: " + response);
            dialog.dismiss();

            try {
                jsonArray = new JSONObject(response).getJSONArray("data");
                if (jsonArray == null) {
                    showToast("Error: nearby places could not be fetched");
                    return;
                }
            } catch (JSONException e) {
                showToast("Error: " + e.getMessage());
                return;
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    placesList = (ListView) findViewById(R.id.places_list);
                    placesList.setOnItemClickListener(Places.this);
                    placesList.setAdapter(new PlacesListAdapter(Places.this));
                }
            });

        }

        public void onFacebookError(FacebookError error) {
            dialog.dismiss();
            showToast("Fetch Places Error: " + error.getMessage());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
        if (!Utility.mFacebook.isSessionValid()) {
            Util.showAlert(this, "Warning", "You must first log in.");
        } else {
            try {
                final String message = "Check-in from the " + getString(R.string.app_name);
                final String name = jsonArray.getJSONObject(position).getString("name");
                final String placeID = jsonArray.getJSONObject(position).getString("id");
                new AlertDialog.Builder(this).setTitle(R.string.check_in_title)
                        .setMessage(String.format(getString(R.string.check_in_at), name))
                        .setPositiveButton(R.string.checkin, new DialogInterface.OnClickListener() {
                            /*
                             * Source tag: check_in_tag Check-in user at the
                             * selected location posting to the me/checkins
                             * endpoint. More info here:
                             * https://developers.facebook
                             * .com/docs/reference/api/user/ - checkins
                             */
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Bundle params = new Bundle();
                                params.putString("place", placeID);
                                params.putString("message", message);
                                params.putString("coordinates", location.toString());
                                Utility.mAsyncRunner.request("me/checkins", params, "POST",
                                        new placesCheckInListener(), null);
                            }
                        }).setNegativeButton(R.string.cancel, null).show();
            } catch (JSONException e) {
                showToast("Error: " + e.getMessage());
            }
        }
    }

    public class placesCheckInListener extends BaseRequestListener {
        @Override
        public void onComplete(final String response, final Object state) {
            showToast("API Response: " + response);
        }

        public void onFacebookError(FacebookError error) {
            dialog.dismiss();
            showToast("Check-in Error: " + error.getMessage());
        }
    }

    public void showToast(final String msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(Places.this, msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    /**
     * Definition of the list adapter
     */
    public class PlacesListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        Places placesList;

        public PlacesListAdapter(Context context) {
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return jsonArray.length();
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
        public View getView(int position, View convertView, ViewGroup parent) {
            JSONObject jsonObject = null;
            try {
                jsonObject = jsonArray.getJSONObject(position);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
            View hView = convertView;
            if (convertView == null) {
                hView = mInflater.inflate(R.layout.place_item, null);
                ViewHolder holder = new ViewHolder();
                holder.name = (TextView) hView.findViewById(R.id.place_name);
                holder.location = (TextView) hView.findViewById(R.id.place_location);
                hView.setTag(holder);
            }

            ViewHolder holder = (ViewHolder) hView.getTag();
            try {
                holder.name.setText(jsonObject.getString("name"));
            } catch (JSONException e) {
                holder.name.setText("");
            }
            try {
                String location = jsonObject.getJSONObject("location").getString("street") + ", "
                        + jsonObject.getJSONObject("location").getString("city") + ", "
                        + jsonObject.getJSONObject("location").getString("state");
                holder.location.setText(location);
            } catch (JSONException e) {
                holder.location.setText("");
            }
            return hView;
        }

    }

    class ViewHolder {
        TextView name;
        TextView location;
    }

    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            dialog.dismiss();
            if (loc != null) {
                try {
                    location.put("latitude", new Double(loc.getLatitude()));
                    location.put("longitude", new Double(loc.getLongitude()));
                } catch (JSONException e) {
                }
                showToast("Location acquired: " + String.valueOf(loc.getLatitude()) + " "
                        + String.valueOf(loc.getLongitude()));
                lm.removeUpdates(this);
                fetchPlaces();
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
