/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook

import android.util.Log
import com.facebook.internal.FacebookRequestErrorClassification
import com.facebook.internal.Logger
import com.facebook.internal.Utility
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.json.JSONTokener

/**
 * Encapsulates the response, successful or otherwise, of a call to the Facebook platform.
 *
 * @param graphRequest The request that this response is for.
 * @param connection The HttpURLConnection that this response was generated from. If the response
 * was retrieved from the cache, this will be null.
 * @param rawResponse The server response as a String that this response is for.
 * @param graphObject The response returned for this request, if it's in object form.
 * @param graphObjectArray:The response returned for this request, if it's in array form.
 * @param error Information about any errors that may have occurred during the request.
 */
class GraphResponse
internal constructor(
    val request: GraphRequest,
    val connection: HttpURLConnection?,
    val rawResponse: String?,
    private val graphObject: JSONObject?,
    private val graphObjectArray: JSONArray?,
    val error: FacebookRequestError?
) {

  constructor(
      request: GraphRequest,
      connection: HttpURLConnection?,
      rawResponse: String,
      graphObject: JSONObject?
  ) : this(request, connection, rawResponse, graphObject, null, null)

  constructor(
      request: GraphRequest,
      connection: HttpURLConnection?,
      rawResponse: String,
      graphObjects: JSONArray
  ) : this(request, connection, rawResponse, null, graphObjects, null)

  constructor(
      request: GraphRequest,
      connection: HttpURLConnection?,
      error: FacebookRequestError
  ) : this(request, connection, null, null, null, error)

  /** Indicates whether paging is being done forward or backward. */
  enum class PagingDirection {
    /** Indicates that paging is being performed in the forward direction. */
    NEXT,

    /** Indicates that paging is being performed in the backward direction. */
    PREVIOUS
  }

  /**
   * The response returned for this request, if it's in object form.
   *
   * @return the returned JSON object, or null if none was returned (or if the result was a JSON
   * array)
   */
  fun getJSONObject(): JSONObject? = graphObject

  /** The response returned for this request, if it's in object form. Otherwise it's null */
  val jsonObject: JSONObject? = graphObject

  /**
   * The response returned for this request, if it's in array form.
   *
   * @return the returned JSON array, or null if none was returned (or if the result was a JSON
   * object)
   */
  fun getJSONArray(): JSONArray? = graphObjectArray

  /** The response returned for this request, if it's in array form. Otherwise it's null */
  val jsonArray: JSONArray? = graphObjectArray

  /**
   * If a Response contains results that contain paging information, returns a new Request that will
   * retrieve the next page of results, in whichever direction is desired. If no paging information
   * is available, returns null.
   *
   * @param direction enum indicating whether to page forward or backward
   * @return a Request that will retrieve the next page of results in the desired direction, or null
   * if no paging information is available
   */
  fun getRequestForPagedResults(direction: PagingDirection): GraphRequest? {
    var link: String? = null
    if (graphObject != null) {
      val pagingInfo = graphObject.optJSONObject("paging")
      if (pagingInfo != null) {
        link =
            if (direction == PagingDirection.NEXT) {
              pagingInfo.optString("next")
            } else {
              pagingInfo.optString("previous")
            }
      }
    }
    if (Utility.isNullOrEmpty(link)) {
      return null
    }
    if (link != null && link == request.urlForSingleRequest) {
      // We got the same "next" link as we just tried to retrieve. This could happen if cached
      // data is invalid. All we can do in this case is pretend we have finished.
      return null
    }
    val pagingRequest: GraphRequest
    pagingRequest =
        try {
          GraphRequest(request.accessToken, URL(link))
        } catch (e: MalformedURLException) {
          return null
        }
    return pagingRequest
  }

  /** Provides a debugging string for this response. */
  override fun toString(): String {
    val responseCode: String =
        try {
          String.format(Locale.US, "%d", connection?.responseCode ?: 200)
        } catch (e: IOException) {
          "unknown"
        }
    return StringBuilder()
        .append("{Response: ")
        .append(" responseCode: ")
        .append(responseCode)
        .append(", graphObject: ")
        .append(graphObject)
        .append(", error: ")
        .append(error)
        .append("}")
        .toString()
  }

  companion object {
    private val TAG: String? = GraphResponse::class.java.canonicalName
    /**
     * Property name of non-JSON results in the GraphObject. Certain calls to Facebook result in a
     * non-JSON response (e.g., the string literal "true" or "false"). To present a consistent way
     * of accessing results, these are represented as a GraphObject with a single string property
     * with this name.
     */
    const val NON_JSON_RESPONSE_PROPERTY = "FACEBOOK_NON_JSON_RESULT"

    // From v2.1 of the Graph API, write endpoints will now return valid JSON with the result as the
    // value for the "success" key
    const val SUCCESS_KEY = "success"

    private const val CODE_KEY = "code"
    private const val BODY_KEY = "body"

    private const val RESPONSE_LOG_TAG = "Response"

    @SuppressWarnings("resource")
    @JvmStatic
    internal fun fromHttpConnection(
        connection: HttpURLConnection,
        requests: GraphRequestBatch
    ): List<GraphResponse> {
      var stream: InputStream? = null
      return try {
        if (!FacebookSdk.isFullyInitialized()) {
          val msg = "GraphRequest can't be used when Facebook SDK isn't fully initialized"
          Log.e(TAG, msg)
          throw FacebookException(msg)
        }
        stream =
            if (connection.responseCode >= 400) {
              connection.errorStream
            } else {
              connection.inputStream
            }
        createResponsesFromStream(stream, connection, requests)
      } catch (facebookException: FacebookException) {
        Logger.log(
            LoggingBehavior.REQUESTS, RESPONSE_LOG_TAG, "Response <Error>: %s", facebookException)
        constructErrorResponses(requests, connection, facebookException)
      } catch (exception: Exception) {
        // Note due to bugs various android devices some devices can throw a
        // SecurityException or NoSuchAlgorithmException. Make sure to handle these
        // exceptions here.
        Logger.log(LoggingBehavior.REQUESTS, RESPONSE_LOG_TAG, "Response <Error>: %s", exception)
        constructErrorResponses(requests, connection, FacebookException(exception))
      } finally {
        Utility.closeQuietly(stream)
      }
    }

    @JvmStatic
    @Throws(FacebookException::class, JSONException::class, IOException::class)
    internal fun createResponsesFromStream(
        stream: InputStream?,
        connection: HttpURLConnection?,
        requests: GraphRequestBatch
    ): List<GraphResponse> {
      val responseString = Utility.readStreamToString(stream)
      Logger.log(
          LoggingBehavior.INCLUDE_RAW_RESPONSES,
          RESPONSE_LOG_TAG,
          "Response (raw)\n  Size: %d\n  Response:\n%s\n",
          responseString.length,
          responseString)
      return createResponsesFromString(responseString, connection, requests)
    }

    @JvmStatic
    @Throws(FacebookException::class, JSONException::class, IOException::class)
    internal fun createResponsesFromString(
        responseString: String,
        connection: HttpURLConnection?,
        requests: GraphRequestBatch
    ): List<GraphResponse> {
      val tokener = JSONTokener(responseString)
      val resultObject = tokener.nextValue()
      val responses = createResponsesFromObject(connection, requests, resultObject)
      Logger.log(
          LoggingBehavior.REQUESTS,
          RESPONSE_LOG_TAG,
          "Response\n  Id: %s\n  Size: %d\n  Responses:\n%s\n",
          requests.id,
          responseString.length,
          responses)
      return responses
    }

    @Throws(FacebookException::class, JSONException::class)
    private fun createResponsesFromObject(
        connection: HttpURLConnection?,
        requests: List<GraphRequest>,
        sourceObject: Any
    ): List<GraphResponse> {
      var obj = sourceObject
      val numRequests = requests.size
      val responses: MutableList<GraphResponse> = ArrayList(numRequests)
      val originalResult = obj
      if (numRequests == 1) {
        val request = requests[0]
        try {
          // Single request case -- the entire response is the result, wrap it as "body" so we
          // can handle it the same as we do in the batched case. We get the response code
          // from the actual HTTP response, as opposed to the batched case where it is
          // returned as a "code" element.
          val jsonObject = JSONObject()
          jsonObject.put(BODY_KEY, obj)
          val responseCode = connection?.responseCode ?: 200
          jsonObject.put(CODE_KEY, responseCode)
          val jsonArray = JSONArray()
          jsonArray.put(jsonObject)

          // Pretend we got an array of 1 back.
          obj = jsonArray
        } catch (e: JSONException) {
          responses.add(GraphResponse(request, connection, FacebookRequestError(connection, e)))
        } catch (e: IOException) {
          responses.add(GraphResponse(request, connection, FacebookRequestError(connection, e)))
        }
      }
      if (obj !is JSONArray || obj.length() != numRequests) {
        throw FacebookException("Unexpected number of results")
      }
      val jsonArray = obj
      for (i in 0 until jsonArray.length()) {
        val request = requests[i]
        try {
          val obj = jsonArray[i]
          responses.add(createResponseFromObject(request, connection, obj, originalResult))
        } catch (e: JSONException) {
          responses.add(GraphResponse(request, connection, FacebookRequestError(connection, e)))
        } catch (e: FacebookException) {
          responses.add(GraphResponse(request, connection, FacebookRequestError(connection, e)))
        }
      }
      return responses
    }

    @Throws(JSONException::class)
    private fun createResponseFromObject(
        request: GraphRequest,
        connection: HttpURLConnection?,
        sourceObject: Any,
        originalResult: Any
    ): GraphResponse {
      var obj = sourceObject
      if (obj is JSONObject) {
        val jsonObject = obj
        val error =
            FacebookRequestError.checkResponseAndCreateError(jsonObject, originalResult, connection)
        if (error != null) {
          Log.e(TAG, error.toString())
          if (error.errorCode == FacebookRequestErrorClassification.EC_INVALID_TOKEN &&
              Utility.isCurrentAccessToken(request.accessToken)) {
            if (error.subErrorCode != FacebookRequestErrorClassification.ESC_APP_INACTIVE) {
              AccessToken.setCurrentAccessToken(null)
            } else if (AccessToken.getCurrentAccessToken()?.isExpired == false) {
              AccessToken.expireCurrentAccessToken()
            }
          }
          return GraphResponse(request, connection, error)
        }
        val body = Utility.getStringPropertyAsJSON(jsonObject, BODY_KEY, NON_JSON_RESPONSE_PROPERTY)
        if (body is JSONObject) {
          return GraphResponse(request, connection, body.toString(), body)
        } else if (body is JSONArray) {
          return GraphResponse(request, connection, body.toString(), body)
        }
        // We didn't get a body we understand how to handle, so pretend we got nothing.
        obj = JSONObject.NULL
      }
      return if (obj === JSONObject.NULL) {
        GraphResponse(request, connection, obj.toString(), null as JSONObject?)
      } else {
        throw FacebookException(
            "Got unexpected object type in response, class: " + obj.javaClass.simpleName)
      }
    }

    /**
     * Build GraphResponse with Error
     *
     * @param requests original graph requests
     * @param connection request url connection
     * @param error error included in response
     *
     * @return graph response with error
     */
    @JvmStatic
    fun constructErrorResponses(
        requests: List<GraphRequest>,
        connection: HttpURLConnection?,
        error: FacebookException?
    ): List<GraphResponse> {
      return requests.map { GraphResponse(it, connection, FacebookRequestError(connection, error)) }
    }
  }
}
