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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

public class Request {

    private Session session;
    private String httpMethod;
    private String graphPath;
    private Object graphObject;
    private String restMethod;
    // TODO (clang) is Bundle the appropriate data type to use for storing, e.g., parameters internally?
    private Bundle parameters;

    // URL components
    private static final String FACEBOOK_COM = "facebook.com";
    private static final String GRAPH_URL_BASE = "https://graph." + FACEBOOK_COM + "/";
    private static final String REST_URL_BASE = "https://api." + FACEBOOK_COM + "/method/";

    // Graph paths
    public static final String ME = "me";
    public static final String MY_FRIENDS = "me/friends";
    public static final String MY_PHOTOS = "me/photos";
    public static final String SEARCH = "search";

    // HTTP methods
    public static final String GET_METHOD = HttpGet.METHOD_NAME;
    public static final String POST_METHOD = HttpPost.METHOD_NAME;

    // Parameter names/values
    private static final String PICTURE_PARAM = "picture";
    private static final String FORMAT_PARAM = "format";
    private static final String FORMAT_JSON = "json";
    private static final String SDK_PARAM = "sdk";
    private static final String SDK_ANDROID = "android";
    private static final String ACCESS_TOKEN_PARAM = "access_token";

    public Request() {
        this(null, null, null, null);
    }

    public Request(Session session, String graphPath) {
        this(session, graphPath, null, null);
    }

    public Request(Session session, String graphPath, Bundle parameters, String httpMethod) {
        this.session = session;
        this.graphPath = graphPath;

        // This handles the null case by using the default.
        setHttpMethod(httpMethod);

        // TODO (clang) parameters -- store as Bundle or collection type?
        if (parameters != null) {
            this.parameters = new Bundle(parameters);
        } else {
            this.parameters = new Bundle();
        }
    }

    public static Request newPostRequest(Session session, String graphPath, Object graphObject) {
        Request request = new Request(session, graphPath, null, POST_METHOD);
        request.setGraphObject(graphObject);
        return request;
    }

    public static Request newRestRequest(
            Session session, String restMethod, Bundle parameters, String httpMethod) {
        return null;
    }

    public static Request newMeRequest(Session session) {
        return new Request(session, ME);
    }

    public static Request newMyFriendsRequest(Session session) {
        return new Request(session, MY_FRIENDS);
    }

    public static Request newUploadPhotoRequest(Session session, Bitmap image) {
        Bundle parameters = new Bundle(1);
        parameters.putParcelable(PICTURE_PARAM, image);

        return new Request(session, MY_PHOTOS, parameters, POST_METHOD);
    }

    public static Request newPlacesSearchRequest(
            Session session, Location location, int radiusInMeters, int resultsLimit, String searchText)
    {
        if (location == null) {
            throw new NullPointerException("location is required");
        }

        Bundle parameters = new Bundle(5);
        parameters.putString("type", "place");
        parameters.putInt("limit", resultsLimit);
        parameters.putInt("distance", radiusInMeters);
        parameters.putString("center", 
                String.format(Locale.US, "%f,%f", location.getLatitude(), location.getLongitude()));
        if (searchText != null) {
            parameters.putString("q", searchText);
        }

        return new Request(session, SEARCH, parameters, GET_METHOD);
    }

    public final Object getGraphObject() {
        return this.graphObject;
    }

    public final void setGraphObject(Object graphObject) {
        this.graphObject = graphObject;
    }

    public final String getGraphPath() {
        return this.graphPath;
    }

    public final void setGraphPath(String graphPath) {
        this.graphPath = graphPath;
    }

    public final String getHttpMethod() {
        return this.httpMethod;
    }

    public final void setHttpMethod(String httpMethod) {
        this.httpMethod = (httpMethod != null) ? httpMethod.toUpperCase() : GET_METHOD;
    }

    public final Bundle getParameters() {
        return this.parameters;
    }

    public final String getRestMethod() {
        return this.restMethod;
    }

