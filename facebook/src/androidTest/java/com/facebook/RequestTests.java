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
import android.test.suitebuilder.annotation.LargeTest;

import com.facebook.internal.GraphUtil;
import com.facebook.share.ShareApi;
import com.facebook.share.Sharer;
import com.facebook.share.internal.ShareInternalUtility;
import com.facebook.share.model.ShareOpenGraphAction;
import com.facebook.share.model.ShareOpenGraphContent;
import com.facebook.share.model.ShareOpenGraphObject;
import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RequestTests extends FacebookTestCase {
    private static final String TEST_OG_OBJECT_TYPE = "facebooksdktests:test";
    private static final String TEST_OG_ACTION_TYPE = "facebooksdktests:run";
    private static final long REQUEST_TIMEOUT_MILLIS = 10000;

    public static final String TEST_PAGE_ID = "1163806960341831";
    public static final String TEST_PAGE_ID_2 = "110774245616525";

    protected String[] getDefaultPermissions()
    {
        return new String[] {
                "email",
                "publish_actions",
                "user_posts",
                "user_photos",
                "user_videos" };
    };

    @Override
    public void setUp() throws Exception {
        super.setUp();
        AccessToken.setCurrentAccessToken(getAccessTokenForSharedUser());
    }

    @Override
    public void tearDown() throws Exception {
        AccessToken.setCurrentAccessToken(null);
        super.tearDown();
    }

    @LargeTest
    public void testBuildsUploadPhotoHttpURLConnection() throws Exception {
        Bitmap image = createTestBitmap(128);

        GraphRequest request = GraphRequest.newUploadPhotoRequest(
                AccessToken.getCurrentAccessToken(),
                ShareInternalUtility.MY_PHOTOS,
                image,
                "Test photo messsage",
                null,
                null);
        HttpURLConnection connection = GraphRequest.toHttpConnection(request);

        assertTrue(connection != null);
        assertNotSame("gzip", connection.getRequestProperty("Content-Encoding"));
        assertNotSame("application/x-www-form-urlencoded", connection.getRequestProperty("Content-Type"));
    }

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

    @LargeTest
    public void testRequestWithNoTokenFails() {
        GraphRequest request = new GraphRequest(null, "me");
        GraphResponse response = request.executeAndWait();

        assertNotNull(response.getError());
    }

    @LargeTest
    public void testExecuteRequestMe() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), null);
        GraphResponse response = request.executeAndWait();

        validateMeResponse(AccessToken.getCurrentAccessToken(), response);
    }

    static void validateMeResponse(AccessToken accessToken, GraphResponse response) {
        assertNull(response.getError());

        JSONObject me = response.getJSONObject();
        assertNotNull(me);
        assertEquals(accessToken.getUserId(), me.optString("id"));
        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecuteMyFriendsRequest() {
        GraphRequest request =
                GraphRequest.newMyFriendsRequest(AccessToken.getCurrentAccessToken(), null);
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

    @LargeTest
    public void testExecutePlaceRequestWithLocation() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        GraphRequest request = GraphRequest.newPlacesSearchRequest(
                AccessToken.getCurrentAccessToken(),
                location,
                5,
                5,
                null,
                null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecutePlaceRequestWithSearchText() {
        // Pass a distance without a location to ensure it is correctly ignored.
        GraphRequest request = GraphRequest.newPlacesSearchRequest(
                AccessToken.getCurrentAccessToken(),
                null,
                1000,
                5,
                "Starbucks",
                null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecutePlaceRequestWithLocationAndSearchText() {
        Location location = new Location("");
        location.setLatitude(47.6204);
        location.setLongitude(-122.3491);

        GraphRequest request = GraphRequest.newPlacesSearchRequest(
                AccessToken.getCurrentAccessToken(),
                location,
                1000,
                5,
                "Starbucks",
                null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);

        JSONArray results = graphResult.optJSONArray("data");
        assertNotNull(results);

        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testShareOpenGraphContent() throws Exception {
        ShareOpenGraphObject ogObject = new ShareOpenGraphObject.Builder()
                .putString("og:title", "a title")
                .putString("og:type", TEST_OG_OBJECT_TYPE)
                .putString("og:description", "a description")
                .build();

        ShareOpenGraphAction ogAction = new ShareOpenGraphAction.Builder()
                .setActionType(TEST_OG_ACTION_TYPE)
                .putObject("test", ogObject)
                .build();

        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setAction(ogAction)
                .setPreviewPropertyName("test")
                .build();

        final ShareApi shareApi = new ShareApi(content);
        final AtomicReference<String> actionId = new AtomicReference<>(null);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shareApi.share(new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        actionId.set(result.getPostId());
                        notifyShareFinished();
                    }

                    @Override
                    public void onCancel() {
                        notifyShareFinished();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        notifyShareFinished();
                    }

                    private void notifyShareFinished() {
                        synchronized (shareApi) {
                            shareApi.notifyAll();
                        }
                    }
                });
            }
        });

        synchronized (shareApi) {
            shareApi.wait(REQUEST_TIMEOUT_MILLIS);
        }
        assertNotNull(actionId.get());
    }

    @LargeTest
    public void testShareOpenGraphContentWithBadType() throws Exception {
        ShareOpenGraphObject ogObject = new ShareOpenGraphObject.Builder()
                .putString("og:title", "a title")
                .putString("og:type", TEST_OG_OBJECT_TYPE)
                .putString("og:description", "a description")
                .build();

        ShareOpenGraphAction ogAction = new ShareOpenGraphAction.Builder()
                .setActionType(TEST_OG_ACTION_TYPE+"bad")
                .putObject("test", ogObject)
                .build();

        ShareOpenGraphContent content = new ShareOpenGraphContent.Builder()
                .setAction(ogAction)
                .setPreviewPropertyName("test")
                .build();

        final ShareApi shareApi = new ShareApi(content);
        final AtomicReference<String> actionId = new AtomicReference<>(null);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shareApi.share(new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        actionId.set(result.getPostId());
                        notifyShareFinished();
                    }

                    @Override
                    public void onCancel() {
                        notifyShareFinished();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        errorOccurred.set(true);
                        notifyShareFinished();
                    }

                    private void notifyShareFinished() {
                        synchronized (shareApi) {
                            shareApi.notifyAll();
                        }
                    }
                });
            }
        });

        synchronized (shareApi) {
            shareApi.wait(REQUEST_TIMEOUT_MILLIS);
        }
        assertNull(actionId.get());
        assertTrue(errorOccurred.get());
    }

    private String executePostOpenGraphRequest() {
        JSONObject data = new JSONObject();
        try {
            data.put("a_property", "hello");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONObject ogObject = GraphUtil.createOpenGraphObjectForPost(
                TEST_OG_OBJECT_TYPE,
                "a title",
                "http://www.facebook.com",
                "http://www.facebook.com/zzzzzzzzzzzzzzzzzzz",
                "a description",
                data,
                null);

        Bundle bundle = new Bundle();
        bundle.putString("object", ogObject.toString());
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/objects/" + TEST_OG_OBJECT_TYPE,
                bundle,
                HttpMethod.POST,
                null);

        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject graphResult = response.getJSONObject();
        assertNotNull(graphResult);
        assertNotNull(graphResult.optString("id"));

        assertNotNull(response.getRawResponse());

        return graphResult.optString("id");
    }

    @LargeTest
    public void testExecutePostOpenGraphRequest() {
        executePostOpenGraphRequest();
    }

    @LargeTest
    public void testCreateOpenGraphObjectWithBadImageType() throws InterruptedException {
        //only image urls are accepted for createOpenGraphObject
        Bitmap image = createTestBitmap(128);
        SharePhoto photo = new SharePhoto.Builder()
                .setBitmap(image)
                .setUserGenerated(true)
                .build();
        ShareOpenGraphObject ogObject = new ShareOpenGraphObject.Builder()
                .putString("og:title", "a title")
                .putString("og:type", TEST_OG_OBJECT_TYPE)
                .putString("og:description", "a description")
                .putPhoto("og:image", photo)
                .build();

        try {
            GraphRequest request = ShareGraphRequest.createOpenGraphObject(ogObject);
            GraphResponse response = request.executeAndWait();
            //should fail because do not accept images without imageurl
            fail();
        }
        catch (Exception e){
            if(!(e instanceof FacebookException
                    && e.getMessage().equals("Unable to attach images"))){
                fail();
            }
        }
    }

    @LargeTest
    public void testCreateOpenGraphObject() throws InterruptedException {
        Uri testImage = Uri.parse("http://i.imgur.com/Diyvl7q.jpg");
        SharePhoto photo = new SharePhoto.Builder()
                .setImageUrl(testImage)
                .setUserGenerated(true)
                .build();
        ShareOpenGraphObject ogObject = new ShareOpenGraphObject.Builder()
                .putString("og:title", "a title")
                .putString("og:type", TEST_OG_OBJECT_TYPE)
                .putString("og:description", "a description")
                .putPhoto("og:image", photo)
                .build();

        try {
            GraphRequest request = ShareGraphRequest.createOpenGraphObject(ogObject);
            GraphResponse response = request.executeAndWait();

            assertNotNull(response);
            assertNull(response.getError());

            JSONObject graphResult = response.getJSONObject();

            assertNotNull(graphResult);
            assertNotNull(graphResult.optString("id"));
            assertNotNull(response.getRawResponse());
        }
        catch (Exception e){
            fail();
        }
    }

    @LargeTest
    public void testDeleteObjectRequest() {
        String id = executePostOpenGraphRequest();

        GraphRequest request = GraphRequest.newDeleteObjectRequest(
                AccessToken.getCurrentAccessToken(),
                id,
                null);
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

        JSONObject ogObject = GraphUtil.createOpenGraphObjectForPost(
                TEST_OG_OBJECT_TYPE,
                "another title",
                null,
                "http://www.facebook.com/aaaaaaaaaaaaaaaaa",
                "another description",
                data,
                null);
        Bundle bundle = new Bundle();
        bundle.putString("object", ogObject.toString());
        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                id,
                bundle,
                HttpMethod.POST,
                null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);
        assertEquals("another title", result.optString("title"));
        assertNotNull(response.getRawResponse());
    }

    @LargeTest
    public void testExecuteUploadPhoto() {
        Bitmap image = createTestBitmap(128);

        GraphRequest request = GraphRequest.newUploadPhotoRequest(
                AccessToken.getCurrentAccessToken(),
                ShareInternalUtility.MY_PHOTOS,
                image,
                "Test photo message",
                null,
                null);
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
            Bitmap image = createTestBitmap(128);

            File outputDir = getActivity().getCacheDir(); // context being the Activity pointer
            outputFile = File.createTempFile("prefix", "extension", outputDir);

            outStream = new FileOutputStream(outputFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
            outStream = null;

            GraphRequest request = GraphRequest.newUploadPhotoRequest(
                    AccessToken.getCurrentAccessToken(),
                    ShareInternalUtility.MY_PHOTOS,
                    outputFile,
                    "Test photo message",
                    null,
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
    public void testExecuteUploadPhotoToAlbum() throws InterruptedException, JSONException {
        // first create an album
        Bundle params = new Bundle();
        params.putString("name", "Foo");
        GraphRequest request =
                new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "me/albums",
                        params,
                        HttpMethod.POST);

        GraphResponse response = request.executeAndWait();
        JSONObject jsonResponse = response.getJSONObject();
        assertNotNull(jsonResponse);
        String albumId = jsonResponse.optString("id");
        assertNotNull(albumId);

        // upload an image to the album
        Bitmap image = createTestBitmap(128);
        SharePhoto photo = new SharePhoto.Builder()
                .setBitmap(image)
                .setUserGenerated(true)
                .build();
        SharePhotoContent content = new SharePhotoContent.Builder().addPhoto(photo).build();
        final ShareApi shareApi = new ShareApi(content);
        shareApi.setGraphNode(albumId);
        final AtomicReference<String> imageId = new AtomicReference<>(null);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                shareApi.share(new FacebookCallback<Sharer.Result>() {
                    @Override
                    public void onSuccess(Sharer.Result result) {
                        imageId.set(result.getPostId());
                        notifyShareFinished();
                    }

                    @Override
                    public void onCancel() {
                        notifyShareFinished();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        notifyShareFinished();
                    }

                    private void notifyShareFinished() {
                        synchronized (shareApi) {
                            shareApi.notifyAll();
                        }
                    }
                });
            }
        });

        synchronized (shareApi) {
            shareApi.wait(REQUEST_TIMEOUT_MILLIS);
        }
        assertNotNull(imageId.get());

        // now check to see if the image is in the album
        GraphRequest listRequest =
                new GraphRequest(AccessToken.getCurrentAccessToken(), albumId + "/photos");

        GraphResponse listResponse = listRequest.executeAndWait();
        JSONObject listObject = listResponse.getJSONObject();
        assertNotNull(listObject);
        JSONArray jsonList = listObject.optJSONArray("data");
        assertNotNull(jsonList);

        boolean found = false;
        for (int i = 0; i < jsonList.length(); i++) {
            JSONObject imageObject = jsonList.getJSONObject(i);
            if (imageId.get().equals(imageObject.optString("id"))) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @LargeTest
    public void testUploadVideoFile() throws IOException, URISyntaxException {
        File tempFile = null;
        try {
            tempFile = createTempFileFromAsset("DarkScreen.mov");
            ShareVideo video = new ShareVideo.Builder()
                    .setLocalUrl(Uri.fromFile(tempFile))
                    .build();
            ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();
            final ShareApi shareApi = new ShareApi(content);
            final AtomicReference<String> videoId = new AtomicReference<>(null);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    shareApi.share(new FacebookCallback<Sharer.Result>() {
                        @Override
                        public void onSuccess(Sharer.Result result) {
                            videoId.set(result.getPostId());
                            notifyShareFinished();
                        }

                        @Override
                        public void onCancel() {
                            notifyShareFinished();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            notifyShareFinished();
                        }

                        private void notifyShareFinished() {
                            synchronized (shareApi) {
                                shareApi.notifyAll();
                            }
                        }
                    });
                }
            });

            synchronized (shareApi) {
                shareApi.wait(REQUEST_TIMEOUT_MILLIS);
            }
            assertNotNull(videoId.get());
        } catch (Exception ex) {
            fail();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @LargeTest
    public void testUploadVideoFileToUserId() throws IOException, URISyntaxException {
        File tempFile = null;
        try {
            GraphRequest meRequest =
                    GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), null);
            GraphResponse meResponse = meRequest.executeAndWait();
            JSONObject meJson = meResponse.getJSONObject();
            assertNotNull(meJson);

            String userId = meJson.optString("id");
            assertNotNull(userId);

            tempFile = createTempFileFromAsset("DarkScreen.mov");
            ShareVideo video = new ShareVideo.Builder()
                    .setLocalUrl(Uri.fromFile(tempFile))
                    .build();
            ShareVideoContent content = new ShareVideoContent.Builder().setVideo(video).build();
            final ShareApi shareApi = new ShareApi(content);
            shareApi.setGraphNode(userId);
            final AtomicReference<String> videoId = new AtomicReference<>(null);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    shareApi.share(new FacebookCallback<Sharer.Result>() {
                        @Override
                        public void onSuccess(Sharer.Result result) {
                            videoId.set(result.getPostId());
                            notifyShareFinished();
                        }

                        @Override
                        public void onCancel() {
                            notifyShareFinished();
                        }

                        @Override
                        public void onError(FacebookException error) {
                            notifyShareFinished();
                        }

                        private void notifyShareFinished() {
                            synchronized (shareApi) {
                                shareApi.notifyAll();
                            }
                        }
                    });
                }
            });

            synchronized (shareApi) {
                shareApi.wait(REQUEST_TIMEOUT_MILLIS);
            }
            assertNotNull(videoId.get());
        } catch (Exception ex) {
            fail();
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @LargeTest
    public void testPostStatusUpdate() {
        JSONObject statusUpdate = createStatusUpdate("");

        JSONObject retrievedStatusUpdate = postGetAndAssert(
                AccessToken.getCurrentAccessToken(),
                "me/feed",
                statusUpdate);

        assertEquals(statusUpdate.optString("message"), retrievedStatusUpdate.optString("message"));
    }

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

    @LargeTest
    public void testOnProgressCallbackIsCalled() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        GraphRequest request = GraphRequest.newUploadPhotoRequest(
                null,
                ShareInternalUtility.MY_PHOTOS,
                image,
                null,
                null,
                null);
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

    @LargeTest
    public void testLastOnProgressCallbackIsCalledOnce() {
        Bitmap image = Bitmap.createBitmap(128, 128, Bitmap.Config.ALPHA_8);

        GraphRequest request = GraphRequest.newUploadPhotoRequest(
                null,
                ShareInternalUtility.MY_PHOTOS,
                image,
                null,
                null,
                null);
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

    @LargeTest
    public void testBatchTimeoutIsApplied() {
        GraphRequest request = new GraphRequest(null, "me");
        GraphRequestBatch batch = new GraphRequestBatch(request);

        // We assume 5 ms is short enough to fail
        batch.setTimeout(1);

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(batch);
        assertNotNull(responses);
        assertTrue(responses.size() == 1);
        GraphResponse response = responses.get(0);
        assertNotNull(response);
        assertNotNull(response.getError());
    }

    @LargeTest
    public void testBatchTimeoutCantBeNegative() {
        try {
            GraphRequestBatch batch = new GraphRequestBatch();
            batch.setTimeout(-1);
            fail();
        } catch (IllegalArgumentException ex) {
        }
    }

    @LargeTest
    public void testCantUseComplexParameterInGetRequest() {
        Bundle parameters = new Bundle();
        parameters.putShortArray("foo", new short[1]);

        GraphRequest request = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "me",
                parameters,
                HttpMethod.GET,
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
}
