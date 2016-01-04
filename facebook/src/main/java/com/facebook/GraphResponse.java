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

import com.facebook.internal.FacebookRequestErrorClassification;
import com.facebook.internal.Logger;
import com.facebook.internal.Utility;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Encapsulates the response, successful or otherwise, of a call to the Facebook platform.
 */
public class GraphResponse {
    private final HttpURLConnection connection;
    private final JSONObject graphObject;
    private final JSONArray graphObjectArray;
    private final FacebookRequestError error;
    private final String rawResponse;
    private final GraphRequest request;

    /**
     * Property name of non-JSON results in the GraphObject. Certain calls to Facebook result in a
     * non-JSON response (e.g., the string literal "true" or "false"). To present a consistent way
     * of accessing results, these are represented as a GraphObject with a single string property
     * with this name.
     */
    public static final String NON_JSON_RESPONSE_PROPERTY = "FACEBOOK_NON_JSON_RESULT";

    // From v2.1 of the Graph API, write endpoints will now return valid JSON with the result as the
    // value for the "success" key
    public static final String SUCCESS_KEY = "success";

    private static final String CODE_KEY = "code";
    private static final String BODY_KEY = "body";

    private static final String RESPONSE_LOG_TAG = "Response";

    GraphResponse(
            GraphRequest request,
            HttpURLConnection connection,
            String rawResponse,
            JSONObject graphObject) {
        this(request, connection, rawResponse, graphObject, null, null);
    }

    GraphResponse(
            GraphRequest request,
            HttpURLConnection connection,
            String rawResponse,
            JSONArray graphObjects) {
        this(request, connection, rawResponse, null, graphObjects, null);
    }

    GraphResponse(
            GraphRequest request,
            HttpURLConnection connection,
            FacebookRequestError error) {
        this(request, connection, null, null, null, error);
    }

    GraphResponse(
            GraphRequest request,
            HttpURLConnection connection,
            String rawResponse,
            JSONObject graphObject,
            JSONArray graphObjects,
            FacebookRequestError error) {
        this.request = request;
        this.connection = connection;
        this.rawResponse = rawResponse;
        this.graphObject = graphObject;
        this.graphObjectArray = graphObjects;
        this.error = error;
    }

    /**
     * Returns information about any errors that may have occurred during the request.
     *
     * @return the error from the server, or null if there was no server error
     */
    public final FacebookRequestError getError() {
        return error;
    }

    /**
     * The response returned for this request, if it's in object form.
     *
     * @return the returned JSON object, or null if none was returned (or if the result was a JSON
     * array)
     */
    public final JSONObject getJSONObject() {
        return graphObject;
    }


    /**
     * The response returned for this request, if it's in array form.
     *
     * @return the returned JSON array, or null if none was returned (or if the result was a JSON
     * object)
     */
    public final JSONArray getJSONArray() {
        return graphObjectArray;
    }

    /**
     * Returns the HttpURLConnection that this response was generated from. If the response was
     * retrieved from the cache, this will be null.
     *
     * @return the connection, or null
     */
    public final HttpURLConnection getConnection() {
        return connection;
    }

    /**
     * Returns the request that this response is for.
     *
     * @return the request that this response is for
     */
    public GraphRequest getRequest() {
        return request;
    }

    /**
     * Returns the server response as a String that this response is for.
     *
     * @return A String representation of the actual response from the server
     */
    public String getRawResponse() {
        return rawResponse;
    }

    /**
     * Indicates whether paging is being done forward or backward.
     */
    public enum PagingDirection {
        /**
         * Indicates that paging is being performed in the forward direction.
         */
        NEXT,
        /**
         * Indicates that paging is being performed in the backward direction.
         */
        PREVIOUS
    }

