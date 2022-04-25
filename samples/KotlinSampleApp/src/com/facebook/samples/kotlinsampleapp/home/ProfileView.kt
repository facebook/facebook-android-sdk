package com.facebook.samples.kotlinsampleapp.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.Profile
import com.facebook.login.LoginBehavior
import com.facebook.login.widget.LoginButton
import com.facebook.login.widget.ProfilePictureView

@Composable
fun ProfileView(
    profile: Profile?,
    loginBehavior: LoginBehavior,
    availableLoginBehaviors: List<LoginBehavior>,
    logout: () -> Unit,
    selectLoginBehavior: (LoginBehavior) -> Unit
) {
  return Row {
    ProfilePhoto(profile)
    Column(
        modifier = Modifier.padding(4.dp).height(96.dp).fillMaxSize().background(Color.White),
        verticalArrangement = Arrangement.Center) {
      if (profile != null) {

        ProfileCardEntries(
            listOf(
                Pair("ID:", profile.id),
                Pair("First name:", profile.firstName),
                Pair("Last name:", profile.lastName)))
        LogoutButton(logout)
      } else {
        SignIn(loginBehavior, availableLoginBehaviors, selectLoginBehavior)
      }
    }
  }
}

@Composable
fun ProfilePhoto(profile: Profile?) {
  val updateProfile = { button: ProfilePictureView -> button.profileId = profile?.id }
  AndroidView(
      factory = { context -> ProfilePictureView(context).apply(updateProfile) },
      update = updateProfile,
      modifier = Modifier.size(width = 100.dp, height = 100.dp).padding(4.dp))
}

@Composable
fun ProfileCardEntries(pairs: List<Pair<String, String?>>) {
  Column {
    pairs
        .filter { it.second != null }
        .map {
          Row {
            Text(it.first, modifier = Modifier.padding(end = 4.dp))
            Text(it.second!!)
          }
        }
  }
}

@Composable
fun SignIn(
    currentBehavior: LoginBehavior,
    availableLoginBehaviors: List<LoginBehavior>,
    onSelected: (LoginBehavior) -> Unit
) {
  Column {
    LoginBehaviorRadio(currentBehavior, availableLoginBehaviors, onSelected)
    FBLoginButton(currentBehavior)
  }
}

@Composable
fun LoginBehaviorRadio(
    currentBehavior: LoginBehavior,
    availableLoginBehaviors: List<LoginBehavior>,
    onSelected: (LoginBehavior) -> Unit
) {
  Column {
    availableLoginBehaviors.forEach { loginBehavior ->
      Row(
          Modifier.fillMaxWidth()
              .selectable(
                  selected = loginBehavior == currentBehavior,
                  onClick = { onSelected(loginBehavior) })
              .padding(horizontal = 16.dp)) {
        RadioButton(
            selected = loginBehavior == currentBehavior, onClick = { onSelected(loginBehavior) })
        Text(loginBehavior.toString())
      }
    }
  }
}

@Composable
fun FBLoginButton(loginBehavior: LoginBehavior) {
  val updateValue = { loginButton: LoginButton ->
    loginButton.setPermissions("email")
    loginButton.loginBehavior = loginBehavior
  }
  AndroidView(
      factory = { context -> LoginButton(context).apply(updateValue) }, update = updateValue)
}

@Composable
fun LogoutButton(onClick: () -> Unit) {
  Button(onClick = onClick) { Text("logout") }
}
