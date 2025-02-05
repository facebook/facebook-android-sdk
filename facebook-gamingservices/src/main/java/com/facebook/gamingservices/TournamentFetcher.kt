/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
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
import org.json.JSONException
import java.util.Locale

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
                            val data = jsonObject.getJSONArray(GRAPH_RESPONSE_DATA_KEY)
                            if (data.length() < 1) {
                                val errorMessage =
                                    String.format(
                                        Locale.ROOT,
                                        "No tournament found",
                                        data.length(),
                                        1
                                    )
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
