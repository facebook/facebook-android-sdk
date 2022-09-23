/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fbloginsample.R;
import com.facebook.fbloginsample.Util;
import com.facebook.fbloginsample.entities.Post;
import com.facebook.login.widget.ProfilePictureView;
import java.util.List;

public class PostAdapter extends ArrayAdapter<Post> {
  private Context mContext;
  private List<Post> mPosts;

  public PostAdapter(Context context, List<Post> posts) {
    super(context, R.layout.news_feed_item, posts);
    mContext = context;
    mPosts = posts;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    Post post = mPosts.get(position);
    View rowView = convertView;

    if (rowView == null) {
      LayoutInflater inflater =
          (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      rowView = inflater.inflate(R.layout.news_feed_item, null);
      ViewHolder viewHolder = new ViewHolder();
      viewHolder.profilePhoto = rowView.findViewById(R.id.profile_photo);
      viewHolder.postDate = rowView.findViewById(R.id.post_date);
      viewHolder.username = rowView.findViewById(R.id.username);
      viewHolder.message = rowView.findViewById(R.id.message);
      viewHolder.postPicture = rowView.findViewById(R.id.post_picture);
      rowView.setTag(viewHolder);
    }

    ViewHolder holder = (ViewHolder) rowView.getTag();
    holder.profilePhoto.setProfileId(post.getFromId());
    holder.postDate.setText(Util.MakePrettyDate(post.getCreatedTime()));
    holder.username.setText(post.getFromName());
    holder.message.setText(post.getMessage());
    holder.postPicture.setImageURI(post.getPicture());

    return rowView;
  }

  private static class ViewHolder {
    ProfilePictureView profilePhoto;
    TextView postDate;
    TextView username;
    TextView message;
    SimpleDraweeView postPicture;
  }
}
