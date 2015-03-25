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
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.facebook.applinks.FacebookAppLinkResolver;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import bolts.Continuation;
import bolts.Task;

public class FacebookActivityTests
        extends FacebookActivityTestCase<FacebookActivityTests.FacebookTestActivity> {
    public FacebookActivityTests() {
        super(FacebookActivityTests.FacebookTestActivity.class);
    }

    @SmallTest
    public void testLaunchingWithEmptyIntent() throws Exception {
        final TestBlocker blocker = getTestBlocker();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    setActivityIntent(intent);
                    FacebookTestActivity activity = getActivity();

                    AccessToken.createFromNativeLinkingIntent(
                            activity.getIntent(),
                            getApplicationId(),
                            new AccessToken.AccessTokenCreationCallback() {
                                @Override
                                public void onSuccess(AccessToken token) {
                                    fail();
                                    blocker.signal();

                                }

                                @Override
                                public void onError(FacebookException error) {
                                    blocker.signal();
                                }
                            });
                } catch (Exception e) {
                    fail(e.getMessage());
                    blocker.signal();
                }
            }
        };
        RunTestWithBlocker(blocker, runnable);
    }

    @SmallTest
    public void testLaunchingWithValidNativeLinkingIntent() {
        final TestBlocker blocker = getTestBlocker();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    final String token = "A token less unique than most";
                    final String userId = "1000";

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.putExtras(getNativeLinkingExtras(token, userId));
                    setActivityIntent(intent);
                    FacebookTestActivity activity = getActivity();

                    AccessToken.createFromNativeLinkingIntent(
                            activity.getIntent(),
                            getApplicationId(),
                            new AccessToken.AccessTokenCreationCallback() {
                                @Override
                                public void onSuccess(AccessToken token) {
                                    assertNotNull(token);
                                    blocker.signal();
                                }

                                @Override
                                public void onError(FacebookException error) {
                                    fail();
                                    blocker.signal();
                                }
                            });
                } catch (Exception e) {
                    fail(e.getMessage());
                    blocker.signal();
                }
            }
        };
        RunTestWithBlocker(blocker, runnable);
    }

    @MediumTest
    public void testLaunchingWithValidNativeLinkingNoUserIntent() throws Exception {
        final TestBlocker blocker = getTestBlocker();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    TestUserManager manager = new TestUserManager(
                            getApplicationSecret(),
                            getApplicationId());
                    AccessToken token = manager.getAccessTokenForSharedUser(null);

                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.putExtras(getNativeLinkingExtras(token.getToken(), null));
                    setActivityIntent(intent);
                    FacebookTestActivity activity = getActivity();
                    AccessToken.createFromNativeLinkingIntent(
                            activity.getIntent(),
                            getApplicationId(),
                            new AccessToken.AccessTokenCreationCallback() {
                                @Override
                                public void onSuccess(AccessToken token) {
                                    assertNotNull(token);
                                    blocker.signal();
                                }

                                @Override
                                public void onError(FacebookException error) {
                                    fail();
                                    blocker.signal();
                                }
                            });
                } catch (Exception e) {
                    // Get back to the test case if there was an uncaught exception
                    fail(e.getMessage());
                    blocker.signal();
                }
            }
        };

        RunTestWithBlocker(blocker, runnable);
    }

    public static class FacebookTestActivity extends Activity {
    }

    private void RunTestWithBlocker(final TestBlocker blocker, Runnable runnable) {
        try {
            Handler handler = new Handler(blocker.getLooper());
            handler.post(runnable);

            blocker.waitForSignals(1);
        } catch (Exception e) {
            // Forcing the test to fail with details
            assertNull(e);
        }
    }
}
