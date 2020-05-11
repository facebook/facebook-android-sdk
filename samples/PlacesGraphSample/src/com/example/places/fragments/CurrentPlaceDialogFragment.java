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
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.places.PlacesGraphSDKHelper;
import com.example.places.R;
import com.example.places.adapters.PlaceListAdapter;
import com.example.places.model.CurrentPlaceResult;
import com.example.places.model.Place;
import com.facebook.GraphResponse;
import com.facebook.places.PlaceManager;

/**
 * This dialog fragment illustrates how to:
 *
 * <ul>
 *   <li>Fetch a collection of current place candidates where the user might be located.
 *   <li>Display a list of current place candidates.
 *   <li>Provide feedback about the current place estimate.
 * </ul>
 */
public class CurrentPlaceDialogFragment extends DialogFragment
    implements PlaceListAdapter.Listener {

  public static final String EXTRA_CURRENT_PLACE = "current_place";

  private RecyclerView recyclerView;
  private View loadingView;
  private CurrentPlaceResult currentPlaceResult;
  private CurrentPlaceRequestListener currentPlaceRequestListener =
      new CurrentPlaceRequestListener();

  public static CurrentPlaceDialogFragment newInstance() {
    return new CurrentPlaceDialogFragment();
  }

  @Override
  public void onDetach() {
    super.onDetach();
    currentPlaceRequestListener = null;
  }

  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    getDialog().setTitle(getString(R.string.current_place));
    return inflater.inflate(R.layout.current_place_fragment, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    recyclerView = (RecyclerView) view.findViewById(R.id.current_place_recyclerview);
    recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    loadingView = view.findViewById(R.id.current_place_loading_container);

    getCurrentPlace();
  }

  private void getCurrentPlace() {
    PlacesGraphSDKHelper.getCurrentPlace(currentPlaceRequestListener);
  }

  @Override
  public void onPlaceSelected(Place place) {

    // Provides feedback to the Places Graph once the user confirms presence at
    // a place. This feedback helps Facebook improve the accuracy of current place estimates.
    PlacesGraphSDKHelper.provideCurrentPlaceFeedback(currentPlaceResult, place, true);

    Intent data = new Intent();
    data.putExtra(EXTRA_CURRENT_PLACE, place);

    getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, data);

    dismiss();
  }

  private void refreshCurrentPlaceList(CurrentPlaceResult result) {
    currentPlaceResult = result;

    PlaceListAdapter adapter =
        new PlaceListAdapter(R.layout.current_place_list_item, result.getPlaces(), this);

    recyclerView.setAdapter(adapter);
    recyclerView.setVisibility(View.VISIBLE);
    loadingView.setVisibility(View.GONE);
  }

  private class CurrentPlaceRequestListener
      implements PlacesGraphSDKHelper.CurrentPlaceRequestListener {

    @Override
    public void onCurrentPlaceResult(
        @Nullable final CurrentPlaceResult result, final GraphResponse response) {

      if (isAdded()) {
        getActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    if (result == null) {
                      Toast.makeText(getActivity(), R.string.error, Toast.LENGTH_SHORT).show();
                      dismiss();
                    } else {
                      refreshCurrentPlaceList(result);
                    }
                  }
                });
      }
    }

    @Override
    public void onLocationError(PlaceManager.LocationError error) {
      if (isAdded()) {
        getActivity()
            .runOnUiThread(
                new Runnable() {
                  @Override
                  public void run() {
                    Toast.makeText(
                            getActivity(), R.string.location_error_unknown, Toast.LENGTH_SHORT)
                        .show();
                    dismiss();
                  }
                });
      }
    }
  }
}
