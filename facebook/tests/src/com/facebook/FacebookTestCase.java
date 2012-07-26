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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Looper;
import android.test.ActivityUnitTestCase;
import android.util.Log;

public class FacebookTestCase extends ActivityUnitTestCase<FacebookTestCase.FacebookTestActivity> {
    private static ThreadLocal<TestBlocker> threadLocalTestBlockers = new ThreadLocal<TestBlocker>();
    private static String applicationId;
    private static String applicationSecret;

    public final static String SECOND_TEST_USER_TAG = "Second";
    public final static String THIRD_TEST_USER_TAG = "Third";

    protected synchronized static TestBlocker getTestBlocker() {
        TestBlocker testBlocker = threadLocalTestBlockers.get();
        if (testBlocker == null) {
            testBlocker = TestBlocker.createTestBlocker();
            threadLocalTestBlockers.set(testBlocker);
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
        openSession(session, blocker);
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
        openSession(session, blocker);
        return session;
    }

    // Turns exceptions from the TestBlocker into JUnit assertions
    protected void waitAndAssertSuccess(TestBlocker testBlocker, int numSignals) {
        try {
            testBlocker.waitForSignalsAndAssertSuccess(numSignals);
        } catch (Throwable t) {
            assertNotNull(t);
        }
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

    protected void openSession(TestSession session) {
        final TestBlocker blocker = getTestBlocker();
        openSession(session, blocker);
    }

    protected void openSession(TestSession session, final TestBlocker blocker) {
        session.open(new SessionStatusCallback() {
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
        Request request = Request.newPostRequest(session, path, graphObject);
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
        TestSession session = getTestSessionWithSharedUser(null);
        String appId = session.getTestApplicationId();
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

    public static class FacebookTestActivity extends Activity {
    }
}
