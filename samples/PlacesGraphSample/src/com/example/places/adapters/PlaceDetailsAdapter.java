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
import com.example.places.utils.PlaceFieldData;
import java.util.List;

public class PlaceDetailsAdapter
    extends RecyclerView.Adapter<PlaceDetailsAdapter.PlaceDetailViewHolder> {

  private List<PlaceFieldData> fields;
  private Listener listener;

  public interface Listener {
    void onPlaceFieldSelected(PlaceFieldData placeFieldData);
  }

  public class PlaceDetailViewHolder extends RecyclerView.ViewHolder
      implements View.OnClickListener {

    private View container;
    private TextView title;
    private TextView text;
    private PlaceFieldData currentPlaceFieldData;

    public PlaceDetailViewHolder(View itemView) {
      super(itemView);
      container = itemView.findViewById(R.id.place_detail_container);
      title = (TextView) itemView.findViewById(R.id.place_detail_title);
      text = (TextView) itemView.findViewById(R.id.place_detail_text);
    }

    void update(PlaceFieldData placeFieldData) {
      currentPlaceFieldData = placeFieldData;
      title.setText(placeFieldData.getTitle());
      text.setText(placeFieldData.getText());
      if (placeFieldData.isClickable()) {
        container.setOnClickListener(this);
      } else {
        container.setOnClickListener(null);
      }
    }

    @Override
    public void onClick(View v) {
      listener.onPlaceFieldSelected(currentPlaceFieldData);
    }
  }

  public PlaceDetailsAdapter(Listener listener, List<PlaceFieldData> fields) {
    this.listener = listener;
    this.fields = fields;
  }

  @Override
  public PlaceDetailViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    Context context = parent.getContext();
    LayoutInflater inflater = LayoutInflater.from(context);
    View view = inflater.inflate(R.layout.place_info_item, parent, false);
    PlaceDetailViewHolder viewHolder = new PlaceDetailViewHolder(view);
    return viewHolder;
  }

  @Override
  public int getItemCount() {
    return fields.size();
  }

  @Override
  public void onBindViewHolder(PlaceDetailViewHolder holder, int position) {
    PlaceFieldData field = fields.get(position);
    holder.update(field);
  }
}
