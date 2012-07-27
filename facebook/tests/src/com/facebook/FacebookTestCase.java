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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.test.ActivityUnitTestCase;
import android.util.Log;

public class FacebookTestCase extends ActivityUnitTestCase<FacebookTestCase.FacebookTestActivity> {
    private static String applicationId;
    private static String applicationSecret;

    public final static String SECOND_TEST_USER_TAG = "Second";
    public final static String THIRD_TEST_USER_TAG = "Third";

    private TestBlocker testBlocker;

    protected synchronized TestBlocker getTestBlocker() {
        if (testBlocker == null) {
            testBlocker = TestBlocker.createTestBlocker();
        }
        return testBlocker;
    }

    public FacebookTestCase() {
        super(FacebookTestActivity.class);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithSharedUser(TestBlocker testBlocker) {
        return getTestSessionWithSharedUser(testBlocker, null);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithSharedUser(TestBlocker testBlocker, String sessionUniqueUserTag) {
        return getTestSessionWithSharedUserAndPermissions(testBlocker, sessionUniqueUserTag, (String[]) null);
    }

    protected TestSession getTestSessionWithSharedUserAndPermissions(TestBlocker testBlocker,
            String sessionUniqueUserTag, String... permissions) {
        Looper looper = (testBlocker != null) ? testBlocker.getLooper() : null;
        List<String> permissionsList = (permissions != null) ? Arrays.asList(permissions) : null;
        return TestSession.createSessionWithSharedUser(getStartedActivity(), permissionsList, sessionUniqueUserTag,
                looper);
    }

    // Returns an un-opened TestSession
    protected TestSession getTestSessionWithPrivateUser(TestBlocker testBlocker) {
        // TODO determine permissions
        Looper looper = (testBlocker != null) ? testBlocker.getLooper() : null;
        return TestSession.createSessionWithPrivateUser(getStartedActivity(), null, looper);
    }

    protected TestSession openTestSessionWithSharedUser(final TestBlocker blocker) {
        return openTestSessionWithSharedUser(blocker, null);
    }

    protected TestSession openTestSessionWithSharedUser(final TestBlocker blocker, String sessionUniqueUserTag) {
        TestSession session = getTestSessionWithSharedUser(blocker);
        openSession(getStartedActivity(), session, blocker);
        return session;
    }

    protected TestSession openTestSessionWithSharedUser() {
        return openTestSessionWithSharedUser((String) null);
    }

    protected TestSession openTestSessionWithSharedUser(String sessionUniqueUserTag) {
        return openTestSessionWithSharedUserAndPermissions(sessionUniqueUserTag, (String[]) null);
    }

    protected TestSession openTestSessionWithSharedUserAndPermissions(String sessionUniqueUserTag,
            String... permissions) {
        final TestBlocker blocker = getTestBlocker();
        TestSession session = getTestSessionWithSharedUserAndPermissions(blocker, sessionUniqueUserTag, permissions);
        openSession(getStartedActivity(), session, blocker);
        return session;
    }

    // Turns exceptions from the TestBlocker into JUnit assertions
    protected void waitAndAssertSuccess(TestBlocker testBlocker, int numSignals) {
        try {
            testBlocker.waitForSignalsAndAssertSuccess(numSignals);
        } catch (AssertionFailedError e) {
            throw e;
        } catch (Exception e) {
            fail("Got exception: " + e.getMessage());
        }
    }

    protected void waitAndAssertSuccess(int numSignals) {
        waitAndAssertSuccess(getTestBlocker(), numSignals);
    }

    protected void waitAndAssertSuccessOrRethrow(int numSignals) throws Exception {
        getTestBlocker().waitForSignalsAndAssertSuccess(numSignals);
    }

    protected Activity getStartedActivity() {
        Activity result = getActivity();
        if (result == null) {
            result = startActivity(new Intent(Intent.ACTION_MAIN), null, null);
        }
        return result;
    }

    protected synchronized void readApplicationIdAndSecret() {
        synchronized (FacebookTestCase.class) {
            if (applicationId != null && applicationSecret != null) {
                return;
            }

            AssetManager assets = getInstrumentation().getContext().getResources().getAssets();
            InputStream stream = null;
            final String errorMessage = "could not read applicationId and applicationSecret from config.json; ensure "
                    + "you have run 'configure_unit_tests.sh'. Error: ";
            try {
                stream = assets.open("config.json");
                String string = Utility.readStreamToString(stream);

                JSONTokener tokener = new JSONTokener(string);
                Object obj = tokener.nextValue();
                if (!(obj instanceof JSONObject)) {
                    fail(errorMessage + "could not deserialize a JSONObject");
                }
                JSONObject jsonObject = (JSONObject) obj;

                applicationId = jsonObject.optString("applicationId");
                applicationSecret = jsonObject.optString("applicationSecret");

                if (Utility.isNullOrEmpty(applicationId) || Utility.isNullOrEmpty(applicationSecret)) {
                    fail(errorMessage + "one or both config values are missing");
                }

                String machineUniqueUserTag = jsonObject.optString("machineUniqueUserTag");

                TestSession.setTestApplicationId(applicationId);
                TestSession.setTestApplicationSecret(applicationSecret);
                TestSession.setMachineUniqueUserTag(machineUniqueUserTag);
            } catch (IOException e) {
                fail(errorMessage + e.toString());
            } catch (JSONException e) {
                fail(errorMessage + e.toString());
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        fail(errorMessage + e.toString());
                    }
                }
            }
        }
    }

