/**
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

package com.facebook;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.facebook.internal.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

/**
 * <p>
 * A single request to be sent to the Facebook Platform through the <a
 * href="https://developers.facebook.com/docs/reference/api/">Graph API</a>. The Request class
 * provides functionality relating to serializing and deserializing requests and responses, making
 * calls in batches (with a single round-trip to the service) and making calls asynchronously.
 * </p>
 * <p>
 * The particular service endpoint that a request targets is determined by a graph path (see the
 * {@link #setGraphPath(String) setGraphPath} method).
 * </p>
 * <p>
 * A Request can be executed either anonymously or representing an authenticated user. In the former
 * case, no AccessToken needs to be specified, while in the latter, an AccessToken must be provided.
 * If requests are executed in a batch, a Facebook application ID must be associated with the batch,
 * either by setting the application ID in the AndroidManifest.xml or via FacebookSdk or by calling
 * the {@link #setDefaultBatchApplicationId(String) setDefaultBatchApplicationId} method.
 * </p>
 * <p>
 * After completion of a request, the AccessToken, if not null and taken from AccessTokenManager,
 * will be checked to determine if its Facebook access token needs to be extended; if so, a request
 * to extend it will be issued in the background.
 * </p>
 */
public class GraphRequest {
    /**
     * The maximum number of requests that can be submitted in a single batch. This limit is
     * enforced on the service side by the Facebook platform, not by the Request class.
     */
    public static final int MAXIMUM_BATCH_SIZE = 50;

    public static final String TAG = GraphRequest.class.getSimpleName();

    private static final String VIDEOS_SUFFIX = "/videos";
    private static final String ME = "me";
    private static final String MY_FRIENDS = "me/friends";
    private static final String MY_PHOTOS = "me/photos";
    private static final String SEARCH = "search";
    private static final String USER_AGENT_BASE = "FBAndroidSDK";
    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final String CONTENT_ENCODING_HEADER = "Content-Encoding";

    // Parameter names/values
    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_JSON = "json";
    private static final String SDK_PARAM = "sdk";
    private static final String SDK_ANDROID = "android";
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    private static final String BATCH_ENTRY_NAME_PARAM = "name";
    private static final String BATCH_ENTRY_OMIT_RESPONSE_ON_SUCCESS_PARAM =
            "omit_response_on_success";
    private static final String BATCH_ENTRY_DEPENDS_ON_PARAM = "depends_on";
    private static final String BATCH_APP_ID_PARAM = "batch_app_id";
    private static final String BATCH_RELATIVE_URL_PARAM = "relative_url";
    private static final String BATCH_BODY_PARAM = "body";
    private static final String BATCH_METHOD_PARAM = "method";
    private static final String BATCH_PARAM = "batch";
    private static final String ATTACHMENT_FILENAME_PREFIX = "file";
    private static final String ATTACHED_FILES_PARAM = "attached_files";
    private static final String ISO_8601_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String DEBUG_PARAM = "debug";
    private static final String DEBUG_SEVERITY_INFO = "info";
    private static final String DEBUG_SEVERITY_WARNING = "warning";
    private static final String DEBUG_KEY = "__debug__";
    private static final String DEBUG_MESSAGES_KEY = "messages";
    private static final String DEBUG_MESSAGE_KEY = "message";
    private static final String DEBUG_MESSAGE_TYPE_KEY = "type";
    private static final String DEBUG_MESSAGE_LINK_KEY = "link";
    private static final String PICTURE_PARAM = "picture";
    private static final String CAPTION_PARAM = "caption";

    public static final String FIELDS_PARAM = "fields";

    private static final String MIME_BOUNDARY = "3i2ndDfv2rTHiSisAbouNdArYfORhtTPEefj3q2f";

    private static String defaultBatchApplicationId;

    // Group 1 in the pattern is the path without the version info
    private static Pattern versionPattern = Pattern.compile("^/?v\\d+\\.\\d+/(.*)");

    private AccessToken accessToken;
    private HttpMethod httpMethod;
    private String graphPath;
    private JSONObject graphObject;
    private String batchEntryName;
    private String batchEntryDependsOn;
    private boolean batchEntryOmitResultOnSuccess = true;
    private Bundle parameters;
    private Callback callback;
    private String overriddenURL;
    private Object tag;
    private String version;
    private boolean skipClientToken = false;

    /**
     * Constructs a request without an access token, graph path, or any other parameters.
     */
    public GraphRequest() {
        this(null, null, null, null, null);
    }

    /**
     * Constructs a request with an access token to retrieve a particular graph path.
     * An access token need not be provided, in which case the request is sent without an access
     * token and thus is not executed in the context of any particular user. Only certain graph
     * requests can be expected to succeed in this case.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve
     */
    public GraphRequest(AccessToken accessToken, String graphPath) {
        this(accessToken, graphPath, null, null, null);
    }

    /**
     * Constructs a request with a specific AccessToken, graph path, parameters, and HTTP method. An
     * access token need not be provided, in which case the request is sent without an access token
     * and thus is not executed in the context of any particular user. Only certain graph requests
     * can be expected to succeed in this case.
     * <p/>
     * Depending on the httpMethod parameter, the object at the graph path may be retrieved,
     * created, or deleted.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve, create, or delete
     * @param parameters  additional parameters to pass along with the Graph API request; parameters
     *                    must be Strings, Numbers, Bitmaps, Dates, or Byte arrays.
     * @param httpMethod  the {@link HttpMethod} to use for the request, or null for default
     *                    (HttpMethod.GET)
     */
    public GraphRequest(
            AccessToken accessToken,
            String graphPath,
            Bundle parameters,
            HttpMethod httpMethod) {
        this(accessToken, graphPath, parameters, httpMethod, null);
    }

    /**
     * Constructs a request with a specific access token, graph path, parameters, and HTTP method.
     * An access token need not be provided, in which case the request is sent without an access
     * token and thus is not executed in the context of any particular user. Only certain graph
     * requests can be expected to succeed in this case.
     * <p/>
     * Depending on the httpMethod parameter, the object at the graph path may be retrieved,
     * created, or deleted.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve, create, or delete
     * @param parameters  additional parameters to pass along with the Graph API request; parameters
     *                    must be Strings, Numbers, Bitmaps, Dates, or Byte arrays.
     * @param httpMethod  the {@link HttpMethod} to use for the request, or null for default
     *                    (HttpMethod.GET)
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     */
    public GraphRequest(
            AccessToken accessToken,
            String graphPath,
            Bundle parameters,
            HttpMethod httpMethod,
            Callback callback) {
        this(accessToken, graphPath, parameters, httpMethod, callback, null);
    }

    /**
     * Constructs a request with a specific access token, graph path, parameters, and HTTP method.
     * An access token need not be provided, in which case the request is sent without an access
     * token and thus is not executed in the context of any particular user. Only certain graph
     * requests can be expected to succeed in this case.
     * <p/>
     * Depending on the httpMethod parameter, the object at the graph path may be retrieved,
     * created, or deleted.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve, create, or delete
     * @param parameters  additional parameters to pass along with the Graph API request; parameters
     *                    must be Strings, Numbers, Bitmaps, Dates, or Byte arrays.
     * @param httpMethod  the {@link HttpMethod} to use for the request, or null for default
     *                    (HttpMethod.GET)
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @param version     the version of the Graph API
     */
    public GraphRequest(
            AccessToken accessToken,
            String graphPath,
            Bundle parameters,
            HttpMethod httpMethod,
            Callback callback,
            String version) {
        this.accessToken = accessToken;
        this.graphPath = graphPath;
        this.version = version;

        setCallback(callback);
        setHttpMethod(httpMethod);

        if (parameters != null) {
            this.parameters = new Bundle(parameters);
        } else {
            this.parameters = new Bundle();
        }

        if (this.version == null) {
            this.version = ServerProtocol.getAPIVersion();
        }
    }

    GraphRequest(AccessToken accessToken, URL overriddenURL) {
        this.accessToken = accessToken;
        this.overriddenURL = overriddenURL.toString();

        setHttpMethod(HttpMethod.GET);

        this.parameters = new Bundle();
    }