    /**
     * If a Response contains results that contain paging information, returns a new
     * Request that will retrieve the next page of results, in whichever direction
     * is desired. If no paging information is available, returns null.
     *
     * @param direction enum indicating whether to page forward or backward
     * @return a Request that will retrieve the next page of results in the desired
     *         direction, or null if no paging information is available
     */
    public GraphRequest getRequestForPagedResults(PagingDirection direction) {
        String link = null;
        if (graphObject != null) {
            JSONObject pagingInfo = graphObject.optJSONObject("paging");
            if (pagingInfo != null) {
                if (direction == PagingDirection.NEXT) {
                    link = pagingInfo.optString("next");
                } else {
                    link = pagingInfo.optString("previous");
                }
            }
        }
        if (Utility.isNullOrEmpty(link)) {
            return null;
        }

        if (link != null && link.equals(request.getUrlForSingleRequest())) {
            // We got the same "next" link as we just tried to retrieve. This could happen if cached
            // data is invalid. All we can do in this case is pretend we have finished.
            return null;
        }

        GraphRequest pagingRequest;
        try {
            pagingRequest = new GraphRequest(request.getAccessToken(), new URL(link));
        } catch (MalformedURLException e) {
            return null;
        }

        return pagingRequest;
    }

    /**
     * Provides a debugging string for this response.
     */
    @Override
    public String toString() {
        String responseCode;
        try {
            responseCode = String.format(
                    Locale.US,
                    "%d",
                    (connection != null) ? connection.getResponseCode() : 200);
        } catch (IOException e) {
            responseCode = "unknown";
        }

        return new StringBuilder()
                .append("{Response: ")
                .append(" responseCode: ")
                .append(responseCode)
                .append(", graphObject: ")
                .append(graphObject)
                .append(", error: ")
                .append(error)
                .append("}")
                .toString();
    }

    @SuppressWarnings("resource")
    static List<GraphResponse> fromHttpConnection(
            HttpURLConnection connection,
            GraphRequestBatch requests) {
        InputStream stream = null;

        try {
            if (connection.getResponseCode() >= 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
            }

            return createResponsesFromStream(stream, connection, requests);
        } catch (FacebookException facebookException) {
            Logger.log(
                    LoggingBehavior.REQUESTS,
                    RESPONSE_LOG_TAG,
                    "Response <Error>: %s",
                    facebookException);
            return constructErrorResponses(requests, connection, facebookException);
        } catch (JSONException exception) {
            Logger.log(
                    LoggingBehavior.REQUESTS,
                    RESPONSE_LOG_TAG,
                    "Response <Error>: %s",
                    exception);
            return constructErrorResponses(requests, connection, new FacebookException(exception));
        } catch (IOException exception) {
            Logger.log(
                    LoggingBehavior.REQUESTS,
                    RESPONSE_LOG_TAG,
                    "Response <Error>: %s",
                    exception);
            return constructErrorResponses(requests, connection, new FacebookException(exception));
        } catch (SecurityException exception) {
            Logger.log(
                    LoggingBehavior.REQUESTS,
                    RESPONSE_LOG_TAG,
                    "Response <Error>: %s",
                    exception);
            return constructErrorResponses(requests, connection, new FacebookException(exception));
        } finally {
            Utility.closeQuietly(stream);
        }
    }

    static List<GraphResponse> createResponsesFromStream(
            InputStream stream,
            HttpURLConnection connection,
            GraphRequestBatch requests
    ) throws FacebookException, JSONException, IOException {

        String responseString = Utility.readStreamToString(stream);
        Logger.log(LoggingBehavior.INCLUDE_RAW_RESPONSES, RESPONSE_LOG_TAG,
                "Response (raw)\n  Size: %d\n  Response:\n%s\n", responseString.length(),
                responseString);

        return createResponsesFromString(responseString, connection, requests);
    }

    static List<GraphResponse> createResponsesFromString(
            String responseString,
            HttpURLConnection connection,
            GraphRequestBatch requests
    ) throws FacebookException, JSONException, IOException {
        JSONTokener tokener = new JSONTokener(responseString);
        Object resultObject = tokener.nextValue();

        List<GraphResponse> responses = createResponsesFromObject(
                connection,
                requests,
                resultObject);
        Logger.log(
                LoggingBehavior.REQUESTS,
                RESPONSE_LOG_TAG,
                "Response\n  Id: %s\n  Size: %d\n  Responses:\n%s\n",
                requests.getId(),
                responseString.length(),
                responses);

        return responses;
    }

