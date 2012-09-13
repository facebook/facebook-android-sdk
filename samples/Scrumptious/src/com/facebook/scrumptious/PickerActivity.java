package com.facebook.scrumptious;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.facebook.*;

import java.util.List;

/**
 * The PickerActivity enhances the Friend or Place Picker by adding a title
 * and a Done button. The selection results are saved in the ScrumptiousApplication
 * instance.
 */
public class PickerActivity extends FragmentActivity {
    public static final Uri FRIEND_PICKER = Uri.parse("picker://friend");
    public static final Uri PLACE_PICKER = Uri.parse("picker://place");

    private static final Location PARIS_LOCATION = new Location("") {{
        setLatitude(48.857875);
        setLongitude(2.294635);
    }};

    private FriendPickerFragment friendPickerFragment;
    private PlacePickerFragment placePickerFragment;
    private List<GraphUser> users = null;
    private GraphPlace place = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pickers);

        Button doneButton = (Button) findViewById(R.id.picker_done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishActivity();
            }
        });

        TextView titleText = (TextView) findViewById(R.id.picker_title);

        Bundle args = getIntent().getExtras();
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragmentToShow = null;
        Uri intentUri = getIntent().getData();

        if (FRIEND_PICKER.equals(intentUri)) {
            titleText.setText(R.string.pick_friends_title);
            if (savedInstanceState == null) {
                friendPickerFragment = new FriendPickerFragment(args);
            } else {
                friendPickerFragment = (FriendPickerFragment) manager.findFragmentById(R.id.picker_fragment);;
            }

            friendPickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
                @Override
                public void onSelectionChanged() {
                    users = friendPickerFragment.getSelection();
                }
            });
            friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
                @Override
                public void onError(FacebookException error) {
                    PickerActivity.this.onError(error);
                }
            });
            fragmentToShow = friendPickerFragment;

        } else if (PLACE_PICKER.equals(intentUri)) {
            titleText.setText(R.string.pick_place_title);
            if (savedInstanceState == null) {
                placePickerFragment = new PlacePickerFragment(args);
            } else {
                placePickerFragment = (PlacePickerFragment) manager.findFragmentById(R.id.picker_fragment);
            }
            placePickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
                @Override
                public void onSelectionChanged() {
                    place = placePickerFragment.getSelection();
                    if (place != null) {
                        finishActivity(); // call finish since you can only pick one place
                    }
                }
            });
            placePickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
                @Override
                public void onError(FacebookException error) {
                    PickerActivity.this.onError(error);
                }
            });
            fragmentToShow = placePickerFragment;
        } else {
            // Nothing to do, finish
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        manager.beginTransaction().replace(R.id.picker_fragment, fragmentToShow).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FRIEND_PICKER.equals(getIntent().getData())) {
            try {
                friendPickerFragment.loadData(false);
            } catch (Exception ex) {
                onError(ex);
            }
        } else if (PLACE_PICKER.equals(getIntent().getData())) {
            try {
                Location location = null;
                Criteria criteria = new Criteria();
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                String bestProvider = locationManager.getBestProvider(criteria, false);
                if (bestProvider != null) {
                    location = locationManager.getLastKnownLocation(bestProvider);
                }
                if (location == null) {
                    String model = Build.MODEL;
                    if (model.equals("sdk") || model.equals("google_sdk") || model.contains("x86")) {
                        // this may be the emulator, pretend we're in an exotic place
                        location = PARIS_LOCATION;
                    }
                }
                if (location != null) {
                    placePickerFragment.setLocation(location);
                    placePickerFragment.loadData(true);
                } else {
                    onError(getResources().getString(R.string.no_location_error), true);
                }
            } catch (Exception ex) {
                onError(ex);
            }
        }
    }

    private void onError(Exception error) {
        onError(error.getLocalizedMessage(), false);
    }

    private void onError(String error, final boolean finishActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.error_dialog_title).
                setMessage(error).
                setPositiveButton(R.string.error_dialog_button_text, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (finishActivity) {
                            finishActivity();
                        }
                    }
                });
        builder.show();
    }

    private void finishActivity() {
        ScrumptiousApplication app = (ScrumptiousApplication) getApplication();
        if (FRIEND_PICKER.equals(getIntent().getData())) {
            app.setSelectedUsers(users);
        } else if (PLACE_PICKER.equals(getIntent().getData())) {
            app.setSelectedPlace(place);
        }
        setResult(RESULT_OK, null);
        finish();
    }
}
