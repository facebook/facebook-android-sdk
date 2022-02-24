package com.facebook.samples.kotlinsampleapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.facebook.samples.kotlinsampleapp.appevents.AppEventScreen
import com.facebook.samples.kotlinsampleapp.home.HomeScreen
import com.facebook.samples.kotlinsampleapp.home.HomeViewModel
import com.facebook.samples.kotlinsampleapp.login.LoginMenuScreen
import com.facebook.samples.kotlinsampleapp.sharing.SharingScreen

enum class Screen {
  MainScreen,
  LoginScreen,
  SharingScreen,
  AppEventsScreen
}

@Composable
fun App(homeViewModel: HomeViewModel) {
  val navController = rememberNavController()
  Scaffold() { innerPadding -> NavHost(navController, modifier = Modifier.padding(innerPadding)) }
}

@Composable
fun NavHost(navController: NavHostController, modifier: Modifier = Modifier) {
  NavHost(
      navController = navController,
      startDestination = Screen.MainScreen.name,
      modifier = modifier) {
    composable(Screen.MainScreen.name) {
      val homeViewModel: HomeViewModel = viewModel()
      HomeScreen(
          homeViewModel,
          { navController.navigate(Screen.LoginScreen.name) },
          { navController.navigate(Screen.SharingScreen.name) },
          { navController.navigate(Screen.AppEventsScreen.name) })
    }
    composable(Screen.LoginScreen.name) { LoginMenuScreen() }
    composable(Screen.SharingScreen.name) { SharingScreen() }
    composable(Screen.AppEventsScreen.name) { AppEventScreen() }
  }
}
