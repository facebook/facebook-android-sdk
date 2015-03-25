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

import com.facebook.share.internal.ShareInternalUtility;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.Override;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchRequestTests extends FacebookTestCase {
    protected void setUp() throws Exception {
        super.setUp();

        // Tests that need this set should explicitly set it.
        GraphRequest.setDefaultBatchApplicationId(null);
    }

    protected String[] getDefaultPermissions()
    {
        return new String[] { "email", "publish_actions", "read_stream" };
    };

    @LargeTest
    public void testCreateNonemptyRequestBatch() {
        GraphRequest meRequest = GraphRequest.newMeRequest(null, null);

        GraphRequestBatch batch = new GraphRequestBatch(new GraphRequest[] { meRequest, meRequest });
        assertEquals(2, batch.size());
        assertEquals(meRequest, batch.get(0));
        assertEquals(meRequest, batch.get(1));
    }

    @LargeTest
    public void testBatchWithoutAppIDIsError() {
        GraphRequest request1 = new GraphRequest(null, "TourEiffel", null, null, new ExpectFailureCallback());
        GraphRequest request2 = new GraphRequest(null, "SpaceNeedle", null, null, new ExpectFailureCallback());
        GraphRequest.executeBatchAndWait(request1, request2);
    }

    @LargeTest
    public void testExecuteBatchRequestsPathEncoding() throws IOException {
        // ensures that paths passed to batch requests are encoded properly before
        // we send it up to the server

        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request1 = new GraphRequest(accessToken, "TourEiffel");
        request1.setBatchEntryName("eiffel");
        request1.setBatchEntryOmitResultOnSuccess(false);
        GraphRequest request2 = new GraphRequest(accessToken, "{result=eiffel:$.id}");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() == null);
        assertTrue(responses.get(1).getError() == null);

        JSONObject eiffelTower1 = responses.get(0).getJSONObject();
        JSONObject eiffelTower2 = responses.get(1).getJSONObject();
        assertTrue(eiffelTower1 != null);
        assertTrue(eiffelTower2 != null);

        assertEquals("Paris", eiffelTower1.optJSONObject("location").optString("city"));
        assertEquals("Paris", eiffelTower2.optJSONObject("location").optString("city"));
    }

    @LargeTest
    public void testExecuteBatchedGets() throws IOException {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request1 = new GraphRequest(accessToken, "TourEiffel");
        GraphRequest request2 = new GraphRequest(accessToken, "SpaceNeedle");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() == null);
        assertTrue(responses.get(1).getError() == null);

        JSONObject eiffelTower = responses.get(0).getJSONObject();
        JSONObject spaceNeedle = responses.get(1).getJSONObject();
        assertTrue(eiffelTower != null);
        assertTrue(spaceNeedle != null);

        assertEquals("Paris", eiffelTower.optJSONObject("location").optString("city"));
        assertEquals("Seattle", spaceNeedle.optJSONObject("location").optString("city"));
    }

    @LargeTest
    public void testFacebookErrorResponsesCreateErrors() {
        setBatchApplicationIdForTestApp();

        GraphRequest request1 = new GraphRequest(null, "somestringthatshouldneverbeavalidfobjectid");
        GraphRequest request2 = new GraphRequest(null, "someotherstringthatshouldneverbeavalidfobjectid");
        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);

        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() != null);
        assertTrue(responses.get(1).getError() != null);

        FacebookRequestError error = responses.get(0).getError();
        assertTrue(error.getException() instanceof FacebookServiceException);
        assertTrue(error.getErrorType() != null);
        assertTrue(error.getErrorCode() != FacebookRequestError.INVALID_ERROR_CODE);
    }

    @LargeTest
    public void testBatchPostStatusUpdate() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        JSONObject statusUpdate1 = createStatusUpdate("1");
        JSONObject statusUpdate2 = createStatusUpdate("2");

        GraphRequest postRequest1 = GraphRequest.newPostRequest(accessToken, "me/feed", statusUpdate1, null);
        postRequest1.setBatchEntryName("postRequest1");
        postRequest1.setBatchEntryOmitResultOnSuccess(false);
        GraphRequest postRequest2 = GraphRequest.newPostRequest(accessToken, "me/feed", statusUpdate2, null);
        postRequest2.setBatchEntryName("postRequest2");
        postRequest2.setBatchEntryOmitResultOnSuccess(false);
        GraphRequest getRequest1 = new GraphRequest(accessToken, "{result=postRequest1:$.id}");
        GraphRequest getRequest2 = new GraphRequest(accessToken, "{result=postRequest2:$.id}");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(postRequest1, postRequest2, getRequest1, getRequest2);
        assertNotNull(responses);
        assertEquals(4, responses.size());
        assertNoErrors(responses);

        JSONObject retrievedStatusUpdate1 = responses.get(2).getJSONObject();
        JSONObject retrievedStatusUpdate2 = responses.get(3).getJSONObject();
        assertNotNull(retrievedStatusUpdate1);
        assertNotNull(retrievedStatusUpdate2);

        assertEquals(statusUpdate1.optString("message"), retrievedStatusUpdate1.optString("message"));
        assertEquals(statusUpdate2.optString("message"), retrievedStatusUpdate2.optString("message"));
    }

    @LargeTest
    public void testTwoDifferentAccessTokens() {
        final AccessToken accessToken1 = getAccessTokenForSharedUser();
        final AccessToken accessToken2 = getAccessTokenForSharedUser(SECOND_TEST_USER_TAG);

        GraphRequest request1 = GraphRequest.newMeRequest(accessToken1, null);
        GraphRequest request2 = GraphRequest.newMeRequest(accessToken2, null);

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        JSONObject user1 = responses.get(0).getJSONObject();
        JSONObject user2 = responses.get(1).getJSONObject();

        assertNotNull(user1);
        assertNotNull(user2);

        assertFalse(user1.optString("id").equals(user2.optString("id")));
        assertEquals(accessToken1.getUserId(), user1.optString("id"));
        assertEquals(accessToken2.getUserId(), user2.optString("id"));
    }

    @LargeTest
    public void testBatchWithValidSessionAndNoSession() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request1 = new GraphRequest(accessToken, "me");
        GraphRequest request2 = new GraphRequest(null, "me");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        JSONObject user1 = responses.get(0).getJSONObject();
        JSONObject user2 = responses.get(1).getJSONObject();

        assertNotNull(user1);
        assertNull(user2);

        assertEquals(accessToken.getUserId(), user1.optString("id"));
    }

    @LargeTest
    public void testBatchWithNoSessionAndValidSession() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest request1 = new GraphRequest(null, "me");
        GraphRequest request2 = new GraphRequest(accessToken, "me");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        JSONObject user1 = responses.get(0).getJSONObject();
        JSONObject user2 = responses.get(1).getJSONObject();

        assertNull(user1);
        assertNotNull(user2);

        assertEquals(accessToken.getUserId(), user2.optString("id"));
    }

    @LargeTest
    public void testMixedSuccessAndFailure() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        final int NUM_REQUESTS = 8;
        GraphRequest[] requests = new GraphRequest[NUM_REQUESTS];
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;
            requests[i] = new GraphRequest(accessToken, shouldSucceed ? "me" : "-1");
        }

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(requests);
        assertNotNull(responses);
        assertEquals(NUM_REQUESTS, responses.size());

        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;

            GraphResponse response = responses.get(i);
            assertNotNull(response);
            if (shouldSucceed) {
                assertNull(response.getError());
                assertNotNull(response.getJSONObject());
            } else {
                assertNotNull(response.getError());
                assertNull(response.getJSONObject());
            }
        }
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
        GraphRequest getRequest1 = new GraphRequest(accessToken, "{result=uploadRequest1:$.id}");
        GraphRequest getRequest2 = new GraphRequest(accessToken, "{result=uploadRequest2:$.id}");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(
                uploadRequest1,
                uploadRequest2,
                getRequest1,
                getRequest2);
        assertNotNull(responses);
        assertEquals(4, responses.size());
        assertNoErrors(responses);

        JSONObject retrievedPhoto1 = responses.get(2).getJSONObject();
        JSONObject retrievedPhoto2 = responses.get(3).getJSONObject();
        assertNotNull(retrievedPhoto1);
        assertNotNull(retrievedPhoto2);

        assertEquals(image1Size, retrievedPhoto1.optInt("width"));
        assertEquals(image2Size, retrievedPhoto2.optInt("width"));
    }

    @LargeTest
    public void testCallbacksAreCalled() {
        setBatchApplicationIdForTestApp();

        ArrayList<GraphRequest> requests = new ArrayList<GraphRequest>();
        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();

        final int NUM_REQUESTS = 4;
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            GraphRequest request = new GraphRequest(null, "4");

            request.setCallback(new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    calledBack.add(true);
                }
            });

            requests.add(request);
        }

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(requests);
        assertNotNull(responses);
        assertTrue(calledBack.size() == NUM_REQUESTS);
    }


    @LargeTest
    public void testExplicitDependencyDefaultsToOmitFirstResponse() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest requestMe = GraphRequest.newMeRequest(accessToken, null);
        requestMe.setBatchEntryName("me_request");

        GraphRequest requestMyFriends = GraphRequest.newMyFriendsRequest(accessToken, null);
        requestMyFriends.setBatchEntryDependsOn("me_request");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(requestMe, requestMyFriends);

        GraphResponse meResponse = responses.get(0);
        GraphResponse myFriendsResponse = responses.get(1);

        assertNull(meResponse.getJSONObject());
        assertNotNull(myFriendsResponse.getJSONObject());
    }

    @LargeTest
    public void testExplicitDependencyCanIncludeFirstResponse() {
        final AccessToken accessToken = getAccessTokenForSharedUser();

        GraphRequest requestMe = GraphRequest.newMeRequest(accessToken, null);
        requestMe.setBatchEntryName("me_request");
        requestMe.setBatchEntryOmitResultOnSuccess(false);

        GraphRequest requestMyFriends = GraphRequest.newMyFriendsRequest(accessToken, null);
        requestMyFriends.setBatchEntryDependsOn("me_request");

        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(requestMe, requestMyFriends);

        GraphResponse meResponse = responses.get(0);
        GraphResponse myFriendsResponse = responses.get(1);

        assertNotNull(meResponse.getJSONObject());
        assertNotNull(myFriendsResponse.getJSONObject());
    }

    @LargeTest
    public void testAddAndRemoveBatchCallbacks() {
        GraphRequestBatch batch = new GraphRequestBatch();

        GraphRequestBatch.Callback callback1 = new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
            }
        };

        GraphRequestBatch.Callback callback2 = new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
            }
        };

        batch.addCallback(callback1);
        batch.addCallback(callback2);

        assertEquals(2, batch.getCallbacks().size());

        batch.removeCallback(callback1);
        batch.removeCallback(callback2);

        assertEquals(0, batch.getCallbacks().size());
    }

    @LargeTest
    public void testBatchCallbackIsCalled() {
        final AtomicInteger count = new AtomicInteger();
        GraphRequest request1 = GraphRequest.newGraphPathRequest(null, "4", new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                count.incrementAndGet();
            }
        });
        GraphRequest request2 = GraphRequest.newGraphPathRequest(null, "4", new GraphRequest.Callback() {
            @Override
            public void onCompleted(GraphResponse response) {
                count.incrementAndGet();
            }
        });

        GraphRequestBatch batch = new GraphRequestBatch(request1, request2);
        batch.addCallback(new GraphRequestBatch.Callback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
                count.incrementAndGet();
            }
        });

        batch.executeAndWait();
        assertEquals(3, count.get());
    }

    @LargeTest
    public void testBatchOnProgressCallbackIsCalled() {
        final AtomicInteger count = new AtomicInteger();

        final AccessToken accessToken = getAccessTokenForSharedUser();

        String appId = getApplicationId();
        GraphRequest.setDefaultBatchApplicationId(appId);

        GraphRequest request1 = GraphRequest.newGraphPathRequest(accessToken, "4", null);
        assertNotNull(request1);
        GraphRequest request2 = GraphRequest.newGraphPathRequest(accessToken, "4", null);
        assertNotNull(request2);

        GraphRequestBatch batch = new GraphRequestBatch(request1, request2);
        batch.addCallback(new GraphRequestBatch.OnProgressCallback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
            }

            @Override
            public void onBatchProgress(GraphRequestBatch batch, long current, long max) {
                count.incrementAndGet();
            }
        });

        batch.executeAndWait();
        assertEquals(1, count.get());
    }

    @LargeTest
    public void testBatchLastOnProgressCallbackIsCalledOnce() {
        final AtomicInteger count = new AtomicInteger();

        final AccessToken accessToken = getAccessTokenForSharedUser();

        String appId = getApplicationId();
        GraphRequest.setDefaultBatchApplicationId(appId);

        GraphRequest request1 = GraphRequest.newGraphPathRequest(accessToken, "4", null);
        assertNotNull(request1);
        GraphRequest request2 = GraphRequest.newGraphPathRequest(accessToken, "4", null);
        assertNotNull(request2);

        GraphRequestBatch batch = new GraphRequestBatch(request1, request2);
        batch.addCallback(new GraphRequestBatch.OnProgressCallback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
            }

            @Override
            public void onBatchProgress(GraphRequestBatch batch, long current, long max) {
                if (current == max) {
                    count.incrementAndGet();
                }
                else if (current > max) {
                    count.set(0);
                }
            }
        });

        batch.executeAndWait();
        assertEquals(1, count.get());
    }


    @LargeTest
    public void testMixedBatchCallbacks() {
        final AtomicInteger requestProgressCount = new AtomicInteger();
        final AtomicInteger requestCompletedCount = new AtomicInteger();
        final AtomicInteger batchProgressCount = new AtomicInteger();
        final AtomicInteger batchCompletedCount = new AtomicInteger();

        final AccessToken accessToken = getAccessTokenForSharedUser();

        String appId = getApplicationId();
        GraphRequest.setDefaultBatchApplicationId(appId);

        GraphRequest request1 = GraphRequest.newGraphPathRequest(
                null, "4", new GraphRequest.OnProgressCallback() {
            @Override
            public void onCompleted(GraphResponse response) {
                requestCompletedCount.incrementAndGet();
            }

            @Override
            public void onProgress(long current, long max) {
                if (current == max) {
                    requestProgressCount.incrementAndGet();
                }
                else if (current > max) {
                    requestProgressCount.set(0);
                }
            }
        });
        assertNotNull(request1);

        GraphRequest request2 = GraphRequest.newGraphPathRequest(null, "4", null);
        assertNotNull(request2);

        GraphRequestBatch batch = new GraphRequestBatch(request1, request2);
        batch.addCallback(new GraphRequestBatch.OnProgressCallback() {
            @Override
            public void onBatchCompleted(GraphRequestBatch batch) {
                batchCompletedCount.incrementAndGet();
            }

            @Override
            public void onBatchProgress(GraphRequestBatch batch, long current, long max) {
                if (current == max) {
                    batchProgressCount.incrementAndGet();
                } else if (current > max) {
                    batchProgressCount.set(0);
                }
            }
        });

        batch.executeAndWait();
        
        assertEquals(1, requestProgressCount.get());
        assertEquals(1, requestCompletedCount.get());
        assertEquals(1, batchProgressCount.get());
        assertEquals(1, batchCompletedCount.get());
    }
}
