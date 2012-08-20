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

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the response, successful or otherwise, of a call to the Facebook platform.
 */
public class Response {
    private final HttpURLConnection connection;
    private final GraphObject graphObject;
    private final GraphObjectList<GraphObject> graphObjectList;
    private final boolean isFromCache;
    private final FacebookException error;

    /**
     * Property name of non-JSON results in the GraphObject. Certain calls to Facebook result in a non-JSON response
     * (e.g., the string literal "true" or "false"). To present a consistent way of accessing results, these are
     * represented as a GraphObject with a single string property with this name.
     */
    public static final String NON_JSON_RESPONSE_PROPERTY = "FACEBOOK_NON_JSON_RESULT";

    private static final String CODE_KEY = "code";
    private static final String BODY_KEY = "body";
    private static final String ERROR_KEY = "error";
    private static final String ERROR_TYPE_FIELD_KEY = "type";
    private static final String ERROR_CODE_FIELD_KEY = "code";
    private static final String ERROR_MESSAGE_FIELD_KEY = "message";
    private static final String ERROR_CODE_KEY = "error_code";
    private static final String ERROR_MSG_KEY = "error_msg";
    private static final String ERROR_REASON_KEY = "error_reason";

    private static final String RESPONSE_LOG_TAG = "Response";

    private static final String RESPONSE_CACHE_TAG = "ResponseCache";
    private static FileLruCache responseCache;

    private Response(HttpURLConnection connection, GraphObject graphObject, GraphObjectList<GraphObject> graphObjects, boolean isFromCache) {
        if (graphObject != null && graphObjects != null) {
            throw new FacebookException("Expected either a graphObject or multiple graphObjects, but not both.");
        }
        this.connection = connection;
        this.graphObject = graphObject;
        this.graphObjectList = graphObjects;
        this.isFromCache = isFromCache;
        this.error = null;
    }

    private Response(HttpURLConnection connection, FacebookException error) {
        this.connection = connection;
        this.graphObject = null;
        this.graphObjectList = null;
        this.isFromCache = false;
        this.error = error;
    }

    /**
     * Returns the error returned for this request, if any.
     * 
     * @return the error encountered, or null if the request succeeded
     */
    public final FacebookException getError() {
        return this.error;
    }

    /**
     * The single graph object returned for this request, if any.
     * 
     * @return the graph object returned, or null if none was returned (or if the result was a list)
     */
    public final GraphObject getGraphObject() {
        return this.graphObject;
    }

    /**
     * The single graph object returned for this request, if any, cast into a particular type of GraphObject.
     * 
     * @param graphObjectClass
     *            the GraphObject-derived interface to cast the graph object into
     * @return the graph object returned, or null if none was returned (or if the result was a list)
     */
    public final <T extends GraphObject> T getGraphObjectAs(Class<T> graphObjectClass) {
        if (this.graphObject == null) {
            return null;
        }
        return this.graphObject.cast(graphObjectClass);
    }

    /**
     * The list of graph objects returned for this request, if any.
     * 
     * @return the list of graph objects returned, or null if none was returned (or if the result was not a list)
     */
    public final GraphObjectList<GraphObject> getGraphObjectList() {
        return this.graphObjectList;
    }

    /**
     * The list of graph objects returned for this request, if any, cast into a particular type of GraphObject.
     * 
     * @param graphObjectClass
     *            the GraphObject-derived interface to cast the graph objects into
     * @return the list of graph objects returned, or null if none was returned (or if the result was not a list)
     */
    public final <T extends GraphObject> GraphObjectList<T> getGraphObjectListAs(Class<T> graphObjectClass) {
        if (this.graphObjectList == null) {
            return null;
        }
        return this.graphObjectList.castToListOf(graphObjectClass);
    }

    /**
     * Returns the HttpURLConnection that this response was built from.
     * 
     * @return the connection
     */
    public final HttpURLConnection getConnection() {
        return this.connection;
    }

    /**
     * Provides a debugging string for this response.
     */
    @Override
    public String toString() {
        String responseCode;
        try {
            responseCode = String.format("%d", this.connection.getResponseCode());
        } catch (IOException e) {
            responseCode = "unknown";
        }

        return new StringBuilder().append("{Response: ").append(" responseCode: ").append(responseCode)
                .append(", graphObject: ").append(this.graphObject).append(", error: ").append(this.error).append("}")
                .toString();
    }

