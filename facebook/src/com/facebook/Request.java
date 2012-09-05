/**
 * Copyright 2012 Facebook
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Pair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * A single request to be sent to the Facebook Platform through either the <a
 * href="https://developers.facebook.com/docs/reference/api/">Graph API</a> or <a
 * href="https://developers.facebook.com/docs/reference/rest/">REST API</a>. The Request class provides functionality
 * relating to serializing and deserializing requests and responses, making calls in batches (with a single round-trip
 * to the service) and making calls asynchronously.
 * 
 * The particular service endpoint that a request targets is determined by either a graph path (see the
 * {@link #setGraphPath(String) setGraphPath} method) or a REST method name (see the {@link #setRestMethod(String)
 * setRestMethod} method); a single request may not target both.
 * 
 * A Request can be executed either anonymously or representing an authenticated user. In the former case, no Session
 * needs to be specified, while in the latter, a Session that is in an opened state must be provided. If requests are
 * executed in a batch, a Facebook application ID must be associated with the batch, either by supplying a Session for
 * at least one of the requests in the batch (the first one found in the batch will be used) or by calling the
 * {@link #setDefaultBatchApplicationId(String) setDefaultBatchApplicationId} method.
 * 
 * After completion of a request, its Session, if any, will be checked to determine if its Facebook access token needs
 * to be extended; if so, a request to extend it will be issued in the background.
 */
public class Request {
    /**
     * The maximum number of requests that can be submitted in a single batch. This limit is enforced on the service
     * side by the Facebook platform, not by the Request class.
     */
    public static final int MAXIMUM_BATCH_SIZE = 50;

    /**
     * The graph path to retrieve the user's profile.
     */
    public static final String ME = "me";
    /**
     * The graph path to retrieve the user's friends.
     */
    public static final String MY_FRIENDS = "me/friends";
    /**
     * The graph path to retrieve the user's photos.
     */
    public static final String MY_PHOTOS = "me/photos";
    /**
     * The graph path to execute a search.
     */
    public static final String SEARCH = "search";

    /**
     * HTTP method "GET".
     */
    public static final String GET_METHOD = "GET";
    /**
     * HTTP method "POST".
     */
    public static final String POST_METHOD = "POST";
    /**
     * HTTP method "DELETE".
     */
    public static final String DELETE_METHOD = "DELETE";

    private static final String USER_AGENT_HEADER = "User-Agent";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    // Parameter names/values
    private static final String PICTURE_PARAM = "picture";
    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_JSON = "json";
    private static final String SDK_PARAM = "sdk";
    private static final String SDK_ANDROID = "android";
    private static final String ACCESS_TOKEN_PARAM = "access_token";
    private static final String BATCH_ENTRY_NAME_PARAM = "name";
    private static final String BATCH_APP_ID_PARAM = "batch_app_id";
    private static final String BATCH_RELATIVE_URL_PARAM = "relative_url";
    private static final String BATCH_BODY_PARAM = "body";
    private static final String BATCH_METHOD_PARAM = "method";
    private static final String BATCH_PARAM = "batch";
    private static final String ATTACHMENT_FILENAME_PREFIX = "file";
    private static final String ATTACHED_FILES_PARAM = "attached_files";

    private static final String MIME_BOUNDARY = "3i2ndDfv2rTHiSisAbouNdArYfORhtTPEefj3q2f";
    private static final SimpleDateFormat iso8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    private static String defaultBatchApplicationId;

    private Session session;
    private String httpMethod;
    private String graphPath;
    private GraphObject graphObject;
    private String restMethod;
    private String batchEntryName;
    private Bundle parameters;
    private Callback callback;

    /**
     * Constructs a request without a session, graph path, or any other parameters.
     */
    public Request() {
        this(null, null, null, null, null);
    }

    /**
     * Constructs a request with a Session to retrieve a particular graph path. A Session need not be provided, in which
     * case the request is sent without an access token and thus is not executed in the context of any particular user.
     * Only certain graph requests can be expected to succeed in this case. If a Session is provided, it must be in an
     * opened state or the request will fail.
     * 
     * @param session
     *            the Session to use, or null
     * @param graphPath
     *            the graph path to retrieve
     */
    public Request(Session session, String graphPath) {
        this(session, graphPath, null, null, null);
    }

