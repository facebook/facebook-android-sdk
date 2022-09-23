/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlinsampleapp.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.facebook.samples.kotlinsampleapp.common.MenuItem

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    navigateToLogin: () -> Unit,
    navigateToSharing: () -> Unit,
    navigateToAppEvents: () -> Unit
) {
  val homeViewState by homeViewModel.homeViewState.observeAsState(HomeViewState())

  Column {
    ProfileView(
        homeViewState.profile,
        homeViewState.loginBehavior,
        homeViewState.availableLoginBehaviors,
        homeViewModel::logout,
        homeViewModel::selectLoginBehavior)
    HomeMenu(navigateToLogin, navigateToSharing, navigateToAppEvents)
  }
}

@Composable
fun HomeMenu(
    navigateToLogin: () -> Unit,
    navigateToSharing: () -> Unit,
    navigateToAppEvents: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
    MenuItem("Login", onClick = navigateToLogin)
    MenuItem("AppEvents", onClick = navigateToAppEvents)
    MenuItem("Sharing", onClick = navigateToSharing)
  }
}