    public final void setRestMethod(String restMethod) {
        this.restMethod = restMethod;
    }

    public final Session getSession() {
        return this.session;
    }

    public final void setSession(Session session) {
        this.session = session;
    }

    public static HttpRequest toHttpRequest(RequestContext context, Request... requests) {
        return toHttpRequest(context, Arrays.asList(requests));
    }

    public static HttpRequest toHttpRequest(RequestContext context, List<Request> requests) {
        if (requests == null) {
            throw new NullPointerException("requests must not be null");
        }
        int numRequests = requests.size();
        if (numRequests == 0) {
            throw new IllegalArgumentException("requests must not be empty");
        }
        for (Request request : requests) {
            if (request == null) {
                throw new NullPointerException("requests must not contain null elements");
            }
        }

        if (numRequests == 1) {
            // Single request case.
            Request request = requests.get(0);
            String url = request.getUrlForSingleRequest();
        } else {
            // Batch case.
        }

        // TODO (clang) determine URL
        // determine if batch
        // store batch metadata somewhere?
        // serialize requests
        return null;
    }

    public static Response execute(Request request) {
        List<Response> responses = executeBatch(request);
        return responses.get(0);
    }

    public static Response execute(RequestContext context, Request request) {
        List<Response> responses = executeBatch(context, request);
        return responses.get(0);
    }

    public static List<Response> executeBatch(Request...requests) {
        return executeBatch(null, requests);
    }

    public static List<Response> executeBatch(RequestContext context, Request...requests) {
        if (requests == null) {
            throw new NullPointerException("requests must not be null");
        }
        int numRequests = requests.length;
        if (numRequests == 0) {
            throw new IllegalArgumentException("requests must not be empty");
        }
        for (Request request : requests) {
            if (request == null) {
                throw new NullPointerException("requests must not contain null elements");
            }
        }

        // TODO (clang) get callback or otherwise handle piggybacked requests
        // TODO (clang) execute the request, parse, etc.

        return null;
    }

    @Override public String toString() {
        return new StringBuilder()
        .append("{Request: ")
        .append(" session: ").append((this.session == null) ? "null" : this.session.toString())
        .append(", graphPath: ").append((this.graphPath == null) ? "null" : this.graphPath)
        .append(", graphObject: ").append((this.graphObject == null) ? "null" : this.graphObject)
        .append(", restMethod: ").append((this.restMethod == null) ? "null" : this.restMethod)
        .append(", httpMethod: ").append(this.getHttpMethod())
        .append(", parameters: ").append((this.parameters == null) ? "null" : this.parameters.toString())
        .append("}").toString();
    }

    private void addCommonParameters() {
        this.parameters.putString(FORMAT_PARAM, FORMAT_JSON);
        this.parameters.putString(SDK_PARAM, SDK_ANDROID);
        // TODO (clang) ACCESS_TOKEN_PARAM
    }

    private String appendParametersToBaseUrl(String baseUrl) {
        Uri.Builder uriBuilder = new Uri.Builder().encodedPath(baseUrl);

        Set<String> keys = this.parameters.keySet();
        for (String key : keys) {
            Object value = this.parameters.get(key);

            // TODO (clang) handle other types? on iOS we assume all parameters are strings, images, or NSData
            if (value instanceof Bitmap ||
                    value instanceof byte[]) {
                if (getHttpMethod().equals(GET_METHOD)) {
                    throw new IllegalArgumentException("Cannot use GET to upload a file.");
                }

                // Skip these. We add them later as attachments.
                continue;
            }
            uriBuilder.appendQueryParameter(key, value.toString());
        }

        return uriBuilder.toString();
    }

    private String getUrlForSingleRequest() {
        String baseUrl = null;
        if (this.restMethod != null) {
            baseUrl = REST_URL_BASE + this.restMethod;
        } else {
            baseUrl = GRAPH_URL_BASE + this.graphPath;
        }

        addCommonParameters();
        return appendParametersToBaseUrl(baseUrl);
    }

}