    /**
     * Constructs a request with a specific Session, graph path, parameters, and HTTP method. A Session need not be
     * provided, in which case the request is sent without an access token and thus is not executed in the context of
     * any particular user. Only certain graph requests can be expected to succeed in this case. If a Session is
     * provided, it must be in an opened state or the request will fail.
     * 
     * Depending on the httpMethod parameter, the object at the graph path may be retrieved, created, or deleted.
     * 
     * @param session
     *            the Session to use, or null
     * @param graphPath
     *            the graph path to retrieve, create, or delete
     * @param parameters
     *            additional parameters to pass along with the Graph API request; parameters must be Strings, Numbers,
     *            Bitmaps, Dates, or Byte arrays.
     * @param httpMethod
     *            the HTTP method to use for the request; must be one of GET, POST, or DELETE
     */
    public Request(Session session, String graphPath, Bundle parameters, String httpMethod) {
        this(session, graphPath, parameters, httpMethod, null);
    }

    /**
     * Constructs a request with a specific Session, graph path, parameters, and HTTP method. A Session need not be
     * provided, in which case the request is sent without an access token and thus is not executed in the context of
     * any particular user. Only certain graph requests can be expected to succeed in this case. If a Session is
     * provided, it must be in an opened state or the request will fail.
     * 
     * Depending on the httpMethod parameter, the object at the graph path may be retrieved, created, or deleted.
     * 
     * @param session
     *            the Session to use, or null
     * @param graphPath
     *            the graph path to retrieve, create, or delete
     * @param parameters
     *            additional parameters to pass along with the Graph API request; parameters must be Strings, Numbers,
     *            Bitmaps, Dates, or Byte arrays.
     * @param httpMethod
     *            the HTTP method to use for the request; must be one of GET, POST, or DELETE
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     */
    public Request(Session session, String graphPath, Bundle parameters, String httpMethod, Callback callback) {
        this.session = session;
        this.graphPath = graphPath;
        this.callback = callback;

        // This handles the null case by using the default.
        setHttpMethod(httpMethod);

        if (parameters != null) {
            this.parameters = new Bundle(parameters);
        } else {
            this.parameters = new Bundle();
        }
    }

    /**
     * Creates a new Request configured to post a GraphObject to a particular graph path, to either create or update the
     * object at that path.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param graphPath
     *            the graph path to retrieve, create, or delete
     * @param graphObject
     *            the GraphObject to create or update
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newPostRequest(Session session, String graphPath, GraphObject graphObject, Callback callback) {
        Request request = new Request(session, graphPath, null, POST_METHOD, callback);
        request.setGraphObject(graphObject);
        return request;
    }

    /**
     * Creates a new Request configured to make a call to the Facebook REST API.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param restMethod
     *            the method in the Facebook REST API to execute
     * @param parameters
     *            additional parameters to pass along with the Graph API request; parameters must be Strings, Numbers,
     *            Bitmaps, Dates, or Byte arrays.
     * @param httpMethod
     *            the HTTP method to use for the request; must be one of GET, POST, or DELETE
     * @return a Request that is ready to execute
     */
    public static Request newRestRequest(Session session, String restMethod, Bundle parameters, String httpMethod) {
        Request request = new Request(session, null, parameters, httpMethod);
        request.setRestMethod(restMethod);
        return request;
    }

    /**
     * Creates a new Request configured to retrieve a user's own profile.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newMeRequest(Session session, Callback callback) {
        return new Request(session, ME, null, null, callback);
    }

    /**
     * Creates a new Request configured to retrieve a user's friend list.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newMyFriendsRequest(Session session, Callback callback) {
        return new Request(session, MY_FRIENDS, null, null, callback);
    }

    /**
     * Creates a new Request configured to upload a photo to the user's default photo album.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param image
     *            the image to upload
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newUploadPhotoRequest(Session session, Bitmap image, Callback callback) {
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(PICTURE_PARAM, image);

        return new Request(session, MY_PHOTOS, parameters, POST_METHOD, callback);
    }

    /**
     * Creates a new Request configured to retrieve a particular graph path.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param graphPath
     *            the graph path to retrieve
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newGraphPathRequest(Session session, String graphPath, Callback callback) {
        return new Request(session, graphPath, null, null, callback);
    }

    /**
     * Creates a new Request that is configured to perform a search for places near a specified location via the Graph
     * API.
     * 
     * @param session
     *            the Session to use, or null; if non-null, the session must be in an opened state
     * @param location
     *            the location around which to search; only the latitude and longitude components of the location are
     *            meaningful
     * @param radiusInMeters
     *            the radius around the location to search, specified in meters
     * @param resultsLimit
     *            the maximum number of results to return
     * @param searchText
     *            optional text to search for as part of the name or type of an object
     * @param callback
     *            a callback that will be called when the request is completed to handle success or error conditions
     * @return a Request that is ready to execute
     */
    public static Request newPlacesSearchRequest(Session session, Location location, int radiusInMeters,
            int resultsLimit, String searchText, Callback callback) {
        if (location == null && Utility.isNullOrEmpty(searchText)) {
            throw new FacebookException("Either location or searchText must be specified.");
        }

        Bundle parameters = new Bundle(5);
        parameters.putString("type", "place");
        parameters.putString("limit", String.format("%d", resultsLimit));
        parameters.putString("distance", String.format("%d", radiusInMeters));
        parameters.putString("center",
                String.format(Locale.US, "%f,%f", location.getLatitude(), location.getLongitude()));
        if (!Utility.isNullOrEmpty(searchText)) {
            parameters.putString("q", searchText);
        }

        return new Request(session, SEARCH, parameters, GET_METHOD, callback);
    }

