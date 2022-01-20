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

package com.facebook

import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.text.TextUtils
import android.util.Log
import android.util.Pair
import androidx.annotation.VisibleForTesting
import com.facebook.GraphResponse.Companion.constructErrorResponses
import com.facebook.GraphResponse.Companion.fromHttpConnection
import com.facebook.internal.AttributionIdentifiers.Companion.getAttributionIdentifiers
import com.facebook.internal.InternalSettings.getCustomUserAgent
import com.facebook.internal.Logger
import com.facebook.internal.Logger.Companion.log
import com.facebook.internal.Logger.Companion.registerAccessToken
import com.facebook.internal.NativeProtocol
import com.facebook.internal.ServerProtocol.getFacebookGraphUrlBase
import com.facebook.internal.ServerProtocol.getGraphUrlBase
import com.facebook.internal.ServerProtocol.getGraphUrlBaseForSubdomain
import com.facebook.internal.ServerProtocol.getGraphVideoUrlBase
import com.facebook.internal.Utility.copyAndCloseInputStream
import com.facebook.internal.Utility.disconnectQuietly
import com.facebook.internal.Utility.getContentSize
import com.facebook.internal.Utility.getMetadataApplicationId
import com.facebook.internal.Utility.isContentUri
import com.facebook.internal.Utility.isFileUri
import com.facebook.internal.Utility.isNullOrEmpty
import com.facebook.internal.Utility.logd
import com.facebook.internal.Validate
import com.facebook.internal.Validate.notEmptyAndContainsNoNulls
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList
import kotlin.collections.Collection
import kotlin.collections.HashMap
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableMap
import kotlin.collections.set
import kotlin.collections.toList
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * A single request to be sent to the Facebook Platform through the
 * [Graph API](https://developers.facebook.com/docs/reference/api/). The Request class provides
 * functionality relating to serializing and deserializing requests and responses, making calls in
 * batches (with a single round-trip to the service) and making calls asynchronously.
 *
 * The particular service endpoint that a request targets is determined by a graph path (see the
 * [setGraphPath][.setGraphPath] method).
 *
 * A Request can be executed either anonymously or representing an authenticated user. In the former
 * case, no AccessToken needs to be specified, while in the latter, an AccessToken must be provided.
 * If requests are executed in a batch, a Facebook application ID must be associated with the batch,
 * either by setting the application ID in the AndroidManifest.xml or via FacebookSdk or by calling
 * the [setDefaultBatchApplicationId][.setDefaultBatchApplicationId] method.
 *
 * After completion of a request, the AccessToken, if not null and taken from AccessTokenManager,
 * will be checked to determine if its Facebook access token needs to be extended; if so, a request
 * to extend it will be issued in the background.
 */
class GraphRequest {
  /** The access token associated with this request. Null if none has been specified. */
  var accessToken: AccessToken?

  /** The graph path of this request, if any. Null if there is none. */
  var graphPath: String? = null
  /**
   * The GraphObject, if any, associated with this request. Null if there is none. This is
   * meaningful only for POST requests.
   */
  var graphObject: JSONObject? = null
  /**
   * The name of this requests entry in a batched request. Null if none has been specified. This
   * value is only used if this request is submitted as part of a batched request. It can be used to
   * specified dependencies between requests. See
   * [Batch Requests](https://developers.facebook.com/docs/reference/api/batch/) in the Graph API
   * documentation for more details.
   *
   * It must be unique within a particular batch of requests.
   */
  var batchEntryName: String? = null

  /**
   * The name of the request that this request entry explicitly depends on in a batched request.
   *
   * This value is only used if this request is submitted as part of a batched request. It can be
   * used to specified dependencies between requests. See
   * [Batch Requests](https://developers.facebook.com/docs/reference/api/batch/) in the Graph API
   * documentation for more details.
   */
  var batchEntryDependsOn: String? = null

  /**
   * Whether or not this batch entry will return a response if it is successful. Only applies if
   * another request entry in the batch specifies this entry as a dependency. Null if none has been
   * specified See [Batch Requests](https://developers.facebook.com/docs/reference/api/batch/) in
   * the Graph API documentation for more details.
   */
  var batchEntryOmitResultOnSuccess = true

  /** The parameters for this request. */
  var parameters: Bundle

  /**
   * The tag on the request; this is an application-defined object that can be used to distinguish
   * between different requests. Its value has no effect on the execution of the request.
   */
  var tag: Any? = null

  /**
   * The version of the API that this request will use. By default this is the current API at the
   * time the SDK is released. Only use this if you need to explicitly override.
   */
  var version: String? = null

  /** The callback which will be called when the request finishes. */
  var callback: Callback? = null
    set(callback) {
      // Wrap callback to parse debug response if Graph Debug Mode is Enabled.
      if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_INFO) ||
          FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
        val wrapper = Callback { response ->
          val responseObject = response.getJSONObject()
          val debug = responseObject?.optJSONObject(DEBUG_KEY)
          val debugMessages = debug?.optJSONArray(DEBUG_MESSAGES_KEY)
          if (debugMessages != null) {
            for (i in 0 until debugMessages.length()) {
              val debugMessageObject = debugMessages.optJSONObject(i)
              var debugMessage = debugMessageObject?.optString(DEBUG_MESSAGE_KEY)
              val debugMessageType = debugMessageObject?.optString(DEBUG_MESSAGE_TYPE_KEY)
              val debugMessageLink = debugMessageObject?.optString(DEBUG_MESSAGE_LINK_KEY)
              if (debugMessage != null && debugMessageType != null) {
                var behavior = LoggingBehavior.GRAPH_API_DEBUG_INFO
                if (debugMessageType == "warning") {
                  behavior = LoggingBehavior.GRAPH_API_DEBUG_WARNING
                }
                if (!isNullOrEmpty(debugMessageLink)) {
                  debugMessage += " Link: $debugMessageLink"
                }
                log(behavior, TAG, debugMessage)
              }
            }
          }
          callback?.onCompleted(response)
        }
        field = wrapper
      } else {
        field = callback
      }
    }

  /** The [HttpMethod] to use for this request. Assign null for default (HttpMethod.GET) */
  var httpMethod: HttpMethod? = null
    set(value) {
      if (this.overriddenURL != null && value != HttpMethod.GET) {
        throw FacebookException("Can't change HTTP method on request with overridden URL.")
      }
      field = value ?: HttpMethod.GET
    }

  private var forceApplicationRequest = false
  private var overriddenURL: String? = null

  companion object {
    /**
     * The maximum number of requests that can be submitted in a single batch. This limit is
     * enforced on the service side by the Facebook platform, not by the Request class.
     */
    const val MAXIMUM_BATCH_SIZE = 50
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @JvmField
    internal val TAG = GraphRequest::class.java.simpleName
    private const val VIDEOS_SUFFIX = "/videos"
    private const val ME = "me"
    private const val MY_FRIENDS = "me/friends"
    private const val MY_PHOTOS = "me/photos"
    private const val SEARCH = "search"
    private const val USER_AGENT_BASE = "FBAndroidSDK"
    private const val USER_AGENT_HEADER = "User-Agent"
    private const val CONTENT_TYPE_HEADER = "Content-Type"
    private const val ACCEPT_LANGUAGE_HEADER = "Accept-Language"
    private const val CONTENT_ENCODING_HEADER = "Content-Encoding"

    // Parameter names/values
    private const val FORMAT_PARAM = "format"
    private const val FORMAT_JSON = "json"
    private const val SDK_PARAM = "sdk"
    private const val SDK_ANDROID = "android"
    const val ACCESS_TOKEN_PARAM = "access_token"
    private const val BATCH_ENTRY_NAME_PARAM = "name"
    private const val BATCH_ENTRY_OMIT_RESPONSE_ON_SUCCESS_PARAM = "omit_response_on_success"
    private const val BATCH_ENTRY_DEPENDS_ON_PARAM = "depends_on"
    private const val BATCH_APP_ID_PARAM = "batch_app_id"
    private const val BATCH_RELATIVE_URL_PARAM = "relative_url"
    private const val BATCH_BODY_PARAM = "body"
    private const val BATCH_METHOD_PARAM = "method"
    private const val BATCH_PARAM = "batch"
    private const val ATTACHMENT_FILENAME_PREFIX = "file"
    private const val ATTACHED_FILES_PARAM = "attached_files"
    private const val ISO_8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssZ"
    private const val DEBUG_PARAM = "debug"
    private const val DEBUG_SEVERITY_INFO = "info"
    private const val DEBUG_SEVERITY_WARNING = "warning"
    private const val DEBUG_KEY = "__debug__"
    private const val DEBUG_MESSAGES_KEY = "messages"
    private const val DEBUG_MESSAGE_KEY = "message"
    private const val DEBUG_MESSAGE_TYPE_KEY = "type"
    private const val DEBUG_MESSAGE_LINK_KEY = "link"
    private const val PICTURE_PARAM = "picture"
    private const val CAPTION_PARAM = "caption"
    const val FIELDS_PARAM = "fields"
    private val MIME_BOUNDARY: String
    private var defaultBatchApplicationId: String? = null

    init {
      // Multipart chars
      val chars = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
      val buffer = StringBuilder()
      val random = SecureRandom()
      val count: Int = random.nextInt(11) + 30 // a random size from 30 to 40
      for (i in 0 until count) {
        buffer.append(chars[random.nextInt(chars.size)])
      }
      MIME_BOUNDARY = buffer.toString()
    }

    /**
     * Gets the default Facebook application ID that will be used to submit batched requests.
     * Batched requests require an application ID, so either at least one request in a batch must
     * provide an access token or the application ID must be specified explicitly.
     *
     * @return the Facebook application ID to use for batched requests if none can be determined
     */
    @JvmStatic fun getDefaultBatchApplicationId(): String? = defaultBatchApplicationId

    /**
     * Sets the default application ID that will be used to submit batched requests if none of those
     * requests specifies an access token. Batched requests require an application ID, so either at
     * least one request in a batch must specify an access token or the application ID must be
     * specified explicitly.
     *
     * @param applicationId the Facebook application ID to use for batched requests if none can be
     * determined
     */
    @JvmStatic
    fun setDefaultBatchApplicationId(applicationId: String?) {
      defaultBatchApplicationId = applicationId
    }

    // Group 1 in the pattern is the path without the version info
    private val versionPattern = Pattern.compile("""^/?v\d+\.\d+/(.*)""")

    /**
     * Creates a new Request configured to delete a resource through the Graph API.
     *
     * @param accessToken the access token to use, or null
     * @param id the id of the object to delete
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newDeleteObjectRequest(
        accessToken: AccessToken?,
        id: String?,
        callback: Callback?
    ): GraphRequest {
      return GraphRequest(accessToken, id, null, HttpMethod.DELETE, callback)
    }

    /**
     * Creates a new Request configured to retrieve a user's own profile.
     *
     * @param accessToken the access token to use, or null
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newMeRequest(accessToken: AccessToken?, callback: GraphJSONObjectCallback?): GraphRequest {
      val wrapper = Callback { response ->
        callback?.onCompleted(response.getJSONObject(), response)
      }
      return GraphRequest(accessToken, ME, null, null, wrapper)
    }

    /**
     * Creates a new Request configured to post a GraphObject to a particular graph path, to either
     * create or update the object at that path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath the graph path to retrieve, create, or delete
     * @param graphObject the graph object to create or update
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newPostRequest(
        accessToken: AccessToken?,
        graphPath: String?,
        graphObject: JSONObject?,
        callback: Callback?
    ): GraphRequest {
      val request = GraphRequest(accessToken, graphPath, null, HttpMethod.POST, callback)
      request.graphObject = graphObject
      return request
    }

    /**
     * Creates a new Request configured to retrieve a user's friend list.
     *
     * @param accessToken the access token to use, or null
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newMyFriendsRequest(
        accessToken: AccessToken?,
        callback: GraphJSONArrayCallback?
    ): GraphRequest {
      val wrapper: Callback =
          object : Callback {
            override fun onCompleted(response: GraphResponse) {
              if (callback != null) {
                val result = response.getJSONObject()
                val data = result?.optJSONArray("data")
                callback.onCompleted(data, response)
              }
            }
          }
      return GraphRequest(accessToken, MY_FRIENDS, null, null, wrapper)
    }

    /**
     * Creates a new Request configured to retrieve a particular graph path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath the graph path to retrieve
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newGraphPathRequest(
        accessToken: AccessToken?,
        graphPath: String?,
        callback: Callback?
    ): GraphRequest {
      return GraphRequest(accessToken, graphPath, null, null, callback)
    }

    /**
     * Creates a new Request that is configured to perform a search for places near a specified
     * location via the Graph API. At least one of location or searchText must be specified.
     *
     * @param accessToken the access token to use, or null
     * @param location the location around which to search; only the latitude and longitude
     * components of the location are meaningful
     * @param radiusInMeters the radius around the location to search, specified in meters; this is
     * ignored if no location is specified
     * @param resultsLimit the maximum number of results to return
     * @param searchText optional text to search for as part of the name or type of an object
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions
     * @return a Request that is ready to execute
     * @throws FacebookException If neither location nor searchText is specified
     */
    @JvmStatic
    fun newPlacesSearchRequest(
        accessToken: AccessToken?,
        location: Location?,
        radiusInMeters: Int,
        resultsLimit: Int,
        searchText: String?,
        callback: GraphJSONArrayCallback?
    ): GraphRequest {
      if (location == null && isNullOrEmpty(searchText)) {
        throw FacebookException("Either location or searchText must be specified.")
      }
      val parameters = Bundle(5)
      parameters.putString("type", "place")
      parameters.putInt("limit", resultsLimit)
      if (location != null) {
        parameters.putString(
            "center", String.format(Locale.US, "%f,%f", location.latitude, location.longitude))
        parameters.putInt("distance", radiusInMeters)
      }
      if (!isNullOrEmpty(searchText)) {
        parameters.putString("q", searchText)
      }
      val wrapper = Callback { response ->
        if (callback != null) {
          val result = response.getJSONObject()
          val data = result?.optJSONArray("data")
          callback.onCompleted(data, response)
        }
      }
      return GraphRequest(accessToken, SEARCH, parameters, HttpMethod.GET, wrapper)
    }

    /**
     * Creates a new Request configured to upload a photo to the specified graph path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath the graph path to use, defaults to me/photos
     * @param image the bitmap image to upload
     * @param caption the user generated caption for the photo, can be null
     * @param params the parameters, can be null
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions, can be null
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newUploadPhotoRequest(
        accessToken: AccessToken?,
        graphPath: String?,
        image: Bitmap?,
        caption: String?,
        params: Bundle?,
        callback: Callback?
    ): GraphRequest {
      var graphPath = graphPath
      graphPath = getDefaultPhotoPathIfNull(graphPath)
      val parameters = Bundle()
      if (params != null) {
        parameters.putAll(params)
      }
      parameters.putParcelable(PICTURE_PARAM, image)
      if (caption != null && caption.isNotEmpty()) {
        parameters.putString(CAPTION_PARAM, caption)
      }
      return GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback)
    }

    /**
     * Creates a new Request configured to upload a photo to the specified graph path. The photo
     * will be read from the specified file.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath the graph path to use, defaults to me/photos
     * @param file the file containing the photo to upload
     * @param caption the user generated caption for the photo, can be null
     * @param params the parameters, can be null
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions, can be null
     * @return a Request that is ready to execute
     * @throws java.io.FileNotFoundException if the file doesn't exist
     */
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun newUploadPhotoRequest(
        accessToken: AccessToken?,
        graphPath: String?,
        file: File?,
        caption: String?,
        params: Bundle?,
        callback: Callback?
    ): GraphRequest {
      var graphPath = graphPath
      graphPath = getDefaultPhotoPathIfNull(graphPath)
      val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
      val parameters = Bundle()
      if (params != null) {
        parameters.putAll(params)
      }
      parameters.putParcelable(PICTURE_PARAM, descriptor)
      if (caption != null && caption.isNotEmpty()) {
        parameters.putString(CAPTION_PARAM, caption)
      }
      return GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback)
    }

    /**
     * Creates a new Request configured to upload a photo to the specified graph path. The photo
     * will be read from the specified Uri.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath the graph path to use, defaults to me/photos
     * @param photoUri the file:// or content:// Uri to the photo on device
     * @param caption the user generated caption for the photo, can be null
     * @param params the parameters, can be null
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions, can be null
     * @return a Request that is ready to execute
     * @throws FileNotFoundException if the Uri does not exist
     */
    @JvmStatic
    @Throws(FileNotFoundException::class)
    fun newUploadPhotoRequest(
        accessToken: AccessToken?,
        graphPath: String?,
        photoUri: Uri,
        caption: String?,
        params: Bundle?,
        callback: Callback?
    ): GraphRequest {
      var graphPath = graphPath
      graphPath = getDefaultPhotoPathIfNull(graphPath)
      if (isFileUri(photoUri)) {
        return newUploadPhotoRequest(
            accessToken, graphPath, File(photoUri.path), caption, params, callback)
      } else if (!isContentUri(photoUri)) {
        throw FacebookException("The photo Uri must be either a file:// or content:// Uri")
      }
      val parameters = Bundle()
      if (params != null) {
        parameters.putAll(params)
      }
      parameters.putParcelable(PICTURE_PARAM, photoUri)
      if (caption != null && caption.isNotEmpty()) {
        parameters.putString(CAPTION_PARAM, caption)
      }
      return GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback)
    }

    /**
     * Creates a new Request configured to retrieve an App User ID for the app's Facebook user.
     * Callers will send this ID back to their own servers, collect up a set to create a Facebook
     * Custom Audience with, and then use the resultant Custom Audience to target ads.
     *
     * The GraphObject in the response will include a "custom_audience_third_party_id" property,
     * with the value being the ID retrieved. This ID is an encrypted encoding of the Facebook
     * user's ID and the invoking Facebook app ID. Multiple calls with the same user will return
     * different IDs, thus these IDs cannot be used to correlate behavior across devices or
     * applications, and are only meaningful when sent back to Facebook for creating Custom
     * Audiences.
     *
     * The ID retrieved represents the Facebook user identified in the following way: if the
     * specified access token (or active access token if `null`) is valid, the ID will represent the
     * user associated with the active access token; otherwise the ID will represent the user logged
     * into the native Facebook app on the device. A `null` ID will be provided into the callback if
     * a) there is no native Facebook app, b) no one is logged into it, or c) the app has previously
     * called [FacebookSdk.setLimitEventAndDataUsage] ;} with `true` for this user. **You must call
     * this method from a background thread for it to work properly.**
     *
     * @param accessToken the access token to issue the Request on, or null If there is no logged-in
     * Facebook user, null is the expected choice.
     * @param context the Application context from which the app ID will be pulled, and from which
     * the 'attribution ID' for the Facebook user is determined. If there has been no app ID set, an
     * exception will be thrown.
     * @param applicationId explicitly specified Facebook App ID. If null, the application ID from
     * the access token will be used, if any; if not, the application ID from metadata will be used.
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions. The GraphObject in the Response will contain a
     * "custom_audience_third_party_id" property that represents the user as described above.
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newCustomAudienceThirdPartyIdRequest(
        accessToken: AccessToken?,
        context: Context,
        applicationId: String?,
        callback: Callback?
    ): GraphRequest {
      var applicationId = applicationId
      if (applicationId == null && accessToken != null) {
        applicationId = accessToken.applicationId
      }
      if (applicationId == null) {
        applicationId = getMetadataApplicationId(context)
      }
      if (applicationId == null) {
        throw FacebookException("Facebook App ID cannot be determined")
      }
      val endpoint = "$applicationId/custom_audience_third_party_id"
      val attributionIdentifiers = getAttributionIdentifiers(context)
      val parameters = Bundle()
      if (accessToken == null) {
        if (attributionIdentifiers == null) {
          throw FacebookException(
              "There is no access token and attribution identifiers could not be " + "retrieved")
        }

        // Only use the attributionID if we don't have an access token.  If we do, then the user
        // token will be used to identify the user, and is more reliable than the attributionID.
        val udid =
            if (attributionIdentifiers.attributionId != null) attributionIdentifiers.attributionId
            else attributionIdentifiers.androidAdvertiserId
        if (attributionIdentifiers.attributionId != null) {
          parameters.putString("udid", udid)
        }
      }

      // Server will choose to not provide the App User ID in the event that event usage has been
      // limited for this user for this app.
      if (FacebookSdk.getLimitEventAndDataUsage(context) ||
          attributionIdentifiers != null && attributionIdentifiers.isTrackingLimited) {
        parameters.putString("limit_event_usage", "1")
      }
      return GraphRequest(accessToken, endpoint, parameters, HttpMethod.GET, callback)
    }

    /**
     * Creates a new Request configured to retrieve an App User ID for the app's Facebook user.
     * Callers will send this ID back to their own servers, collect up a set to create a Facebook
     * Custom Audience with, and then use the resultant Custom Audience to target ads.
     *
     * The GraphObject in the response will include a "custom_audience_third_party_id" property,
     * with the value being the ID retrieved. This ID is an encrypted encoding of the Facebook
     * user's ID and the invoking Facebook app ID. Multiple calls with the same user will return
     * different IDs, thus these IDs cannot be used to correlate behavior across devices or
     * applications, and are only meaningful when sent back to Facebook for creating Custom
     * Audiences.
     *
     * The ID retrieved represents the Facebook user identified in the following way: if the
     * specified access token (or active access token if `null`) is valid, the ID will represent the
     * user associated with the active access token; otherwise the ID will represent the user logged
     * into the native Facebook app on the device. A `null` ID will be provided into the callback if
     * a) there is no native Facebook app, b) no one is logged into it, or c) the app has previously
     * called [FacebookSdk.setLimitEventAndDataUsage] with `true` for this user. **You must call
     * this method from a background thread for it to work properly.**
     *
     * @param accessToken the access token to issue the Request on, or null If there is no logged-in
     * Facebook user, null is the expected choice.
     * @param context the Application context from which the app ID will be pulled, and from which
     * the 'attribution ID' for the Facebook user is determined. If there has been no app ID set, an
     * exception will be thrown.
     * @param callback a callback that will be called when the request is completed to handle
     * success or error conditions. The GraphObject in the Response will contain a
     * "custom_audience_third_party_id" property that represents the user as described above.
     * @return a Request that is ready to execute
     */
    @JvmStatic
    fun newCustomAudienceThirdPartyIdRequest(
        accessToken: AccessToken?,
        context: Context,
        callback: Callback?
    ): GraphRequest {
      return newCustomAudienceThirdPartyIdRequest(accessToken, context, null, callback)
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException If any of the requests in the batch are badly constructed or if
     * there are problems contacting the service
     * @throws IllegalArgumentException if the passed in array is zero-length
     * @throws NullPointerException if the passed in array or any of its contents are null
     */
    @JvmStatic
    fun toHttpConnection(vararg requests: GraphRequest): HttpURLConnection {
      return toHttpConnection(requests.toList())
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException If any of the requests in the batch are badly constructed or if
     * there are problems contacting the service
     * @throws IllegalArgumentException if the passed in collection is empty
     * @throws NullPointerException if the passed in collection or any of its contents are null
     */
    @JvmStatic
    fun toHttpConnection(requests: Collection<GraphRequest>): HttpURLConnection {
      Validate.notEmpty(requests, "requests")
      return toHttpConnection(GraphRequestBatch(requests))
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests a RequestBatch to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException If any of the requests in the batch are badly constructed or if
     * there are problems contacting the service
     * @throws IllegalArgumentException
     */
    @JvmStatic
    fun toHttpConnection(requests: GraphRequestBatch): HttpURLConnection {
      validateFieldsParamForGetRequests(requests)
      val url: URL
      url =
          try {
            if (requests.size == 1) {
              // Single request case.
              val request = requests[0]
              // In the non-batch case, the URL we use really is the same one returned by
              // getUrlForSingleRequest.
              URL(request.urlForSingleRequest)
            } else {
              // Batch case -- URL is just the graph API base, individual request URLs are
              // serialized as relative_url parameters within each batch entry.
              URL(getGraphUrlBase())
            }
          } catch (e: MalformedURLException) {
            throw FacebookException("could not construct URL for request", e)
          }
      var connection: HttpURLConnection? = null
      try {
        connection = createConnection(url)
        serializeToUrlConnection(requests, connection)
      } catch (e: IOException) {
        disconnectQuietly(connection)
        throw FacebookException("could not construct request body", e)
      } catch (e: JSONException) {
        disconnectQuietly(connection)
        throw FacebookException("could not construct request body", e)
      }
      return connection
    }

    /**
     * Executes a single request on the current thread and blocks while waiting for the response.
     *
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param request the Request to execute
     * @return the Response object representing the results of the request
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     */
    @JvmStatic
    fun executeAndWait(request: GraphRequest): GraphResponse {
      val responses = executeBatchAndWait(request)
      if (responses.size != 1) {
        throw FacebookException("invalid state: expected a single response")
      }
      return responses[0]
    }

    /**
     * Executes requests on the current thread as a single batch and blocks while waiting for the
     * response.
     *
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws NullPointerException In case of a null request
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     */
    @JvmStatic
    fun executeBatchAndWait(vararg requests: GraphRequest): List<GraphResponse> {
      return executeBatchAndWait(requests.toList())
    }

    /**
     * Executes requests as a single batch on the current thread and blocks while waiting for the
     * responses.
     *
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     */
    @JvmStatic
    fun executeBatchAndWait(requests: Collection<GraphRequest>): List<GraphResponse> {
      return executeBatchAndWait(GraphRequestBatch(requests))
    }

    /**
     * Executes requests on the current thread as a single batch and blocks while waiting for the
     * responses.
     *
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the batch of Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException if the passed in RequestBatch or any of its contents are null
     */
    @JvmStatic
    fun executeBatchAndWait(requests: GraphRequestBatch): List<GraphResponse> {
      notEmptyAndContainsNoNulls(requests, "requests")
      var connection: HttpURLConnection? = null
      return try {
        var exception: Exception? = null
        connection =
            try {
              toHttpConnection(requests)
            } catch (ex: Exception) {
              exception = ex
              null
            }
        if (connection != null) {
          executeConnectionAndWait(connection, requests)
        } else {
          val responses =
              constructErrorResponses(requests.requests, null, FacebookException(exception))
          runCallbacks(requests, responses)
          responses
        }
      } finally {
        disconnectQuietly(connection)
      }
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the [setCallback][.setCallback] method).
     *
     * This should only be called from the UI thread.
     *
     * @param requests the Requests to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws NullPointerException If a null request is passed in
     */
    @JvmStatic
    fun executeBatchAsync(vararg requests: GraphRequest): GraphRequestAsyncTask {
      return executeBatchAsync(requests.toList())
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the [setCallback][.setCallback] method).
     *
     * This should only be called from the UI thread.
     *
     * @param requests the Requests to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws IllegalArgumentException if the passed in collection is empty
     * @throws NullPointerException if the passed in collection or any of its contents are null
     */
    @JvmStatic
    fun executeBatchAsync(requests: Collection<GraphRequest>): GraphRequestAsyncTask {
      return executeBatchAsync(GraphRequestBatch(requests))
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the [setCallback][.setCallback] method).
     *
     * This should only be called from the UI thread.
     *
     * @param requests the RequestBatch to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException if the passed in RequestBatch or any of its contents are null
     */
    @JvmStatic
    fun executeBatchAsync(requests: GraphRequestBatch): GraphRequestAsyncTask {
      notEmptyAndContainsNoNulls(requests, "requests")
      val asyncTask = GraphRequestAsyncTask(requests)
      asyncTask.executeOnExecutor(FacebookSdk.getExecutor())
      return asyncTask
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation
     * is done that the contents of the connection actually reflect the serialized requests, so it
     * is the caller's responsibility to ensure that it will correctly generate the desired
     * responses.
     *
     * This should only be called if you have transitioned off the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests the requests represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     */
    @JvmStatic
    fun executeConnectionAndWait(
        connection: HttpURLConnection,
        requests: Collection<GraphRequest>
    ): List<GraphResponse> {
      return executeConnectionAndWait(connection, GraphRequestBatch(requests))
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation
     * is done that the contents of the connection actually reflect the serialized requests, so it
     * is the caller's responsibility to ensure that it will correctly generate the desired
     * responses.
     *
     * This should only be called if you have transitioned off the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests the RequestBatch represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     * service
     */
    @JvmStatic
    fun executeConnectionAndWait(
        connection: HttpURLConnection,
        requests: GraphRequestBatch
    ): List<GraphResponse> {
      val responses = fromHttpConnection(connection, requests)
      disconnectQuietly(connection)
      val numRequests = requests.size
      if (numRequests != responses.size) {
        throw FacebookException(
            String.format(
                Locale.US, "Received %d responses while expecting %d", responses.size, numRequests))
      }
      runCallbacks(requests, responses)

      // Try extending the current access token in case it's needed.
      AccessTokenManager.getInstance().extendAccessTokenIfNeeded()
      return responses
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection.
     * No validation is done that the contents of the connection actually reflect the serialized
     * requests, so it is the caller's responsibility to ensure that it will correctly generate the
     * desired responses. This function will return immediately, and the requests will be processed
     * on a separate thread. In order to process results of a request, or determine whether a
     * request succeeded or failed, a callback must be specified (see the [
     * setCallback][.setCallback] method).
     *
     * This should only be called from the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests the requests represented by the HttpURLConnection
     * @return a RequestAsyncTask that is executing the request
     */
    @JvmStatic
    fun executeConnectionAsync(
        connection: HttpURLConnection,
        requests: GraphRequestBatch
    ): GraphRequestAsyncTask {
      return executeConnectionAsync(null, connection, requests)
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection.
     * No validation is done that the contents of the connection actually reflect the serialized
     * requests, so it is the caller's responsibility to ensure that it will correctly generate the
     * desired responses. This function will return immediately, and the requests will be processed
     * on a separate thread. In order to process results of a request, or determine whether a
     * request succeeded or failed, a callback must be specified (see the [
     * setCallback][.setCallback] method)
     *
     * This should only be called from the UI thread.
     *
     * @param callbackHandler a Handler that will be used to post calls to the callback for each
     * request; if null, a Handler will be instantiated on the calling thread
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests the requests represented by the HttpURLConnection
     * @return a RequestAsyncTask that is executing the request
     */
    @JvmStatic
    fun executeConnectionAsync(
        callbackHandler: Handler?,
        connection: HttpURLConnection,
        requests: GraphRequestBatch
    ): GraphRequestAsyncTask {
      val asyncTask = GraphRequestAsyncTask(connection, requests)
      requests.callbackHandler = callbackHandler
      asyncTask.executeOnExecutor(FacebookSdk.getExecutor())
      return asyncTask
    }

    @JvmStatic
    internal fun runCallbacks(requests: GraphRequestBatch, responses: List<GraphResponse>) {
      val numRequests = requests.size

      // Compile the list of callbacks to call and then run them either on this thread or via the
      // Handler we received
      val callbacks = ArrayList<Pair<Callback, GraphResponse>>()
      for (i in 0 until numRequests) {
        val request = requests[i]
        if (request.callback != null) {
          callbacks.add(Pair(request.callback, responses[i]))
        }
      }
      if (callbacks.size > 0) {
        val runnable = Runnable {
          for (pair in callbacks) {
            pair.first.onCompleted(pair.second)
          }
          val batchCallbacks: List<GraphRequestBatch.Callback> = requests.callbacks
          for (batchCallback in batchCallbacks) {
            batchCallback.onBatchCompleted(requests)
          }
        }
        val callbackHandler = requests.callbackHandler
        // Post to the handler.
        callbackHandler?.post(runnable) ?: // Run on this thread.
        runnable.run()
      }
    }

    private fun getDefaultPhotoPathIfNull(graphPath: String?): String {
      return graphPath ?: MY_PHOTOS
    }

    @Throws(IOException::class)
    private fun createConnection(url: URL): HttpURLConnection {
      val connection = url.openConnection() as HttpURLConnection
      connection.setRequestProperty(USER_AGENT_HEADER, userAgent)
      connection.setRequestProperty(ACCEPT_LANGUAGE_HEADER, Locale.getDefault().toString())
      connection.setChunkedStreamingMode(0)
      return connection
    }

    private fun hasOnProgressCallbacks(requests: GraphRequestBatch): Boolean {
      for (callback in requests.callbacks) {
        if (callback is GraphRequestBatch.OnProgressCallback) {
          return true
        }
      }
      for (request in requests) {
        if (request.callback is OnProgressCallback) {
          return true
        }
      }
      return false
    }

    private fun setConnectionContentType(connection: HttpURLConnection, shouldUseGzip: Boolean) {
      if (shouldUseGzip) {
        connection.setRequestProperty(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded")
        connection.setRequestProperty(CONTENT_ENCODING_HEADER, "gzip")
      } else {
        connection.setRequestProperty(CONTENT_TYPE_HEADER, mimeContentType)
      }
    }

    private fun isGzipCompressible(requests: GraphRequestBatch): Boolean {
      for (request in requests) {
        for (key in request.parameters.keySet()) {
          val value = request.parameters[key]
          if (isSupportedAttachmentType(value)) {
            return false
          }
        }
      }
      return true
    }

    @JvmStatic
    internal fun validateFieldsParamForGetRequests(requests: GraphRequestBatch) {
      // validate that the GET requests all have a "fields" param
      for (request in requests) {
        if (HttpMethod.GET == request.httpMethod) {
          if (isNullOrEmpty(request.parameters.getString(FIELDS_PARAM))) {
            log(
                LoggingBehavior.DEVELOPER_ERRORS,
                Log.WARN,
                "Request",
                "GET requests for /${request.graphPath?:""} should contain an explicit \"fields\" parameter.")
          }
        }
      }
    }

    @Throws(IOException::class, JSONException::class)
    @JvmStatic
    internal fun serializeToUrlConnection(
        requests: GraphRequestBatch,
        connection: HttpURLConnection
    ) {
      val logger = Logger(LoggingBehavior.REQUESTS, "Request")
      val numRequests = requests.size
      val shouldUseGzip = isGzipCompressible(requests)
      val connectionHttpMethod =
          (if (numRequests == 1) requests[0].httpMethod else null) ?: HttpMethod.POST
      connection.requestMethod = connectionHttpMethod.name
      setConnectionContentType(connection, shouldUseGzip)
      val url = connection.url
      logger.append("Request:\n")
      logger.appendKeyValue("Id", requests.id)
      logger.appendKeyValue("URL", url)
      logger.appendKeyValue("Method", connection.requestMethod)
      logger.appendKeyValue("User-Agent", connection.getRequestProperty("User-Agent"))
      logger.appendKeyValue("Content-Type", connection.getRequestProperty("Content-Type"))
      connection.connectTimeout = requests.timeout
      connection.readTimeout = requests.timeout

      // If we have a single non-POST request, don't try to serialize anything or
      // HttpURLConnection will turn it into a POST.
      val isPost = connectionHttpMethod == HttpMethod.POST
      if (!isPost) {
        logger.log()
        return
      }
      connection.doOutput = true
      var outputStream: OutputStream? = null
      try {
        outputStream = BufferedOutputStream(connection.outputStream)
        if (shouldUseGzip) {
          outputStream = GZIPOutputStream(outputStream)
        }
        if (hasOnProgressCallbacks(requests)) {
          var countingStream: ProgressNoopOutputStream? = null
          countingStream = ProgressNoopOutputStream(requests.callbackHandler)
          processRequest(requests, null, numRequests, url, countingStream, shouldUseGzip)
          val max = countingStream.maxProgress
          val progressMap = countingStream.getProgressMap()
          outputStream = ProgressOutputStream(outputStream, requests, progressMap, max.toLong())
        }
        processRequest(requests, logger, numRequests, url, outputStream, shouldUseGzip)
      } finally {
        outputStream?.close()
      }
      logger.log()
    }

    private fun processRequest(
        requests: GraphRequestBatch,
        logger: Logger?,
        numRequests: Int,
        url: URL,
        outputStream: OutputStream,
        shouldUseGzip: Boolean
    ) {
      val serializer = Serializer(outputStream, logger, shouldUseGzip)
      if (numRequests == 1) {
        val request = requests[0]
        val attachments: MutableMap<String, Attachment> = HashMap()
        for (key in request.parameters.keySet()) {
          val value = request.parameters[key]
          if (isSupportedAttachmentType(value)) {
            attachments[key] = Attachment(request, value)
          }
        }
        logger?.append("  Parameters:\n")
        serializeParameters(request.parameters, serializer, request)
        logger?.append("  Attachments:\n")
        serializeAttachments(attachments, serializer)
        val graphObject = request.graphObject
        if (graphObject != null) {
          processGraphObject(graphObject, url.path, serializer)
        }
      } else {
        val batchAppID = getBatchAppId(requests)
        if (batchAppID.isEmpty()) {
          throw FacebookException("App ID was not specified at the request or Settings.")
        }
        serializer.writeString(BATCH_APP_ID_PARAM, batchAppID)

        // We write out all the requests as JSON, remembering which file attachments they have,
        // then write out the attachments.
        val attachments: MutableMap<String, Attachment> = HashMap()
        serializeRequestsAsJSON(serializer, requests, attachments)
        logger?.append("  Attachments:\n")
        serializeAttachments(attachments, serializer)
      }
    }

    private fun isMeRequest(path: String): Boolean {
      var path = path
      val matcher = versionPattern.matcher(path)
      if (matcher.matches()) {
        // Group 1 contains the path aside from version
        path = matcher.group(1)
      }
      return path.startsWith("me/") || path.startsWith("/me/")
    }

    private fun processGraphObject(
        graphObject: JSONObject,
        path: String,
        serializer: KeyValueSerializer
    ) {
      // In general, graph objects are passed by reference (ID/URL). But if this is an OG Action,
      // we need to pass the entire values of the contents of the 'image' property, as they
      // contain important metadata beyond just a URL. We don't have a 100% foolproof way of
      // knowing if we are posting an OG Action, given that batched requests can have parameter
      // substitution, but passing the OG Action type as a substituted parameter is unlikely.
      // It looks like an OG Action if it's posted to me/namespace:action[?other=stuff].
      var isOGAction = false
      if (isMeRequest(path)) {
        val colonLocation = path.indexOf(":")
        val questionMarkLocation = path.indexOf("?")
        isOGAction =
            colonLocation > 3 &&
                (questionMarkLocation == -1 || colonLocation < questionMarkLocation)
      }
      val keyIterator = graphObject.keys()
      while (keyIterator.hasNext()) {
        val key = keyIterator.next()
        val value = graphObject.opt(key)
        val passByValue = isOGAction && key.equals("image", ignoreCase = true)
        processGraphObjectProperty(key, value, serializer, passByValue)
      }
    }

    private fun processGraphObjectProperty(
        key: String,
        value: Any,
        serializer: KeyValueSerializer,
        passByValue: Boolean
    ) {
      val valueClass: Class<*> = value.javaClass
      if (JSONObject::class.java.isAssignableFrom(valueClass)) {
        val jsonObject = value as JSONObject
        if (passByValue) {
          // We need to pass all properties of this object in key[propertyName] format.
          val keys = jsonObject.keys()
          while (keys.hasNext()) {
            val propertyName = keys.next()
            val subKey = String.format("%s[%s]", key, propertyName)
            processGraphObjectProperty(
                subKey, jsonObject.opt(propertyName), serializer, passByValue)
          }
        } else {
          // Normal case is passing objects by reference, so just pass the ID or URL, if any,
          // as the value for "key"
          if (jsonObject.has("id")) {
            processGraphObjectProperty(key, jsonObject.optString("id"), serializer, passByValue)
          } else if (jsonObject.has("url")) {
            processGraphObjectProperty(key, jsonObject.optString("url"), serializer, passByValue)
          } else if (jsonObject.has(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY)) {
            processGraphObjectProperty(key, jsonObject.toString(), serializer, passByValue)
          }
        }
      } else if (JSONArray::class.java.isAssignableFrom(valueClass)) {
        val jsonArray = value as JSONArray
        val length = jsonArray.length()
        for (i in 0 until length) {
          val subKey = String.format(Locale.ROOT, "%s[%d]", key, i)
          processGraphObjectProperty(subKey, jsonArray.opt(i), serializer, passByValue)
        }
      } else if (String::class.java.isAssignableFrom(valueClass) ||
          Number::class.java.isAssignableFrom(valueClass) ||
          Boolean::class.java.isAssignableFrom(valueClass)) {
        serializer.writeString(key, value.toString())
      } else if (Date::class.java.isAssignableFrom(valueClass)) {
        val date = value as Date
        // The "Events Timezone" platform migration affects what date/time formats Facebook
        // accepts and returns. Apps created after 8/1/12 (or apps that have explicitly enabled
        // the migration) should send/receive dates in ISO-8601 format. Pre-migration apps can
        // send as Unix timestamps. Since the future is ISO-8601, that is what we support here.
        // Apps that need pre-migration behavior can explicitly send these as integer timestamps
        // rather than Dates.
        val iso8601DateFormat = SimpleDateFormat(ISO_8601_FORMAT_STRING, Locale.US)
        serializer.writeString(key, iso8601DateFormat.format(date))
      }
    }

    private fun serializeParameters(bundle: Bundle, serializer: Serializer, request: GraphRequest) {
      val keys = bundle.keySet()
      for (key in keys) {
        val value = bundle[key]
        if (isSupportedParameterType(value)) {
          serializer.writeObject(key, value, request)
        }
      }
    }

    private fun serializeAttachments(attachments: Map<String, Attachment>, serializer: Serializer) {
      attachments.forEach {
        if (isSupportedAttachmentType(it.value.value)) {
          serializer.writeObject(it.key, it.value.value, it.value.request)
        }
      }
    }

    private fun serializeRequestsAsJSON(
        serializer: Serializer,
        requests: Collection<GraphRequest>,
        attachments: MutableMap<String, Attachment>
    ) {
      val batch = JSONArray()
      for (request in requests) {
        request.serializeToBatch(batch, attachments)
      }
      serializer.writeRequestsAsJson(BATCH_PARAM, batch, requests)
    }

    private val mimeContentType: String
      get() = String.format("multipart/form-data; boundary=%s", MIME_BOUNDARY)

    // For the unity sdk we need to append the unity user agent
    @Volatile
    private var userAgent: String? = null
      get() {
        if (field == null) {
          field = String.format("%s.%s", USER_AGENT_BASE, FacebookSdkVersion.BUILD)

          // For the unity sdk we need to append the unity user agent
          val customUserAgent = getCustomUserAgent()
          if (!isNullOrEmpty(customUserAgent)) {
            field = String.format(Locale.ROOT, "%s/%s", field, customUserAgent)
          }
        }
        return field
      }

    private fun getBatchAppId(batch: GraphRequestBatch): String {
      val batchApplicationId = batch.batchApplicationId
      if (batchApplicationId != null && batch.isNotEmpty()) {
        return batchApplicationId
      }
      for (request in batch) {
        val accessToken = request.accessToken
        if (accessToken != null) {
          return accessToken.applicationId
        }
      }
      val defaultBatchApplicationId = this.defaultBatchApplicationId
      return if (defaultBatchApplicationId != null && defaultBatchApplicationId.isNotEmpty()) {
        defaultBatchApplicationId
      } else FacebookSdk.getApplicationId()
    }

    private fun isSupportedAttachmentType(value: Any?): Boolean {
      return value is Bitmap ||
          value is ByteArray ||
          value is Uri ||
          value is ParcelFileDescriptor ||
          value is ParcelableResourceWithMimeType<*>
    }

    private fun isSupportedParameterType(value: Any?): Boolean {
      return value is String || value is Boolean || value is Number || value is Date
    }

    private fun parameterToString(value: Any?): String {
      if (value is String) {
        return value
      } else if (value is Boolean || value is Number) {
        return value.toString()
      } else if (value is Date) {
        val iso8601DateFormat = SimpleDateFormat(ISO_8601_FORMAT_STRING, Locale.US)
        return iso8601DateFormat.format(value)
      }
      throw IllegalArgumentException("Unsupported parameter type.")
    }
  }
  /**
   * Constructs a request with a specific access token, graph path, parameters, and HTTP method. An
   * access token need not be provided, in which case the request is sent without an access token
   * and thus is not executed in the context of any particular user. Only certain graph requests can
   * be expected to succeed in this case.
   *
   * Depending on the httpMethod parameter, the object at the graph path may be retrieved, created,
   * or deleted.
   *
   * @param accessToken the access token to use, or null
   * @param graphPath the graph path to retrieve, create, or delete
   * @param parameters additional parameters to pass along with the Graph API request; parameters
   * must be Strings, Numbers, Bitmaps, Dates, or Byte arrays.
   * @param httpMethod the [HttpMethod] to use for the request, or null for default (HttpMethod.GET)
   * @param callback a callback that will be called when the request is completed to handle success
   * or error conditions
   * @param version the version of the Graph API
   */
  @JvmOverloads
  constructor(
      accessToken: AccessToken? = null,
      graphPath: String? = null,
      parameters: Bundle? = null,
      httpMethod: HttpMethod? = null,
      callback: Callback? = null,
      version: String? = null
  ) {
    this.accessToken = accessToken
    this.graphPath = graphPath
    this.version = version
    this.callback = callback
    this.httpMethod = httpMethod
    if (parameters != null) {
      this.parameters = Bundle(parameters)
    } else {
      this.parameters = Bundle()
    }
    if (this.version == null) {
      this.version = FacebookSdk.getGraphApiVersion()
    }
  }

  internal constructor(accessToken: AccessToken?, overriddenURL: URL) {
    this.accessToken = accessToken
    this.overriddenURL = overriddenURL.toString()
    this.httpMethod = HttpMethod.GET
    parameters = Bundle()
  }

  /** This is an internal function that is not meant to be used by developers. */
  fun setForceApplicationRequest(forceOverride: Boolean) {
    this.forceApplicationRequest = forceOverride
  }

  /**
   * Executes this request on the current thread and blocks while waiting for the response.
   *
   * This should only be called if you have transitioned off the UI thread.
   *
   * @return the Response object representing the results of the request
   * @throws FacebookException If there was an error in the protocol used to communicate with the
   * service
   * @throws IllegalArgumentException
   */
  fun executeAndWait(): GraphResponse {
    return executeAndWait(this)
  }

  /**
   * Executes the request asynchronously. This function will return immediately, and the request
   * will be processed on a separate thread. In order to process result of a request, or determine
   * whether a request succeeded or failed, a callback must be specified (see the [ ][.setCallback]
   * method).
   *
   * This should only be called from the UI thread.
   *
   * @return a RequestAsyncTask that is executing the request
   * @throws IllegalArgumentException
   */
  fun executeAsync(): GraphRequestAsyncTask {
    return executeBatchAsync(this)
  }

  /**
   * Returns a string representation of this Request, useful for debugging.
   *
   * @return the debugging information
   */
  override fun toString(): String {
    return StringBuilder()
        .append("{Request: ")
        .append(" accessToken: ")
        .append(if (accessToken == null) "null" else accessToken)
        .append(", graphPath: ")
        .append(graphPath)
        .append(", graphObject: ")
        .append(graphObject)
        .append(", httpMethod: ")
        .append(httpMethod)
        .append(", parameters: ")
        .append(parameters)
        .append("}")
        .toString()
  }

  private fun addCommonParameters() {
    val parameters = this.parameters
    if (shouldForceClientTokenForRequest()) {
      parameters.putString(ACCESS_TOKEN_PARAM, getClientTokenForRequest())
    } else {
      val accessTokenForRequest = getAccessTokenToUseForRequest()
      if (accessTokenForRequest != null) {
        parameters.putString(ACCESS_TOKEN_PARAM, accessTokenForRequest)
      }
    }
    // check access token again to confirm that client token is available
    if (!parameters.containsKey(ACCESS_TOKEN_PARAM) &&
        isNullOrEmpty(FacebookSdk.getClientToken())) {
      Log.w(
          TAG,
          "Starting with v13 of the SDK, a client token must be embedded in your client code before making Graph API calls. Visit https://developers.facebook.com/docs/android/getting-started#client-token to learn how to implement this change.")
    }
    parameters.putString(SDK_PARAM, SDK_ANDROID)
    parameters.putString(FORMAT_PARAM, FORMAT_JSON)
    if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_INFO)) {
      parameters.putString(DEBUG_PARAM, DEBUG_SEVERITY_INFO)
    } else if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
      parameters.putString(DEBUG_PARAM, DEBUG_SEVERITY_WARNING)
    }
  }

  private fun getAccessTokenToUseForRequest(): String? {
    val accessToken = this.accessToken
    if (accessToken != null) {
      if (!this.parameters.containsKey(ACCESS_TOKEN_PARAM)) {
        val token = accessToken.token
        registerAccessToken(token)
        return token
      }
    } else if (!this.parameters.containsKey(ACCESS_TOKEN_PARAM)) {
      return getClientTokenForRequest()
    }
    return this.parameters.getString(ACCESS_TOKEN_PARAM)
  }

  private fun getClientTokenForRequest(): String? {
    var accessToken: String? = null
    val appID = FacebookSdk.getApplicationId()
    val clientToken = FacebookSdk.getClientToken()
    if (appID.isNotEmpty() && clientToken.isNotEmpty()) {
      accessToken = "$appID|$clientToken"
    } else {
      logd(TAG, "Warning: Request without access token missing application ID or client token.")
    }
    return accessToken
  }

  private fun appendParametersToBaseUrl(baseUrl: String, isBatch: Boolean): String {
    if (!isBatch && httpMethod == HttpMethod.POST) {
      return baseUrl
    }
    val uriBuilder = Uri.parse(baseUrl).buildUpon()
    val keys = parameters.keySet()
    for (key in keys) {
      var value = parameters[key]
      if (value == null) {
        value = ""
      }
      value =
          if (isSupportedParameterType(value)) {
            parameterToString(value)
          } else {
            if (httpMethod != HttpMethod.GET) {
              throw IllegalArgumentException(
                  String.format(
                      Locale.US,
                      "Unsupported parameter type for GET request: %s",
                      value.javaClass.simpleName))
            }
            continue
          }
      uriBuilder.appendQueryParameter(key, value.toString())
    }
    return uriBuilder.toString()
  }

  val relativeUrlForBatchedRequest: String
    get() {
      if (overriddenURL != null) {
        throw FacebookException("Can't override URL for a batch request")
      }
      val baseUrl = getUrlWithGraphPath(getGraphUrlBase())
      addCommonParameters()
      val fullUrl = appendParametersToBaseUrl(baseUrl, true)
      val uri = Uri.parse(fullUrl)
      return String.format("%s?%s", uri.path, uri.query)
    }

  val urlForSingleRequest: String
    get() {
      if (overriddenURL != null) {
        return overriddenURL.toString()
      }
      val graphPath = this.graphPath
      val graphBaseUrlBase =
          if (httpMethod == HttpMethod.POST &&
              graphPath != null &&
              graphPath.endsWith(VIDEOS_SUFFIX)) {
            getGraphVideoUrlBase()
          } else {
            getGraphUrlBaseForSubdomain(FacebookSdk.getGraphDomain())
          }
      val baseUrl = getUrlWithGraphPath(graphBaseUrlBase)
      addCommonParameters()
      return appendParametersToBaseUrl(baseUrl, false)
    }

  private val graphPathWithVersion: String?
    private get() {
      val matcher = versionPattern.matcher(graphPath)
      return if (matcher.matches()) {
        graphPath
      } else String.format("%s/%s", version, graphPath)
    }

  private fun getUrlWithGraphPath(baseUrl: String): String {
    var baseUrl = baseUrl
    if (!isValidGraphRequestForDomain()) {
      // If not valid for current domain, re-route back to facebook domain
      baseUrl = getFacebookGraphUrlBase()
    }
    return String.format("%s/%s", baseUrl, graphPathWithVersion)
  }

  private fun shouldForceClientTokenForRequest(): Boolean {
    val tokenToUse = getAccessTokenToUseForRequest()
    val isAlreadyUsingClientToken = tokenToUse?.contains("|") ?: false
    // Requests to the application endpoint with a "cached" IG user token should be overwritten
    // with a client token
    val isInstagramUserToken =
        tokenToUse != null && tokenToUse.startsWith("IG") && !isAlreadyUsingClientToken
    if (isInstagramUserToken && isApplicationRequest()) {
      return true
    }
    // Re-routed requests should also use a client token by default
    if (!isValidGraphRequestForDomain() && !isAlreadyUsingClientToken) {
      return true
    }
    return false
  }

  private fun isValidGraphRequestForDomain(): Boolean {
    val graphDomain = FacebookSdk.getGraphDomain()
    if (graphDomain != FacebookSdk.INSTAGRAM_COM) {
      // All graph paths are valid for facebook and gaming domains
      return true
    }
    // Application requests should always go to the Facebook domain
    return !isApplicationRequest()
  }

  private fun isApplicationRequest(): Boolean {
    if (this.graphPath == null) {
      return false
    }
    val applicationIdEndpointRegex = "^/?" + FacebookSdk.getApplicationId() + "/?.*"
    val appNodeEndpointRegex = "^/?app/?.*"
    return this.forceApplicationRequest ||
        Pattern.matches(applicationIdEndpointRegex, this.graphPath) ||
        Pattern.matches(appNodeEndpointRegex, this.graphPath)
  }

  private class Attachment(val request: GraphRequest, val value: Any?)

  @Throws(JSONException::class, IOException::class)
  private fun serializeToBatch(batch: JSONArray, attachments: MutableMap<String, Attachment>) {
    val batchEntry = JSONObject()
    if (batchEntryName != null) {
      batchEntry.put(BATCH_ENTRY_NAME_PARAM, batchEntryName)
      batchEntry.put(BATCH_ENTRY_OMIT_RESPONSE_ON_SUCCESS_PARAM, batchEntryOmitResultOnSuccess)
    }
    if (batchEntryDependsOn != null) {
      batchEntry.put(BATCH_ENTRY_DEPENDS_ON_PARAM, batchEntryDependsOn)
    }
    val relativeURL = relativeUrlForBatchedRequest
    batchEntry.put(BATCH_RELATIVE_URL_PARAM, relativeURL)
    batchEntry.put(BATCH_METHOD_PARAM, httpMethod)
    val accessToken = this.accessToken
    if (accessToken != null) {
      val token = accessToken.token
      registerAccessToken(token)
    }

    // Find all of our attachments. Remember their names and put them in the attachment map.
    val attachmentNames = ArrayList<String?>()
    val keys = parameters.keySet()
    for (key in keys) {
      val value = parameters[key]
      if (isSupportedAttachmentType(value)) {
        // Make the name unique across this entire batch.
        val name = String.format(Locale.ROOT, "%s%d", ATTACHMENT_FILENAME_PREFIX, attachments.size)
        attachmentNames.add(name)
        attachments[name] = Attachment(this, value)
      }
    }
    if (!attachmentNames.isEmpty()) {
      val attachmentNamesString = TextUtils.join(",", attachmentNames)
      batchEntry.put(ATTACHED_FILES_PARAM, attachmentNamesString)
    }
    val graphObject = this.graphObject
    if (graphObject != null) {
      // Serialize the graph object into the "body" parameter.
      val keysAndValues = ArrayList<String?>()
      processGraphObject(
          graphObject,
          relativeURL,
          object : KeyValueSerializer {
            @Throws(IOException::class)
            override fun writeString(key: String, value: String) {
              keysAndValues.add(
                  String.format(Locale.US, "%s=%s", key, URLEncoder.encode(value, "UTF-8")))
            }
          })
      val bodyValue = TextUtils.join("&", keysAndValues)
      batchEntry.put(BATCH_BODY_PARAM, bodyValue)
    }
    batch.put(batchEntry)
  }

  private fun interface KeyValueSerializer {
    fun writeString(key: String, value: String)
  }

  private class Serializer(
      private val outputStream: OutputStream,
      private val logger: Logger?,
      useUrlEncode: Boolean
  ) : KeyValueSerializer {
    private var firstWrite = true
    private val useUrlEncode = useUrlEncode
    fun writeObject(key: String, value: Any?, request: GraphRequest?) {
      if (outputStream is RequestOutputStream) {
        (outputStream as RequestOutputStream).setCurrentRequest(request)
      }
      if (isSupportedParameterType(value)) {
        writeString(key, parameterToString(value))
      } else if (value is Bitmap) {
        writeBitmap(key, value)
      } else if (value is ByteArray) {
        writeBytes(key, value)
      } else if (value is Uri) {
        writeContentUri(key, value, null)
      } else if (value is ParcelFileDescriptor) {
        writeFile(key, value, null)
      } else if (value is ParcelableResourceWithMimeType<*>) {
        val resource = value.resource
        val mimeType = value.mimeType
        if (resource is ParcelFileDescriptor) {
          writeFile(key, resource, mimeType)
        } else if (resource is Uri) {
          writeContentUri(key, resource, mimeType)
        } else {
          throw invalidTypeError
        }
      } else {
        throw invalidTypeError
      }
    }

    private val invalidTypeError: RuntimeException
      get() = IllegalArgumentException("value is not a supported type.")

    fun writeRequestsAsJson(
        key: String,
        requestJsonArray: JSONArray,
        requests: Collection<GraphRequest>
    ) {
      if (outputStream !is RequestOutputStream) {
        writeString(key, requestJsonArray.toString())
        return
      }
      val requestOutputStream = outputStream as RequestOutputStream
      writeContentDisposition(key, null, null)
      write("[")
      for ((i, request) in requests.withIndex()) {
        val requestJson = requestJsonArray.getJSONObject(i)
        requestOutputStream.setCurrentRequest(request)
        if (i > 0) {
          write(",%s", requestJson.toString())
        } else {
          write("%s", requestJson.toString())
        }
      }
      write("]")
      logger?.appendKeyValue("    $key", requestJsonArray.toString())
    }

    override fun writeString(key: String, value: String) {
      writeContentDisposition(key, null, null)
      writeLine("%s", value)
      writeRecordBoundary()
      logger?.appendKeyValue("    $key", value)
    }

    fun writeBitmap(key: String, bitmap: Bitmap) {
      writeContentDisposition(key, key, "image/png")
      // Note: quality parameter is ignored for PNG
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
      writeLine("")
      writeRecordBoundary()
      logger?.appendKeyValue("    $key", "<Image>")
    }

    fun writeBytes(key: String, bytes: ByteArray) {
      writeContentDisposition(key, key, "content/unknown")
      outputStream.write(bytes)
      writeLine("")
      writeRecordBoundary()
      logger?.appendKeyValue("    $key", String.format(Locale.ROOT, "<Data: %d>", bytes.size))
    }

    fun writeContentUri(key: String, contentUri: Uri, mimeType: String?) {
      var mimeType = mimeType
      if (mimeType == null) {
        mimeType = "content/unknown"
      }
      writeContentDisposition(key, key, mimeType)
      var totalBytes = 0
      if (outputStream is ProgressNoopOutputStream) {
        // If we are only counting bytes then skip reading the file
        val contentSize = getContentSize(contentUri)
        outputStream.addProgress(contentSize)
      } else {
        val inputStream =
            FacebookSdk.getApplicationContext().contentResolver.openInputStream(contentUri)
        totalBytes += copyAndCloseInputStream(inputStream, outputStream)
      }
      writeLine("")
      writeRecordBoundary()
      logger?.appendKeyValue("    $key", String.format(Locale.ROOT, "<Data: %d>", totalBytes))
    }

    fun writeFile(key: String, descriptor: ParcelFileDescriptor, mimeType: String?) {
      var mimeType = mimeType
      if (mimeType == null) {
        mimeType = "content/unknown"
      }
      writeContentDisposition(key, key, mimeType)
      var totalBytes = 0
      if (outputStream is ProgressNoopOutputStream) {
        // If we are only counting bytes then skip reading the file
        outputStream.addProgress(descriptor.statSize)
      } else {
        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(descriptor)
        totalBytes += copyAndCloseInputStream(inputStream, outputStream)
      }
      writeLine("")
      writeRecordBoundary()
      logger?.appendKeyValue("    $key", String.format(Locale.ROOT, "<Data: %d>", totalBytes))
    }

    fun writeRecordBoundary() {
      if (!useUrlEncode) {
        writeLine("--%s", MIME_BOUNDARY)
      } else {
        outputStream.write("&".toByteArray())
      }
    }

    fun writeContentDisposition(name: String?, filename: String?, contentType: String?) {
      if (!useUrlEncode) {
        write("Content-Disposition: form-data; name=\"%s\"", name)
        if (filename != null) {
          write("; filename=\"%s\"", filename)
        }
        writeLine("") // newline after Content-Disposition
        if (contentType != null) {
          writeLine("%s: %s", CONTENT_TYPE_HEADER, contentType)
        }
        writeLine("") // blank line before content
      } else {
        outputStream.write(String.format("%s=", name).toByteArray())
      }
    }

    fun write(format: String, vararg args: Any?) {
      if (!useUrlEncode) {
        if (firstWrite) {
          // Prepend all of our output with a boundary string.
          outputStream.write("--".toByteArray())
          outputStream.write(MIME_BOUNDARY.toByteArray())
          outputStream.write("\r\n".toByteArray())
          firstWrite = false
        }
        outputStream.write(String.format(format, *args).toByteArray())
      } else {
        outputStream.write(
            URLEncoder.encode(String.format(Locale.US, format, *args), "UTF-8").toByteArray())
      }
    }

    fun writeLine(format: String, vararg args: Any?) {
      write(format, *args)
      if (!useUrlEncode) {
        write("\r\n")
      }
    }
  }

  /**
   * Specifies the interface that consumers of the Request class can implement in order to be
   * notified when a particular request completes, either successfully or with an error.
   */
  fun interface Callback {
    /**
     * The method that will be called when a request completes.
     *
     * @param response the Response of this request, which may include error information if the
     * request was unsuccessful
     */
    fun onCompleted(response: GraphResponse)
  }

  /**
   * Specifies the interface that consumers of the Request class can implement in order to be
   * notified when a progress is made on a particular request. The frequency of the callbacks can be
   * controlled using [FacebookSdk.setOnProgressThreshold]
   */
  interface OnProgressCallback : Callback {
    /**
     * The method that will be called when progress is made.
     *
     * @param current the current value of the progress of the request.
     * @param max the maximum value (target) value that the progress will have.
     */
    fun onProgress(current: Long, max: Long)
  }

  /** Callback for requests that result in an array of JSONObjects. */
  fun interface GraphJSONArrayCallback {
    /**
     * The method that will be called when the request completes.
     *
     * @param objects the list of GraphObjects representing the returned objects, or null
     * @param response the Response of this request, which may include error information if the
     * request was unsuccessful
     */
    fun onCompleted(objects: JSONArray?, response: GraphResponse?)
  }

  /** Callback for requests that result in a JSONObject. */
  fun interface GraphJSONObjectCallback {
    /**
     * The method that will be called when the request completes.
     *
     * @param obj the GraphObject representing the returned object, or null
     * @param response the Response of this request, which may include error information if the
     * request was unsuccessful
     */
    fun onCompleted(obj: JSONObject?, response: GraphResponse?)
  }

  class ParcelableResourceWithMimeType<RESOURCE : Parcelable?> : Parcelable {
    val mimeType: String?
    val resource: RESOURCE?

    override fun describeContents(): Int {
      return Parcelable.CONTENTS_FILE_DESCRIPTOR
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
      out.writeString(mimeType)
      out.writeParcelable(resource, flags)
    }

    /**
     * The constructor.
     *
     * @param resource The resource to parcel.
     * @param mimeType The mime type.
     */
    constructor(resource: RESOURCE, mimeType: String?) {
      this.mimeType = mimeType
      this.resource = resource
    }

    private constructor(source: Parcel) {
      mimeType = source.readString()
      resource = source.readParcelable(FacebookSdk.getApplicationContext().classLoader)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<ParcelableResourceWithMimeType<*>?> =
          object : Parcelable.Creator<ParcelableResourceWithMimeType<*>?> {
            override fun createFromParcel(source: Parcel): ParcelableResourceWithMimeType<*> {
              return ParcelableResourceWithMimeType<Parcelable>(source)
            }

            override fun newArray(size: Int): Array<ParcelableResourceWithMimeType<*>?> {
              return arrayOfNulls(size)
            }
          }
    }
  }
}
