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

import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import com.facebook.internal.GraphUtil;
import com.facebook.internal.ServerProtocol;
import com.facebook.share.internal.ShareInternalUtility;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.net.HttpURLConnection;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest( { FacebookSdk.class, AccessTokenManager.class })
public class GraphRequestTest extends FacebookPowerMockTestCase {

    @Before
    public void before() {
        mockStatic(FacebookSdk.class);
        when(FacebookSdk.isInitialized()).thenReturn(true);
        when(FacebookSdk.getApplicationId()).thenReturn("1234");
        when(FacebookSdk.getClientToken()).thenReturn("5678");
    }

    @Test
    public void testCreateRequest() {
        GraphRequest request = new GraphRequest();
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
    }

    @Test
    public void testCreatePostRequest() {
        JSONObject graphObject = new JSONObject();
        GraphRequest request = GraphRequest.newPostRequest(null, "me/statuses", graphObject, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.POST, request.getHttpMethod());
        assertEquals("me/statuses", request.getGraphPath());
        assertEquals(graphObject, request.getGraphObject());
    }

    @Test
    public void testCreateMeRequest() {
        GraphRequest request = GraphRequest.newMeRequest(null, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("me", request.getGraphPath());
    }

    @Test
    public void testCreateMyFriendsRequest() {
        GraphRequest request = GraphRequest.newMyFriendsRequest(null, null);
        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("me/friends", request.getGraphPath());
    }

    @Test
    public void testCreateUploadPhotoRequest() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(null, image, null);
        assertTrue(request != null);

        Bundle parameters = request.getParameters();
        assertTrue(parameters != null);

        assertTrue(parameters.containsKey("picture"));
        assertEquals(image, parameters.getParcelable("picture"));
        assertEquals("me/photos", request.getGraphPath());
    }

    @Test
    public void testCreatePlacesSearchRequestWithLocation() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        GraphRequest request = GraphRequest.newPlacesSearchRequest(null, location, 1000, 50, null, null);

        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("search", request.getGraphPath());
    }

    @Test
    public void testCreatePlacesSearchRequestWithSearchText() {
        GraphRequest request = GraphRequest.newPlacesSearchRequest(null, null, 1000, 50, "Starbucks", null);

        assertTrue(request != null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
        assertEquals("search", request.getGraphPath());
    }

    @Test
    public void testCreatePlacesSearchRequestRequiresLocationOrSearchText() {
        try {
            GraphRequest.newPlacesSearchRequest(null, null, 1000, 50, null, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequestRequiresObject() {
        try {
            ShareInternalUtility.newPostOpenGraphObjectRequest(null, null, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequestRequiresObjectType() {
        try {
            JSONObject object = GraphUtil.createOpenGraphObjectForPost(null);
            ShareInternalUtility.newPostOpenGraphObjectRequest(null, object, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequestRequiresNonEmptyObjectType() throws JSONException {
        try {
            JSONObject object = GraphUtil.createOpenGraphObjectForPost("");
            object.put("title", "bar");
            ShareInternalUtility.newPostOpenGraphObjectRequest(null, object, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequestRequiresTitle() {
        try {
            JSONObject object = GraphUtil.createOpenGraphObjectForPost("foo");
            ShareInternalUtility.newPostOpenGraphObjectRequest(null, object, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequestRequiresNonEmptyTitle() throws JSONException {
        try {
            JSONObject object = GraphUtil.createOpenGraphObjectForPost("foo");
            object.put("title", "");
            ShareInternalUtility.newPostOpenGraphObjectRequest(null, object, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphObjectRequest() throws JSONException {
        JSONObject object = GraphUtil.createOpenGraphObjectForPost("foo");
        object.put("title", "bar");
        GraphRequest request = ShareInternalUtility.newPostOpenGraphObjectRequest(
                null,
                object,
                null);
        assertNotNull(request);
    }

    @Test
    public void testNewPostOpenGraphActionRequestRequiresAction() {
        try {
            ShareInternalUtility.newPostOpenGraphActionRequest(null, null, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphActionRequestRequiresActionType() {
        try {
            JSONObject action = GraphUtil.createOpenGraphActionForPost(null);
            ShareInternalUtility.newPostOpenGraphActionRequest(null, action, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphActionRequestRequiresNonEmptyActionType() {
        try {
            JSONObject action = GraphUtil.createOpenGraphActionForPost("");
            ShareInternalUtility.newPostOpenGraphActionRequest(null, action, null);
            fail("expected exception");
        } catch (FacebookException exception) {
            // Success
        }
    }

    @Test
    public void testNewPostOpenGraphActionRequest() {
        JSONObject action = GraphUtil.createOpenGraphActionForPost("foo");
        GraphRequest request = ShareInternalUtility.newPostOpenGraphActionRequest(
                null,
                action,
                null);
        assertNotNull(request);
    }

    @Test
    public void testSetHttpMethodToNilGivesDefault() {
        GraphRequest request = new GraphRequest();
        assertEquals(HttpMethod.GET, request.getHttpMethod());

        request.setHttpMethod(null);
        assertEquals(HttpMethod.GET, request.getHttpMethod());
    }

    @Test
    public void testExecuteBatchWithNullRequestsThrows() {
        try {
            GraphRequest.executeBatchAndWait((GraphRequest[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @Test
    public void testExecuteBatchWithZeroRequestsThrows() {
        try {
            GraphRequest.executeBatchAndWait(new GraphRequest[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testExecuteBatchWithNullRequestThrows() {
        try {
            GraphRequest.executeBatchAndWait(new GraphRequest[]{null});
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @Test
    public void testToHttpConnectionWithNullRequestsThrows() {
        try {
            GraphRequest.toHttpConnection((GraphRequest[]) null);
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @Test
    public void testToHttpConnectionWithZeroRequestsThrows() {
        try {
            GraphRequest.toHttpConnection(new GraphRequest[]{});
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @Test
    public void testToHttpConnectionWithNullRequestThrows() {
        try {
            GraphRequest.toHttpConnection(new GraphRequest[]{null});
            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @Test
    public void testSingleGetToHttpRequest() throws Exception {
        GraphRequest requestMe = new GraphRequest(null, "TourEiffel");
        HttpURLConnection connection = GraphRequest.toHttpConnection(requestMe);

        assertTrue(connection != null);

        assertEquals("GET", connection.getRequestMethod());
        assertEquals("/" + ServerProtocol.getAPIVersion() + "/TourEiffel",
            connection.getURL().getPath());

        assertTrue(connection.getRequestProperty("User-Agent").startsWith("FBAndroidSDK"));

        Uri uri = Uri.parse(connection.getURL().toString());
        assertEquals("android", uri.getQueryParameter("sdk"));
        assertEquals("json", uri.getQueryParameter("format"));
    }

    @Test
    public void testBuildsClientTokenIfNeeded() throws Exception {
        GraphRequest requestMe = new GraphRequest(null, "TourEiffel");
        HttpURLConnection connection = GraphRequest.toHttpConnection(requestMe);

        assertTrue(connection != null);

        Uri uri = Uri.parse(connection.getURL().toString());
        String accessToken = uri.getQueryParameter("access_token");
        assertNotNull(accessToken);
        assertTrue(accessToken.contains(FacebookSdk.getApplicationId()));
        assertTrue(accessToken.contains(FacebookSdk.getClientToken()));
    }
}