    protected void openSession(Activity activity, TestSession session) {
        final TestBlocker blocker = getTestBlocker();
        openSession(activity, session, blocker);
    }

    protected void openSession(Activity activity, TestSession session, final TestBlocker blocker) {
        session.open(activity, new SessionStatusCallback() {
            @Override
            public void call(Session session, SessionState state, Exception exception) {
                if (exception != null) {
                    // TODO constant for logging tag
                    Log.w("FacebookTestCase", "openSession: received an error opening session: " + exception.toString());
                }
                assertTrue(exception == null);
                blocker.signal();
            }
        });

        waitAndAssertSuccess(blocker, 1);
    }

    protected void setUp() throws Exception {
        super.setUp();

        // Make sure we have read application ID and secret.
        readApplicationIdAndSecret();

        // These are useful for debugging unit test failures.
        Settings.addLoggingBehavior(LoggingBehaviors.REQUESTS);
        Settings.addLoggingBehavior(LoggingBehaviors.INCLUDE_ACCESS_TOKENS);
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        if (testBlocker != null) {
            testBlocker.quit();
        }
    }

    interface GraphObjectPostResult extends GraphObject {
        String getId();
    }

    protected GraphObject getAndAssert(Session session, String id) {
        Request request = new Request(session, id);
        Response response = request.execute();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObject result = response.getGraphObject();
        assertNotNull(result);

        return result;
    }

    protected GraphObject postGetAndAssert(Session session, String path, GraphObject graphObject) {
        Request request = Request.newPostRequest(session, path, graphObject, null);
        Response response = request.execute();
        assertNotNull(response);

        Exception exception = response.getError();
        assertNull(exception);

        GraphObjectPostResult result = response.getGraphObjectAs(GraphObjectPostResult.class);
        assertNotNull(result);
        assertNotNull(result.getId());

        return getAndAssert(session, result.getId());
    }

    protected void setBatchApplicationIdForTestApp() {
        String appId = TestSession.getTestApplicationId();
        Request.setDefaultBatchApplicationId(appId);
    }

    protected GraphObject createStatusUpdate() {
        GraphObject statusUpdate = GraphObjectWrapper.createGraphObject();
        String message = String.format(
                "Check out my awesome new status update posted at: %s. Some chars for you: \"[]:,", new Date());
        statusUpdate.put("message", message);
        return statusUpdate;
    }

    protected Bitmap createTestBitmap(int size) {
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        image.eraseColor(Color.BLUE);
        return image;
    }

    protected void assertNoErrors(List<Response> responses) {
        for (int i = 0; i < responses.size(); ++i) {
            Response response = responses.get(i);
            assertNotNull(responses);
            assertNull(response.getError());
        }
    }