    private static List<GraphResponse> createResponsesFromObject(
            HttpURLConnection connection,
            List<GraphRequest> requests,
            Object object
    ) throws FacebookException, JSONException {
        int numRequests = requests.size();
        List<GraphResponse> responses = new ArrayList<GraphResponse>(numRequests);
        Object originalResult = object;

        if (numRequests == 1) {
            GraphRequest request = requests.get(0);
            try {
                // Single request case -- the entire response is the result, wrap it as "body" so we
                // can handle it the same as we do in the batched case. We get the response code
                // from the actual HTTP response, as opposed to the batched case where it is
                // returned as a "code" element.
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(BODY_KEY, object);
                int responseCode = (connection != null) ? connection.getResponseCode() : 200;
                jsonObject.put(CODE_KEY, responseCode);

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonObject);

                // Pretend we got an array of 1 back.
                object = jsonArray;
            } catch (JSONException e) {
                responses.add(
                        new GraphResponse(
                                request,
                                connection,
                                new FacebookRequestError(connection, e)));
            } catch (IOException e) {
                responses.add(
                        new GraphResponse(
                                request,
                                connection,
                                new FacebookRequestError(connection, e)));
            }
        }

        if (!(object instanceof JSONArray) || ((JSONArray) object).length() != numRequests) {
            FacebookException exception = new FacebookException("Unexpected number of results");
            throw exception;
        }

        JSONArray jsonArray = (JSONArray) object;

        for (int i = 0; i < jsonArray.length(); ++i) {
            GraphRequest request = requests.get(i);
            try {
                Object obj = jsonArray.get(i);
                responses.add(
                        createResponseFromObject(
                                request,
                                connection,
                                obj,
                                originalResult));
            } catch (JSONException e) {
                responses.add(
                        new GraphResponse(
                                request,
                                connection,
                                new FacebookRequestError(connection, e)));
            } catch (FacebookException e) {
                responses.add(
                        new GraphResponse(
                                request,
                                connection,
                                new FacebookRequestError(connection, e)));
            }
        }

        return responses;
    }

    private static GraphResponse createResponseFromObject(
            GraphRequest request,
            HttpURLConnection connection,
            Object object,
            Object originalResult
    ) throws JSONException {
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;

            FacebookRequestError error =
                    FacebookRequestError.checkResponseAndCreateError(
                            jsonObject,
                            originalResult,
                            connection);
            if (error != null) {
                if (error.getErrorCode() == FacebookRequestErrorClassification.EC_INVALID_TOKEN
                        && Utility.isCurrentAccessToken(request.getAccessToken())) {
                    AccessToken.setCurrentAccessToken(null);
                }
                return new GraphResponse(request, connection, error);
            }

            Object body = Utility.getStringPropertyAsJSON(
                    jsonObject,
                    BODY_KEY,
                    NON_JSON_RESPONSE_PROPERTY);

            if (body instanceof JSONObject) {
                return new GraphResponse(request, connection, body.toString(), (JSONObject)body);
            } else if (body instanceof JSONArray) {
                return new GraphResponse(request, connection, body.toString(), (JSONArray)body);
            }
            // We didn't get a body we understand how to handle, so pretend we got nothing.
            object = JSONObject.NULL;
        }

        if (object == JSONObject.NULL) {
            return new GraphResponse(request, connection, object.toString(), (JSONObject)null);
        } else {
            throw new FacebookException("Got unexpected object type in response, class: "
                    + object.getClass().getSimpleName());
        }
    }

    static List<GraphResponse> constructErrorResponses(
            List<GraphRequest> requests,
            HttpURLConnection connection,
            FacebookException error) {
        int count = requests.size();
        List<GraphResponse> responses = new ArrayList<GraphResponse>(count);
        for (int i = 0; i < count; ++i) {
            GraphResponse response = new GraphResponse(
                    requests.get(i),
                    connection,
                    new FacebookRequestError(connection, error));
            responses.add(response);
        }
        return responses;
    }
}
