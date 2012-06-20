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
package com.facebook.sdk.tests;

import org.apache.http.HttpRequest;

import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.util.Log;

import com.facebook.Request;

public class RequestTests extends AndroidTestCase {
    public void testCreateRequest() {
        Request request = new Request();
        assertFalse(request == null);
        assertEquals("GET", request.getHttpMethod());
    }

    public void testCreatePostRequest() {
        Bundle graphObject = new Bundle();
        Request request = Request.newPostRequest(null, "me/statuses", graphObject);
        assertFalse(request == null);
        assertEquals("POST", request.getHttpMethod());
        assertEquals("me/statuses", request.getGraphPath());
        assertEquals(graphObject, request.getGraphObject());
    }

    public void testCreateMeRequest() {
        Request request = Request.newMeRequest(null);
        assertFalse(request == null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me", request.getGraphPath());
    }

    public void testCreateMyFriendsRequest() {
        Request request = Request.newMyFriendsRequest(null);
        assertFalse(request == null);
        assertEquals("GET", request.getHttpMethod());
        assertEquals("me/friends", request.getGraphPath());
    }

    public void testCreateUploadPhotoRequest() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        Request request = Request.newUploadPhotoRequest(null, image);
        assertFalse(request == null);

        Bundle parameters = request.getParameters();
        assertFalse(parameters == null);

        assertTrue(parameters.containsKey("picture"));
        assertEquals(image, parameters.getParcelable("picture"));
        assertEquals("me/photos", request.getGraphPath());
    }

    public void testCreatePlacesSearchRequest() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        Request request = Request.newPlacesSearchRequest(null, location, 1000, 50, null);

        assertFalse(request == null);
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
            Request.executeBatch((Request[])null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testExecuteBatchWithZeroRequestsThrows() {
        try {
            Request.executeBatch(new Request[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    public void testExecuteBatchWithNullRequestThrows() {
        try {
            Request.executeBatch(new Request[]{ null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testToHttpRequestWithNullRequestsThrows() {
        try {
            Request.toHttpRequest(null, (Request[])null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    public void testToHttpRequestWithZeroRequestsThrows() {
        try {
            Request.toHttpRequest(null, new Request[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    public void testToHttpRequestWithNullRequestThrows() {
        try {
            Request.toHttpRequest(null, new Request[]{ null });
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    /* Not yet implemented
    public void testToHttpRequestSingleRequest() {
    	Request requestMe = Request.newMeRequest(null);
    	HttpRequest httpRequest = Request.toHttpRequest(null, requestMe);
    	assertFalse(httpRequest == null);
    }
     */
}
