/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.kotlinsampleapp.login

import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.samples.kotlinsampleapp.common.MenuItem

@Composable
fun LoginMenuScreen() {
  val context = LocalContext.current
  Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(16.dp)) {
    Text("This screen shows how to implement login without the LoginButton")

    MenuItem(
        "Login",
        onClick = {
          if (context is ActivityResultRegistryOwner) {
            val callbackManager = CallbackManager.Factory.create()
            val loginManager = LoginManager.getInstance()
            loginManager.registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                  override fun onCancel() {
                    Toast.makeText(context, "Login canceled!", Toast.LENGTH_LONG).show()
                  }

                  override fun onError(error: FacebookException) {
                    Log.e("Login", error.message ?: "Unknown error")
                    Toast.makeText(context, "Login failed with errors!", Toast.LENGTH_LONG).show()
                  }

                  override fun onSuccess(result: LoginResult) {
                    Toast.makeText(context, "Login succeed!", Toast.LENGTH_LONG).show()
                  }
                })
            LoginManager.getInstance().logIn(context, callbackManager, listOf("email"))
          } else {
            Toast.makeText(
                    context,
                    "This login should only happens with an AndroidX activity.",
                    Toast.LENGTH_LONG)
                .show()
          }
        })
    MenuItem("Logout", onClick = { LoginManager.getInstance().logOut() })
  }
}
