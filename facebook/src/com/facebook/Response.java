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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Response {
    private final HttpURLConnection connection;
    private final GraphObject graphObject;
    private final GraphObjectList<GraphObject> graphObjectList;
    private final FacebookException error;

    public static final String NON_JSON_RESPONSE_PROPERTY = "FacebookSDK_NON_JSON_RESULT";

    private static final String[] ERROR_KEYS = new String[] { "error", "error_code", "error_msg", "error_reason", };
    private static final String CODE_KEY = "code";
    private static final String BODY_KEY = "body";
    private static final String ERROR_TYPE_KEY = "type";
    private static final String ERROR_CODE_KEY = "code";
    private static final String ERROR_MESSAGE_KEY = "message";

    private Response(HttpURLConnection connection, GraphObject graphObject, GraphObjectList<GraphObject> graphObjects) {
        if (graphObject != null && graphObjects != null) {
            throw new FacebookException("Expected either a graphObject or multiple graphObjects, but not both.");
        }
        this.connection = connection;
        this.graphObject = graphObject;
        this.graphObjectList = graphObjects;
        this.error = null;
    }

    private Response(HttpURLConnection connection, FacebookException error) {
        this.connection = connection;
        this.graphObject = null;
        this.graphObjectList = null;
        this.error = error;
    }

    public final FacebookException getError() {
        return this.error;
    }

    public final GraphObject getGraphObject() {
        return this.graphObject;
    }

    public final <T extends GraphObject> T getGraphObjectAs(Class<T> graphObjectClass) {
        if (this.graphObject == null) {
            return null;
        }
        return this.graphObject.cast(graphObjectClass);
    }

    public final GraphObjectList<GraphObject> getGraphObjectList() {
        return this.graphObjectList;
    }

    public final <T extends GraphObject> GraphObjectList<T> getGraphObjectListAs(Class<T> graphObjectClass) {
        if (this.graphObjectList == null) {
            return null;
        }
        return this.graphObjectList.castToListOf(graphObjectClass);
    }

    public final HttpURLConnection getConnection() {
        return this.connection;
    }

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

    static List<Response> fromHttpConnection(RequestContext context, HttpURLConnection connection,
            List<Request> requests) {
        Logger logger = new Logger(LoggingBehaviors.REQUESTS, "Response");
        Logger rawResponseLogger = new Logger(LoggingBehaviors.INCLUDE_RAW_RESPONSES, "Response");

        try {
            String responseString = readHttpResponseToString(connection);

            Object resultObject = null;
            JSONTokener tokener = new JSONTokener(responseString);
            rawResponseLogger.append("Response (raw)\n  Size: %d\n  Response:\n%s\n", responseString.length(),
                    responseString);
            try {
                resultObject = tokener.nextValue();
            } catch (JSONException exception) {
                throw exception;
            }

            // TODO port: skip connection-related errors in cache case

            List<Response> responses = createResponsesFromObject(connection, requests, resultObject);

            logger.append("Response\n  Size: %d\n  Responses:\n%s\n", responseString.length(), responses);
            return responses;
        } catch (FacebookException facebookException) {
            logger.append("Response <Error>: %s", facebookException);
            return constructErrorResponses(connection, requests.size(), facebookException);
        } catch (JSONException exception) {
            logger.append("Response <Error>: %s", exception);
            FacebookException facebookException = new FacebookException(exception);
            return constructErrorResponses(connection, requests.size(), facebookException);
        } catch (IOException exception) {
            logger.append("Response <Error>: %s", exception);
            FacebookException facebookException = new FacebookException(exception);
            return constructErrorResponses(connection, requests.size(), facebookException);
        } finally {
            logger.log();
            rawResponseLogger.log();
        }
    }

    private static String readHttpResponseToString(HttpURLConnection connection) throws IOException, JSONException,
            FacebookServiceErrorException {
        String responseString = null;
        InputStream responseStream = null;
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                responseStream = connection.getErrorStream();
                responseString = Utility.readStreamToString(responseStream);
            } else {
                responseStream = connection.getInputStream();
                responseString = Utility.readStreamToString(responseStream);
            }
        } finally {
            Utility.closeQuietly(responseStream);
        }
        return responseString;
    }

    private static FacebookServiceErrorException checkResponseAndCreateException(JSONObject jsonObject) {
        try {
            if (jsonObject.has(CODE_KEY)) {
                int responseCode = jsonObject.getInt(CODE_KEY);
                if (responseCode < 200 || responseCode >= 300) {
                    Object body = Utility.getStringPropertyAsJSON(jsonObject, BODY_KEY, NON_JSON_RESPONSE_PROPERTY);

                    if (body != null && body instanceof JSONObject) {
                        JSONObject jsonBody = (JSONObject) body;
                        // Does this response represent an error from the service?
                        for (String errorKey : ERROR_KEYS) {
                            if (jsonBody.has(errorKey)) {
                                // We assume the error object is correctly formatted.
                                JSONObject error = (JSONObject) Utility.getStringPropertyAsJSON(jsonBody, errorKey,
                                        null);

                                String errorType = error.optString(ERROR_TYPE_KEY);
                                String errorMessage = error.optString(ERROR_MESSAGE_KEY);
                                int errorCode = error.optInt(ERROR_CODE_KEY, -1);

                                return new FacebookServiceErrorException(responseCode, errorCode, errorType,
                                        errorMessage, jsonBody);
                            }
                        }
                    }
                    return new FacebookServiceErrorException(responseCode);
                }
            }
        } catch (JSONException e) {
        }

        return null;
    }

    private static List<Response> createResponsesFromObject(HttpURLConnection connection, List<Request> requests,
            Object object) throws FacebookException, JSONException {
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
                responses.add(createResponseFromObject(connection, obj));
            } catch (JSONException e) {
                responses.add(new Response(connection, new FacebookException(e)));
            } catch (FacebookException e) {
                responses.add(new Response(connection, e));
            }
        }

        return responses;
    }

    private static Response createResponseFromObject(HttpURLConnection connection, Object object) throws JSONException {
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
                graphObject = GraphObjectWrapper.wrapJson((JSONObject) body);
            } else if (body instanceof JSONArray) {
                graphObjectList = GraphObjectWrapper.wrapArray((JSONArray) body, GraphObject.class);
            }
            return new Response(connection, graphObject, graphObjectList);
        } else if (object == JSONObject.NULL) {
            return new Response(connection, null, null);
        } else {
            throw new FacebookException("Got unexpected object type in response, class: "
                    + object.getClass().getSimpleName());
        }
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
