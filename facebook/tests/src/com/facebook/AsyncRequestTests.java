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
import java.util.Arrays;
import java.util.List;

import android.graphics.Bitmap;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.Request;
import com.facebook.Response;

public class AsyncRequestTests extends FacebookTestCase {

    @SmallTest
    @MediumTest
    @LargeTest
    public void testExecuteWithNullRequestsThrows() throws Exception {
        try {
            TestRequestAsyncTask task = new TestRequestAsyncTask((Request[]) null);

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
            TestRequestAsyncTask task = new TestRequestAsyncTask(new Request[] {});

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
            TestRequestAsyncTask task = new TestRequestAsyncTask(new Request[] { null });

            task.executeOnBlockerThread();

            waitAndAssertSuccessOrRethrow(1);

            fail("expected NullPointerException");
        } catch (NullPointerException exception) {
        }

    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGet() {
        Request request = new Request(null, "TourEiffel", null, null, new ExpectSuccessCallback() {
            @Override
            protected void performAsserts(Response response) {
                assertNotNull(response);
                GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
                assertEquals("Paris", graphPlace.getLocation().getCity());
            }
        });

        TestRequestAsyncTask task = new TestRequestAsyncTask(request);

        task.executeOnBlockerThread();

        // Note: 2, not 1, because the overall async task signals as well.
        waitAndAssertSuccess(2);
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetUsingHttpURLConnection() {
        Request request = new Request(null, "TourEiffel", null, null, new ExpectSuccessCallback() {
            @Override
            protected void performAsserts(Response response) {
                assertNotNull(response);
                GraphPlace graphPlace = response.getGraphObjectAs(GraphPlace.class);
                assertEquals("Paris", graphPlace.getLocation().getCity());
            }
        });
        HttpURLConnection connection = Request.toHttpConnection(request);

        TestRequestAsyncTask task = new TestRequestAsyncTask(connection, Arrays.asList(new Request[] { request }));

        task.executeOnBlockerThread();

        // Note: 2, not 1, because the overall async task signals as well.
        waitAndAssertSuccess(2);
    }

    @MediumTest
    @LargeTest
    public void testExecuteSingleGetFailureCase() {
        Request request = new Request(null, "-1", null, null, new ExpectFailureCallback());

        TestRequestAsyncTask task = new TestRequestAsyncTask(request);

        task.executeOnBlockerThread();

        // Note: 2, not 1, because the overall async task signals as well.
        waitAndAssertSuccess(2);
    }

    @SmallTest
    @MediumTest
    @LargeTest
    public void testBatchWithoutAppIDThrows() throws Throwable {
        try {
            Request request1 = new Request(null, "TourEiffel", null, null, new ExpectSuccessCallback());
            Request request2 = new Request(null, "SpaceNeedle", null, null, new ExpectSuccessCallback());

            TestRequestAsyncTask task = new TestRequestAsyncTask(request1, request2);

            task.executeOnBlockerThread();

            // Note: 2, not 1, because the overall async task signals as well.
            waitAndAssertSuccessOrRethrow(2);

            fail("expected FacebookException");
        } catch (FacebookException exception) {
        }
    }

    @LargeTest
    public void testMixedSuccessAndFailure() {
        TestSession session = openTestSessionWithSharedUser();

        final int NUM_REQUESTS = 8;
        Request[] requests = new Request[NUM_REQUESTS];
        for (int i = 0; i < NUM_REQUESTS; ++i) {
            boolean shouldSucceed = (i % 2) == 1;
            if (shouldSucceed) {
                requests[i] = new Request(session, "me", null, null, new ExpectSuccessCallback());
            } else {
                requests[i] = new Request(session, "-1", null, null, new ExpectFailureCallback());
            }
        }

        TestRequestAsyncTask task = new TestRequestAsyncTask(requests);

        task.executeOnBlockerThread();

        // Note: plus 1, because the overall async task signals as well.
        waitAndAssertSuccess(NUM_REQUESTS + 1);
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
        Request getRequest1 = new Request(session, "{result=uploadRequest1:$.id}", null, null,
                new ExpectSuccessCallback() {
                    @Override
                    protected void performAsserts(Response response) {
                        assertNotNull(response);
                        GraphObject retrievedPhoto = response.getGraphObject();
                        assertNotNull(retrievedPhoto);
                        assertEquals(image1Size, retrievedPhoto.get("width"));
                    }
                });
        Request getRequest2 = new Request(session, "{result=uploadRequest2:$.id}", null, null,
                new ExpectSuccessCallback() {
                    @Override
                    protected void performAsserts(Response response) {
                        assertNotNull(response);
                        GraphObject retrievedPhoto = response.getGraphObject();
                        assertNotNull(retrievedPhoto);
                        assertEquals(image2Size, retrievedPhoto.get("width"));
                    }
                });

        TestRequestAsyncTask task = new TestRequestAsyncTask(uploadRequest1, uploadRequest2, getRequest1, getRequest2);
        task.executeOnBlockerThread();

        // Note: 2, not 1, because the overall async task signals as well.
        waitAndAssertSuccess(3);
    }

}