    protected void runOnBlockerThread(final Runnable runnable, boolean waitForCompletion) {
        Runnable runnableToPost = runnable;
        final ConditionVariable condition = waitForCompletion ? new ConditionVariable(!waitForCompletion) : null;

        if (waitForCompletion) {
            runnableToPost = new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    condition.open();
                }
            };
        }

        TestBlocker blocker = getTestBlocker();
        Handler handler = blocker.getHandler();
        handler.post(runnableToPost);

        if (waitForCompletion) {
            boolean success = condition.block(2000);
            assertTrue(success);
        }
    }

    protected void closeBlockerAndAssertSuccess() {
        TestBlocker blocker = getTestBlocker();
        testBlocker = null;

        blocker.quit();

        boolean joined = false;
        while (!joined) {
            try {
                blocker.join();
                joined = true;
            } catch (InterruptedException e) {
            }
        }

        try {
            blocker.assertSuccess();
        } catch (Exception e) {
            fail(e.toString());
        }
    }

    public static class FacebookTestActivity extends Activity {
    }

    /*
     * Classes and helpers related to asynchronous requests.
     */

    // A subclass of RequestAsyncTask that knows how to interact with TestBlocker to ensure that tests can wait
    // on and assert success of async tasks.
    protected class TestRequestAsyncTask extends RequestAsyncTask {
        private final TestBlocker blocker = FacebookTestCase.this.getTestBlocker();

        public TestRequestAsyncTask(Request... requests) {
            super(requests);
        }

        public TestRequestAsyncTask(List<Request> requests) {
            super(requests);
        }

        public TestRequestAsyncTask(HttpURLConnection connection, Request... requests) {
            super(connection, requests);
        }

        public TestRequestAsyncTask(HttpURLConnection connection, List<Request> requests) {
            super(connection, requests);
        }

        public final TestBlocker getBlocker() {
            return blocker;
        }

        public final Exception getThrowable() {
            return getException();
        }

        protected void onPostExecute(List<Response> result) {
            try {
                super.onPostExecute(result);

                if (getException() != null) {
                    blocker.setException(getException());
                }
            } finally {
                Log.d("TestRequestAsyncTask", "signaling blocker");
                blocker.signal();
            }
        }

        // In order to be able to block and accumulate exceptions, we want to ensure the async task is really
        // being started on the blocker's thread, rather than the test's thread. Use this instead of calling
        // execute directly in unit tests.
        public void executeOnBlockerThread() {
            Runnable runnable = new Runnable() {
                public void run() {
                    execute();
                }
            };
            Handler handler = new Handler(blocker.getLooper());
            handler.post(runnable);
        }
    }

    // Provides an implementation of Request.Callback that will assert either success (no error) or failure (error)
    // of a request, and allow derived classes to perform additional asserts.
    protected class TestCallback implements Request.Callback {
        private final TestBlocker blocker;
        private final boolean expectSuccess;

        public TestCallback(TestBlocker blocker, boolean expectSuccess) {
            this.blocker = blocker;
            this.expectSuccess = expectSuccess;
        }

        public TestCallback(boolean expectSuccess) {
            this(FacebookTestCase.this.getTestBlocker(), expectSuccess);
        }

        @Override
        public void onCompleted(Response response) {
            try {
                // We expect to be called on the right thread.
                if (Thread.currentThread() != blocker) {
                    throw new FacebookException("Invalid thread " + Thread.currentThread().getId()
                            + "; expected to be called on thread " + blocker.getId());
                }

                // We expect either success or failure.
                if (expectSuccess && response.getError() != null) {
                    throw response.getError();
                } else if (!expectSuccess && response.getError() == null) {
                    throw new FacebookException("Expected failure case, received no error");
                }

                // Some tests may want more fine-grained control and assert additional conditions.
                performAsserts(response);
            } catch (Exception e) {
                blocker.setException(e);
            } finally {
                // Tell anyone waiting on us that this callback was called.
                blocker.signal();
            }
        }

        protected void performAsserts(Response response) {
        }
    }

    // A callback that will assert if the request resulted in an error.
    protected class ExpectSuccessCallback extends TestCallback {
        public ExpectSuccessCallback() {
            super(true);
        }
    }

    // A callback that will assert if the request did NOT result in an error.
    protected class ExpectFailureCallback extends TestCallback {
        public ExpectFailureCallback() {
            super(false);
        }
    }

}
