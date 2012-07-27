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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.FacebookException;
import com.facebook.Request;
import com.facebook.Response;

public class BatchRequestTests extends FacebookTestCase {
    protected void setUp() throws Exception {
        super.setUp();

        // Tests that need this set should explicitly set it.
        Request.setDefaultBatchApplicationId(null);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testBatchWithoutAppIDThrows() {
        try {
            Request request1 = new Request(null, "TourEiffel");
            Request request2 = new Request(null, "SpaceNeedle");
            Request.executeBatch(request1, request2);
            fail("expected FacebookException");
        } catch (FacebookException exception) {
        }
    }

    @MediumTest
    @LargeTest
    public void testExecuteBatchedGets() throws IOException {
        setBatchApplicationIdForTestApp();

        Request request1 = new Request(null, "TourEiffel");
        Request request2 = new Request(null, "SpaceNeedle");

        List<Response> responses = Request.executeBatch(request1, request2);
        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() == null);
        assertTrue(responses.get(1).getError() == null);

        GraphPlace eiffelTower = responses.get(0).getGraphObjectAs(GraphPlace.class);
        GraphPlace spaceNeedle = responses.get(1).getGraphObjectAs(GraphPlace.class);
        assertTrue(eiffelTower != null);
        assertTrue(spaceNeedle != null);

        assertEquals("Paris", eiffelTower.getLocation().getCity());
        assertEquals("Seattle", spaceNeedle.getLocation().getCity());
    }

    @MediumTest
    @LargeTest
    public void testFacebookErrorResponsesCreateErrors() {
        setBatchApplicationIdForTestApp();

        Request request1 = new Request(null, "somestringthatshouldneverbeavalidfobjectid");
        Request request2 = new Request(null, "someotherstringthatshouldneverbeavalidfobjectid");
        List<Response> responses = Request.executeBatch(request1, request2);

        assertEquals(2, responses.size());
        assertTrue(responses.get(0).getError() != null);
        assertTrue(responses.get(1).getError() != null);

        FacebookException exception1 = responses.get(0).getError();
        assertTrue(exception1 instanceof FacebookServiceErrorException);
        FacebookServiceErrorException serviceException1 = (FacebookServiceErrorException) exception1;
        assertTrue(serviceException1.getFacebookErrorType() != null);
        assertTrue(serviceException1.getFacebookErrorCode() != FacebookServiceErrorException.UNKNOWN_ERROR_CODE);
    }

    @LargeTest
    public void testBatchPostStatusUpdate() {
        TestSession session = openTestSessionWithSharedUser();

        GraphObject statusUpdate1 = createStatusUpdate();
        GraphObject statusUpdate2 = createStatusUpdate();

        Request postRequest1 = Request.newPostRequest(session, "me/feed", statusUpdate1, null);
        postRequest1.setBatchEntryName("postRequest1");
        Request postRequest2 = Request.newPostRequest(session, "me/feed", statusUpdate2, null);
        postRequest2.setBatchEntryName("postRequest2");
        Request getRequest1 = new Request(session, "{result=postRequest1:$.id}");
        Request getRequest2 = new Request(session, "{result=postRequest2:$.id}");

        List<Response> responses = Request.executeBatch(postRequest1, postRequest2, getRequest1, getRequest2);
        assertNotNull(responses);
        assertEquals(4, responses.size());
        assertNoErrors(responses);

        GraphObject retrievedStatusUpdate1 = responses.get(2).getGraphObject();
        GraphObject retrievedStatusUpdate2 = responses.get(3).getGraphObject();
        assertNotNull(retrievedStatusUpdate1);
        assertNotNull(retrievedStatusUpdate2);

        assertEquals(statusUpdate1.get("message"), retrievedStatusUpdate1.get("message"));
        assertEquals(statusUpdate2.get("message"), retrievedStatusUpdate2.get("message"));
    }

