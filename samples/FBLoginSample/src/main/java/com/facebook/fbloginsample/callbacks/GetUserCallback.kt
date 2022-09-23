/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fbloginsample.callbacks

import android.net.Uri
import com.facebook.GraphRequest
import com.facebook.fbloginsample.entities.User
import java.lang.StringBuilder
import org.json.JSONException
import org.json.JSONObject

class GetUserCallback(private val getUserResponse: IGetUserResponse) {
  interface IGetUserResponse {
    fun onCompleted(user: User)
  }

  val callback: GraphRequest.Callback
  @Throws(JSONException::class)
  private fun jsonToUser(user: JSONObject): User {
    val picture = Uri.parse(user.getJSONObject("picture").getJSONObject("data").getString("url"))
    val name = user.getString("name")
    val id = user.getString("id")
    var email: String? = null
    if (user.has("email")) {
      email = user.getString("email")
    }

    // Build permissions display string
    val builder = StringBuilder()
    val perms = user.getJSONObject("permissions").getJSONArray("data")
    builder.append("Permissions:\n")
    for (i in 0 until perms.length()) {
      val jsonObject = perms.getJSONObject(i)
      val permissionString = "${jsonObject["permission"]}: ${jsonObject["status"]}\n"
      builder.append(permissionString)
    }
    val permissions = builder.toString()
    return User(picture, name, id, email, permissions)
  }

  init {
    callback =
        GraphRequest.Callback { response ->
          try {
            val userObj = response.getJSONObject() ?: return@Callback
            val user = jsonToUser(userObj)
            // Handled by ProfileActivity
            getUserResponse.onCompleted(user)
          } catch (e: JSONException) {
            // Handle exception ...
          }
        }
  }
}