    /**
     * Returns the GraphObject, if any, associated with this request.
     * 
     * @return the GraphObject associated with this requeset, or null if there is none
     */
    public final GraphObject getGraphObject() {
        return this.graphObject;
    }

    /**
     * Sets the GraphObject associated with this request. This is meaningful only for POST requests.
     * 
     * @param graphObject
     *            the GraphObject to upload along with this request
     */
    public final void setGraphObject(GraphObject graphObject) {
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
     * Sets the graph path of this request. A graph path may not be set if a REST method has been specified.
     * 
     * @param graphPath
     *            the graph path for this request
     */
    public final void setGraphPath(String graphPath) {
        this.graphPath = graphPath;
    }

    /**
     * Returns the HTTP method to use for this request.
     * 
     * @return the HTTP method
     */
    public final String getHttpMethod() {
        return this.httpMethod;
    }

    /**
     * Sets the HTTP method to use for this request.
     * 
     * @param httpMethod
     *            the HTTP method, which should be one of GET, POST, or DELETE
     */
    public final void setHttpMethod(String httpMethod) {
        this.httpMethod = (httpMethod != null) ? httpMethod.toUpperCase() : GET_METHOD;
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
     * @param parameters
     *            the parameters
     */
    public final void setParameters(Bundle parameters) {
        this.parameters = parameters;
    }

    /**
     * Returns the REST method to call for this request.
     * 
     * @return the REST method
     */
    public final String getRestMethod() {
        return this.restMethod;
    }

    /**
     * Sets the REST method to call for this request. A REST method may not be set if a graph path has been specified.
     * 
     * @param restMethod
     *            the REST method to call
     */
    public final void setRestMethod(String restMethod) {
        this.restMethod = restMethod;
    }

    /**
     * Returns the Session associated with this request.
     * 
     * @return the Session associated with this request, or null if none has been specified
     */
    public final Session getSession() {
        return this.session;
    }

    /**
     * Sets the Session to use for this request. The Session does not need to be opened at the time it is specified, but
     * it must be opened by the time the request is executed.
     * 
     * @param session
     *            the Session to use for this request
     */
    public final void setSession(Session session) {
        this.session = session;
    }

    /**
     * Returns the name of this request's entry in a batched request.
     * 
     * @return the name of this request's batch entry, or null if none has been specified
     */
    public final String getBatchEntryName() {
        return this.batchEntryName;
    }

    /**
     * Sets the name of this request's entry in a batched request. This value is only used if this request is submitted
     * as part of a batched request. It can be used to specified dependencies between requests. See <a
     * href="https://developers.facebook.com/docs/reference/api/batch/">Batch Requests</a> in the Graph API
     * documentation for more details.
     * 
     * @param batchEntryName
     *            the name of this request's entry in a batched request, which must be unique within a particular batch
     *            of requests
     */
    public final void setBatchEntryName(String batchEntryName) {
        this.batchEntryName = batchEntryName;
    }

    /**
     * Gets the default Facebook application ID that will be used to submit batched requests if none of those requests
     * specifies a Session. Batched requests require an application ID, so either at least one request in a batch must
     * specify a Session or the application ID must be specified explicitly.
     * 
     * @return the Facebook application ID to use for batched requests if none can be determined
     */
    public static final String getDefaultBatchApplicationId() {
        return Request.defaultBatchApplicationId;
    }

    /**
     * Sets the default application ID that will be used to submit batched requests if none of those requests specifies
     * a Session. Batched requests require an application ID, so either at least one request in a batch must specify a
     * Session or the application ID must be specified explicitly.
     * 
     * @param applicationId
     *            the Facebook application ID to use for batched requests if none can be determined
     */
    public static final void setDefaultBatchApplicationId(String applicationId) {
        Request.defaultBatchApplicationId = applicationId;
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
     * @param callback
     *            the callback
     */
    public final void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * Executes this request and returns the response.
     * 
     * @return the Response object representing the results of the request
     */
    public final Response execute() {
        return Request.execute(this);
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection can be executed
     * explicitly by the caller.
     * 
     * @param requests
     *            one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     */
    public static HttpURLConnection toHttpConnection(Request... requests) {
        return toHttpConnection(Arrays.asList(requests));
    }

    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection can be executed
     * explicitly by the caller.
     *
     * @param requests
     *            one or more Requests to serialize
     * @return an HttpURLConnection which is ready to execute
     */
    public static HttpURLConnection toHttpConnection(Collection<Request> requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        return toHttpConnection(new RequestBatch(requests));
    }


    /**
     * Serializes one or more requests but does not execute them. The resulting HttpURLConnection can be executed
     * explicitly by the caller.
     *
     * @param requests
     *            a RequestBatch to serialize
     * @return an HttpURLConnection which is ready to execute
     */
    public static HttpURLConnection toHttpConnection(RequestBatch requests) {

        for (Request request : requests) {
            request.validate();
        }

        URL url = null;
        try {
            if (requests.size() == 1) {
                // Single request case.
                Request request = requests.get(0);
                url = request.getUrlForSingleRequest();
            } else {
                // Batch case -- URL is just the graph API base, individual request URLs are serialized
                // as relative_url parameters within each batch entry.
                url = new URL(ServerProtocol.GRAPH_URL);
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
     * Executes a single request and returns the response.
     * 
     * @param request
     *            the Request to execute
     * 
     * @return the Response object representing the results of the request
     */
    public static Response execute(Request request) {
        List<Response> responses = executeBatch(request);

        if (responses == null || responses.size() != 1) {
            throw new FacebookException("invalid state: expected a single response");
        }

        return responses.get(0);
    }

    /**
     * Executes requests as a single batch and returns the responses.
     * 
     * @param requests
     *            the Requests to execute
     * 
     * @return a list of Response objects representing the results of the requests; responses are returned in the same
     *         order as the requests were specified.
     */
    public static List<Response> executeBatch(Request... requests) {
        Validate.notNull(requests, "requests");

        return executeBatch(Arrays.asList(requests));
    }

    /**
     * Executes requests as a single batch and returns the responses.
     *
     * @param requests
     *            the Requests to execute
     *
     * @return a list of Response objects representing the results of the requests; responses are returned in the same
     *         order as the requests were specified.
     */
    public static List<Response> executeBatch(Collection<Request> requests) {
        return executeBatch(new RequestBatch(requests));
    }

    /**
     * Executes requests as a single batch and returns the responses.
     *
     * @param requests
     *            the batch of Requests to execute
     *
     * @return a list of Response objects representing the results of the requests; responses are returned in the same
     *         order as the requests were specified.
     */
    public static List<Response> executeBatch(RequestBatch requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        HttpURLConnection connection = toHttpConnection(requests);
        List<Response> responses = executeConnection(connection, requests);

        return responses;
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately, and the requests will
     * be processed on a separate thread. In order to process results of a request, or determine whether a request
     * succeeded or failed, a callback must be specified (see the {@link #setCallback(Callback) setCallback} method).
     * 
     * @param requests
     *            the Requests to execute
     */
    public static void executeBatchAsync(Request... requests) {
        Validate.notNull(requests, "requests");

        executeBatchAsync(Arrays.asList(requests));
    }

    /**
     * Executes requests as a single batch asynchronously. This function will return immediately, and the requests will
     * be processed on a separate thread. In order to process results of a request, or determine whether a request
     * succeeded or failed, a callback must be specified (see the {@link #setCallback(Callback) setCallback} method).
     * 
     * @param requests
     *            the Requests to execute
     */
    public static void executeBatchAsync(Collection<Request> requests) {
        executeBatchAsync(new RequestBatch(requests));
    }


    /**
     * Executes requests as a single batch asynchronously. This function will return immediately, and the requests will
     * be processed on a separate thread. In order to process results of a request, or determine whether a request
     * succeeded or failed, a callback must be specified (see the {@link #setCallback(Callback) setCallback} method).
     *
     * @param requests
     *            the RequestBatch to execute
     */
    public static void executeBatchAsync(RequestBatch requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        RequestAsyncTask asyncTask = new RequestAsyncTask(requests);
        asyncTask.execute();
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation is done that the
     * contents of the connection actually reflect the serialized requests, so it is the caller's responsibility to
     * ensure that it will correctly generate the desired responses.
     * 
     * @param connection
     *            the HttpURLConnection that the requests were serialized into
     * @param requests
     *            the requests represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     */
    public static List<Response> executeConnection(HttpURLConnection connection, Collection<Request> requests) {
        return executeConnection(connection, new RequestBatch(requests));
    }

    /**
     * Executes requests that have already been serialized into an HttpURLConnection. No validation is done that the
     * contents of the connection actually reflect the serialized requests, so it is the caller's responsibility to
     * ensure that it will correctly generate the desired responses.
     *
     * @param connection
     *            the HttpURLConnection that the requests were serialized into
     * @param requests
     *            the RequestBatch represented by the HttpURLConnection
     * @return a list of Responses corresponding to the requests
     */
    public static List<Response> executeConnection(HttpURLConnection connection, RequestBatch requests) {
        List<Response> responses = Response.fromHttpConnection(connection, requests);

        int numRequests = requests.size();
        if (numRequests != responses.size()) {
            throw new FacebookException(String.format("Received %d responses while expecting %d", responses.size(),
                    numRequests));
        }

        runCallbacks(requests, responses);

        // See if any of these sessions needs its token to be extended. We do this after issuing the request so as to
        // reduce network contention.
        HashSet<Session> sessions = new HashSet<Session>();
        for (Request request : requests) {
            if (request.session != null) {
                sessions.add(request.session);
            }
        }
        for (Session session : sessions) {
            session.extendAccessTokenIfNeeded();
        }

        return responses;
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection. No validation is
     * done that the contents of the connection actually reflect the serialized requests, so it is the caller's
     * responsibility to ensure that it will correctly generate the desired responses. This function will return
     * immediately, and the requests will be processed on a separate thread. In order to process results of a request,
     * or determine whether a request succeeded or failed, a callback must be specified (see the
     * {@link #setCallback(Callback) setCallback} method).
     *
     * @param connection
     *            the HttpURLConnection that the requests were serialized into
     * @param requests
     *            the requests represented by the HttpURLConnection
     */
    public static void executeConnectionAsync(HttpURLConnection connection, RequestBatch requests) {
        executeConnectionAsync(null, connection, requests);
    }

    /**
     * Asynchronously executes requests that have already been serialized into an HttpURLConnection. No validation is
     * done that the contents of the connection actually reflect the serialized requests, so it is the caller's
     * responsibility to ensure that it will correctly generate the desired responses. This function will return
     * immediately, and the requests will be processed on a separate thread. In order to process results of a request,
     * or determine whether a request succeeded or failed, a callback must be specified (see the
     * {@link #setCallback(Callback) setCallback} method)
     *
     * @param callbackHandler
     *            a Handler that will be used to post calls to the callback for each request; if null, a Handler will be
     *            instantiated on the calling thread
     * @param connection
     *            the HttpURLConnection that the requests were serialized into
     * @param requests
     *            the requests represented by the HttpURLConnection
     */
    public static void executeConnectionAsync(Handler callbackHandler, HttpURLConnection connection,
            RequestBatch requests) {
        RequestAsyncTask asyncTask = new RequestAsyncTask(connection, requests);
        requests.setCallbackHandler(callbackHandler);
        asyncTask.execute();
    }

    /**
     * Returns a string representation of this Request, useful for debugging.
     * 
     * @return the debugging information
     */
    @Override
    public String toString() {
        return new StringBuilder().append("{Request: ").append(" session: ").append(session).append(", graphPath: ")
                .append(graphPath).append(", graphObject: ").append(graphObject).append(", restMethod: ")
                .append(restMethod).append(", httpMethod: ").append(httpMethod).append(", parameters: ")
                .append(parameters).append("}").toString();
    }

    private static void runCallbacks(RequestBatch requests, List<Response> responses) {
        int numRequests = requests.size();

        // Compile the list of callbacks to call and then run them either on this thread or via the Handler we received
        final ArrayList<Pair<Callback, Response>> callbacks = new ArrayList<Pair<Callback, Response>>();
        for (int i = 0; i < numRequests; ++i) {
            Request request = requests.get(i);
            if (request.callback != null) {
                callbacks.add(new Pair<Callback, Response>(request.callback, responses.get(i)));
            }
        }

        if (callbacks.size() > 0) {
            Runnable runnable = new Runnable() {
                public void run() {
                    for (Pair<Callback, Response> pair : callbacks) {
                        pair.first.onCompleted(pair.second);
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

    static HttpURLConnection createConnection(URL url) throws IOException {
        HttpURLConnection connection;
        connection = (HttpURLConnection) url.openConnection();

        connection.setRequestProperty(USER_AGENT_HEADER, getUserAgent());
        connection.setRequestProperty(CONTENT_TYPE_HEADER, getMimeContentType());

        connection.setChunkedStreamingMode(0);
        return connection;
    }


    private void addCommonParameters() {
        if (this.session != null && !this.parameters.containsKey(ACCESS_TOKEN_PARAM)) {
            String accessToken = this.session.getAccessToken();
            Logger.registerAccessToken(accessToken);
            this.parameters.putString(ACCESS_TOKEN_PARAM, accessToken);
        }
        this.parameters.putString(SDK_PARAM, SDK_ANDROID);
        this.parameters.putString(FORMAT_PARAM, FORMAT_JSON);
    }

    private String appendParametersToBaseUrl(String baseUrl) {
        Uri.Builder uriBuilder = new Uri.Builder().encodedPath(baseUrl);

        Set<String> keys = this.parameters.keySet();
        for (String key : keys) {
            Object value = this.parameters.get(key);

            if (!(value instanceof String)) {
                if (httpMethod.equals(GET_METHOD)) {
                    throw new IllegalArgumentException("Cannot use GET to upload a file.");
                }

                // Skip non-strings. We add them later as attachments.
                continue;
            }
            uriBuilder.appendQueryParameter(key, value.toString());
        }

        return uriBuilder.toString();
    }

    final String getUrlStringForBatchedRequest() throws MalformedURLException {
        String baseUrl = null;
        if (this.restMethod != null) {
            baseUrl = ServerProtocol.BATCHED_REST_METHOD_URL_BASE + this.restMethod;
        } else {
            baseUrl = this.graphPath;
        }

        addCommonParameters();
        // We don't convert to a URL because it may only be part of a URL.
        return appendParametersToBaseUrl(baseUrl);
    }

    final URL getUrlForSingleRequest() throws MalformedURLException {
        String baseUrl = null;
        if (this.restMethod != null) {
            baseUrl = ServerProtocol.REST_URL_BASE + this.restMethod;
        } else {
            baseUrl = ServerProtocol.GRAPH_URL_BASE + this.graphPath;
        }

        addCommonParameters();
        return new URL(appendParametersToBaseUrl(baseUrl));
    }

    private void serializeToBatch(JSONArray batch, Bundle attachments) throws JSONException, IOException {
        JSONObject batchEntry = new JSONObject();

        if (this.batchEntryName != null) {
            batchEntry.put(BATCH_ENTRY_NAME_PARAM, this.batchEntryName);
        }

        String relativeURL = getUrlStringForBatchedRequest();
        batchEntry.put(BATCH_RELATIVE_URL_PARAM, relativeURL);
        batchEntry.put(BATCH_METHOD_PARAM, httpMethod);
        if (this.session != null) {
            String accessToken = this.session.getAccessToken();
            Logger.registerAccessToken(accessToken);
        }

        // Find all of our attachments. Remember their names and put them in the attachment map.
        ArrayList<String> attachmentNames = new ArrayList<String>();
        Set<String> keys = this.parameters.keySet();
        for (String key : keys) {
            Object value = this.parameters.get(key);
            if (Serializer.isSupportedAttachmentType(value)) {
                // Make the name unique across this entire batch.
                String name = String.format("%s%d", ATTACHMENT_FILENAME_PREFIX, attachments.size());
                attachmentNames.add(name);
                Utility.putObjectInBundle(attachments, name, value);
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
                    keysAndValues.add(String.format("%s=%s", key, URLEncoder.encode(value, "UTF-8")));
                }
            });
            String bodyValue = TextUtils.join("&", keysAndValues);
            batchEntry.put(BATCH_BODY_PARAM, bodyValue);
        }

        batch.put(batchEntry);
    }

    private void validate() {
        if (graphPath != null && restMethod != null) {
            throw new IllegalArgumentException("Only one of a graph path or REST method may be specified per request.");
        }
    }

    final static void serializeToUrlConnection(List<Request> requests, HttpURLConnection connection)
    throws IOException, JSONException {
        Logger logger = new Logger(LoggingBehaviors.REQUESTS, "Request");

        int numRequests = requests.size();

        String connectionHttpMethod = (numRequests == 1) ? requests.get(0).httpMethod : POST_METHOD;
        connection.setRequestMethod(connectionHttpMethod);

        URL url = connection.getURL();
        logger.append("Request:\n"); // TODO port: serial number
        logger.appendKeyValue("URL", url);
        logger.appendKeyValue("Method", connection.getRequestMethod());
        logger.appendKeyValue("User-Agent", connection.getRequestProperty("User-Agent"));
        logger.appendKeyValue("Content-Type", connection.getRequestProperty("Content-Type"));

        // If we have a single non-POST request, don't try to serialize anything or HttpURLConnection will
        // turn it into a POST.
        boolean isPost = connectionHttpMethod.equals(POST_METHOD);
        if (!isPost) {
            logger.log();
            return;
        }

        connection.setDoOutput(true);

        OutputStream outputStream = new BufferedOutputStream(connection.getOutputStream());
        try {
            Serializer serializer = new Serializer(outputStream, logger);

            if (numRequests == 1) {
                Request request = requests.get(0);

                logger.append("  Parameters:\n");
                serializeParameters(request.parameters, serializer);

                logger.append("  Attachments:\n");
                serializeAttachments(request.parameters, serializer);

                if (request.graphObject != null) {
                    processGraphObject(request.graphObject, url.getPath(), serializer);
                }
            } else {
                String batchAppID = getBatchAppId(requests);
                if (Utility.isNullOrEmpty(batchAppID)) {
                    throw new FacebookException("At least one request in a batch must have an open Session, or a "
                            + "default app ID must be specified.");
                }

                serializer.writeString(BATCH_APP_ID_PARAM, batchAppID);

                // We write out all the requests as JSON, remembering which file attachments they have, then
                // write out the attachments.
                Bundle attachments = new Bundle();
                serializeRequestsAsJSON(serializer, requests, attachments);

                logger.append("  Attachments:\n");
                serializeAttachments(attachments, serializer);
            }
        } finally {
            outputStream.close();
        }

        logger.log();
    }

    private static void processGraphObject(GraphObject graphObject, String path, KeyValueSerializer serializer)
            throws IOException {
        // In general, graph objects are passed by reference (ID/URL). But if this is an OG Action,
        // we need to pass the entire values of the contents of the 'image' property, as they
        // contain important metadata beyond just a URL. We don't have a 100% foolproof way of knowing
        // if we are posting an OG Action, given that batched requests can have parameter substitution,
        // but passing the OG Action type as a substituted parameter is unlikely.
        // It looks like an OG Action if it's posted to me/namespace:action[?other=stuff].
        boolean isOGAction = false;
        if (path.startsWith("me/") || path.startsWith("/me/")) {
            int colonLocation = path.indexOf(":");
            int questionMarkLocation = path.indexOf("?");
            isOGAction = colonLocation > 3 && (questionMarkLocation == -1 || colonLocation < questionMarkLocation);
        }

        Set<Entry<String, Object>> entries = graphObject.entrySet();
        for (Entry<String, Object> entry : entries) {
            boolean passByValue = isOGAction && entry.getKey().equalsIgnoreCase("image");
            processGraphObjectProperty(entry.getKey(), entry.getValue(), serializer, passByValue);
        }
    }

    private static void processGraphObjectProperty(String key, Object value, KeyValueSerializer serializer,
            boolean passByValue) throws IOException {
        Class<?> valueClass = value.getClass();
        if (GraphObject.class.isAssignableFrom(valueClass)) {
            value = ((GraphObject) value).getInnerJSONObject();
            valueClass = value.getClass();
        } else if (GraphObjectList.class.isAssignableFrom(valueClass)) {
            value = ((GraphObjectList<?>) value).getInnerJSONArray();
            valueClass = value.getClass();
        }

        if (JSONObject.class.isAssignableFrom(valueClass)) {
            JSONObject jsonObject = (JSONObject) value;
            if (passByValue) {
                // We need to pass all properties of this object in key[propertyName] format.
                @SuppressWarnings("unchecked")
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String propertyName = keys.next();
                    String subKey = String.format("%s[%s]", key, propertyName);
                    processGraphObjectProperty(subKey, jsonObject.opt(propertyName), serializer, passByValue);
                }
            } else {
                // Normal case is passing objects by reference, so just pass the ID or URL, if any, as the value
                // for "key"
                if (jsonObject.has("id")) {
                    processGraphObjectProperty(key, jsonObject.optString("id"), serializer, passByValue);
                } else if (jsonObject.has("url")) {
                    processGraphObjectProperty(key, jsonObject.optString("url"), serializer, passByValue);
                }
            }
        } else if (JSONArray.class.isAssignableFrom(valueClass)) {
            JSONArray jsonArray = (JSONArray) value;
            int length = jsonArray.length();
            for (int i = 0; i < length; ++i) {
                String subKey = String.format("%s[%d]", key, i);
                processGraphObjectProperty(subKey, jsonArray.opt(i), serializer, passByValue);
            }
        } else if (String.class.isAssignableFrom(valueClass) ||
                Number.class.isAssignableFrom(valueClass) ||
                Boolean.class.isAssignableFrom(valueClass)) {
            serializer.writeString(key, value.toString());
        } else if (Date.class.isAssignableFrom(valueClass)) {
            Date date = (Date) value;
            // The "Events Timezone" platform migration affects what date/time formats Facebook accepts and returns.
            // Apps created after 8/1/12 (or apps that have explicitly enabled the migration) should send/receive
            // dates in ISO-8601 format. Pre-migration apps can send as Unix timestamps. Since the future is ISO-8601,
            // that is what we support here. Apps that need pre-migration behavior can explicitly send these as
            // integer timestamps rather than Dates.
            serializer.writeString(key, iso8601DateFormat.format(date));
        }
    }

    private static void serializeParameters(Bundle bundle, Serializer serializer) throws IOException {
        Set<String> keys = bundle.keySet();

        for (String key : keys) {
            Object value = bundle.get(key);
            if (Serializer.isSupportedParameterType(value)) {
                serializer.writeObject(key, value);
            }
        }
    }

    private static void serializeAttachments(Bundle bundle, Serializer serializer) throws IOException {
        Set<String> keys = bundle.keySet();

        for (String key : keys) {
            Object value = bundle.get(key);
            if (Serializer.isSupportedAttachmentType(value)) {
                serializer.writeObject(key, value);
            }
        }
    }

    private static void serializeRequestsAsJSON(Serializer serializer, Collection<Request> requests, Bundle attachments)
            throws JSONException, IOException {
        JSONArray batch = new JSONArray();
        for (Request request : requests) {
            request.serializeToBatch(batch, attachments);
        }

        String batchAsString = batch.toString();
        serializer.writeString(BATCH_PARAM, batchAsString);
    }

    private static String getMimeContentType() {
        return String.format("multipart/form-data; boundary=%s", MIME_BOUNDARY);
    }

    private static String getUserAgent() {
        // TODO port: construct user agent string with version
        return "FBAndroidSDK";
    }

    private static String getBatchAppId(Collection<Request> requests) {
        for (Request request : requests) {
            Session session = request.session;
            if (session != null) {
                return session.getApplicationId();
            }
        }
        return Request.defaultBatchApplicationId;
    }

    private interface KeyValueSerializer {
        void writeString(String key, String value) throws IOException;
    }

    private static class Serializer implements KeyValueSerializer {
        private final OutputStream outputStream;
        private final Logger logger;
        private boolean firstWrite = true;

        public Serializer(OutputStream outputStream, Logger logger) {
            this.outputStream = outputStream;
            this.logger = logger;
        }

        public void writeObject(String key, Object value) throws IOException {
            if (value instanceof String) {
                writeString(key, (String) value);
            } else if (value instanceof Bitmap) {
                writeBitmap(key, (Bitmap) value);
            } else if (value instanceof byte[]) {
                writeBytes(key, (byte[]) value);
            } else {
                throw new IllegalArgumentException("value is not a supported type: String, Bitmap, byte[]");
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
            logger.appendKeyValue("    " + key, "<Image>");
        }

        public void writeBytes(String key, byte[] bytes) throws IOException {
            writeContentDisposition(key, key, "content/unknown");
            this.outputStream.write(bytes);
            writeLine("");
            writeRecordBoundary();
            logger.appendKeyValue("    " + key, String.format("<Data: %d>", bytes.length));
        }

        public void writeRecordBoundary() throws IOException {
            writeLine("--%s", MIME_BOUNDARY);
        }

        public void writeContentDisposition(String name, String filename, String contentType) throws IOException {
            write("Content-Disposition: form-data; name=\"%s\"", name);
            if (filename != null) {
                write("; filename=\"%s\"", filename);
            }
            writeLine(""); // newline after Content-Disposition
            if (contentType != null) {
                writeLine("%s: %s", CONTENT_TYPE_HEADER, contentType);
            }
            writeLine(""); // blank line before content
        }

        public void write(String format, Object... args) throws IOException {
            if (firstWrite) {
                // Prepend all of our output with a boundary string.
                this.outputStream.write("--".getBytes());
                this.outputStream.write(MIME_BOUNDARY.getBytes());
                this.outputStream.write("\r\n".getBytes());
                firstWrite = false;
            }
            this.outputStream.write(String.format(format, args).getBytes());
        }

        public void writeLine(String format, Object... args) throws IOException {
            write(format, args);
            write("\r\n");
        }

        public static boolean isSupportedAttachmentType(Object value) {
            return value instanceof Bitmap || value instanceof byte[];
        }

        public static boolean isSupportedParameterType(Object value) {
            return value instanceof String;
        }

    }

    /**
     * Specifies the interface that consumers of the Request class can implement in order to be notified when a
     * particular request completes, either successfully or with an error.
     */
    public interface Callback {
        /**
         * The method that will be called when a request completes.
         * 
         * @param response
         *            the Response of this request, which may include error information if the request was unsuccessful
         */
        void onCompleted(Response response);
    }
}
