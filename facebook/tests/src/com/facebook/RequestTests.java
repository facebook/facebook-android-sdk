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

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.List;

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.Request;
import com.facebook.Response;

public class RequestTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateRequest() {
        Request request = new Request();
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePostRequest() {
        GraphObject graphObject = GraphObjectWrapper.createGraphObject();
        Request request = Request.newPostRequest(null, "me/statuses", graphObject);
        assertTrue(request != null);
        assertEquals("POST", request.getHttpMethod());
        assertEquals("me/statuses", request.getGraphPath());
        assertEquals(graphObject, request.getGraphObject());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateMeRequest() {
        Request request = Request.newMeRequest(null);
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreateMyFriendsRequest() {
        Request request = Request.newMyFriendsRequest(null);
        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me/friends", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
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

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePlacesSearchRequest() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        Request request = Request.newPlacesSearchRequest(null, location, 1000, 50, null);

        assertTrue(request != null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("search", request.getGraphPath());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCreatePlacesSearchRequestRequiresLocation() {
        try {
            Request.newPlacesSearchRequest(null, null, 1000, 50, null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testSetHttpMethodToNilGivesDefault() {
        Request request = new Request();
        assertEquals("GET", request.getHttpMethod());

        request.setHttpMethod(null);
        assertEquals("GET", request.getHttpMethod());
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithNullRequestsThrows() {
        try {
            Request.executeBatch((Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithZeroRequestsThrows() {
        try {
            Request.executeBatch(new Request[] {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithNullRequestThrows() {
        try {
            Request.executeBatch(new Request[] { null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithNullRequestsThrows() {
        try {
            Request.toHttpConnection(null, (Request[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithZeroRequestsThrows() {
        try {
            Request.toHttpConnection(null, new Request[] {});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testToHttpConnectionWithNullRequestThrows() {
        try {
            Request.toHttpConnection(null, new Request[] { null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
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

    @MediumTest
    @LargeTest
    public void testExecuteSingleGet() { // throws Exception {
        Request request = new Request(null, "TourEiffel");
        Response response = request.execute();

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertTrue(response.getGraphObject() != null);

        GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
        assertEquals("Paris", graphPlace.getLocation().getCity());
    }

    @MediumTest
    @LargeTest
    public void testFacebookErrorResponseCreatesError() {
        Request request = new Request(null, "somestringthatshouldneverbeavalidfobjectid");
        Response response = request.execute();

        assertTrue(response != null);

        FacebookException exception = response.getError();
        assertTrue(exception != null);

        assertTrue(exception instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException = (FacebookServiceErrorException) exception;
        assertTrue(serviceException.getFacebookErrorType() != null);
        assertTrue(serviceException.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
        assertTrue(serviceException.getResponseBody() != null);
    }

    @MediumTest
    @LargeTest
    public void testRequestWithUnopenedSessionFails() {
        TestSession session = getTestSessionWithSharedUser(null);
        Request request = new Request(session, "me");
        Response response = request.execute();

        FacebookException exception = response.getError();
        assertNotNull(exception);
    }

    @MediumTest
    @LargeTest
    public void testExecuteRequestMe() {
        TestSession session = openTestSessionWithSharedUser();
        Request request = Request.newMeRequest(session);
        Response response = request.execute();

        FacebookException exception = response.getError();
        assertNull(exception);

        GraphUser me = response.getGraphObjectAs(GraphUser.class);
        assertNotNull(me);
        assertEquals(session.getTestUserId(), me.getId());
    }

    @MediumTest
    @LargeTest
    public void testExecuteMyFriendsRequest() {
        TestSession session = openTestSessionWithSharedUser();

        Request request = Request.newMyFriendsRequest(session);
        Response response = request.execute();
        assertNotNull(response);

        assertNull(response.getError());

        GraphMultiResult graphResult = response.getGraphObjectAs(GraphMultiResult.class);
        assertNotNull(graphResult);

        List<GraphObject> results = graphResult.getData();
        assertNotNull(results);
    }

    @LargeTest
    public void testExecuteUploadPhoto() {
        TestSession session = openTestSessionWithSharedUser();
        Bitmap image = createTestBitmap(128);

        Request request = Request.newUploadPhotoRequest(session, image);
        Response response = request.execute();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObject result = response.getGraphObject();
        assertNotNull(result);
    }

    @LargeTest
    public void testPostStatusUpdate() {
        TestSession session = openTestSessionWithSharedUser();

        GraphObject statusUpdate = createStatusUpdate();

        GraphObject retrievedStatusUpdate = postGetAndAssert(session, "me/feed", statusUpdate);

        assertEquals(statusUpdate.get("message"), retrievedStatusUpdate.get("message"));
    }
}
