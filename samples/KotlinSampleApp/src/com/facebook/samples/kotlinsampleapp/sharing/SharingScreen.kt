/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlinsampleapp.sharing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.facebook.samples.kotlinsampleapp.R
import com.facebook.samples.kotlinsampleapp.common.MenuItem

@Composable
fun SharingScreen() {
  val context = LocalContext.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
    Text("This screen shows how to to share content using FB SDK")
    MenuItem("Share link", onClick = { shareLink(context, "https://developers.facebook.com") })
    MenuItem(
        "Share link with hashtag",
        onClick = {
          shareLink(context, "https://developers.facebook.com", hashtag = "#ConnectTheWorld")
        })
    MenuItem(
        "Share link with quote",
        onClick = {
          shareLink(
              context, "https://developers.facebook.com", quote = "Connect on a global scale.")
        })
    MenuItem(
        "Share photos",
        onClick = {
          sharePhotos(
              context,
              listOf(
                  R.drawable.red,
                  R.drawable.black,
                  R.drawable.green,
              ))
        })
    MenuItem("Share video", onClick = { shareVideo(context, R.raw.test) })
  }
}
