/*
 * Copyright (c) 2017-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