    /**
     * Creates a new Request configured to delete a resource through the Graph API.
     *
     * @param accessToken the access token to use, or null
     * @param id          the id of the object to delete
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newDeleteObjectRequest(
            AccessToken accessToken,
            String id,
            Callback callback) {
        return new GraphRequest(accessToken, id, null, HttpMethod.DELETE, callback);
    }

    /**
     * Creates a new Request configured to retrieve a user's own profile.
     *
     * @param accessToken the access token to use, or null
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newMeRequest(
            AccessToken accessToken,
            final GraphJSONObjectCallback callback) {
        Callback wrapper = new Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (callback != null) {
                    callback.onCompleted(response.getJSONObject(), response);
                }
            }
        };
        return new GraphRequest(accessToken, ME, null, null, wrapper);
    }

    /**
     * Creates a new Request configured to post a GraphObject to a particular graph path, to either
     * create or update the object at that path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve, create, or delete
     * @param graphObject the graph object to create or update
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newPostRequest(
            AccessToken accessToken,
            String graphPath,
            JSONObject graphObject,
            Callback callback) {
        GraphRequest request = new GraphRequest(
                accessToken,
                graphPath,
                null,
                HttpMethod.POST,
                callback);
        request.setGraphObject(graphObject);
        return request;
    }

    /**
     * Creates a new Request configured to retrieve a user's friend list.
     *
     * @param accessToken the access token to use, or null
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newMyFriendsRequest(
            AccessToken accessToken,
            final GraphJSONArrayCallback callback) {
        Callback wrapper = new Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (callback != null) {
                    JSONObject result = response.getJSONObject();
                    JSONArray data = result != null ? result.optJSONArray("data") : null;
                    callback.onCompleted(data, response);
                }
            }
        };
        return new GraphRequest(accessToken, MY_FRIENDS, null, null, wrapper);
    }

    /**
     * Creates a new Request configured to retrieve a particular graph path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to retrieve
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions
     * @return a Request that is ready to execute
     */
    public static GraphRequest newGraphPathRequest(
            AccessToken accessToken,
            String graphPath,
            Callback callback) {
        return new GraphRequest(accessToken, graphPath, null, null, callback);
    }

