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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

public class Request {

    private Session session;
    private String httpMethod;
    private String graphPath;
    private GraphObject graphObject;
    private String restMethod;
    private String batchEntryName;
    private Bundle parameters;

    private static String defaultBatchApplicationId;

    // Graph paths
    public static final String ME = "me";
    public static final String MY_FRIENDS = "me/friends";
    public static final String MY_PHOTOS = "me/photos";
    public static final String SEARCH = "search";

    // HTTP methods/headers
    public static final String GET_METHOD = "GET";
    public static final String POST_METHOD = "POST";
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

        if (parameters != null) {
            this.parameters = new Bundle(parameters);
        } else {
            this.parameters = new Bundle();
        }
    }

    public static Request newPostRequest(Session session, String graphPath, GraphObject graphObject) {
        Request request = new Request(session, graphPath, null, POST_METHOD);
        request.setGraphObject(graphObject);
        return request;
    }

    public static Request newRestRequest(Session session, String restMethod, Bundle parameters, String httpMethod) {
        Request request = new Request(session, null, parameters, httpMethod);
        request.setRestMethod(restMethod);
        return request;
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

    public static Request newPlacesSearchRequest(Session session, Location location, int radiusInMeters,
            int resultsLimit, String searchText) {
        Validate.notNull(location, "location");

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

    public final GraphObject getGraphObject() {
        return this.graphObject;
    }

    public final void setGraphObject(GraphObject graphObject) {
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

    public final String getBatchEntryName() {
        return this.batchEntryName;
    }

    public final void setBatchEntryName(String batchEntryName) {
        this.batchEntryName = batchEntryName;
    }

    public final Response execute() {
        return Request.execute(this);
    }

    public static final String getDefaultBatchApplicationId() {
        return Request.defaultBatchApplicationId;
    }

    public static final void setDefaultBatchApplicationId(String applicationId) {
        Request.defaultBatchApplicationId = applicationId;
    }

    public static HttpURLConnection toHttpConnection(RequestContext context, Request... requests) {
        return toHttpConnection(context, Arrays.asList(requests));
    }

    public static HttpURLConnection toHttpConnection(RequestContext context, List<Request> requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

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
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestProperty(USER_AGENT_HEADER, getUserAgent());
            connection.setRequestProperty(CONTENT_TYPE_HEADER, getMimeContentType());

            connection.setChunkedStreamingMode(0);

            serializeToUrlConnection(requests, connection);
        } catch (IOException e) {
            throw new FacebookException("could not construct request body", e);
        } catch (JSONException e) {
            throw new FacebookException("could not construct request body", e);
        }

        return connection;
    }

    public static Response execute(Request request) {
        return execute(null, request);
    }

    public static Response execute(RequestContext context, Request request) {
        List<Response> responses = executeBatch(context, request);

        if (responses == null || responses.size() != 1) {
            throw new FacebookException("invalid state: expected a single response");
        }

        return responses.get(0);
    }

    public static List<Response> executeBatch(Request... requests) {
        return executeBatch(null, requests);
    }

    public static List<Response> executeBatch(RequestContext context, Request... requests) {
        Validate.notNull(requests, "requests");

        return executeBatch(context, Arrays.asList(requests));
    }

    public static List<Response> executeBatch(RequestContext context, List<Request> requests) {
        Validate.notEmptyAndContainsNoNulls(requests, "requests");

        // TODO port: piggyback requests onto batch if needed

        HttpURLConnection connection = toHttpConnection(context, requests);
        List<Response> responses = Response.fromHttpConnection(context, connection, requests);

        // TODO port: callback or otherwise handle piggybacked requests
        // TODO port: strip out responses from piggybacked requests

        return responses;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{Request: ").append(" session: ").append(this.session)
                .append(", graphPath: ").append(this.graphPath).append(", graphObject: ").append(this.graphObject)
                .append(", restMethod: ").append(this.restMethod).append(", httpMethod: ").append(this.getHttpMethod())
                .append(", parameters: ").append(this.parameters).append("}").toString();
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

            // TODO port: handle other types? on iOS we assume all parameters
            // are strings, images, or NSData
            if (!(value instanceof String)) {
                if (getHttpMethod().equals(GET_METHOD)) {
                    throw new IllegalArgumentException("Cannot use GET to upload a file.");
                }

                // Skip non-strings. We add them later as attachments.
                continue;
            }
            uriBuilder.appendQueryParameter(key, value.toString());
        }

        return uriBuilder.toString();
    }

    private String getUrlStringForBatchedRequest() throws MalformedURLException {
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

    private URL getUrlForSingleRequest() throws MalformedURLException {
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
        batchEntry.put(BATCH_METHOD_PARAM, getHttpMethod());
        if (this.session != null) {
            String accessToken = this.session.getAccessToken();
            Logger.registerAccessToken(accessToken);
//            batchEntry.put(ACCESS_TOKEN_PARAM, accessToken);
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
                    keysAndValues.add(String.format("%s=%s", key, value));
                }
            });
            String bodyValue = TextUtils.join("&", keysAndValues);
            batchEntry.put(BATCH_BODY_PARAM, bodyValue);
        }

        batch.put(batchEntry);
    }

    private static void serializeToUrlConnection(List<Request> requests, HttpURLConnection connection)
            throws IOException, JSONException {
        Logger logger = new Logger(LoggingBehaviors.REQUESTS, "Request");

        int numRequests = requests.size();

        String connectionHttpMethod = (numRequests == 1) ? requests.get(0).getHttpMethod() : POST_METHOD;
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
        } else if (String.class.isAssignableFrom(valueClass) || Number.class.isAssignableFrom(valueClass)) {
            serializer.writeString(key, value.toString());
        } else if (Date.class.isAssignableFrom(valueClass)) {
            Date date = (Date) value;
            // Seconds since 1/1/70 midnight GMT
            serializer.writeString(key, String.format("%d", date.getTime() * 1000));
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

    private static void serializeRequestsAsJSON(Serializer serializer, List<Request> requests, Bundle attachments)
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

    private static String getBatchAppId(List<Request> requests) {
        for (Request request : requests) {
            Session session = request.getSession();
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
            writeLine(value);
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

}