    final boolean getIsFromCache() {
        return isFromCache;
    }

    static FileLruCache getResponseCache() {
        if (responseCache == null) {
            Context applicationContext = Session.getApplicationContext();
            if (applicationContext != null) {
                responseCache = new FileLruCache(applicationContext, RESPONSE_CACHE_TAG, new FileLruCache.Limits());
            }
        }

        return responseCache;
    }

    static List<Response> fromHttpConnection(HttpURLConnection connection, RequestBatch requests) {
        InputStream stream = null;

        // Try loading from cache.  If that fails, load from the network.
        FileLruCache cache = getResponseCache();
        String cacheKey = requests.getCacheKey();
        if (!requests.getForceRoundTrip() && (cache != null) && (cacheKey != null)) {
            try {
                stream = cache.get(cacheKey);
                if (stream != null) {
                    return createResponsesFromStream(stream, connection, requests, true);
                }
            } catch (FacebookException exception) { // retry via roundtrip below
            } catch (JSONException exception) {
            } catch (IOException exception) {
            } finally {
                Utility.closeQuietly(stream);
            }
        }

        // Load from the network, and cache the result if not an error.
        try {
            if (connection.getResponseCode() >= 400) {
                stream = connection.getErrorStream();
            } else {
                stream = connection.getInputStream();
                if ((cache != null) && (cacheKey != null) && (stream != null)) {
                    InputStream interceptStream = cache.interceptAndPut(cacheKey, stream);
                    if (interceptStream != null) {
                        stream = interceptStream;
                    }
                }
            }

            return createResponsesFromStream(stream, connection, requests, false);
        } catch (FacebookException facebookException) {
            Logger.log(LoggingBehaviors.REQUESTS, RESPONSE_LOG_TAG, "Response <Error>: %s", facebookException);
            return constructErrorResponses(connection, requests.size(), facebookException);
        } catch (JSONException exception) {
            Logger.log(LoggingBehaviors.REQUESTS, RESPONSE_LOG_TAG, "Response <Error>: %s", exception);
            return constructErrorResponses(connection, requests.size(), new FacebookException(exception));
        } catch (IOException exception) {
            Logger.log(LoggingBehaviors.REQUESTS, RESPONSE_LOG_TAG, "Response <Error>: %s", exception);
            return constructErrorResponses(connection, requests.size(), new FacebookException(exception));
        } finally {
            Utility.closeQuietly(stream);
        }
    }

    static List<Response> createResponsesFromStream(InputStream stream, HttpURLConnection connection,
            RequestBatch requests, boolean isFromCache) throws FacebookException, JSONException, IOException {
        String responseString = Utility.readStreamToString(stream);
        Logger.log(LoggingBehaviors.INCLUDE_RAW_RESPONSES, RESPONSE_LOG_TAG,
                "Response (raw)\n  Size: %d\n  Response:\n%s\n", responseString.length(),
                responseString);

        JSONTokener tokener = new JSONTokener(responseString);
        Object resultObject = tokener.nextValue();
        List<Response> responses = createResponsesFromObject(connection, requests, resultObject, isFromCache);
        Logger.log(LoggingBehaviors.REQUESTS, RESPONSE_LOG_TAG, "Response\n  Size: %d\n  Responses:\n%s\n",
                responseString.length(), responses);

        return responses;
    }

    private static List<Response> createResponsesFromObject(HttpURLConnection connection, List<Request> requests,
            Object object, boolean isFromCache) throws FacebookException, JSONException {
        int numRequests = requests.size();
        List<Response> responses = new ArrayList<Response>(numRequests);
        if (numRequests == 1) {
            try {
                // Single request case -- the entire response is the result, wrap it as "body" so we can handle it
                // the same as we do in the batched case. We get the response code from the actual HTTP response,
                // as opposed to the batched case where it is returned as a "code" element.
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(BODY_KEY, object);
                jsonObject.put(CODE_KEY, connection.getResponseCode());

                JSONArray jsonArray = new JSONArray();
                jsonArray.put(jsonObject);

                // Pretend we got an array of 1 back.
                object = jsonArray;
            } catch (JSONException e) {
                responses.add(new Response(connection, new FacebookException(e)));
            } catch (IOException e) {
                responses.add(new Response(connection, new FacebookException(e)));
            }
        }

        if (!(object instanceof JSONArray) || ((JSONArray) object).length() != numRequests) {
            FacebookException exception = new FacebookException("TODO unexpected number of results");
            throw exception;
        }

        JSONArray jsonArray = (JSONArray) object;
        for (int i = 0; i < jsonArray.length(); ++i) {
            try {
                Object obj = jsonArray.get(i);
                responses.add(createResponseFromObject(connection, obj, isFromCache));
            } catch (JSONException e) {
                responses.add(new Response(connection, new FacebookException(e)));
            } catch (FacebookException e) {
                responses.add(new Response(connection, e));
            }
        }

        return responses;
    }

