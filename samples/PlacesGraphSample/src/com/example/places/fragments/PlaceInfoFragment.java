/*
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

package com.example.places.fragments;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.places.PlacesGraphSDKHelper;
import com.example.places.R;
import com.example.places.adapters.PlaceDetailsAdapter;
import com.example.places.model.Place;
import com.example.places.model.PlaceTextUtils;
import com.example.places.utils.BitmapDownloadTask;
import com.example.places.utils.PlaceFieldData;
import com.example.places.utils.PlaceFieldDataFactory;
import com.facebook.GraphResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment illustrates how to get detailed place information from the Places Graph SDK.
 *
 * <p>Refer to {@link PlacesGraphSDKHelper}, {@link Place}, and {@link PlaceTextUtils} to see how
 * the place info request is created, and to see how the place info is parsed and displayed.
 */
public class PlaceInfoFragment extends Fragment
    implements PlacesGraphSDKHelper.PlaceInfoRequestListener,
        PlaceDetailsAdapter.Listener,
        BitmapDownloadTask.Listener {

  private static final String TAG = PlaceInfoFragment.class.getSimpleName();

  public static final String EXTRA_PLACE = "place";

  /** These are the place fields that will be displayed in the recycler view. */
  private static final String[] PLACE_FIELDS_TO_DISPLAY_IN_RECYCLERVIEW =
      new String[] {
        Place.CATEGORY_LIST,
        Place.ABOUT,
        Place.LOCATION,
        Place.PHONE,
        Place.WEBSITE,
        Place.HOURS,
        Place.IS_ALWAYS_OPEN,
        Place.IS_PERMANENTLY_CLOSED,
        Place.DESCRIPTION,
        Place.CHECKINS,
        Place.OVERALL_STAR_RATING,
        Place.ENGAGEMENT,
        Place.RESTAURANT_SPECIALTIES,
        Place.RESTAURANT_SERVICES,
        Place.PRICE_RANGE,
        Place.PAYMENT_OPTIONS,
        Place.IS_VERIFIED,
        Place.APP_LINKS,
        Place.PARKING,
        Place.LINK,
      };

  public interface Listener {
    void onCallPhone(Intent intent);
  }

  private Listener listener;
  private Place place;
  private RecyclerView recyclerView;
  private ImageView coverView;
  private Toolbar toolbar;
  private FloatingActionButton floatingActionButton;
  private ProgressBar progressBar;

  public static PlaceInfoFragment newInstance() {
    return new PlaceInfoFragment();
  }

  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof Activity) {
      listener = (Listener) context;
    }
  }

  public void onDestroy() {
    super.onDestroy();
    listener = null;
  }

  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    if (savedInstanceState == null) {
      final Bundle args = getArguments();
      place = args.getParcelable(EXTRA_PLACE);
    } else {
      place = savedInstanceState.getParcelable(EXTRA_PLACE);
    }
    return inflater.inflate(R.layout.place_info_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    toolbar = (Toolbar) view.findViewById(R.id.place_details_toolbar);
    ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
    ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    ((AppCompatActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);

    progressBar = (ProgressBar) view.findViewById(R.id.place_details_progress);
    coverView = (ImageView) view.findViewById(R.id.place_details_cover);
    recyclerView = (RecyclerView) view.findViewById(R.id.place_details_recyclerview);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    floatingActionButton =
        (FloatingActionButton) view.findViewById(R.id.place_details_actionbutton);
    floatingActionButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            String phoneNumber = place.get(Place.PHONE);
            if (phoneNumber != null) {
              String strippedNumber = PhoneNumberUtils.stripSeparators(phoneNumber);
              Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + strippedNumber));
              listener.onCallPhone(intent);
            }
          }
        });

    toolbar.setTitle(place.get(Place.NAME));
    int actionButtonVisibility = place.has(Place.PHONE) ? View.VISIBLE : View.GONE;
    floatingActionButton.setVisibility(actionButtonVisibility);

    fetchPlaceInfo();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(EXTRA_PLACE, place);
  }

  public void onDestroyView() {
    toolbar = null;
    coverView = null;
    recyclerView = null;
    floatingActionButton = null;
    super.onDestroyView();
  }

  private void fetchPlaceInfo() {
    // Creates and executes a Place Info request on the Places Graph SDK
    PlacesGraphSDKHelper.fetchPlaceInfo(place, this);
    progressBar.setVisibility(View.VISIBLE);
  }

  private void updateList() {
    List<PlaceFieldData> fields = new ArrayList<>();
    for (String fieldName : PLACE_FIELDS_TO_DISPLAY_IN_RECYCLERVIEW) {
      PlaceFieldData fieldData =
          PlaceFieldDataFactory.newPlaceField(getActivity(), fieldName, place);
      if (fieldData != null) {
        fields.add(fieldData);
      }
    }
    PlaceDetailsAdapter adapter = new PlaceDetailsAdapter(this, fields);
    recyclerView.setAdapter(adapter);
  }

  @Override
  public void onPlaceInfoResult(final Place place, final GraphResponse response) {
    if (isAdded()) {
      getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  progressBar.setVisibility(View.INVISIBLE);
                  if (place == null) {
                    // The response object contains additional information on the error.
                    Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                  } else {
                    // Refresh the UI with the new place information.
                    PlaceInfoFragment.this.place = place;
                    updateList();
                    downloadCoverPhoto();
                  }
                }
              });
    }
  }

  private void downloadCoverPhoto() {
    // Downloads the place cover photo.
    PlacesGraphSDKHelper.downloadPlaceCoverPhoto(place, PlaceInfoFragment.this);
  }

  @Override
  public void onPlaceFieldSelected(PlaceFieldData placeFieldData) {
    try {
      Intent intent = placeFieldData.getActionIntent();
      if (Intent.ACTION_CALL.equals(intent.getAction())) {
        listener.onCallPhone(intent);
      } else {
        getActivity().startActivity(intent);
      }
    } catch (ActivityNotFoundException e) {
      Log.e(TAG, "failed to start activity", e);
    }
  }

  @Override
  public void onBitmapDownloadSuccess(final String url, final Bitmap bitmap) {
    if (isAdded()) {
      getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  coverView.setImageBitmap(bitmap);
                }
              });
    }
  }

  @Override
  public void onBitmapDownloadFailure(final String url) {
    if (isAdded()) {
      getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                }
              });
    }
  }
}
