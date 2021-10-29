// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.gamingservices

import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import com.facebook.bolts.TaskCompletionSource

private const val GRAPH_RESPONSE_SUCCESS_KEY = "success"

class TournamentUpdater {

  /**
   * Attempts to update the provided tournament with the provided score
   *
   * @return The task completion source that contains a boolean value on whether or not the Graph
   * API call succeed.
   */
  fun update(tournament: Tournament, score: Number): TaskCompletionSource<Boolean>? {
    val currentAccessToken: AccessToken? = getCurrentAccessToken()
    if (currentAccessToken == null || currentAccessToken.isExpired) {
      throw FacebookException("Attempted to fetch tournament with an invalid access token")
    }
    val isGamingLoggedIn =
        (currentAccessToken.graphDomain != null &&
            FacebookSdk.GAMING == currentAccessToken.graphDomain)
    if (!isGamingLoggedIn) {
      throw FacebookException("User is not using gaming login")
    }

    val task: TaskCompletionSource<Boolean> = TaskCompletionSource<Boolean>()
    val graphPath = "${tournament.identifier}/update_score"
    val params = Bundle()
    params.putInt("score", score.toInt())
    val request =
        GraphRequest(
            currentAccessToken,
            graphPath,
            params,
            HttpMethod.POST,
            GraphRequest.Callback { response ->
              if (response.error != null) {
                if (response.error?.exception != null) {
                  task.setError(response.error?.exception)
                  return@Callback
                }
                task.setError(GraphAPIException("Graph API Error"))
                return@Callback
              }
              val jsonObject = response.getJSONObject()
              val result = jsonObject?.optString(GRAPH_RESPONSE_SUCCESS_KEY)
              if (result == null || result.isEmpty()) {
                val errorMessage = "Graph API Error"
                task.setError(GraphAPIException(errorMessage))
                return@Callback
              }
              val success = result.equals("true")
              task.setResult(success)
            })
    request.executeAsync()
    return task
  }
}
