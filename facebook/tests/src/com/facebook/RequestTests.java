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
import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import com.facebook.share.internal.ShareInternalUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RequestTests extends FacebookTestCase {
    private final static String TEST_OG_TYPE = "facebooksdktests:test";

    protected String[] getDefaultPermissions()
    {
        return new String[] { "email", "publish_actions", "read_stream" };
    };

    @MediumTest
    @LargeTest
    public void testExecuteSingleGet() {
        final AccessToken accessToken = getAccessTokenForSharedUser();
        GraphRequest request = new GraphRequest(accessToken, "TourEiffel");
        GraphResponse response = request.executeAndWait();

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertNotNull(response.getJSONObject());
        assertNotNull(response.getRawResponse());

        JSONObject graphPlace = response.getJSONObject();
        assertEquals("Paris", graphPlace.optJSONObject("location").optString("city"));
    }

    @LargeTest
    public void testBuildsUploadPhotoHttpURLConnection() throws Exception {
        final AccessToken accessToken = getAccessTokenForSharedUser();
        Bitmap image = createTestBitmap(128);

        GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(accessToken, image, null);
        HttpURLConnection connection = GraphRequest.toHttpConnection(request);

        assertTrue(connection != null);
        assertNotSame("gzip", connection.getRequestProperty("Content-Encoding"));
        assertNotSame("application/x-www-form-urlencoded", connection.getRequestProperty("Content-Type"));
    }

    @LargeTest
    public void testBuildsUploadVideoHttpURLConnection() throws IOException, URISyntaxException {
        File tempFile = null;
        try {
            final AccessToken accessToken = getAccessTokenForSharedUser();
            tempFile = createTempFileFromAsset("DarkScreen.mov");

            GraphRequest request = ShareInternalUtility.newUploadVideoRequest(
                    accessToken,
                    tempFile,
                    null);
            HttpURLConnection connection = GraphRequest.toHttpConnection(request);

            assertTrue(connection != null);
            assertNotSame("gzip", connection.getRequestProperty("Content-Encoding"));
            assertNotSame("application/x-www-form-urlencoded", connection.getRequestProperty("Content-Type"));

        } catch (Exception ex) {
            return;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetUsingHttpURLConnection() throws IOException {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        GraphRequest request = new GraphRequest(accessToken, "TourEiffel");
        HttpURLConnection connection = GraphRequest.toHttpConnection(request);

        assertEquals("gzip", connection.getRequestProperty("Content-Encoding"));
        assertEquals("application/x-www-form-urlencoded", connection.getRequestProperty("Content-Type"));

        List<GraphResponse> responses = GraphRequest.executeConnectionAndWait(connection, Arrays.asList(new GraphRequest[]{request}));
        assertNotNull(responses);
        assertEquals(1, responses.size());

        GraphResponse response = responses.get(0);

        assertTrue(response != null);
        assertTrue(response.getError() == null);
        assertNotNull(response.getJSONObject());
        assertNotNull(response.getRawResponse());

        JSONObject graphPlace = response.getJSONObject();
        assertEquals("Paris", graphPlace.optJSONObject("location").optString("city"));

        // Make sure calling code can still access HTTP headers and call disconnect themselves.
        int code = connection.getResponseCode();
        assertEquals(200, code);
        assertTrue(connection.getHeaderFields().keySet().contains("Content-Type"));
        connection.disconnect();
    }

    @MediumTest
    @LargeTest
    public void testFacebookErrorResponseCreatesError() {
        GraphRequest request = new GraphRequest(null, "somestringthatshouldneverbeavalidfobjectid");
        GraphResponse response = request.executeAndWait();

        assertTrue(response != null);

        FacebookRequestError error = response.getError();
        assertNotNull(error);
        FacebookException exception = error.getException();
        assertNotNull(exception);

        assertTrue(exception instanceof FacebookServiceException);
        assertNotNull(error.getErrorType());
        assertTrue(error.getErrorCode() != FacebookRequestError.INVALID_ERROR_CODE);
        assertNotNull(error.getRequestResultBody());
    }

    @MediumTest
    @LargeTest
    public void testRequestWithNoTokenFails() {
        GraphRequest request = new GraphRequest(null, "me");
        GraphResponse response = request.executeAndWait();

        assertNotNull(response.getError());
    }

    @MediumTest
    @LargeTest
    public void testExecuteRequestMe() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        GraphRequest request = GraphRequest.newMeRequest(accessToken, null);
        GraphResponse response = request.executeAndWait();

        validateMeResponse(accessToken, response);
    }

    static void validateMeResponse(AccessToken accessToken, GraphResponse response) {
        assertNull(response.getError());

        JSONObject me = response.getJSONObject();
        assertNotNull(me);
        assertEquals(accessToken.getUserId(), me.optString("id"));
        assertNotNull(response.getRawResponse());
    }

    @MediumTest
    @LargeTest
    public void testExecuteMyFriendsRequest() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        GraphRequest request = GraphRequest.newMyFriendsRequest(accessToken, null);
        GraphResponse response = request.executeAndWait();

        validateMyFriendsResponse(response);
    }

    static void validateMyFriendsResponse(GraphResponse response) {
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @MediumTest
    @LargeTest
    public void testExecutePlaceRequestWithLocation() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        GraphRequest request = GraphRequest.newPlacesSearchRequest(accessToken, location, 5, 5, null, null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @MediumTest
    @LargeTest
    public void testExecutePlaceRequestWithSearchText() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        // Pass a distance without a location to ensure it is correctly ignored.
        GraphRequest request = GraphRequest.newPlacesSearchRequest(accessToken, null, 1000, 5, "Starbucks", null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @MediumTest
    @LargeTest
    public void testExecutePlaceRequestWithLocationAndSearchText() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        GraphRequest request = GraphRequest.newPlacesSearchRequest(accessToken, location, 1000, 5, "Starbucks", null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    private String executePostOpenGraphRequest() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        JSONObject data = new JSONObject();
        try {
            data.put("a_property", "hello");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        GraphRequest request = ShareInternalUtility.newPostOpenGraphObjectRequest(
                accessToken,
                TEST_OG_TYPE,
                "a title",
                "http://www.facebook.com",
                "http://www.facebook.com/zzzzzzzzzzzzzzzzzzz",
                "a description",
                data,
                null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);
        assertNotNull(graphResult.optString("id"));

        assertNotNull(response.getRawResponse());

        return (String) graphResult.optString("id");
    }

    @LargeTest
    public void testExecutePostOpenGraphRequest() {
        executePostOpenGraphRequest();
    }

    @LargeTest
    public void testDeleteObjectRequest() {
        String id = executePostOpenGraphRequest();

        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        GraphRequest request = GraphRequest.newDeleteObjectRequest(accessToken, id, null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);

        assertTrue(result.optBoolean(GraphResponse.SUCCESS_KEY));
        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testUpdateOpenGraphObjectRequest() throws JSONException {
        String id = executePostOpenGraphRequest();

        JSONObject data = new JSONObject();
        data.put("a_property", "goodbye");

        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        GraphRequest request = ShareInternalUtility.newUpdateOpenGraphObjectRequest(accessToken, id,
                "another title", null, "http://www.facebook.com/aaaaaaaaaaaaaaaaa",
                "another description", data, null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);
        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecuteUploadPhoto() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        Bitmap image = createTestBitmap(128);

        GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(accessToken, image, null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);
        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecuteUploadPhotoViaFile() throws IOException {
        File outputFile = null;
        FileOutputStream outStream = null;

        try {
            final AccessToken accessToken = getAccessTokenForSharedUser(); 
            Bitmap image = createTestBitmap(128);

            File outputDir = getActivity().getCacheDir(); // context being the Activity pointer
            outputFile = File.createTempFile("prefix", "extension", outputDir);

            outStream = new FileOutputStream(outputFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
            outStream = null;

            GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(
                    accessToken,
                    outputFile,
                    null);
            GraphResponse response = request.executeAndWait();
            assertNotNull(response);

            assertNull(response.getError());

            JSONObject result = response.getJSONObject();
            assertNotNull(result);
            assertNotNull(response.getRawResponse());
        } finally {
            if (outStream != null) {
                outStream.close();
            }
            if (outputFile != null) {
                outputFile.delete();
            }
        }
    }

    @LargeTest
    public void testUploadVideoFile() throws IOException, URISyntaxException {
        File tempFile = null;
        try {
            final AccessToken accessToken = getAccessTokenForSharedUser();
            tempFile = createTempFileFromAsset("DarkScreen.mov");

            GraphRequest request = ShareInternalUtility.newUploadVideoRequest(accessToken, tempFile,
                    null);
            GraphResponse response = request.executeAndWait();
            assertNotNull(response);

            assertNull(response.getError());

            JSONObject result = response.getJSONObject();
            assertNotNull(result);
            assertNotNull(response.getRawResponse());
        } catch (Exception ex) {
            return;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @LargeTest
    public void testPostStatusUpdate() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        JSONObject statusUpdate = createStatusUpdate("");

        JSONObject retrievedStatusUpdate = postGetAndAssert(accessToken, "me/feed",
                statusUpdate);

        assertEquals(statusUpdate.optString("message"), retrievedStatusUpdate.optString("message"));
    }

    @MediumTest
    @LargeTest
    public void testCallbackIsCalled() {
        GraphRequest request = new GraphRequest(null, "4");

        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();
        request.setCallback(new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                calledBack.add(true);
            }
        });

        GraphResponse response = request.executeAndWait();
        assertNotNull(response);
        assertTrue(calledBack.size() == 1);
    }

    @MediumTest
    @LargeTest
    public void testOnProgressCallbackIsCalled() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(null, image, null);
        assertTrue(request != null);

        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();
        request.setCallback(new GraphRequest.OnProgressCallback() {
            @Override
            public void onCompleted(GraphResponse response) {
            }

            @Override
            public void onProgress(long current, long max) {
                calledBack.add(true);
            }
        });

        GraphResponse response = request.executeAndWait();
        assertNotNull(response);
        assertFalse(calledBack.isEmpty());
    }

    @MediumTest
    @LargeTest
    public void testLastOnProgressCallbackIsCalledOnce() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        GraphRequest request = ShareInternalUtility.newUploadPhotoRequest(null, image, null);
        assertTrue(request != null);

        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();
        request.setCallback(new GraphRequest.OnProgressCallback() {
            @Override
            public void onCompleted(GraphResponse response) {
            }

            @Override
            public void onProgress(long current, long max) {
                if (current == max) calledBack.add(true);
                else if (current > max) calledBack.clear();
            }
        });

        GraphResponse response = request.executeAndWait();
        assertNotNull(response);
        assertEquals(1, calledBack.size());
    }

    @MediumTest
    @LargeTest
    public void testBatchTimeoutIsApplied() {
        GraphRequest request = new GraphRequest(null, "me");
        GraphRequestBatch batch = new GraphRequestBatch(request);

        // We assume 1 ms is short enough to fail
        batch.setTimeout(1);

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(batch);
        assertNotNull(responses);
        assertTrue(responses.size() == 1);
        GraphResponse response = responses.get(0);
        assertNotNull(response);
        assertNotNull(response.getError());
    }

    @MediumTest
    @LargeTest
    public void testBatchTimeoutCantBeNegative() {
        try {
            GraphRequestBatch batch = new GraphRequestBatch();
            batch.setTimeout(-1);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    @MediumTest
    @LargeTest
    public void testCantUseComplexParameterInGetRequest() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 

        Bundle parameters = new Bundle();
        parameters.putShortArray("foo", new short[1]);

        GraphRequest request = new GraphRequest(accessToken, "me", parameters, HttpMethod.GET,
                new ExpectFailureCallback());
        GraphResponse response = request.executeAndWait();

        FacebookRequestError error = response.getError();
        assertNotNull(error);
        FacebookException exception = error.getException();
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("short[]"));
    }

    private final Location SEATTLE_LOCATION = new Location("") {
        {
            setLatitude(47.6097);
            setLongitude(-122.3331);
        }
    };

    @LargeTest
    public void testPaging() {
        final AccessToken accessToken = getAccessTokenForSharedUser(); 
        final List<JSONObject> returnedPlaces = new ArrayList<JSONObject>();
        GraphRequest request = GraphRequest
                .newPlacesSearchRequest(accessToken, SEATTLE_LOCATION, 1000, 3, null,
                        new GraphRequest.GraphJSONArrayCallback() {
                            @Override
                            public void onCompleted(JSONArray places, GraphResponse response) {
                                if (places == null) {
                                    assertNotNull(places);
                                }
                                for (int i = 0; i < places.length(); ++i) {
                                    returnedPlaces.add(places.optJSONObject(i));
                                }
                            }
                        });
        GraphResponse response = request.executeAndWait();

        assertNull(response.getError());
        assertNotNull(response.getJSONObject());
        assertNotSame(0, returnedPlaces.size());

        returnedPlaces.clear();

        GraphRequest nextRequest = response.getRequestForPagedResults(GraphResponse.PagingDirection.NEXT);
        assertNotNull(nextRequest);

        nextRequest.setCallback(request.getCallback());
        response = nextRequest.executeAndWait();

        assertNull(response.getError());
        assertNotNull(response.getJSONObject());
        assertNotSame(0, returnedPlaces.size());

        returnedPlaces.clear();

        GraphRequest previousRequest = response.getRequestForPagedResults(GraphResponse.PagingDirection.PREVIOUS);
        assertNotNull(previousRequest);

        previousRequest.setCallback(request.getCallback());
        response = previousRequest.executeAndWait();

        assertNull(response.getError());
        assertNotNull(response.getJSONObject());
        assertNotSame(0, returnedPlaces.size());
    }
}
