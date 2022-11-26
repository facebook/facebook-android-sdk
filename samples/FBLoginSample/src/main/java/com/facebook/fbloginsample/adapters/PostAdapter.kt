/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.fbloginsample.R
import com.facebook.fbloginsample.Util
import com.facebook.fbloginsample.entities.Post
import com.facebook.login.widget.ProfilePictureView

class PostAdapter(private val _context: Context, private val posts: List<Post>) :
    ArrayAdapter<Post>(_context, R.layout.news_feed_item, posts) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val post = posts[position]
    var rowView = convertView
    if (rowView == null) {
      val inflater = _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
      rowView = inflater.inflate(R.layout.news_feed_item, null)
      val viewHolder = ViewHolder()
      viewHolder.profilePhoto = rowView.findViewById(R.id.profile_photo)
      viewHolder.postDate = rowView.findViewById(R.id.post_date)
      viewHolder.username = rowView.findViewById(R.id.username)
      viewHolder.message = rowView.findViewById(R.id.message)
      viewHolder.postPicture = rowView.findViewById(R.id.post_picture)
      rowView.tag = viewHolder
    }

    checkNotNull(rowView)

    val viewHolder = rowView.tag as ViewHolder
    viewHolder.profilePhoto.profileId = post.fromId
    viewHolder.postDate.text = Util.makePrettyDate(post.createdTime)
    viewHolder.username.text = post.fromName
    viewHolder.message.text = post.message
    viewHolder.postPicture.setImageURI(post.picture)
    return rowView
  }

  private class ViewHolder {
    lateinit var profilePhoto: ProfilePictureView
    lateinit var postDate: TextView
    lateinit var username: TextView
    lateinit var message: TextView
    lateinit var postPicture: SimpleDraweeView
  }
}
