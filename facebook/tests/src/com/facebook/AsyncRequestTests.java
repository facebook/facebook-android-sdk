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
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.share.internal.ShareInternalUtility;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.Arrays;

public class AsyncRequestTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testCanLaunchAsyncRequestFromUiThread() {
        GraphRequest request = GraphRequest.newPostRequest(null, "me/feeds", null, null);
        try {
            TestGraphRequestAsyncTask task = createAsyncTaskOnUiThread(request);
            assertNotNull(task);
        } catch (Throwable throwable) {
            assertNull(throwable);
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteWithNullRequestsThrows() throws Exception {
        try {
            TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask((GraphRequest[]) null);

            task.executeOnBlockerThread();

            waitAndAssertSuccessOrRethrow(1);

            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithZeroRequestsThrows() throws Exception {
        try {
            TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(new GraphRequest[] {});

            task.executeOnBlockerThread();

            waitAndAssertSuccessOrRethrow(1);

            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException exception) {
        }
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteBatchWithNullRequestThrows() throws Exception {
        try {
            TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(
                    new GraphRequest[] { null });

            task.executeOnBlockerThread();

            waitAndAssertSuccessOrRethrow(1);

            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }

    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGet() {
        final AccessToken accessToken = getAccessTokenForSharedUser();
        GraphRequest request = new GraphRequest(accessToken, "TourEiffel", null, null,
                new ExpectSuccessCallback() {
            @Override
            protected void performAsserts(GraphResponse response) {
                assertNotNull(response);
                JSONObject graphPlace = response.getJSONObject();
                assertEquals("Paris", graphPlace.optJSONObject("location").optString("city"));
            }
        });

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(request);

        task.executeOnBlockerThread();

        // Wait on 2 signals: request and task will both signal.
        waitAndAssertSuccess(2);
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetUsingHttpURLConnection() {
        final AccessToken accessToken = getAccessTokenForSharedUser();
        GraphRequest request = new GraphRequest(accessToken, "TourEiffel", null, null,
                new ExpectSuccessCallback() {
            @Override
            protected void performAsserts(GraphResponse response) {
                assertNotNull(response);
                JSONObject graphPlace = response.getJSONObject();
                assertEquals("Paris", graphPlace.optJSONObject("location").optString("city"));
            }
        });
        HttpURLConnection connection = GraphRequest.toHttpConnection(request);

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(connection, Arrays.asList(new GraphRequest[] { request }));

        task.executeOnBlockerThread();

        // Wait on 2 signals: request and task will both signal.
        waitAndAssertSuccess(2);
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetFailureCase() {
        final AccessToken accessToken = getAccessTokenForSharedUser();
        GraphRequest request = new GraphRequest(accessToken, "-1", null, null,
                new ExpectFailureCallback());

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(request);

        task.executeOnBlockerThread();

        // Wait on 2 signals: request and task will both signal.
        waitAndAssertSuccess(2);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testBatchWithoutAppIDIsError() throws Throwable {
        GraphRequest request1 = new GraphRequest(null, "TourEiffel", null, null, new ExpectFailureCallback());
        GraphRequest request2 = new GraphRequest(null, "SpaceNeedle", null, null, new ExpectFailureCallback());

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(request1, request2);

        task.executeOnBlockerThread();

        // Wait on 3 signals: request1, request2, and task will all signal.
        waitAndAssertSuccessOrRethrow(3);
    }

    @LargeTest
    public void testMixedSuccessAndFailure() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        final int NUM_REQUESTS = 8;
        GraphRequest[] requests = new GraphRequest[NUM_REQUESTS];
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;
            if (shouldSucceed) {
                requests[i] = new GraphRequest(accessToken, "me", null, null,
                        new ExpectSuccessCallback());
            } else {
                requests[i] = new GraphRequest(accessToken, "-1", null, null,
                        new ExpectFailureCallback());
            }
        }

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(requests);

        task.executeOnBlockerThread();

        // Note: plus 1, because the overall async task signals as well.
        waitAndAssertSuccess(NUM_REQUESTS + 1);
    }

    @MediumTest
    @LargeTest
    public void testStaticExecuteMeAsync() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        class MeCallback extends ExpectSuccessCallback implements GraphRequest.GraphJSONObjectCallback {
            @Override
            public void onCompleted(JSONObject me, GraphResponse response) {
                assertNotNull(me);
                assertEquals(accessToken.getUserId(), me.optString("id"));
                RequestTests.validateMeResponse(accessToken, response);
                onCompleted(response);
            }
        }

        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                GraphRequest.newMeRequest(accessToken, new MeCallback()).executeAsync();
            }
        }, false);
        waitAndAssertSuccess(1);
    }

    @MediumTest
    @LargeTest
    public void testStaticExecuteMyFriendsAsync() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        class FriendsCallback extends ExpectSuccessCallback implements GraphRequest.GraphJSONArrayCallback {
            @Override
            public void onCompleted(JSONArray friends, GraphResponse response) {
                assertNotNull(friends);
                RequestTests.validateMyFriendsResponse(response);
                onCompleted(response);
            }
        }

        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                GraphRequest.newMyFriendsRequest(accessToken, new FriendsCallback()).executeAsync();
            }
        }, false);
        waitAndAssertSuccess(1);
    }

    @LargeTest
    public void testBatchUploadPhoto() {
        final AccessToken accessToken = getAccessTokenForSharedUserWithPermissions(null,
                "user_photos", "publish_actions");

        final int image1Size = 120;
        final int image2Size = 150;

        Bitmap bitmap1 = createTestBitmap(image1Size);
        Bitmap bitmap2 = createTestBitmap(image2Size);

        GraphRequest uploadRequest1 = ShareInternalUtility.newUploadPhotoRequest(
                accessToken,
                bitmap1,
                null);
        uploadRequest1.setBatchEntryName("uploadRequest1");
        GraphRequest uploadRequest2 = ShareInternalUtility.newUploadPhotoRequest(
                accessToken,
                bitmap2,
                null);
        uploadRequest2.setBatchEntryName("uploadRequest2");
        GraphRequest getRequest1 = new GraphRequest(
                accessToken,
                "{result=uploadRequest1:$.id}",
                null,
                null,
                new ExpectSuccessCallback() {
                    @Override
                    protected void performAsserts(GraphResponse response) {
                        assertNotNull(response);
                        JSONObject retrievedPhoto = response.getJSONObject();
                        assertNotNull(retrievedPhoto);
                        assertEquals(image1Size, retrievedPhoto.optInt("width"));
                    }
                });
        GraphRequest getRequest2 = new GraphRequest(
                accessToken,
                "{result=uploadRequest2:$.id}",
                null,
                null,
                new ExpectSuccessCallback() {
                    @Override
                    protected void performAsserts(GraphResponse response) {
                        assertNotNull(response);
                        JSONObject retrievedPhoto = response.getJSONObject();
                        assertNotNull(retrievedPhoto);
                        assertEquals(image2Size, retrievedPhoto.optInt("width"));
                    }
                });

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(
                uploadRequest1,
                uploadRequest2,
                getRequest1,
                getRequest2);
        task.executeOnBlockerThread();

        // Wait on 3 signals: getRequest1, getRequest2, and task will all signal.
        waitAndAssertSuccess(3);
    }

    @MediumTest
    @LargeTest
    public void testShortTimeoutCausesFailure() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request = new GraphRequest(accessToken, "me/likes", null, null,
                new ExpectFailureCallback());

        GraphRequestBatch requestBatch = new GraphRequestBatch(request);

        // 1 millisecond timeout should be too short for response from server.
        requestBatch.setTimeout(1);

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(requestBatch);
        task.executeOnBlockerThread();

        // Note: plus 1, because the overall async task signals as well.
        waitAndAssertSuccess(2);
    }

    @LargeTest
    public void testLongTimeoutAllowsSuccess() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request = new GraphRequest(accessToken, "me", null, null,
                new ExpectSuccessCallback());

        GraphRequestBatch requestBatch = new GraphRequestBatch(request);

        // 10 second timeout should be long enough for successful response from server.
        requestBatch.setTimeout(10000);

        TestGraphRequestAsyncTask task = new TestGraphRequestAsyncTask(requestBatch);
        task.executeOnBlockerThread();

        // Note: plus 1, because the overall async task signals as well.
        waitAndAssertSuccess(2);
    }
}
