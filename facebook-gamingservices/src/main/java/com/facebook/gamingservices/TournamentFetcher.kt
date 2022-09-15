/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 *
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 *
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.gamingservices

import android.os.Bundle
import com.facebook.AccessToken
import com.facebook.AccessToken.Companion.getCurrentAccessToken
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.GraphRequest
import com.facebook.HttpMethod
import com.facebook.bolts.TaskCompletionSource
import com.google.gson.GsonBuilder
import java.util.Locale
import org.json.JSONException

private const val GRAPH_RESPONSE_DATA_KEY = "data"

class TournamentFetcher {

  /**
   * Attempts to fetch the list tournaments where the current user is a participant
   *
   * @return TaskCompletionSource The task completion source that contains a list of tournaments as
   * a result
   */
  fun fetchTournaments(): TaskCompletionSource<List<Tournament>> {
    val task: TaskCompletionSource<List<Tournament>> = TaskCompletionSource<List<Tournament>>()
    val params = Bundle()

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
    val request =
        GraphRequest(
            getCurrentAccessToken(),
            "me/tournaments",
            params,
            HttpMethod.GET,
            GraphRequest.Callback { response ->
              response.let { graphResponse ->
                if (graphResponse.error != null) {
                  if (graphResponse.error?.exception != null) {
                    task.setError(graphResponse.error?.exception)
                    return@Callback
                  }
                  task.setError(GraphAPIException("Graph API Error"))
                  return@Callback
                }
                try {
                  val jsonObject = graphResponse.getJSONObject()
                  if (jsonObject == null) {
                    val errorMessage = "Failed to get response"
                    task.setError(GraphAPIException(errorMessage))
                    return@Callback
                  }
                  val data = jsonObject?.getJSONArray(GRAPH_RESPONSE_DATA_KEY)
                  if (data == null || data.length() < 1) {
                    val errorMessage =
                        String.format(Locale.ROOT, "No tournament found", data.length(), 1)
                    task.setError(GraphAPIException(errorMessage))
                    return@Callback
                  }
                  val gson = GsonBuilder().create()
                  val dataString = data.toString()
                  val tournaments =
                      gson.fromJson(dataString, Array<Tournament>::class.java).toList()
                  task.setResult(tournaments)
                } catch (ex: JSONException) {
                  task.setError(ex)
                }
              }
            })
    request.parameters = params
    request.executeAsync()
    return task
  }
}

class GraphAPIException(message: String) : Exception(message)
