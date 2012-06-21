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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

public class Response {
    private HttpURLConnection connection;
    private GraphObject graphObject;
    private Exception error;

    private Response(HttpURLConnection connection, GraphObject graphObject, Exception error) {
        this.connection = connection;
        this.graphObject = graphObject;
        this.error = error;
    }

    public final Exception getError() {
        return this.error;
    }

    // TODO port: what about arrays of GraphObject?
    public final GraphObject getGraphObject() {
        return this.graphObject;
    }

    public final HttpURLConnection getConnection() {
        return this.connection;
    }

    static List<Response> fromHttpConnection(RequestContext context, HttpURLConnection connection,
            List<Request> requests) {

        String responseString = null;
        try {
            responseString = readResponseToString(connection);
        } catch (IOException exception) {
            // All responses get this exception.
            return constructErrorResponses(connection, requests.size(), exception);
        }

        Object resultObject = null;
        JSONTokener tokener = new JSONTokener(responseString);
        try {
            resultObject = tokener.nextValue();
        } catch (JSONException exception) {
            // TODO port: handle 'true' and 'false' by turning into dictionary; other failures are more fatal.
            return constructErrorResponses(connection, requests.size(), exception);
        }

        // TODO port: skip connection-related errors in cache case

        List<Response> responses = new ArrayList<Response>(requests.size());
        if (requests.size() == 1) {
            // Single request case -- the entire response is the result, wrap it as "body"
            // TODO port: convert to graph object
            // TODO port: handle getting a JSONArray here
            GraphObject graphObject = GraphObjectWrapper.wrapJson((JSONObject) resultObject);
            responses.add(new Response(connection, graphObject, null));
        } else {
            // TODO port: handle the batch case
        }

        return responses;
    }

    private static List<Response> constructErrorResponses(HttpURLConnection connection, int count, Exception error) {
        List<Response> responses = new ArrayList<Response>(count);
        for (int i = 0; i < count; ++i) {
            Response response = new Response(connection, null, error);
            responses.add(response);
        }
        return responses;
    }

    private static String readResponseToString(HttpURLConnection connection) throws IOException {
        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
        InputStreamReader reader = new InputStreamReader(inputStream);
        StringBuilder stringBuilder = new StringBuilder();

        final int bufferSize = 1024 * 2;
        char[] buffer = new char[bufferSize];
        int n = 0;
        while (-1 != (n = reader.read(buffer))) {
            stringBuilder.append(buffer, 0, n);
        }
        reader.close();
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        // TODO port: debugging output
        return null;
    }
}
