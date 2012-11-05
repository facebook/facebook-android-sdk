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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Looper;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SessionTestsBase extends FacebookTestCase {
    public static final int DEFAULT_TIMEOUT_MILLISECONDS = 10 * 1000;
    static final int SIMULATED_WORKING_MILLISECONDS = 20;
    public static final int STRAY_CALLBACK_WAIT_MILLISECONDS = 50;

    public ScriptedSession createScriptedSessionOnBlockerThread(TokenCache cache) {
        return createScriptedSessionOnBlockerThread("SomeApplicationId", cache);
    }

    ScriptedSession createScriptedSessionOnBlockerThread(final String applicationId,
            final TokenCache cache) {
        class MutableState {
            ScriptedSession session;
        }
        ;
        final MutableState mutable = new MutableState();

        runOnBlockerThread(new Runnable() {
            @Override
            public void run() {
                mutable.session = new ScriptedSession(getActivity(), applicationId, cache);
            }
        }, true);

        return mutable.session;
    }

    public static void stall(int stallMsec) {
        try {
            Thread.sleep(stallMsec);
        } catch (InterruptedException e) {
            fail("InterruptedException while stalling");
        }
    }

    public static class ScriptedSession extends Session {
        private static final long serialVersionUID = 1L;
        private final LinkedList<AuthorizeResult> pendingAuthorizations = new LinkedList<AuthorizeResult>();

        public ScriptedSession(Context currentContext, String applicationId, TokenCache tokenCache) {
            super(currentContext, applicationId, tokenCache, false);
        }

        public void addAuthorizeResult(String token, List<String> permissions, AccessTokenSource source) {
            addAuthorizeResult(AccessToken.createFromString(token, permissions, source));
        }

        public void addAuthorizeResult(AccessToken token) {
            pendingAuthorizations.add(new AuthorizeResult(token));
        }

        public void addAuthorizeResult(Exception exception) {
            pendingAuthorizations.add(new AuthorizeResult(exception));
        }

        // Overrides authorize to return the next AuthorizeResult we added.
        @Override
        void authorize(final AuthorizationRequest request) {
            Settings.getExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    stall(SIMULATED_WORKING_MILLISECONDS);
                    AuthorizeResult result = pendingAuthorizations.poll();

                    if (result == null) {
                        fail("Missing call to addScriptedAuthorization");
                    }

                    finishAuth(result.token, result.exception);
                }
            });
        }

        private static class AuthorizeResult {
            final AccessToken token;
            final Exception exception;

            private AuthorizeResult(AccessToken token, Exception exception) {
                this.token = token;
                this.exception = exception;
            }

            AuthorizeResult(AccessToken token) {
                this(token, null);
            }

            AuthorizeResult(Exception exception) {
                this(null, exception);
            }
        }
    }

    public static class SessionStatusCallbackRecorder implements Session.StatusCallback {
        private final BlockingQueue<Call> calls = new LinkedBlockingQueue<Call>();
        volatile boolean isClosed = false;

        public void waitForCall(Session session, SessionState state, Exception exception) {
            Call call = null;

            try {
                call = calls.poll(DEFAULT_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS);
                if (call == null) {
                    fail("Did not get a status callback within timeout.");
                }
            } catch (InterruptedException e) {
                fail("InterruptedException while waiting for status callback: " + e);
            }

            assertEquals(session, call.session);
            assertEquals(state, call.state);
            assertEquals(exception, call.exception);
        }

        public void close() {
            isClosed = true;
            assertEquals(0, calls.size());
        }

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            Call call = new Call(session, state, exception);
            if (!calls.offer(call)) {
                fail("Test Error: Blocking queue ran out of capacity");
            }
            if (isClosed) {
                fail("Reauthorize callback called after closed");
            }
            assertEquals("Callback should run on main UI thread", Thread.currentThread(),
                    Looper.getMainLooper().getThread());
        }

        private static class Call {
            final Session session;
            final SessionState state;
            final Exception exception;

            Call(Session session, SessionState state, Exception exception) {
                this.session = session;
                this.state = state;
                this.exception = exception;
            }
        }

    }

    public static class MockTokenCache extends TokenCache {
        private final String token;
        private final long expires_in;
        private Bundle saved;

        MockTokenCache() {
            this("FakeToken", DEFAULT_TIMEOUT_MILLISECONDS);
        }

        public MockTokenCache(String token, long expires_in) {
            this.token = token;
            this.expires_in = expires_in;
            this.saved = null;
        }

        public Bundle getSavedState() {
            return saved;
        }

        @Override
        public Bundle load() {
            Bundle bundle = null;

            if (token != null) {
                bundle = new Bundle();

                TokenCache.putToken(bundle, token);
                TokenCache.putExpirationMilliseconds(bundle, System.currentTimeMillis() + expires_in);
            }

            return bundle;
        }

        @Override
        public void save(Bundle bundle) {
            this.saved = bundle;
        }

        @Override
        public void clear() {
            this.saved = null;
        }
    }

    static class WaitForBroadcastReceiver extends BroadcastReceiver {
        static int idGenerator = 0;
        final int id = idGenerator++;

        ConditionVariable condition = new ConditionVariable(true);
        int expectCount;
        int actualCount;

        public void incrementExpectCount() {
            incrementExpectCount(1);
        }

        public void incrementExpectCount(int n) {
            expectCount += n;
            if (actualCount < expectCount) {
                condition.close();
            }
        }

        public void waitForExpectedCalls() {
            if (!condition.block(DEFAULT_TIMEOUT_MILLISECONDS)) {
                assertTrue(false);
            }
        }

        public static void incrementExpectCounts(WaitForBroadcastReceiver... receivers) {
            for (WaitForBroadcastReceiver receiver : receivers) {
                receiver.incrementExpectCount();
            }
        }

        public static void waitForExpectedCalls(WaitForBroadcastReceiver... receivers) {
            for (WaitForBroadcastReceiver receiver : receivers) {
                receiver.waitForExpectedCalls();
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (++actualCount == expectCount) {
                condition.open();
            }
            assertTrue(actualCount <= expectCount);
            assertEquals("BroadcastReceiver should receive on main UI thread",
                    Thread.currentThread(), Looper.getMainLooper().getThread());
        }
    }
}
