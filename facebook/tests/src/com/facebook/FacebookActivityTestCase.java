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

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;
import com.facebook.internal.Utility;
import junit.framework.AssertionFailedError;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FacebookActivityTestCase<T extends Activity> extends ActivityInstrumentationTestCase2<T> {
    private static final String TAG = FacebookActivityTestCase.class.getSimpleName();

    private static String applicationId;
    private static String applicationSecret;
    private static String clientToken;
    private static TestUserManager testUserManager;

    public final static String SECOND_TEST_USER_TAG = "Second";
    public final static String THIRD_TEST_USER_TAG = "Third";

    private TestBlocker testBlocker;

    protected synchronized TestBlocker getTestBlocker() {
        if (testBlocker == null) {
            testBlocker = TestBlocker.createTestBlocker();
        }
        return testBlocker;
    }

    public FacebookActivityTestCase(Class<T> activityClass) {
        super("", activityClass);
    }

    protected String[] getDefaultPermissions() { return null; };

    protected AccessToken getAccessTokenForSharedUser() {
        return getAccessTokenForSharedUser(null);
    }

    protected AccessToken getAccessTokenForSharedUser(String sessionUniqueUserTag) {
        return getAccessTokenForSharedUserWithPermissions(sessionUniqueUserTag,
                getDefaultPermissions());
    }

    protected AccessToken getAccessTokenForSharedUserWithPermissions(String sessionUniqueUserTag,
        List<String> permissions) {
        return getTestUserManager().getAccessTokenForSharedUser(permissions, sessionUniqueUserTag);
    }

    protected AccessToken getAccessTokenForSharedUserWithPermissions(String sessionUniqueUserTag,
                                                                      String... permissions) {
        List<String> permissionList = (permissions != null) ? Arrays.asList(permissions) : null;
        return getAccessTokenForSharedUserWithPermissions(sessionUniqueUserTag, permissionList);
    }

    protected TestUserManager getTestUserManager() {
        if (testUserManager == null) {
            synchronized (FacebookActivityTestCase.class) {
                if (testUserManager == null) {
                    readApplicationIdAndSecret();
                    testUserManager = new TestUserManager(applicationSecret, applicationId);
                }
            }
        }

        return testUserManager;
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

    protected void runAndBlockOnUiThread(final int expectedSignals, final Runnable runnable) throws Throwable {
        final TestBlocker blocker = getTestBlocker();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                blocker.signal();
            }
        });
        // We wait for the operation to complete; wait for as many other signals as we expect.
        blocker.waitForSignals(1 + expectedSignals);
        // Wait for the UI thread to become idle so any UI updates the runnable triggered have a chance
        // to finish before we return.
        getInstrumentation().waitForIdleSync();
    }

    protected synchronized void readApplicationIdAndSecret() {
        synchronized (FacebookTestCase.class) {
            if (applicationId != null && applicationSecret != null && clientToken != null) {
                return;
            }

            AssetManager assets = getInstrumentation().getTargetContext().getResources().getAssets();
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
                clientToken = jsonObject.optString("clientToken");

                if (Utility.isNullOrEmpty(applicationId) || Utility.isNullOrEmpty(applicationSecret) ||
                        Utility.isNullOrEmpty(clientToken)) {
                    fail(errorMessage + "config values are missing");
                }
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

    protected static String getApplicationId() {
        return applicationId;
    }

    protected static String getApplicationSecret() {
        return applicationSecret;
    }

    protected void setUp() throws Exception {
        super.setUp();

        // Make sure the logging is turned on.
        FacebookSdk.setIsDebugEnabled(true);

        // Make sure we have read application ID and secret.
        readApplicationIdAndSecret();

        FacebookSdk.sdkInitialize(getInstrumentation().getTargetContext());
        FacebookSdk.setApplicationId(applicationId);
        FacebookSdk.setClientToken(clientToken);

        // These are useful for debugging unit test failures.
        FacebookSdk.addLoggingBehavior(LoggingBehavior.REQUESTS);
        FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        // We want the UI thread to be in StrictMode to catch any violations.
        turnOnStrictModeForUiThread();

        // Needed to bypass a dexmaker bug for mockito
        System.setProperty("dexmaker.dexcache",
                getInstrumentation().getTargetContext().getCacheDir().getPath());
    }

    protected void tearDown() throws Exception {
        super.tearDown();

        synchronized (this) {
            if (testBlocker != null) {
                testBlocker.quit();
            }
        }
    }

    protected Bundle getNativeLinkingExtras(String token, String userId) {
        readApplicationIdAndSecret();

        Bundle extras = new Bundle();
        String extraLaunchUriString = String
                .format("fbrpc://facebook/nativethirdparty?app_id=%s&package_name=com.facebook.sdk.tests&class_name=com.facebook.FacebookActivityTests$FacebookTestActivity&access_token=%s",
                        applicationId, token);
        extras.putString("extra_launch_uri", extraLaunchUriString);
        extras.putString("expires_in", "3600");
        extras.putLong("app_id", Long.parseLong(applicationId));
        extras.putString("access_token", token);
        if(userId != null && !userId.isEmpty()) {
            extras.putString("user_id", userId);
        }

        return extras;
    }

    protected JSONObject getAndAssert(AccessToken accessToken, String id) {
        GraphRequest request = new GraphRequest(accessToken, id);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);

        return result;
    }

    protected JSONObject postGetAndAssert(AccessToken accessToken, String path,
                                          JSONObject graphObject) {
        GraphRequest request = GraphRequest.newPostRequest(accessToken, path, graphObject, null);
        GraphResponse response = request.executeAndWait();
        assertNotNull(response);

        assertNull(response.getError());

        JSONObject result = response.getJSONObject();
        assertNotNull(result);
        assertNotNull(result.optString("id"));

        return getAndAssert(accessToken, result.optString("id"));
    }

    protected void setBatchApplicationIdForTestApp() {
        readApplicationIdAndSecret();
        GraphRequest.setDefaultBatchApplicationId(applicationId);
    }

    protected JSONObject batchCreateAndGet(AccessToken accessToken, String graphPath,
                                           JSONObject graphObject, String fields) {
        GraphRequest create = GraphRequest.newPostRequest(accessToken, graphPath, graphObject,
                new ExpectSuccessCallback());
        create.setBatchEntryName("create");
        GraphRequest get = GraphRequest.newGraphPathRequest(accessToken, "{result=create:$.id}",
                new ExpectSuccessCallback());
        if (fields != null) {
            Bundle parameters = new Bundle();
            parameters.putString("fields", fields);
            get.setParameters(parameters);
        }

        return batchPostAndGet(create, get);
    }

    protected JSONObject batchUpdateAndGet(AccessToken accessToken, String graphPath,
                                           JSONObject graphObject, String fields) {
        GraphRequest update = GraphRequest.newPostRequest(accessToken, graphPath, graphObject,
                new ExpectSuccessCallback());
        GraphRequest get = GraphRequest.newGraphPathRequest(accessToken, graphPath,
                new ExpectSuccessCallback());
        if (fields != null) {
            Bundle parameters = new Bundle();
            parameters.putString("fields", fields);
            get.setParameters(parameters);
        }

        return batchPostAndGet(update, get);
    }

    protected JSONObject batchPostAndGet(GraphRequest post, GraphRequest get) {
        List<GraphResponse> responses = GraphRequest.executeBatchAndWait(post, get);
        assertEquals(2, responses.size());

        JSONObject resultGraphObject = responses.get(1).getJSONObject();
        assertNotNull(resultGraphObject);
        return resultGraphObject;
    }

    protected JSONObject createStatusUpdate(String unique) {
        JSONObject statusUpdate = new JSONObject();
        String message = String.format(
                "Check out my awesome new status update posted at: %s. Some chars for you: +\"[]:,%s", new Date(),
                unique);
        try {
            statusUpdate.put("message", message);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return statusUpdate;
    }

    protected Bitmap createTestBitmap(int size) {
        Bitmap image = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        image.eraseColor(Color.BLUE);
        return image;
    }

    protected void assertDateEqualsWithinDelta(Date expected, Date actual, long deltaInMsec) {
        long delta = Math.abs(expected.getTime() - actual.getTime());
        assertTrue(delta < deltaInMsec);
    }

    protected void assertDateDiffersWithinDelta(Date expected, Date actual, long expectedDifference, long deltaInMsec) {
        long delta = Math.abs(expected.getTime() - actual.getTime()) - expectedDifference;
        assertTrue(delta < deltaInMsec);
    }

    protected void assertNoErrors(List<GraphResponse> responses) {
        for (int i = 0; i < responses.size(); ++i) {
            GraphResponse response = responses.get(i);
            assertNotNull(response);
            assertNull(response.getError());
        }
    }

    protected File createTempFileFromAsset(String assetPath) throws IOException {
        InputStream inputStream = null;
        FileOutputStream outStream = null;

        try {
            AssetManager assets = getActivity().getResources().getAssets();
            inputStream = assets.open(assetPath);

            File outputDir = getActivity().getCacheDir(); // context being the Activity pointer
            File outputFile = File.createTempFile("prefix", assetPath, outputDir);
            outStream = new FileOutputStream(outputFile);

            final int bufferSize = 1024 * 2;
            byte[] buffer = new byte[bufferSize];
            int n = 0;
            while ((n = inputStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, n);
            }

            return outputFile;
        } finally {
            Utility.closeQuietly(outStream);
            Utility.closeQuietly(inputStream);
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
            boolean success = condition.block(10000);
            assertTrue(success);
        }
    }

    protected void closeBlockerAndAssertSuccess() {
        TestBlocker blocker;
        synchronized (this) {
            blocker = getTestBlocker();
            testBlocker = null;
        }

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

    protected TestGraphRequestAsyncTask createAsyncTaskOnUiThread(final GraphRequest... requests) throws Throwable {
        final ArrayList<TestGraphRequestAsyncTask> result = new ArrayList<TestGraphRequestAsyncTask>();
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result.add(new TestGraphRequestAsyncTask(requests));
            }
        });
        return result.isEmpty() ? null : result.get(0);
    }

    /*
     * Classes and helpers related to asynchronous requests.
     */

    // A subclass of RequestAsyncTask that knows how to interact with TestBlocker to ensure that tests can wait
    // on and assert success of async tasks.
    protected class TestGraphRequestAsyncTask extends GraphRequestAsyncTask {
        private final TestBlocker blocker = FacebookActivityTestCase.this.getTestBlocker();

        public TestGraphRequestAsyncTask(GraphRequest... requests) {
            super(requests);
        }

        public TestGraphRequestAsyncTask(List<GraphRequest> requests) {
            super(requests);
        }

        public TestGraphRequestAsyncTask(GraphRequestBatch requests) {
            super(requests);
        }

        public TestGraphRequestAsyncTask(HttpURLConnection connection, GraphRequest... requests) {
            super(connection, requests);
        }

        public TestGraphRequestAsyncTask(HttpURLConnection connection, List<GraphRequest> requests) {
            super(connection, requests);
        }

        public TestGraphRequestAsyncTask(HttpURLConnection connection, GraphRequestBatch requests) {
            super(connection, requests);
        }

        public final TestBlocker getBlocker() {
            return blocker;
        }

        public final Exception getThrowable() {
            return getException();
        }

        protected void onPostExecute(List<GraphResponse> result) {
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
            ensureAsyncTaskLoaded();

            Runnable runnable = new Runnable() {
                public void run() {
                    execute();
                }
            };
            Handler handler = new Handler(blocker.getLooper());
            handler.post(runnable);
        }

        private void ensureAsyncTaskLoaded() {
            // Work around this issue on earlier frameworks: http://stackoverflow.com/a/7818839/782044
            try {
                runAndBlockOnUiThread(0, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Class.forName("android.os.AsyncTask");
                        } catch (ClassNotFoundException e) {
                        }
                    }
                });
            } catch (Throwable throwable) {
            }
        }
    }

    // Provides an implementation of Request.Callback that will assert either success (no error) or failure (error)
    // of a request, and allow derived classes to perform additional asserts.
    protected class TestCallback implements GraphRequest.Callback {
        private final TestBlocker blocker;
        private final boolean expectSuccess;

        public TestCallback(TestBlocker blocker, boolean expectSuccess) {
            this.blocker = blocker;
            this.expectSuccess = expectSuccess;
        }

        public TestCallback(boolean expectSuccess) {
            this(FacebookActivityTestCase.this.getTestBlocker(), expectSuccess);
        }

        @Override
        public void onCompleted(GraphResponse response) {
            try {
                // We expect to be called on the right thread.
                if (Thread.currentThread() != blocker) {
                    throw new FacebookException("Invalid thread " + Thread.currentThread().getId()
                            + "; expected to be called on thread " + blocker.getId());
                }

                // We expect either success or failure.
                if (expectSuccess && response.getError() != null) {
                    throw response.getError().getException();
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

        protected void performAsserts(GraphResponse response) {
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

    public static abstract class MockGraphRequest extends GraphRequest {
        public abstract GraphResponse createResponse();
    }

    public static class MockGraphRequestBatch extends GraphRequestBatch {
        public MockGraphRequestBatch(MockGraphRequest... requests) {
            super(requests);
        }

        // Caller must ensure that all the requests in the batch are, in fact, MockRequests.
        public MockGraphRequestBatch(GraphRequestBatch requests) {
            super(requests);
        }

        @Override
        List<GraphResponse> executeAndWaitImpl() {
            List<GraphRequest> requests = getRequests();

            List<GraphResponse> responses = new ArrayList<GraphResponse>();
            for (GraphRequest request : requests) {
                MockGraphRequest mockRequest = (MockGraphRequest) request;
                responses.add(mockRequest.createResponse());
            }

            GraphRequest.runCallbacks(this, responses);

            return responses;
        }
    }

    private AtomicBoolean strictModeOnForUiThread = new AtomicBoolean();

    protected void turnOnStrictModeForUiThread() {
        // We only ever need to do this once. If the boolean is true, we know that the next runnable
        // posted to the UI thread will have strict mode on.
        if (strictModeOnForUiThread.get() == false) {
            try {
                runTestOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Double-check whether we really need to still do this on the UI thread.
                        if (strictModeOnForUiThread.compareAndSet(false, true)) {
                            turnOnStrictModeForThisThread();
                        }
                    }
                });
            } catch (Throwable throwable) {
            }
        }
    }

    protected void turnOnStrictModeForThisThread() {
        // We use reflection, because Instrumentation will complain about any references to
        // StrictMode in API versions < 9 when attempting to run the unit tests. No particular
        // effort has been made to make this efficient, since we expect to call it just once.
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> strictModeClass = Class.forName("android.os.StrictMode", true, loader);
            Class<?> threadPolicyClass = Class.forName(
                    "android.os.StrictMode$ThreadPolicy",
                    true,
                    loader);
            Class<?> threadPolicyBuilderClass = Class.forName(
                    "android.os.StrictMode$ThreadPolicy$Builder",
                    true,
                    loader);

            Object threadPolicyBuilder = threadPolicyBuilderClass.getConstructor().newInstance();
            threadPolicyBuilder = threadPolicyBuilderClass.getMethod("detectAll").invoke(
                    threadPolicyBuilder);
            threadPolicyBuilder = threadPolicyBuilderClass.getMethod("penaltyDeath").invoke(
                    threadPolicyBuilder);

            Object threadPolicy = threadPolicyBuilderClass.getMethod("build").invoke(
                    threadPolicyBuilder);
            strictModeClass.getMethod("setThreadPolicy", threadPolicyClass).invoke(
                    strictModeClass,
                    threadPolicy);
        } catch (Exception ex) {
        }
    }
}