    private static Response createResponseFromObject(HttpURLConnection connection, Object object, boolean isFromCache) throws JSONException {
        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;

            FacebookException exception = checkResponseAndCreateException(jsonObject);
            if (exception != null) {
                throw exception;
            }

            Object body = Utility.getStringPropertyAsJSON(jsonObject, BODY_KEY, NON_JSON_RESPONSE_PROPERTY);

            GraphObject graphObject = null;
            GraphObjectList<GraphObject> graphObjectList = null;
            if (body instanceof JSONObject) {
                graphObject = GraphObjectWrapper.createGraphObject((JSONObject) body);
            } else if (body instanceof JSONArray) {
                graphObjectList = GraphObjectWrapper.wrapArray((JSONArray) body, GraphObject.class);
            }
            return new Response(connection, graphObject, graphObjectList, isFromCache);
        } else if (object == JSONObject.NULL) {
            return new Response(connection, null, null, isFromCache);
        } else {
            throw new FacebookException("Got unexpected object type in response, class: "
                    + object.getClass().getSimpleName());
        }
    }

    private static FacebookServiceErrorException checkResponseAndCreateException(JSONObject jsonObject) {
        try {
            if (jsonObject.has(CODE_KEY)) {
                int responseCode = jsonObject.getInt(CODE_KEY);
                Object body = Utility.getStringPropertyAsJSON(jsonObject, BODY_KEY, NON_JSON_RESPONSE_PROPERTY);

                if (body != null && body instanceof JSONObject) {
                    JSONObject jsonBody = (JSONObject) body;
                    // Does this response represent an error from the service? We might get either an "error"
                    // with several sub-properties, or else one or more top-level fields containing error info.
                    String errorType = null;
                    String errorMessage = null;
                    int errorCode = -1;

                    boolean hasError = false;
                    if (jsonBody.has(ERROR_KEY)) {
                        // We assume the error object is correctly formatted.
                        JSONObject error = (JSONObject) Utility.getStringPropertyAsJSON(jsonBody, ERROR_KEY, null);

                        errorType = error.optString(ERROR_TYPE_FIELD_KEY, null);
                        errorMessage = error.optString(ERROR_MESSAGE_FIELD_KEY, null);
                        errorCode = error.optInt(ERROR_CODE_FIELD_KEY, -1);
                        hasError = true;
                    } else if (jsonBody.has(ERROR_CODE_KEY) || jsonBody.has(ERROR_MSG_KEY)
                            || jsonBody.has(ERROR_REASON_KEY)) {
                        errorType = jsonBody.optString(ERROR_REASON_KEY, null);
                        errorMessage = jsonBody.optString(ERROR_MSG_KEY, null);
                        errorCode = jsonBody.optInt(ERROR_CODE_KEY, -1);
                        hasError = true;
                    }

                    if (hasError) {
                        return new FacebookServiceErrorException(responseCode, errorCode, errorType, errorMessage,
                                jsonBody);
                    }
                }

                // If we didn't get error details, but we did get a failure response code, report it.
                if (responseCode < 200 || responseCode >= 300) {
                    return new FacebookServiceErrorException(responseCode);
                }
            }
        } catch (JSONException e) {
        }

        return null;
    }

    private static List<Response> constructErrorResponses(HttpURLConnection connection, int count,
            FacebookException error) {
        List<Response> responses = new ArrayList<Response>(count);
        for (int i = 0; i < count; ++i) {
            Response response = new Response(connection, error);
            responses.add(response);
        }
        return responses;
    }
}
