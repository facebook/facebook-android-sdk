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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Looper;
import android.test.ActivityUnitTestCase;
import android.util.Log;

public class FacebookTestCase extends ActivityUnitTestCase<FacebookTestCase.FacebookTestActivity> {
    private static ThreadLocal<TestBlocker> threadLocalTestBlockers = new ThreadLocal<TestBlocker>();
    private static String applicationId;
    private static String applicationSecret;

    // Note that getTestBlocker will call reset on the TestBlocker each time, so don't call it multiple
    // times during execution of a test.
    protected synchronized static TestBlocker getTestBlocker() {
        TestBlocker testBlocker = threadLocalTestBlockers.get();
        if (testBlocker == null) {
            testBlocker = TestBlocker.createTestBlocker();
            threadLocalTestBlockers.set(testBlocker);
        }
        testBlocker.reset();
        return testBlocker;
    }

    public FacebookTestCase() {
        super(FacebookTestActivity.class);
    }

    protected TestSession getTestSessionWithSharedUser(TestBlocker testBlocker) {
        // TODO determine permissions, session user tag
        Looper looper = (testBlocker != null) ? testBlocker.getLooper() : null;
        return TestSession.createSessionWithSharedUser(getStartedActivity(), null, null, looper);
    }

    protected TestSession getTestSessionWithSharedUser() {
        return getTestSessionWithSharedUser(null);
    }

    protected TestSession getTestSessionWithPrivateUser(TestBlocker testBlocker) {
        // TODO determine permissions
        Looper looper = (testBlocker != null) ? testBlocker.getLooper() : null;
        return TestSession.createSessionWithPrivateUser(getStartedActivity(), null, looper);
    }

    protected TestSession getTestSessionWithPrivateUser() {
        return getTestSessionWithPrivateUser(null);
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
            final String errorMessage = "could not read applicationId and applicationSecret from config.json; ensure you have run 'configure_unit_tests.sh'. Error: ";
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

                TestSession.setTestApplicationId(applicationId);
                TestSession.setTestApplicationSecret(applicationSecret);
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

    // openSession will call getTestBlocker and thus reset the thread-local blocker. Do not call in the middle
    // of other test logic that relies on the blocker's signal count.
    protected void openSession(TestSession session) throws Throwable {
        final TestBlocker blocker = getTestBlocker();

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

        blocker.waitForSignalsAndAssertSuccess(1);
    }

    protected void setUp() throws Exception {
        super.setUp();
        // Make sure we have read application ID and secret.
        readApplicationIdAndSecret();
    }

    public static class FacebookTestActivity extends Activity {
    }
}
