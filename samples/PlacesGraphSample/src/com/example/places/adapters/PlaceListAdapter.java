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

package com.example.places.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.places.R;
import com.example.places.model.Place;
import com.example.places.model.PlaceTextUtils;
import java.util.List;

public class PlaceListAdapter extends RecyclerView.Adapter<PlaceListAdapter.PlaceViewHolder> {

  private List<Place> places;
  private Listener listener;
  private int layoutId;

  public interface Listener {
    void onPlaceSelected(Place place);
  }

  public class PlaceViewHolder extends RecyclerView.ViewHolder {
    private View container;
    private TextView placeNameTextView;
    private TextView placeAddressTextView;
    private Place currentPlace;

    public PlaceViewHolder(View itemView) {
      super(itemView);
      container = itemView.findViewById(R.id.place_container);
      placeNameTextView = (TextView) itemView.findViewById(R.id.place_name);
      placeAddressTextView = (TextView) itemView.findViewById(R.id.place_address);
      container.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              listener.onPlaceSelected(currentPlace);
            }
          });
    }

    void refresh(Place place) {
      this.currentPlace = place;
      placeNameTextView.setText(place.get(Place.NAME));
      placeAddressTextView.setText(PlaceTextUtils.getAddress(place));
    }
  }

  public PlaceListAdapter(int laoyutId, List<Place> places, Listener listener) {
    this.layoutId = laoyutId;
    this.places = places;
    this.listener = listener;
  }

  @Override
  public PlaceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    Context context = parent.getContext();
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(layoutId, parent, false);
    PlaceViewHolder viewHolder = new PlaceViewHolder(view);
    return viewHolder;
  }

  @Override
  public void onBindViewHolder(PlaceViewHolder holder, int position) {
    Place place = places.get(position);
    holder.refresh(place);
  }

  @Override
  public int getItemCount() {
    return places.size();
  }
}