    /**
     * Creates a new Request that is configured to perform a search for places near a specified
     * location via the Graph API. At least one of location or searchText must be specified.
     *
     * @param accessToken    the access token to use, or null
     * @param location       the location around which to search; only the latitude and longitude
     *                       components of the location are meaningful
     * @param radiusInMeters the radius around the location to search, specified in meters; this is
     *                       ignored if no location is specified
     * @param resultsLimit   the maximum number of results to return
     * @param searchText     optional text to search for as part of the name or type of an object
     * @param callback       a callback that will be called when the request is completed to handle
     *                       success or error conditions
     * @return a Request that is ready to execute
     * @throws FacebookException If neither location nor searchText is specified
     */
    public static GraphRequest newPlacesSearchRequest(
            AccessToken accessToken,
            Location location,
            int radiusInMeters,
            int resultsLimit,
            String searchText,
            final GraphJSONArrayCallback callback) {
        if (location == null && Utility.isNullOrEmpty(searchText)) {
            throw new FacebookException("Either location or searchText must be specified.");
        }

        Bundle parameters = new Bundle(5);
        parameters.putString("type", "place");
        parameters.putInt("limit", resultsLimit);
        if (location != null) {
            parameters.putString("center",
                    String.format(
                            Locale.US,
                            "%f,%f",
                            location.getLatitude(),
                            location.getLongitude()));
            parameters.putInt("distance", radiusInMeters);
        }
        if (!Utility.isNullOrEmpty(searchText)) {
            parameters.putString("q", searchText);
        }

        Callback wrapper = new Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                if (callback != null) {
                    JSONObject result = response.getJSONObject();
                    JSONArray data = result != null ? result.optJSONArray("data") : null;
                    callback.onCompleted(data, response);
                }
            }
        };

        return new GraphRequest(accessToken, SEARCH, parameters, HttpMethod.GET, wrapper);
    }


    /**
     * Creates a new Request configured to upload a photo to the specified graph path.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to use, defaults to me/photos
     * @param image       the bitmap image to upload
     * @param caption     the user generated caption for the photo, can be null
     * @param params      the parameters, can be null
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions, can be null
     * @return a Request that is ready to execute
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            String graphPath,
            Bitmap image,
            String caption,
            Bundle params,
            Callback callback) {
        graphPath = getDefaultPhotoPathIfNull(graphPath);
        Bundle parameters = new Bundle();
        if (params != null) {
            parameters.putAll(params);
        }
        parameters.putParcelable(PICTURE_PARAM, image);
        if (caption != null && !caption.isEmpty()) {
            parameters.putString(CAPTION_PARAM, caption);
        }

        return new GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the specified graph path. The
     * photo will be read from the specified file.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to use, defaults to me/photos
     * @param file        the file containing the photo to upload
     * @param caption     the user generated caption for the photo, can be null
     * @param params      the parameters, can be null
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions, can be null
     * @return a Request that is ready to execute
     * @throws java.io.FileNotFoundException if the file doesn't exist
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            String graphPath,
            File file,
            String caption,
            Bundle params,
            Callback callback
    ) throws FileNotFoundException {
        graphPath = getDefaultPhotoPathIfNull(graphPath);
        ParcelFileDescriptor descriptor =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        Bundle parameters = new Bundle();
        if (params != null) {
            parameters.putAll(params);
        }
        parameters.putParcelable(PICTURE_PARAM, descriptor);
        if (caption != null && !caption.isEmpty()) {
            parameters.putString(CAPTION_PARAM, caption);
        }

        return new GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the specified graph path. The
     * photo will be read from the specified Uri.
     *
     * @param accessToken the access token to use, or null
     * @param graphPath   the graph path to use, defaults to me/photos
     * @param photoUri    the file:// or content:// Uri to the photo on device
     * @param caption     the user generated caption for the photo, can be null
     * @param params      the parameters, can be null
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions, can be null
     * @return a Request that is ready to execute
     * @throws FileNotFoundException if the Uri does not exist
     */
    public static GraphRequest newUploadPhotoRequest(
            AccessToken accessToken,
            String graphPath,
            Uri photoUri,
            String caption,
            Bundle params,
            Callback callback)
            throws FileNotFoundException {
        graphPath = getDefaultPhotoPathIfNull(graphPath);
        if (Utility.isFileUri(photoUri)) {
            return newUploadPhotoRequest(
                    accessToken,
                    graphPath,
                    new File(photoUri.getPath()),
                    caption,
                    params,
                    callback);
        } else if (!Utility.isContentUri(photoUri)) {
            throw new FacebookException("The photo Uri must be either a file:// or content:// Uri");
        }

        Bundle parameters = new Bundle();
        if (params != null) {
            parameters.putAll(params);
        }
        parameters.putParcelable(PICTURE_PARAM, photoUri);

        return new GraphRequest(accessToken, graphPath, parameters, HttpMethod.POST, callback);
    }


    /**
     * Creates a new Request configured to retrieve an App User ID for the app's Facebook user.
     * Callers will send this ID back to their own servers, collect up a set to create a Facebook
     * Custom Audience with, and then use the resultant Custom Audience to target ads.
     * <p/>
     * The GraphObject in the response will include a "custom_audience_third_party_id" property,
     * with the value being the ID retrieved.  This ID is an encrypted encoding of the Facebook
     * user's ID and the invoking Facebook app ID.  Multiple calls with the same user will return
     * different IDs, thus these IDs cannot be used to correlate behavior across devices or
     * applications, and are only meaningful when sent back to Facebook for creating Custom
     * Audiences.
     * <p/>
     * The ID retrieved represents the Facebook user identified in the following way: if the
     * specified access token (or active access token if `null`) is valid, the ID will represent the
     * user associated with the active access token; otherwise the ID will represent the user logged
     * into the native Facebook app on the device. A `null` ID will be provided into the callback if
     * a) there is no native Facebook app, b) no one is logged into it, or c) the app has previously
     * called {@link FacebookSdk#setLimitEventAndDataUsage(android.content.Context, boolean)} ;}
     * with `true` for this user. <b>You must call this method from a background thread for it to
     * work properly.</b>
     *
     * @param accessToken   the access token to issue the Request on, or null If there is no
     *                      logged-in Facebook user, null is the expected choice.
     * @param context       the Application context from which the app ID will be pulled, and from
     *                      which the 'attribution ID' for the Facebook user is determined.  If
     *                      there has been no app ID set, an exception will be thrown.
     * @param applicationId explicitly specified Facebook App ID.  If null, the application ID from
     *                      the access token will be used, if any; if not, the application ID from
     *                      metadata will be used.
     * @param callback      a callback that will be called when the request is completed to handle
     *                      success or error conditions. The GraphObject in the Response will
     *                      contain a "custom_audience_third_party_id" property that represents the
     *                      user as described above.
     * @return a Request that is ready to execute
     */
    public static GraphRequest newCustomAudienceThirdPartyIdRequest(AccessToken accessToken,
                                                                    Context context,
                                                                    String applicationId,
                                                                    Callback callback) {

        if (applicationId == null && accessToken != null) {
            applicationId = accessToken.getApplicationId();
        }

        if (applicationId == null) {
            applicationId = Utility.getMetadataApplicationId(context);
        }

        if (applicationId == null) {
            throw new FacebookException("Facebook App ID cannot be determined");
        }

        String endpoint = applicationId + "/custom_audience_third_party_id";
        AttributionIdentifiers attributionIdentifiers =
                AttributionIdentifiers.getAttributionIdentifiers(context);
        Bundle parameters = new Bundle();

        if (accessToken == null) {
            // Only use the attributionID if we don't have an access token.  If we do, then the user
            // token will be used to identify the user, and is more reliable than the attributionID.
            String udid = attributionIdentifiers.getAttributionId() != null
                    ? attributionIdentifiers.getAttributionId()
                    : attributionIdentifiers.getAndroidAdvertiserId();
            if (attributionIdentifiers.getAttributionId() != null) {
                parameters.putString("udid", udid);
            }
        }

        // Server will choose to not provide the App User ID in the event that event usage has been
        // limited for this user for this app.
        if (FacebookSdk.getLimitEventAndDataUsage(context)
                || attributionIdentifiers.isTrackingLimited()) {
            parameters.putString("limit_event_usage", "1");
        }

        return new GraphRequest(accessToken, endpoint, parameters, HttpMethod.GET, callback);
    }

    /**
     * Creates a new Request configured to retrieve an App User ID for the app's Facebook user.
     * Callers will send this ID back to their own servers, collect up a set to create a Facebook
     * Custom Audience with, and then use the resultant Custom Audience to target ads.
     * <p/>
     * The GraphObject in the response will include a "custom_audience_third_party_id" property,
     * with the value being the ID retrieved.  This ID is an encrypted encoding of the Facebook
     * user's ID and the invoking Facebook app ID.  Multiple calls with the same user will return
     * different IDs, thus these IDs cannot be used to correlate behavior across devices or
     * applications, and are only meaningful when sent back to Facebook for creating Custom
     * Audiences.
     * <p/>
     * The ID retrieved represents the Facebook user identified in the following way: if the
     * specified access token (or active access token if `null`) is valid, the ID will represent the
     * user associated with the active access token; otherwise the ID will represent the user logged
     * into the native Facebook app on the device. A `null` ID will be provided into the callback if
     * a) there is no native Facebook app, b) no one is logged into it, or c) the app has previously
     * called {@link FacebookSdk#setLimitEventAndDataUsage(android.content.Context, boolean)} with
     * `true` for this user. <b>You must call this method from a background thread for it to work
     * properly.</b>
     *
     * @param accessToken the access token to issue the Request on, or null If there is no logged-in
     *                    Facebook user, null is the expected choice.
     * @param context     the Application context from which the app ID will be pulled, and from
     *                    which the 'attribution ID' for the Facebook user is determined.  If there
     *                    has been no app ID set, an exception will be thrown.
     * @param callback    a callback that will be called when the request is completed to handle
     *                    success or error conditions. The GraphObject in the Response will contain
     *                    a "custom_audience_third_party_id" property that represents the user as
     *                    described above.
     * @return a Request that is ready to execute
     */
    public static GraphRequest newCustomAudienceThirdPartyIdRequest(
            AccessToken accessToken,
            Context context,
            Callback callback) {
        return newCustomAudienceThirdPartyIdRequest(accessToken, context, null, callback);
    }

    /**
     * Returns the GraphObject, if any, associated with this request.
     *
     * @return the GraphObject associated with this request, or null if there is none
     */
    public final JSONObject getGraphObject() {
        return this.graphObject;
    }

    /**
     * Sets the GraphObject associated with this request. This is meaningful only for POST
     * requests.
     *
     * @param graphObject the GraphObject to upload along with this request
     */
    public final void setGraphObject(JSONObject graphObject) {
        this.graphObject = graphObject;
    }

    /**
     * Returns the graph path of this request, if any.
     *
     * @return the graph path of this request, or null if there is none
     */
    public final String getGraphPath() {
        return this.graphPath;
    }

    /**
     * Sets the graph path of this request.
     *
     * @param graphPath the graph path for this request
     */
    public final void setGraphPath(String graphPath) {
        this.graphPath = graphPath;
    }

    /**
     * Returns the {@link HttpMethod} to use for this request.
     *
     * @return the HttpMethod
     */
    public final HttpMethod getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * Sets the {@link HttpMethod} to use for this request.
     *
     * @param httpMethod the HttpMethod, or null for the default (HttpMethod.GET).
     */
    public final void setHttpMethod(HttpMethod httpMethod) {
        if (overriddenURL != null && httpMethod != HttpMethod.GET) {
            throw new FacebookException("Can't change HTTP method on request with overridden URL.");
        }
        this.httpMethod = (httpMethod != null) ? httpMethod : HttpMethod.GET;
    }

    /**
     * Returns the version of the API that this request will use.  By default this is the current
     * API at the time the SDK is released.
     *
     * @return the version that this request will use
     */
    public final String getVersion() {
        return this.version;
    }

    /**
     * Set the version to use for this request.  By default the version will be the current API at
     * the time the SDK is released.  Only use this if you need to explicitly override.
     *
     * @param version The version to use.  Should look like "v2.0"
     */
    public final void setVersion(String version) {
        this.version = version;
    }

    /**
     * This is an internal function that is not meant to be used by developers.
     */
    public final void setSkipClientToken(boolean skipClientToken) {
        this.skipClientToken = skipClientToken;
    }

    /**
     * Returns the parameters for this request.
     *
     * @return the parameters
     */
    public final Bundle getParameters() {
        return this.parameters;
    }

    /**
     * Sets the parameters for this request.
     *
     * @param parameters the parameters
     */
    public final void setParameters(Bundle parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the access token associated with this request.
     *
     * @return the access token associated with this request, or null if none has been specified
     */
    public final AccessToken getAccessToken() {
        return this.accessToken;
    }

    /**
     * Sets the access token to use for this request.
     *
     * @param accessToken the access token to use for this request
     */
    public final void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns the name of this requests entry in a batched request.
     *
     * @return the name of this requests batch entry, or null if none has been specified
     */
    public final String getBatchEntryName() {
        return this.batchEntryName;
    }

    /**
     * Sets the name of this request's entry in a batched request. This value is only used if this
     * request is submitted as part of a batched request. It can be used to specified dependencies
     * between requests.
     * See <a href="https://developers.facebook.com/docs/reference/api/batch/">Batch Requests</a> in
     * the Graph API documentation for more details.
     *
     * @param batchEntryName the name of this requests entry in a batched request, which must be
     *                       unique within a particular batch of requests
     */
    public final void setBatchEntryName(String batchEntryName) {
        this.batchEntryName = batchEntryName;
    }

    /**
     * Returns the name of the request that this request entry explicitly depends on in a batched
     * request.
     *
     * @return the name of this requests dependency, or null if none has been specified
     */
    public final String getBatchEntryDependsOn() {
        return this.batchEntryDependsOn;
    }

    /**
     * Sets the name of the request entry that this request explicitly depends on in a batched
     * request. This value is only used if this request is submitted as part of a batched request.
     * It can be used to specified dependencies between requests. See <a
     * href="https://developers.facebook.com/docs/reference/api/batch/">Batch Requests</a> in the
     * Graph API documentation for more details.
     *
     * @param batchEntryDependsOn the name of the request entry that this entry depends on in a
     *                            batched request
     */
    public final void setBatchEntryDependsOn(String batchEntryDependsOn) {
        this.batchEntryDependsOn = batchEntryDependsOn;
    }


    /**
     * Returns whether or not this batch entry will return a response if it is successful. Only
     * applies if another request entry in the batch specifies this entry as a dependency.
     *
     * @return the name of this requests dependency, or null if none has been specified
     */
    public final boolean getBatchEntryOmitResultOnSuccess() {
        return this.batchEntryOmitResultOnSuccess;
    }

    /**
     * Sets whether or not this batch entry will return a response if it is successful. Only applies
     * if another request entry in the batch specifies this entry as a dependency. See <a
     * href="https://developers.facebook.com/docs/reference/api/batch/">Batch Requests</a> in the
     * Graph API documentation for more details.
     *
     * @param batchEntryOmitResultOnSuccess the name of the request entry that this entry depends on
     *                                      in a batched request
     */
    public final void setBatchEntryOmitResultOnSuccess(boolean batchEntryOmitResultOnSuccess) {
        this.batchEntryOmitResultOnSuccess = batchEntryOmitResultOnSuccess;
    }

    /**
     * Gets the default Facebook application ID that will be used to submit batched requests.
     * Batched requests require an application ID, so either at least one request in a batch must
     * provide an access token or the application ID must be specified explicitly.
     *
     * @return the Facebook application ID to use for batched requests if none can be determined
     */
    public static final String getDefaultBatchApplicationId() {
        return GraphRequest.defaultBatchApplicationId;
    }

    /**
     * Sets the default application ID that will be used to submit batched requests if none of those
     * requests specifies an access token. Batched requests require an application ID, so either at
     * least one request in a batch must specify an access token or the application ID must be
     * specified explicitly.
     *
     * @param applicationId the Facebook application ID to use for batched requests if none can
     *                      be determined
     */
    public static final void setDefaultBatchApplicationId(String applicationId) {
        defaultBatchApplicationId = applicationId;
    }

    /**
     * Returns the callback which will be called when the request finishes.
     *
     * @return the callback
     */
    public final Callback getCallback() {
        return callback;
    }

    /**
     * Sets the callback which will be called when the request finishes.
     *
     * @param callback the callback
     */
    public final void setCallback(final Callback callback) {
        // Wrap callback to parse debug response if Graph Debug Mode is Enabled.
        if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_INFO)
                || FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
            Callback wrapper = new Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    JSONObject responseObject = response.getJSONObject();
                    JSONObject debug =
                            responseObject != null ? responseObject.optJSONObject(DEBUG_KEY) : null;
                    JSONArray debugMessages =
                            debug != null ? debug.optJSONArray(DEBUG_MESSAGES_KEY) : null;
                    if (debugMessages != null) {
                        for (int i = 0; i < debugMessages.length(); ++i) {
                            JSONObject debugMessageObject = debugMessages.optJSONObject(i);
                            String debugMessage = debugMessageObject != null
                                    ? debugMessageObject.optString(DEBUG_MESSAGE_KEY)
                                    : null;
                            String debugMessageType = debugMessageObject != null
                                    ? debugMessageObject.optString(DEBUG_MESSAGE_TYPE_KEY)
                                    : null;
                            String debugMessageLink = debugMessageObject != null
                                    ? debugMessageObject.optString(DEBUG_MESSAGE_LINK_KEY)
                                    : null;
                            if (debugMessage != null && debugMessageType != null) {
                                LoggingBehavior behavior = LoggingBehavior.GRAPH_API_DEBUG_INFO;
                                if (debugMessageType.equals("warning")) {
                                    behavior = LoggingBehavior.GRAPH_API_DEBUG_WARNING;
                                }
                                if (!Utility.isNullOrEmpty(debugMessageLink)) {
                                    debugMessage += " Link: " + debugMessageLink;
                                }
                                Logger.log(behavior, TAG, debugMessage);
                            }
                        }
                    }
                    if (callback != null) {
                        callback.onCompleted(response);
                    }
                }
            };
            this.callback = wrapper;
        } else {
            this.callback = callback;
        }

    }

    /**
     * Sets the tag on the request; this is an application-defined object that can be used to
     * distinguish between different requests. Its value has no effect on the execution of the
     * request.
     *
     * @param tag an object to serve as a tag, or null
     */
    public final void setTag(Object tag) {
        this.tag = tag;
    }

    /**
     * Gets the tag on the request; this is an application-defined object that can be used to
     * distinguish between different requests. Its value has no effect on the execution of the
     * request.
     *
     * @return an object that serves as a tag, or null
     */
    public final Object getTag() {
        return tag;
    }

    /**
     * Executes this request on the current thread and blocks while waiting for the response.
     * <p/>
     * This should only be called if you have transitioned off the UI thread.
     *
     * @return the Response object representing the results of the request
     * @throws FacebookException        If there was an error in the protocol used to communicate
     * with the service
     * @throws IllegalArgumentException
     */
    public final GraphResponse executeAndWait() {
        return GraphRequest.executeAndWait(this);
    }

    /**
     * Executes the request asynchronously. This function will return immediately,
     * and the request will be processed on a separate thread. In order to process result of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the {@link #setCallback(Callback) setCallback} method).
     * <p/>
     * This should only be called from the UI thread.
     *
     * @return a RequestAsyncTask that is executing the request
     * @throws IllegalArgumentException
     */
    public final GraphRequestAsyncTask executeAsync() {
        return GraphRequest.executeBatchAsync(this);
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException        If any of the requests in the batch are badly constructed or
     *                                  if there are problems contacting the service
     * @throws IllegalArgumentException if the passed in array is zero-length
     * @throws NullPointerException     if the passed in array or any of its contents are null
     */
    public static HttpURLConnection toHttpConnection(GraphRequest... requests) {
        return toHttpConnection(Arrays.asList(requests));
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException        If any of the requests in the batch are badly constructed or
     *                                  if there are problems contacting the service
     * @throws IllegalArgumentException if the passed in collection is empty
     * @throws NullPointerException     if the passed in collection or any of its contents are null
     */
    public static HttpURLConnection toHttpConnection(Collection<GraphRequest> requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        return toHttpConnection(new GraphRequestBatch(requests));
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection
     * can be executed explicitly by the caller.
     *
     * @param requests a RequestBatch to serialize
     * @return an HttpURLConnection which is ready to execute
     * @throws FacebookException        If any of the requests in the batch are badly constructed or
     *                                  if there are problems contacting the service
     * @throws IllegalArgumentException
     */
    public static HttpURLConnection toHttpConnection(GraphRequestBatch requests) {

        validateFieldsParamForGetRequests(requests);

        URL url;
        try {
            if (requests.size() == 1) {
                // Single request case.
                GraphRequest request = requests.get(0);
                // In the non-batch case, the URL we use really is the same one returned by
                // getUrlForSingleRequest.
                url = new URL(request.getUrlForSingleRequest());
            } else {
                // Batch case -- URL is just the graph API base, individual request URLs are
                // serialized as relative_url parameters within each batch entry.
                url = new URL(ServerProtocol.getGraphUrlBase());
            }
        } catch (MalformedURLException e) {
            throw new FacebookException("could not construct URL for request", e);
        }

        HttpURLConnection connection;
        try {
            connection = createConnection(url);

            serializeToUrlConnection(requests, connection);
        } catch (IOException e) {
            throw new FacebookException("could not construct request body", e);
        } catch (JSONException e) {
            throw new FacebookException("could not construct request body", e);
        }

        return connection;
    }

    /**
     * Executes a single request on the current thread and blocks while waiting for the response.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param request the Request to execute
     * @return the Response object representing the results of the request
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     *                           service
     */
    public static GraphResponse executeAndWait(GraphRequest request) {
        List<GraphResponse> responses = executeBatchAndWait(request);

        if (responses == null || responses.size() != 1) {
            throw new FacebookException("invalid state: expected a single response");
        }

        return responses.get(0);
    }

    /**
     * Executes requests on the current thread as a single batch and blocks while waiting for the
     * response.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws NullPointerException In case of a null request
     * @throws FacebookException    If there was an error in the protocol used to communicate with
     *                              the service
     */
    public static List<GraphResponse> executeBatchAndWait(GraphRequest... requests) {
        Validate.notNull(requests, "requests");

        return executeBatchAndWait(Arrays.asList(requests));
    }

    /**
     * Executes requests as a single batch on the current thread and blocks while waiting for the
     * responses.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     *                           service
     */
    public static List<GraphResponse> executeBatchAndWait(Collection<GraphRequest> requests) {
        return executeBatchAndWait(new GraphRequestBatch(requests));
    }

    /**
     * Executes requests on the current thread as a single batch and blocks while waiting for the
     * responses.
     * <p/>
     * This should only be used if you have transitioned off the UI thread.
     *
     * @param requests the batch of Requests to execute
     * @return a list of Response objects representing the results of the requests; responses are
     * returned in the same order as the requests were specified.
     * @throws FacebookException        If there was an error in the protocol used to communicate
     *                                  with the service
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException     if the passed in RequestBatch or any of its contents are
     *                                  null
     */
    public static List<GraphResponse> executeBatchAndWait(GraphRequestBatch requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        HttpURLConnection connection = null;
        try {
            connection = toHttpConnection(requests);
        } catch (Exception ex) {
            List<GraphResponse> responses = GraphResponse.constructErrorResponses(
                    requests.getRequests(),
                    null,
                    new FacebookException(ex));
            runCallbacks(requests, responses);
            return responses;
        }

        List<GraphResponse> responses = executeConnectionAndWait(connection, requests);
        return responses;
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the {@link #setCallback(Callback) setCallback} method).
     * <p/>
     * This should only be called from the UI thread.
     *
     * @param requests the Requests to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws NullPointerException If a null request is passed in
     */
    public static GraphRequestAsyncTask executeBatchAsync(GraphRequest... requests) {
        Validate.notNull(requests, "requests");

        return executeBatchAsync(Arrays.asList(requests));
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the {@link #setCallback(Callback) setCallback} method).
     * <p/>
     * This should only be called from the UI thread.
     *
     * @param requests the Requests to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws IllegalArgumentException if the passed in collection is empty
     * @throws NullPointerException     if the passed in collection or any of its contents are null
     */
    public static GraphRequestAsyncTask executeBatchAsync(Collection<GraphRequest> requests) {
        return executeBatchAsync(new GraphRequestBatch(requests));
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately,
     * and the requests will be processed on a separate thread. In order to process results of a
     * request, or determine whether a request succeeded or failed, a callback must be specified
     * (see the {@link #setCallback(Callback) setCallback} method).
     * <p/>
     * This should only be called from the UI thread.
     *
     * @param requests the RequestBatch to execute
     * @return a RequestAsyncTask that is executing the request
     * @throws IllegalArgumentException if the passed in RequestBatch is empty
     * @throws NullPointerException     if the passed in RequestBatch or any of its contents are
     *                                  null
     */
    public static GraphRequestAsyncTask executeBatchAsync(GraphRequestBatch requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        GraphRequestAsyncTask asyncTask = new GraphRequestAsyncTask(requests);
        asyncTask.executeOnSettingsExecutor();
        return asyncTask;
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation
     * is done that the contents of the connection actually reflect the serialized requests, so it
     * is the caller's responsibility to ensure that it will correctly generate the desired
     * responses.
     * <p/>
     * This should only be called if you have transitioned off the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests   the requests represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     *                           service
     */
    public static List<GraphResponse> executeConnectionAndWait(
            HttpURLConnection connection,
            Collection<GraphRequest> requests) {
        return executeConnectionAndWait(connection, new GraphRequestBatch(requests));
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation
     * is done that the contents of the connection actually reflect the serialized requests, so it
     * is the caller's responsibility to ensure that it will correctly generate the desired
     * responses.
     * <p/>
     * This should only be called if you have transitioned off the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests   the RequestBatch represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     * @throws FacebookException If there was an error in the protocol used to communicate with the
     *                           service
     */
    public static List<GraphResponse> executeConnectionAndWait(
            HttpURLConnection connection,
            GraphRequestBatch requests) {
        List<GraphResponse> responses = GraphResponse.fromHttpConnection(connection, requests);

        Utility.disconnectQuietly(connection);

        int numRequests = requests.size();
        if (numRequests != responses.size()) {
            throw new FacebookException(
                    String.format(Locale.US,
                            "Received %d responses while expecting %d",
                            responses.size(),
                            numRequests));
        }

        runCallbacks(requests, responses);

        // Try extending the current access token in case it's needed.
        AccessTokenManager.getInstance().extendAccessTokenIfNeeded();

        return responses;
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection.
     * No validation is done that the contents of the connection actually reflect the serialized
     * requests, so it is the caller's responsibility to ensure that it will correctly generate the
     * desired responses. This function will return immediately, and the requests will be processed
     * on a separate thread. In order to process results of a request, or determine whether a
     * request succeeded or failed, a callback must be specified (see the {@link
     * #setCallback(Callback) setCallback} method).
     * <p/>
     * This should only be called from the UI thread.
     *
     * @param connection the HttpURLConnection that the requests were serialized into
     * @param requests   the requests represented by the HttpURLConnection
     * @return a RequestAsyncTask that is executing the request
     */
    public static GraphRequestAsyncTask executeConnectionAsync(
            HttpURLConnection connection,
            GraphRequestBatch requests) {
        return executeConnectionAsync(null, connection, requests);
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection.
     * No validation is done that the contents of the connection actually reflect the serialized
     * requests, so it is the caller's responsibility to ensure that it will correctly generate the
     * desired responses. This function will return immediately, and the requests will be processed
     * on a separate thread. In order to process results of a request, or determine whether a
     * request succeeded or failed, a callback must be specified (see the {@link
     * #setCallback(Callback) setCallback} method)
     * <p/>
     * This should only be called from the UI thread.
     *
     * @param callbackHandler a Handler that will be used to post calls to the callback for each
     *                        request; if null, a Handler will be instantiated on the calling
     *                        thread
     * @param connection      the HttpURLConnection that the requests were serialized into
     * @param requests        the requests represented by the HttpURLConnection
     * @return a RequestAsyncTask that is executing the request
     */
    public static GraphRequestAsyncTask executeConnectionAsync(
            Handler callbackHandler,
            HttpURLConnection connection,
            GraphRequestBatch requests) {
        Validate.notNull(connection, "connection");

        GraphRequestAsyncTask asyncTask = new GraphRequestAsyncTask(connection, requests);
        requests.setCallbackHandler(callbackHandler);
        asyncTask.executeOnSettingsExecutor();
        return asyncTask;
    }

    /**
     * Returns a string representation of this Request, useful for debugging.
     *
     * @return the debugging information
     */
    @Override
    public String toString() {
        return new StringBuilder()
                .append("{Request: ")
                .append(" accessToken: ")
                .append(accessToken == null ? "null" : accessToken)
                .append(", graphPath: ")
                .append(graphPath)
                .append(", graphObject: ")
                .append(graphObject)
                .append(", httpMethod: ")
                .append(httpMethod)
                .append(", parameters: ")
                .append(parameters)
                .append("}")
                .toString();
    }

    static void runCallbacks(final GraphRequestBatch requests, List<GraphResponse> responses) {
        int numRequests = requests.size();

        // Compile the list of callbacks to call and then run them either on this thread or via the
        // Handler we received
        final ArrayList<Pair<Callback, GraphResponse>> callbacks = new ArrayList<Pair<Callback, GraphResponse>>();
        for (int i = 0; i < numRequests; ++i) {
            GraphRequest request = requests.get(i);
            if (request.callback != null) {
                callbacks.add(
                        new Pair<Callback, GraphResponse>(request.callback, responses.get(i)));
            }
        }

        if (callbacks.size() > 0) {
            Runnable runnable = new Runnable() {
                public void run() {
                    for (Pair<Callback, GraphResponse> pair : callbacks) {
                        pair.first.onCompleted(pair.second);
                    }

                    List<GraphRequestBatch.Callback> batchCallbacks = requests.getCallbacks();
                    for (GraphRequestBatch.Callback batchCallback : batchCallbacks) {
                        batchCallback.onBatchCompleted(requests);
                    }
                }
            };

            Handler callbackHandler = requests.getCallbackHandler();
            if (callbackHandler == null) {
                // Run on this thread.
                runnable.run();
            } else {
                // Post to the handler.
                callbackHandler.post(runnable);
            }
        }
    }

    private static String getDefaultPhotoPathIfNull(String graphPath) {
        return graphPath == null ? MY_PHOTOS : graphPath;
    }

    private static HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty(USER_AGENT_HEADER, getUserAgent());
        connection.setRequestProperty(ACCEPT_LANGUAGE_HEADER, Locale.getDefault().toString());

        connection.setChunkedStreamingMode(0);
        return connection;
    }


    private void addCommonParameters() {
        if (this.accessToken != null) {
            if (!this.parameters.containsKey(ACCESS_TOKEN_PARAM)) {
                String token = accessToken.getToken();
                Logger.registerAccessToken(token);
                this.parameters.putString(ACCESS_TOKEN_PARAM, token);
            }
        } else if (!skipClientToken && !this.parameters.containsKey(ACCESS_TOKEN_PARAM)) {
            String appID = FacebookSdk.getApplicationId();
            String clientToken = FacebookSdk.getClientToken();
            if (!Utility.isNullOrEmpty(appID) && !Utility.isNullOrEmpty(clientToken)) {
                String accessToken = appID + "|" + clientToken;
                this.parameters.putString(ACCESS_TOKEN_PARAM, accessToken);
            } else {
                Log.d(TAG, "Warning: Request without access token missing application ID or" +
                        " client token.");
            }
        }
        this.parameters.putString(SDK_PARAM, SDK_ANDROID);
        this.parameters.putString(FORMAT_PARAM, FORMAT_JSON);

        if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_INFO)) {
            this.parameters.putString(DEBUG_PARAM, DEBUG_SEVERITY_INFO);
        } else if (FacebookSdk.isLoggingBehaviorEnabled(LoggingBehavior.GRAPH_API_DEBUG_WARNING)) {
            this.parameters.putString(DEBUG_PARAM, DEBUG_SEVERITY_WARNING);
        }
    }

    private String appendParametersToBaseUrl(String baseUrl) {
        Uri.Builder uriBuilder = new Uri.Builder().encodedPath(baseUrl);

        Set<String> keys = this.parameters.keySet();
        for (String key : keys) {
            Object value = this.parameters.get(key);

            if (value == null) {
                value = "";
            }

            if (isSupportedParameterType(value)) {
                value = parameterToString(value);
            } else {
                if (httpMethod == HttpMethod.GET) {
                    throw new IllegalArgumentException(
                            String.format(
                                    Locale.US,
                                    "Unsupported parameter type for GET request: %s",
                                    value.getClass().getSimpleName()));
                }
                continue;
            }

            uriBuilder.appendQueryParameter(key, value.toString());
        }

        return uriBuilder.toString();
    }

    final String getUrlForBatchedRequest() {
        if (overriddenURL != null) {
            throw new FacebookException("Can't override URL for a batch request");
        }

        String baseUrl = getGraphPathWithVersion();
        addCommonParameters();
        return appendParametersToBaseUrl(baseUrl);
    }

    final String getUrlForSingleRequest() {
        if (overriddenURL != null) {
            return overriddenURL.toString();
        }

        String graphBaseUrlBase;
        if (this.getHttpMethod() == HttpMethod.POST
                && graphPath != null
                && graphPath.endsWith(VIDEOS_SUFFIX)) {
            graphBaseUrlBase = ServerProtocol.getGraphVideoUrlBase();
        } else {
            graphBaseUrlBase = ServerProtocol.getGraphUrlBase();
        }
        String baseUrl = String.format("%s/%s", graphBaseUrlBase, getGraphPathWithVersion());

        addCommonParameters();
        return appendParametersToBaseUrl(baseUrl);
    }

    private String getGraphPathWithVersion() {
        Matcher matcher = versionPattern.matcher(this.graphPath);
        if (matcher.matches()) {
            return this.graphPath;
        }
        return String.format("%s/%s", this.version, this.graphPath);
    }

    private static class Attachment {
        private final GraphRequest request;
        private final Object value;

        public Attachment(GraphRequest request, Object value) {
            this.request = request;
            this.value = value;
        }

        public GraphRequest getRequest() {
            return request;
        }

        public Object getValue() {
            return value;
        }
    }

    private void serializeToBatch(
            JSONArray batch,
            Map<String, Attachment> attachments
    ) throws JSONException, IOException {
        JSONObject batchEntry = new JSONObject();

        if (this.batchEntryName != null) {
            batchEntry.put(BATCH_ENTRY_NAME_PARAM, this.batchEntryName);
            batchEntry.put(
                    BATCH_ENTRY_OMIT_RESPONSE_ON_SUCCESS_PARAM,
                    this.batchEntryOmitResultOnSuccess);
        }
        if (this.batchEntryDependsOn != null) {
            batchEntry.put(BATCH_ENTRY_DEPENDS_ON_PARAM, this.batchEntryDependsOn);
        }

        String relativeURL = getUrlForBatchedRequest();
        batchEntry.put(BATCH_RELATIVE_URL_PARAM, relativeURL);
        batchEntry.put(BATCH_METHOD_PARAM, httpMethod);
        if (this.accessToken != null) {
            String token = this.accessToken.getToken();
            Logger.registerAccessToken(token);
        }

        // Find all of our attachments. Remember their names and put them in the attachment map.
        ArrayList<String> attachmentNames = new ArrayList<String>();
        Set<String> keys = this.parameters.keySet();
        for (String key : keys) {
            Object value = this.parameters.get(key);
            if (isSupportedAttachmentType(value)) {
                // Make the name unique across this entire batch.
                String name = String.format(
                        Locale.ROOT,
                        "%s%d",
                        ATTACHMENT_FILENAME_PREFIX,
                        attachments.size());
                attachmentNames.add(name);
                attachments.put(name, new Attachment(this, value));
            }
        }

        if (!attachmentNames.isEmpty()) {
            String attachmentNamesString = TextUtils.join(",", attachmentNames);
            batchEntry.put(ATTACHED_FILES_PARAM, attachmentNamesString);
        }

        if (this.graphObject != null) {
            // Serialize the graph object into the "body" parameter.
            final ArrayList<String> keysAndValues = new ArrayList<String>();
            processGraphObject(this.graphObject, relativeURL, new KeyValueSerializer() {
                @Override
                public void writeString(String key, String value) throws IOException {
                    keysAndValues.add(String.format(
                            Locale.US,
                            "%s=%s",
                            key,
                            URLEncoder.encode(value, "UTF-8")));
                }
            });
            String bodyValue = TextUtils.join("&", keysAndValues);
            batchEntry.put(BATCH_BODY_PARAM, bodyValue);
        }

        batch.put(batchEntry);
    }

    private static boolean hasOnProgressCallbacks(GraphRequestBatch requests) {
        for (GraphRequestBatch.Callback callback : requests.getCallbacks()) {
            if (callback instanceof GraphRequestBatch.OnProgressCallback) {
                return true;
            }
        }

        for (GraphRequest request : requests) {
            if (request.getCallback() instanceof OnProgressCallback) {
                return true;
            }
        }

        return false;
    }

    private static void setConnectionContentType(
            HttpURLConnection connection,
            boolean shouldUseGzip) {
        if (shouldUseGzip) {
            connection.setRequestProperty(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded");
            connection.setRequestProperty(CONTENT_ENCODING_HEADER, "gzip");
        } else {
            connection.setRequestProperty(CONTENT_TYPE_HEADER, getMimeContentType());
        }
    }

    private static boolean isGzipCompressible(GraphRequestBatch requests) {
        for (GraphRequest request : requests) {
            for (String key : request.parameters.keySet()) {
                Object value = request.parameters.get(key);
                if (isSupportedAttachmentType(value)) {
                    return false;
                }
            }
        }
        return true;
    }

    final static boolean shouldWarnOnMissingFieldsParam(GraphRequest request) {
        String version = request.getVersion();
        if (Utility.isNullOrEmpty(version)) {
            // null implies latest version
            return true;
        }
        if (version.startsWith("v")) {
            version = version.substring(1);
        }
        String [] versionParts = version.split("\\.");
        // We should warn on missing "fields" params for API 2.4 and above
        return versionParts.length >= 2
                && Integer.parseInt(versionParts[0]) > 2
                || (Integer.parseInt(versionParts[0]) >= 2
                    && Integer.parseInt(versionParts[1]) >= 4);
    }

    final static void validateFieldsParamForGetRequests(GraphRequestBatch requests) {
        // validate that the GET requests all have a "fields" param
        for (GraphRequest request : requests) {
            if (HttpMethod.GET.equals(request.getHttpMethod())
                    && shouldWarnOnMissingFieldsParam(request)) {
                Bundle params = request.getParameters();
                if (!params.containsKey(FIELDS_PARAM)
                        || Utility.isNullOrEmpty(params.getString(FIELDS_PARAM))) {
                    Logger.log(
                            LoggingBehavior.DEVELOPER_ERRORS,
                            Log.WARN,
                            "Request",
                            "starting with Graph API v2.4, GET requests for /%s should contain an" +
                            " explicit \"fields\" parameter.",
                            request.getGraphPath()
                    );
                }
            }
        }
    }

    final static void serializeToUrlConnection(
            GraphRequestBatch requests,
            HttpURLConnection connection
    ) throws IOException, JSONException {
        Logger logger = new Logger(LoggingBehavior.REQUESTS, "Request");

        int numRequests = requests.size();
        boolean shouldUseGzip = isGzipCompressible(requests);

        HttpMethod connectionHttpMethod =
                (numRequests == 1) ? requests.get(0).httpMethod : HttpMethod.POST;
        connection.setRequestMethod(connectionHttpMethod.name());
        setConnectionContentType(connection, shouldUseGzip);

        URL url = connection.getURL();
        logger.append("Request:\n");
        logger.appendKeyValue("Id", requests.getId());
        logger.appendKeyValue("URL", url);
        logger.appendKeyValue("Method", connection.getRequestMethod());
        logger.appendKeyValue("User-Agent", connection.getRequestProperty("User-Agent"));
        logger.appendKeyValue("Content-Type", connection.getRequestProperty("Content-Type"));

        connection.setConnectTimeout(requests.getTimeout());
        connection.setReadTimeout(requests.getTimeout());

        // If we have a single non-POST request, don't try to serialize anything or
        // HttpURLConnection will turn it into a POST.
        boolean isPost = (connectionHttpMethod == HttpMethod.POST);
        if (!isPost) {
            logger.log();
            return;
        }

        connection.setDoOutput(true);

        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(connection.getOutputStream());
            if (shouldUseGzip) {
                outputStream = new GZIPOutputStream(outputStream);
            }

            if (hasOnProgressCallbacks(requests)) {
                ProgressNoopOutputStream countingStream = null;
                countingStream = new ProgressNoopOutputStream(requests.getCallbackHandler());
                processRequest(requests, null, numRequests, url, countingStream, shouldUseGzip);

                int max = countingStream.getMaxProgress();
                Map<GraphRequest, RequestProgress> progressMap = countingStream.getProgressMap();

                outputStream = new ProgressOutputStream(outputStream, requests, progressMap, max);
            }

            processRequest(requests, logger, numRequests, url, outputStream, shouldUseGzip);
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }

        logger.log();
    }

    private static void processRequest(GraphRequestBatch requests, Logger logger, int numRequests,
                                       URL url, OutputStream outputStream, boolean shouldUseGzip)
            throws IOException, JSONException {
        Serializer serializer = new Serializer(outputStream, logger, shouldUseGzip);

        if (numRequests == 1) {
            GraphRequest request = requests.get(0);

            Map<String, Attachment> attachments = new HashMap<String, Attachment>();
            for (String key : request.parameters.keySet()) {
                Object value = request.parameters.get(key);
                if (isSupportedAttachmentType(value)) {
                    attachments.put(key, new Attachment(request, value));
                }
            }

            if (logger != null) {
                logger.append("  Parameters:\n");
            }
            serializeParameters(request.parameters, serializer, request);

            if (logger != null) {
                logger.append("  Attachments:\n");
            }
            serializeAttachments(attachments, serializer);

            if (request.graphObject != null) {
                processGraphObject(request.graphObject, url.getPath(), serializer);
            }
        } else {
            String batchAppID = getBatchAppId(requests);
            if (Utility.isNullOrEmpty(batchAppID)) {
                throw new FacebookException(
                        "App ID was not specified at the request or Settings.");
            }

            serializer.writeString(BATCH_APP_ID_PARAM, batchAppID);

            // We write out all the requests as JSON, remembering which file attachments they have,
            // then write out the attachments.
            Map<String, Attachment> attachments = new HashMap<String, Attachment>();
            serializeRequestsAsJSON(serializer, requests, attachments);

            if (logger != null) {
                logger.append("  Attachments:\n");
            }
            serializeAttachments(attachments, serializer);
        }
    }

    private static boolean isMeRequest(String path) {
        Matcher matcher = versionPattern.matcher(path);
        if (matcher.matches()) {
            // Group 1 contains the path aside from version
            path = matcher.group(1);
        }
        if (path.startsWith("me/") || path.startsWith("/me/")) {
            return true;
        }
        return false;
    }

    private static void processGraphObject(
            JSONObject graphObject,
            String path,
            KeyValueSerializer serializer
    ) throws IOException {
        // In general, graph objects are passed by reference (ID/URL). But if this is an OG Action,
        // we need to pass the entire values of the contents of the 'image' property, as they
        // contain important metadata beyond just a URL. We don't have a 100% foolproof way of
        // knowing if we are posting an OG Action, given that batched requests can have parameter
        // substitution, but passing the OG Action type as a substituted parameter is unlikely.
        // It looks like an OG Action if it's posted to me/namespace:action[?other=stuff].
        boolean isOGAction = false;
        if (isMeRequest(path)) {
            int colonLocation = path.indexOf(":");
            int questionMarkLocation = path.indexOf("?");
            isOGAction = colonLocation > 3
                    && (questionMarkLocation == -1 || colonLocation < questionMarkLocation);
        }

        Iterator<String> keyIterator = graphObject.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            Object value = graphObject.opt(key);
            boolean passByValue = isOGAction && key.equalsIgnoreCase("image");
            processGraphObjectProperty(key, value, serializer, passByValue);
        }
    }

    private static void processGraphObjectProperty(
            String key,
            Object value,
            KeyValueSerializer serializer,
            boolean passByValue
    ) throws IOException {
        Class<?> valueClass = value.getClass();

        if (JSONObject.class.isAssignableFrom(valueClass)) {
            JSONObject jsonObject = (JSONObject) value;
            if (passByValue) {
                // We need to pass all properties of this object in key[propertyName] format.
                @SuppressWarnings("unchecked")
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String propertyName = keys.next();
                    String subKey = String.format("%s[%s]", key, propertyName);
                    processGraphObjectProperty(
                            subKey,
                            jsonObject.opt(propertyName),
                            serializer,
                            passByValue);
                }
            } else {
                // Normal case is passing objects by reference, so just pass the ID or URL, if any,
                // as the value for "key"
                if (jsonObject.has("id")) {
                    processGraphObjectProperty(
                            key,
                            jsonObject.optString("id"),
                            serializer,
                            passByValue);
                } else if (jsonObject.has("url")) {
                    processGraphObjectProperty(
                            key,
                            jsonObject.optString("url"),
                            serializer,
                            passByValue);
                } else if (jsonObject.has(NativeProtocol.OPEN_GRAPH_CREATE_OBJECT_KEY)) {
                    processGraphObjectProperty(key, jsonObject.toString(), serializer, passByValue);
                }
            }
        } else if (JSONArray.class.isAssignableFrom(valueClass)) {
            JSONArray jsonArray = (JSONArray) value;
            int length = jsonArray.length();
            for (int i = 0; i < length; ++i) {
                String subKey = String.format(Locale.ROOT, "%s[%d]", key, i);
                processGraphObjectProperty(subKey, jsonArray.opt(i), serializer, passByValue);
            }
        } else if (String.class.isAssignableFrom(valueClass) ||
                Number.class.isAssignableFrom(valueClass) ||
                Boolean.class.isAssignableFrom(valueClass)) {
            serializer.writeString(key, value.toString());
        } else if (Date.class.isAssignableFrom(valueClass)) {
            Date date = (Date) value;
            // The "Events Timezone" platform migration affects what date/time formats Facebook
            // accepts and returns. Apps created after 8/1/12 (or apps that have explicitly enabled
            // the migration) should send/receive dates in ISO-8601 format. Pre-migration apps can
            // send as Unix timestamps. Since the future is ISO-8601, that is what we support here.
            // Apps that need pre-migration behavior can explicitly send these as integer timestamps
            // rather than Dates.
            final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(
                    ISO_8601_FORMAT_STRING,
                    Locale.US);
            serializer.writeString(key, iso8601DateFormat.format(date));
        }
    }

    private static void serializeParameters(
            Bundle bundle,
            Serializer serializer,
            GraphRequest request
    ) throws IOException {
        Set<String> keys = bundle.keySet();

        for (String key : keys) {
            Object value = bundle.get(key);
            if (isSupportedParameterType(value)) {
                serializer.writeObject(key, value, request);
            }
        }
    }

    private static void serializeAttachments(
            Map<String, Attachment> attachments,
            Serializer serializer
    ) throws IOException {
        Set<String> keys = attachments.keySet();

        for (String key : keys) {
            Attachment attachment = attachments.get(key);
            if (isSupportedAttachmentType(attachment.getValue())) {
                serializer.writeObject(key, attachment.getValue(), attachment.getRequest());
            }
        }
    }

    private static void serializeRequestsAsJSON(
            Serializer serializer,
            Collection<GraphRequest> requests,
            Map<String, Attachment> attachments
    ) throws JSONException, IOException {
        JSONArray batch = new JSONArray();
        for (GraphRequest request : requests) {
            request.serializeToBatch(batch, attachments);
        }

        serializer.writeRequestsAsJson(BATCH_PARAM, batch, requests);
    }

    private static String getMimeContentType() {
        return String.format("multipart/form-data; boundary=%s", MIME_BOUNDARY);
    }

    private static volatile String userAgent;

    private static String getUserAgent() {
        if (userAgent == null) {
            userAgent = String.format("%s.%s", USER_AGENT_BASE, FacebookSdkVersion.BUILD);

            // For the unity sdk we need to append the unity user agent
            String customUserAgent = InternalSettings.getCustomUserAgent();
            if (!Utility.isNullOrEmpty(customUserAgent)) {
                userAgent = String.format(
                        Locale.ROOT,
                        "%s/%s",
                        userAgent,
                        customUserAgent);
            }
        }

        return userAgent;
    }

    private static String getBatchAppId(GraphRequestBatch batch) {
        if (!Utility.isNullOrEmpty(batch.getBatchApplicationId())) {
            return batch.getBatchApplicationId();
        }

        for (GraphRequest request : batch) {
            AccessToken accessToken = request.accessToken;
            if (accessToken != null) {
                String applicationId = accessToken.getApplicationId();
                if (applicationId != null) {
                    return applicationId;
                }
            }
        }
        if (!Utility.isNullOrEmpty(GraphRequest.defaultBatchApplicationId)) {
            return GraphRequest.defaultBatchApplicationId;
        }
        return FacebookSdk.getApplicationId();
    }

    private static boolean isSupportedAttachmentType(Object value) {
        return value instanceof Bitmap ||
                value instanceof byte[] ||
                value instanceof Uri ||
                value instanceof ParcelFileDescriptor ||
                value instanceof ParcelableResourceWithMimeType;
    }

    private static boolean isSupportedParameterType(Object value) {
        return value instanceof String || value instanceof Boolean || value instanceof Number ||
                value instanceof Date;
    }

    private static String parameterToString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        } else if (value instanceof Date) {
            final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat(
                    ISO_8601_FORMAT_STRING, Locale.US);
            return iso8601DateFormat.format(value);
        }
        throw new IllegalArgumentException("Unsupported parameter type.");
    }

    private interface KeyValueSerializer {
        void writeString(String key, String value) throws IOException;
    }

    private static class Serializer implements KeyValueSerializer {
        private final OutputStream outputStream;
        private final Logger logger;
        private boolean firstWrite = true;
        private boolean useUrlEncode = false;

        public Serializer(OutputStream outputStream, Logger logger, boolean useUrlEncode) {
            this.outputStream = outputStream;
            this.logger = logger;
            this.useUrlEncode = useUrlEncode;
        }

        public void writeObject(String key, Object value, GraphRequest request) throws IOException {
            if (outputStream instanceof RequestOutputStream) {
                ((RequestOutputStream) outputStream).setCurrentRequest(request);
            }

            if (isSupportedParameterType(value)) {
                writeString(key, parameterToString(value));
            } else if (value instanceof Bitmap) {
                writeBitmap(key, (Bitmap) value);
            } else if (value instanceof byte[]) {
                writeBytes(key, (byte[]) value);
            } else if (value instanceof Uri) {
                writeContentUri(key, (Uri) value, null);
            } else if (value instanceof ParcelFileDescriptor) {
                writeFile(key, (ParcelFileDescriptor) value, null);
            } else if (value instanceof ParcelableResourceWithMimeType) {
                ParcelableResourceWithMimeType resourceWithMimeType =
                        (ParcelableResourceWithMimeType) value;
                Parcelable resource = resourceWithMimeType.getResource();
                String mimeType = resourceWithMimeType.getMimeType();
                if (resource instanceof ParcelFileDescriptor) {
                    writeFile(key, (ParcelFileDescriptor) resource, mimeType);
                } else if (resource instanceof Uri) {
                    writeContentUri(key, (Uri) resource, mimeType);
                } else {
                    throw getInvalidTypeError();
                }
            } else {
                throw getInvalidTypeError();
            }
        }

        private RuntimeException getInvalidTypeError() {
            return new IllegalArgumentException("value is not a supported type.");
        }

        public void writeRequestsAsJson(
                String key,
                JSONArray requestJsonArray,
                Collection<GraphRequest> requests
        ) throws IOException, JSONException {
            if (!(outputStream instanceof RequestOutputStream)) {
                writeString(key, requestJsonArray.toString());
                return;
            }

            RequestOutputStream requestOutputStream = (RequestOutputStream) outputStream;
            writeContentDisposition(key, null, null);
            write("[");
            int i = 0;
            for (GraphRequest request : requests) {
                JSONObject requestJson = requestJsonArray.getJSONObject(i);
                requestOutputStream.setCurrentRequest(request);
                if (i > 0) {
                    write(",%s", requestJson.toString());
                } else {
                    write("%s", requestJson.toString());
                }
                i++;
            }
            write("]");
            if (logger != null) {
                logger.appendKeyValue("    " + key, requestJsonArray.toString());
            }
        }

        public void writeString(String key, String value) throws IOException {
            writeContentDisposition(key, null, null);
            writeLine("%s", value);
            writeRecordBoundary();
            if (logger != null) {
                logger.appendKeyValue("    " + key, value);
            }
        }

        public void writeBitmap(String key, Bitmap bitmap) throws IOException {
            writeContentDisposition(key, key, "image/png");
            // Note: quality parameter is ignored for PNG
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            writeLine("");
            writeRecordBoundary();
            if (logger != null) {
                logger.appendKeyValue("    " + key, "<Image>");
            }
        }

        public void writeBytes(String key, byte[] bytes) throws IOException {
            writeContentDisposition(key, key, "content/unknown");
            this.outputStream.write(bytes);
            writeLine("");
            writeRecordBoundary();
            if (logger != null) {
                logger.appendKeyValue(
                        "    " + key,
                        String.format(Locale.ROOT, "<Data: %d>", bytes.length));
            }
        }

        public void writeContentUri(String key, Uri contentUri, String mimeType)
                throws IOException {
            if (mimeType == null) {
                mimeType = "content/unknown";
            }
            writeContentDisposition(key, key, mimeType);

            InputStream inputStream = FacebookSdk
                    .getApplicationContext()
                    .getContentResolver()
                    .openInputStream(contentUri);

            int totalBytes = 0;
            if (outputStream instanceof ProgressNoopOutputStream) {
                // If we are only counting bytes then skip reading the file
                long contentSize = Utility.getContentSize(contentUri);

                ((ProgressNoopOutputStream) outputStream).addProgress(contentSize);
            } else {
                totalBytes += Utility.copyAndCloseInputStream(inputStream, outputStream);
            }

            writeLine("");
            writeRecordBoundary();
            if (logger != null) {
                logger.appendKeyValue(
                        "    " + key,
                        String.format(Locale.ROOT, "<Data: %d>", totalBytes));
            }
        }

        public void writeFile(
                String key,
                ParcelFileDescriptor descriptor,
                String mimeType
        ) throws IOException {
            if (mimeType == null) {
                mimeType = "content/unknown";
            }
            writeContentDisposition(key, key, mimeType);

            int totalBytes = 0;

            if (outputStream instanceof ProgressNoopOutputStream) {
                // If we are only counting bytes then skip reading the file
                ((ProgressNoopOutputStream) outputStream).addProgress(descriptor.getStatSize());
            } else {
                ParcelFileDescriptor.AutoCloseInputStream inputStream =
                        new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
                totalBytes += Utility.copyAndCloseInputStream(inputStream, outputStream);
            }
            writeLine("");
            writeRecordBoundary();
            if (logger != null) {
                logger.appendKeyValue(
                        "    " + key,
                        String.format(Locale.ROOT, "<Data: %d>", totalBytes));
            }
        }

        public void writeRecordBoundary() throws IOException {
            if (!useUrlEncode) {
                writeLine("--%s", MIME_BOUNDARY);
            } else {
                this.outputStream.write("&".getBytes());
            }
        }

        public void writeContentDisposition(
                String name,
                String filename,
                String contentType
        ) throws IOException {
            if (!useUrlEncode) {
                write("Content-Disposition: form-data; name=\"%s\"", name);
                if (filename != null) {
                    write("; filename=\"%s\"", filename);
                }
                writeLine(""); // newline after Content-Disposition
                if (contentType != null) {
                    writeLine("%s: %s", CONTENT_TYPE_HEADER, contentType);
                }
                writeLine(""); // blank line before content
            } else {
                this.outputStream.write(String.format("%s=", name).getBytes());
            }
        }

        public void write(String format, Object... args) throws IOException {
            if (!useUrlEncode) {
                if (firstWrite) {
                    // Prepend all of our output with a boundary string.
                    this.outputStream.write("--".getBytes());
                    this.outputStream.write(MIME_BOUNDARY.getBytes());
                    this.outputStream.write("\r\n".getBytes());
                    firstWrite = false;
                }
                this.outputStream.write(String.format(format, args).getBytes());
            } else {
                this.outputStream.write(
                        URLEncoder.encode(
                                String.format(Locale.US, format, args), "UTF-8").getBytes());
            }
        }

        public void writeLine(String format, Object... args) throws IOException {
            write(format, args);
            if (!useUrlEncode) {
                write("\r\n");
            }
        }

    }

    /**
     * Specifies the interface that consumers of the Request class can implement in order to be
     * notified when a particular request completes, either successfully or with an error.
     */
    public interface Callback {
        /**
         * The method that will be called when a request completes.
         *
         * @param response the Response of this request, which may include error information if the
         *                 request was unsuccessful
         */
        void onCompleted(GraphResponse response);
    }

    /**
     * Specifies the interface that consumers of the Request class can implement in order to be
     * notified when a progress is made on a particular request. The frequency of the callbacks can
     * be controlled using {@link FacebookSdk#setOnProgressThreshold(long)}
     */
    public interface OnProgressCallback extends Callback {
        /**
         * The method that will be called when progress is made.
         *
         * @param current the current value of the progress of the request.
         * @param max     the maximum value (target) value that the progress will have.
         */
        void onProgress(long current, long max);
    }

    /**
     * Callback for requests that result in an array of JSONObjects.
     */
    public interface GraphJSONArrayCallback {
        /**
         * The method that will be called when the request completes.
         *
         * @param objects  the list of GraphObjects representing the returned objects, or null
         * @param response the Response of this request, which may include error information if the
         *                 request was unsuccessful
         */
        void onCompleted(JSONArray objects, GraphResponse response);
    }

    /**
     * Callback for requests that result in a JSONObject.
     */
    public interface GraphJSONObjectCallback {
        /**
         * The method that will be called when the request completes.
         *
         * @param object   the GraphObject representing the returned object, or null
         * @param response the Response of this request, which may include error information if the
         *                 request was unsuccessful
         */
        void onCompleted(JSONObject object, GraphResponse response);
    }

    /**
     * Used during serialization for the graph request.
     * @param <RESOURCE> The Parcelable type parameter.
     */
    public static class ParcelableResourceWithMimeType<RESOURCE extends Parcelable>
            implements Parcelable {
        private final String mimeType;
        private final RESOURCE resource;

        public String getMimeType() {
            return mimeType;
        }

        public RESOURCE getResource() {
            return resource;
        }

        public int describeContents() {
            return CONTENTS_FILE_DESCRIPTOR;
        }

        public void writeToParcel(Parcel out, int flags) {
            out.writeString(mimeType);
            out.writeParcelable(resource, flags);
        }

        @SuppressWarnings("unused")
        public static final Parcelable.Creator<ParcelableResourceWithMimeType> CREATOR
                = new Parcelable.Creator<ParcelableResourceWithMimeType>() {
            public ParcelableResourceWithMimeType createFromParcel(Parcel in) {
                return new ParcelableResourceWithMimeType(in);
            }

            public ParcelableResourceWithMimeType[] newArray(int size) {
                return new ParcelableResourceWithMimeType[size];
            }
        };

        /**
         * The constructor.
         * @param resource The resource to parcel.
         * @param mimeType The mime type.
         */
        public ParcelableResourceWithMimeType(
                RESOURCE resource,
                String mimeType
        ) {
            this.mimeType = mimeType;
            this.resource = resource;
        }

        private ParcelableResourceWithMimeType(Parcel in) {
            mimeType = in.readString();
            resource = in.readParcelable(FacebookSdk.getApplicationContext().getClassLoader());
        }
    }
}
