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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.facebook.Request;
import com.facebook.Response;

public class RequestTests extends AndroidTestCase {

    public void testCreateRequest() {
        Request request = new Request();
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
    }

    public void testCreatePostRequest() {
        Bundle graphObject = new Bundle();
        Request request = Request.newPostRequest(null, "me/statuses", graphObject);
        assertTrue(request != null);
        assertEquals("POST", request.getHttpMethod());
        assertEquals("me/statuses", request.getGraphPath());
        assertEquals(graphObject, request.getGraphObject());
    }

    public void testCreateMeRequest() {
        Request request = Request.newMeRequest(null);
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me", request.getGraphPath());
    }

    public void testCreateMyFriendsRequest() {
        Request request = Request.newMyFriendsRequest(null);
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me/friends", request.getGraphPath());
    }

    public void testCreateUploadPhotoRequest() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        Request request = Request.newUploadPhotoRequest(null, image);
        assertTrue(request != null);

        Bundle parameters = request.getParameters();
        assertTrue(parameters != null);

        assertTrue(parameters.containsKey("picture"));
        assertEquals(image, parameters.getParcelable("picture"));
        assertEquals("me/photos", request.getGraphPath());
    }

    public void testCreatePlacesSearchRequest() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        Request request = Request.newPlacesSearchRequest(null, location, 1000, 50, null);

        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("search", request.getGraphPath());
    }

    public void testCreatePlacesSearchRequestRequiresLocation() {
        try {
            Request.newPlacesSearchRequest(null, null, 1000, 50, null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testSetHttpMethodToNilGivesDefault() {
        Request request = new Request();
        assertEquals("GET", request.getHttpMethod());

        request.setHttpMethod(null);
        assertEquals("GET", request.getHttpMethod());
    }

    public void testExecuteBatchWithNullRequestsThrows() {
        try {
            Request.executeBatch((Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testExecuteBatchWithZeroRequestsThrows() {
        try {
            Request.executeBatch(new Request[] {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    public void testExecuteBatchWithNullRequestThrows() {
        try {
            Request.executeBatch(new Request[] { null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testToHttpConnectionWithNullRequestsThrows() {
        try {
            Request.toHttpConnection(null, (Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testToHttpConnectionWithZeroRequestsThrows() {
        try {
            Request.toHttpConnection(null, new Request[] {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    public void testToHttpConnectionWithNullRequestThrows() {
        try {
            Request.toHttpConnection(null, new Request[] { null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testSingleGetToHttpRequest() throws Exception {
        Request requestMe = new Request(null, "TourEiffel");
        HttpURLConnection connection = Request.toHttpConnection(null, requestMe);

        assertTrue(connection != null);

        assertEquals("GET", connection.getRequestMethod());
        assertEquals("/TourEiffel", connection.getURL().getPath());

        assertTrue(connection.getRequestProperty("User-Agent").startsWith("FBAndroidSDK"));

        Uri uri = Uri.parse(connection.getURL().toString());
        assertEquals("android", uri.getQueryParameter("sdk"));
        assertEquals("json", uri.getQueryParameter("format"));

        // Uncomment for debug output of result.
        // logHttpResult(connection);
    }

    public void testExecuteSingleGet() { // throws Exception {
        Request request = new Request(null, "TourEiffel");
        Response response = Request.execute(request);

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertTrue(response.getGraphObject() != null);

        GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
        assertEquals("Paris", graphPlace.getLocation().getCity());
    }

    public void testFacebookErrorResponseCreatesError() {
        Request request = new Request(null, "somestringthatshouldneverbeavalidfobjectid");
        Response response = Request.execute(request);

        assertTrue(response != null);
        
        FacebookException exception = response.getError();
        assertTrue(exception != null);

        assertTrue(exception instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException = (FacebookServiceErrorException)exception;
        assertTrue(serviceException.getFacebookErrorType() != null);
        assertTrue(serviceException.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
        assertTrue(serviceException.getResponseBody() != null);
}

    /*
     * public void testExecuteUploadPhoto() { Bitmap image = new Bitmap() Request request =
     * Request.newUploadPhotoRequest(null, image); }
     */

    @SuppressWarnings("unused")
    private void logHttpResult(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            Log.d("FBAndroidSDKTest", inputLine);
        in.close();

    }
}