    @LargeTest
    public void testTwoDifferentAccessTokens() {
        TestSession session1 = openTestSessionWithSharedUser();
        TestSession session2 = openTestSessionWithSharedUser(SECOND_TEST_USER_TAG);

        Request request1 = Request.newMeRequest(session1, null);
        Request request2 = Request.newMeRequest(session2, null);

        List<Response> responses = Request.executeBatch(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        GraphUser user1 = responses.get(0).getGraphObjectAs(GraphUser.class);
        GraphUser user2 = responses.get(1).getGraphObjectAs(GraphUser.class);

        assertNotNull(user1);
        assertNotNull(user2);

        assertFalse(user1.getId().equals(user2.getId()));
        assertEquals(session1.getTestUserId(), user1.getId());
        assertEquals(session2.getTestUserId(), user2.getId());
    }

    @LargeTest
    public void testBatchWithValidSessionAndNoSession() {
        TestSession session = openTestSessionWithSharedUser();

        Request request1 = new Request(session, "me");
        Request request2 = new Request(null, "zuck");

        List<Response> responses = Request.executeBatch(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        GraphUser user1 = responses.get(0).getGraphObjectAs(GraphUser.class);
        GraphUser user2 = responses.get(1).getGraphObjectAs(GraphUser.class);

        assertNotNull(user1);
        assertNotNull(user2);

        assertFalse(user1.getId().equals(user2.getId()));
        assertEquals(session.getTestUserId(), user1.getId());
        assertEquals("4", user2.getId());
    }

    @LargeTest
    public void testBatchWithNoSessionAndValidSession() {
        TestSession session = openTestSessionWithSharedUser();

        Request request1 = new Request(null, "zuck");
        Request request2 = new Request(session, "me");

        List<Response> responses = Request.executeBatch(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        GraphUser user1 = responses.get(0).getGraphObjectAs(GraphUser.class);
        GraphUser user2 = responses.get(1).getGraphObjectAs(GraphUser.class);

        assertNotNull(user1);
        assertNotNull(user2);

        assertFalse(user1.getId().equals(user2.getId()));
        assertEquals("4", user1.getId());
        assertEquals(session.getTestUserId(), user2.getId());
    }

    @LargeTest
    public void testBatchWithTwoSessionlessRequestsAndDefaultAppID() {
        TestSession session = getTestSessionWithSharedUser(null);
        String appId = session.getApplicationId();
        Request.setDefaultBatchApplicationId(appId);

        Request request1 = new Request(null, "zuck");
        Request request2 = new Request(null, "zuck");

        List<Response> responses = Request.executeBatch(request1, request2);
        assertNotNull(responses);
        assertEquals(2, responses.size());

        GraphUser user1 = responses.get(0).getGraphObjectAs(GraphUser.class);
        GraphUser user2 = responses.get(1).getGraphObjectAs(GraphUser.class);

        assertNotNull(user1);
        assertNotNull(user2);

        assertEquals("4", user1.getId());
        assertEquals("4", user2.getId());
    }

    @LargeTest
    public void testMixedSuccessAndFailure() {
        TestSession session = openTestSessionWithSharedUser();

        final int NUM_REQUESTS = 8;
        Request[] requests = new Request[NUM_REQUESTS];
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;
            requests[i] = new Request(session, shouldSucceed ? "me" : "-1");
        }

        List<Response> responses = Request.executeBatch(requests);
        assertNotNull(responses);
        assertEquals(NUM_REQUESTS, responses.size());

        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;

            Response response = responses.get(i);
            assertNotNull(response);
            if (shouldSucceed) {
                assertNull(response.getError());
                assertNotNull(response.getGraphObject());
            } else {
                assertNotNull(response.getError());
                assertNull(response.getGraphObject());
            }
        }
    }

    @LargeTest
    public void testBatchUploadPhoto() {
        TestSession session = openTestSessionWithSharedUserAndPermissions(null, "user_photos");

        final int image1Size = 120;
        final int image2Size = 150;

        Bitmap bitmap1 = createTestBitmap(image1Size);
        Bitmap bitmap2 = createTestBitmap(image2Size);

        Request uploadRequest1 = Request.newUploadPhotoRequest(session, bitmap1, null);
        uploadRequest1.setBatchEntryName("uploadRequest1");
        Request uploadRequest2 = Request.newUploadPhotoRequest(session, bitmap2, null);
        uploadRequest2.setBatchEntryName("uploadRequest2");
        Request getRequest1 = new Request(session, "{result=uploadRequest1:$.id}");
        Request getRequest2 = new Request(session, "{result=uploadRequest2:$.id}");

        List<Response> responses = Request.executeBatch(uploadRequest1, uploadRequest2, getRequest1, getRequest2);
        assertNotNull(responses);
        assertEquals(4, responses.size());
        assertNoErrors(responses);

        GraphObject retrievedPhoto1 = responses.get(2).getGraphObject();
        GraphObject retrievedPhoto2 = responses.get(3).getGraphObject();
        assertNotNull(retrievedPhoto1);
        assertNotNull(retrievedPhoto2);

        assertEquals(image1Size, retrievedPhoto1.get("width"));
        assertEquals(image2Size, retrievedPhoto2.get("width"));
    }

    @MediumTest
    @LargeTest
    public void testCallbacksAreCalled() {
        setBatchApplicationIdForTestApp();

        ArrayList<Request> requests = new ArrayList<Request>();
        final ArrayList<Boolean> calledBack = new ArrayList<Boolean>();

        final int NUM_REQUESTS = 4;
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            Request request = new Request(null, "4");

            request.setCallback(new Request.Callback() {
                @Override
                public void onCompleted(Response response) {
                    calledBack.add(true);
                }
            });

            requests.add(request);
        }

        List<Response> responses = Request.executeBatch(requests);
        assertNotNull(responses);
        assertTrue(calledBack.size() == NUM_REQUESTS);
    }
}
