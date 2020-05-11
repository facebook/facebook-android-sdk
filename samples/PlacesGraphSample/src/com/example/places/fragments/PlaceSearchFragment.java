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
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.places.PlacesGraphSDKHelper;
import com.example.places.R;
import com.example.places.adapters.PlaceListAdapter;
import com.example.places.model.Place;
import com.example.places.model.PlaceTextUtils;
import com.facebook.GraphResponse;
import com.facebook.places.PlaceManager;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment illustrates how to use the Places Graph SDK to:
 *
 * <ul>
 *   <li>Search for nearby places.
 *   <li>Display places on a map.
 * </ul>
 */
public class PlaceSearchFragment extends Fragment
    implements PlacesGraphSDKHelper.PlaceSearchRequestListener,
        PlaceListAdapter.Listener,
        OnMapReadyCallback {

  private static final int SCROLL_DIRECTION_UP = -1;
  private static final int REQUEST_CODE_GET_CURRENT_PLACE = 1;

  private Listener listener;
  private ProgressBar progressBar;
  private RecyclerView recyclerView;
  private PlaceListAdapter placeListAdapter;
  private TextView currentPlaceNameTextView;
  private TextView currentPlaceAddressTextView;
  private CardView searchCardView;
  private CardView currentPlaceCardView;
  private EditText searchEditText;
  private FloatingActionButton actionButton;
  private SupportMapFragment mapFragment;
  private GoogleMap map;
  private State state = State.LIST;

  private List<Place> placesToDisplay = new ArrayList<>(0);

  public enum State {
    LIST,
    MAP
  };

  public interface Listener {
    void onPlaceSelected(Place place);

    void onLocationPermissionsError();

    boolean hasLocationPermission();
  }

  public static PlaceSearchFragment newInstance() {
    return new PlaceSearchFragment();
  }

  public void onAttach(Context context) {
    super.onAttach(context);
    if (context instanceof Activity) {
      listener = (Listener) context;
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.place_search_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    progressBar = (ProgressBar) view.findViewById(R.id.place_search_progressbar);
    recyclerView = (RecyclerView) view.findViewById(R.id.place_search_recyclerview);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    actionButton = (FloatingActionButton) view.findViewById(R.id.place_search_toggle_button);
    actionButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            onActionButtonClicked();
          }
        });

    currentPlaceCardView = (CardView) view.findViewById(R.id.current_place_cardview);
    currentPlaceCardView.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            openCurrentPlaceDialog();
          }
        });

    currentPlaceNameTextView = (TextView) view.findViewById(R.id.current_place_name);
    currentPlaceAddressTextView = (TextView) view.findViewById(R.id.current_place_address);
    searchCardView = (CardView) view.findViewById(R.id.place_search_cardview);
    searchEditText = (EditText) view.findViewById(R.id.place_search_edittext);
    searchEditText.setOnEditorActionListener(
        new TextView.OnEditorActionListener() {
          @Override
          public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // When the search soft input key is clicked,
            // hide soft input and search nearby places
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
              InputMethodManager imm =
                  (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
              imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
              searchPlace(searchEditText.getText().toString());
            }
            return true;
          }
        });

    RecyclerView.ItemDecoration itemDecoration =
        new RecyclerView.ItemDecoration() {
          @Override
          public void getItemOffsets(
              Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            if (parent.getChildAdapterPosition(view) == 0) {
              outRect.top =
                  getResources().getDimensionPixelOffset(R.dimen.place_search_list_header_height);
            }
          }
        };
    recyclerView.addItemDecoration(itemDecoration);

    recyclerView.addOnScrollListener(
        new RecyclerView.OnScrollListener() {
          @Override
          public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            float elevation;
            if (recyclerView.canScrollVertically(SCROLL_DIRECTION_UP)) {
              elevation = getResources().getDimension(R.dimen.search_scrolling_elevation);
            } else {
              elevation = getResources().getDimension(R.dimen.search_resting_elevation);
            }
            ViewCompat.setElevation(currentPlaceCardView, elevation);
            ViewCompat.setElevation(searchCardView, elevation);
          }
        });
  }

  public void onDestroyView() {
    recyclerView = null;
    searchEditText = null;
    searchCardView = null;
    actionButton = null;
    progressBar = null;
    mapFragment = null;
    map = null;
    super.onDestroyView();
  }

  private void setLoading(boolean isLoading) {
    progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
  }

  private void searchPlace(String searchQuery) {
    setLoading(true);
    PlacesGraphSDKHelper.searchPlace(searchQuery, this);
  }

  private void onActionButtonClicked() {
    toggleMapAndList();
    // Hide soft input
    InputMethodManager imm =
        (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
  }

  private void toggleMapAndList() {
    state = state == State.MAP ? State.LIST : State.MAP;

    if (state == State.LIST) {
      if (mapFragment != null) {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.hide(mapFragment).commit();
      }
      recyclerView.setVisibility(View.VISIBLE);
      currentPlaceCardView.setVisibility(View.VISIBLE);
    }
    if (state == State.MAP) {
      if (mapFragment == null) {
        mapFragment = SupportMapFragment.newInstance();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.place_search_map_placeholder, mapFragment);
        transaction.commit();
        mapFragment.getMapAsync(this);
      } else {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();
        ft.show(mapFragment).commit();
      }
      recyclerView.setVisibility(View.INVISIBLE);
      currentPlaceCardView.setVisibility(View.INVISIBLE);
    }
    displayPlaces(placesToDisplay);
  }

  private void openCurrentPlaceDialog() {

    CurrentPlaceDialogFragment currentPlaceDialogFragment =
        CurrentPlaceDialogFragment.newInstance();
    currentPlaceDialogFragment.setTargetFragment(this, REQUEST_CODE_GET_CURRENT_PLACE);
    currentPlaceDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog");
  }

  @Override
  public void onPlaceSearchResult(final List<Place> places, GraphResponse response) {
    if (isAdded()) {
      getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {

                  setLoading(false);

                  if (places == null) {
                    // The response object does contain more information on the error
                    Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                  } else {
                    placesToDisplay = places;
                    displayPlaces(placesToDisplay);
                  }
                }
              });
    }
  }

  @Override
  public void onLocationError(final PlaceManager.LocationError error) {

    // This event is invoked when the Places Graph SDK fails to retrieve the device location.

    if (isAdded()) {
      getActivity()
          .runOnUiThread(
              new Runnable() {
                @Override
                public void run() {
                  if (error == PlaceManager.LocationError.LOCATION_PERMISSION_DENIED) {
                    // Trigger the activity to prompt the user for location permissions.
                    listener.onLocationPermissionsError();
                  } else if (error == PlaceManager.LocationError.LOCATION_SERVICES_DISABLED) {
                    String message = getString(R.string.location_error_disabled);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                  } else {
                    String message = getString(R.string.location_error_unknown);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                  }
                }
              });
    }
  }

  private void displayPlaces(List<Place> places) {
    if (state == State.LIST) {
      placeListAdapter = new PlaceListAdapter(R.layout.place_list_item, places, this);
      recyclerView.setAdapter(placeListAdapter);
      placeListAdapter.notifyDataSetChanged();
    } else if (state == State.MAP) {
      displayPlacesOnMap(places);
    }
  }

  private void displayPlacesOnMap(List<Place> places) {
    if (map != null) {
      map.clear();
      if (!places.isEmpty()) {
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (Place place : places) {
          // Creates a marker at the place location and with the place name
          LatLng position = place.getPosition();
          String placeName = place.get(Place.NAME);
          if (position != null) {
            MarkerOptions markerOptions = new MarkerOptions().position(position).title(placeName);
            Marker marker = map.addMarker(markerOptions);
            marker.setTag(place);
            boundsBuilder.include(position);
          }
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100);
        map.moveCamera(cameraUpdate);
      }
    }
  }

  @Override
  @SuppressWarnings("MissingPermission")
  public void onMapReady(GoogleMap googleMap) {
    map = googleMap;
    map.getUiSettings().setMapToolbarEnabled(false);
    if (listener.hasLocationPermission()) {
      map.setMyLocationEnabled(true);
      map.getUiSettings().setMyLocationButtonEnabled(false);
    }
    map.setOnInfoWindowClickListener(
        new GoogleMap.OnInfoWindowClickListener() {
          @Override
          public void onInfoWindowClick(Marker marker) {
            if (marker.getTag() instanceof Place) {
              Place place = (Place) marker.getTag();
              listener.onPlaceSelected(place);
            }
          }
        });
    if (state == State.MAP) {
      displayPlaces(placesToDisplay);
    }
  }

  @Override
  public void onPlaceSelected(Place place) {
    listener.onPlaceSelected(place);
  }

  private void onCurrentPlaceSelected(Place place) {
    currentPlaceNameTextView.setText(place.get(Place.NAME));
    currentPlaceNameTextView.setVisibility(View.VISIBLE);
    currentPlaceAddressTextView.setText(PlaceTextUtils.getAddress(place));
    currentPlaceAddressTextView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_GET_CURRENT_PLACE) {
      if (resultCode == Activity.RESULT_OK) {
        Place place =
            (Place) data.getParcelableExtra(CurrentPlaceDialogFragment.EXTRA_CURRENT_PLACE);
        onCurrentPlaceSelected(place);
      }
    }
  }
}
