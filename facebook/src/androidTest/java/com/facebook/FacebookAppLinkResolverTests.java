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

import android.net.Uri;
import android.os.Handler;

import com.facebook.applinks.FacebookAppLinkResolver;

import bolts.AppLink;
import bolts.Continuation;
import bolts.Task;

import java.util.ArrayList;
import java.util.List;

public class FacebookAppLinkResolverTests extends FacebookTestCase {
    private Task resolveTask;

    public void testSingleUrl() {
        String testUrlString = "https://fb.me/732873156764191";
        Uri testUrl = Uri.parse(testUrlString);
        Uri testWebUri = Uri.parse("http://www.facebook.com/");
        ArrayList<AppLink.Target> testTargets = new ArrayList<AppLink.Target>();
        testTargets.add(new AppLink.Target(
                "com.myapp",
                null,
                Uri.parse("myapp://3"),
                "my app"));
        testTargets.add(new AppLink.Target(
                "com.myapp-test",
                null,
                Uri.parse("myapp-test://4"),
                "my test app"));
        try {
            executeResolverOnBlockerThread(new FacebookAppLinkResolver(), testUrl);

            getTestBlocker().waitForSignals(1);

            assertNotNull(resolveTask);

            Task<AppLink> singleUrlResolveTask = (Task<AppLink>)resolveTask;

            assertTrue(singleUrlResolveTask.isCompleted() &&
                    !singleUrlResolveTask.isCancelled() &&
                    !singleUrlResolveTask.isFaulted());

            AppLink appLink = singleUrlResolveTask.getResult();

            assertEquals(appLink.getSourceUrl(), testUrl);
            assertEquals(appLink.getWebUrl(), testWebUri);
            assertTrue(targetListsAreEqual(appLink.getTargets(), testTargets));
        } catch (Exception e) {
            // Forcing the test to fail with details
            assertNull(e);
        }
    }

    public void testUrlWithNoAppLinkData() {
        String testNoAppLinkUrlString = "https://fb.me/732873156764191_no_app_link";
        Uri testNoAppLinkUrl = Uri.parse(testNoAppLinkUrlString);
        try {
            executeResolverOnBlockerThread(new FacebookAppLinkResolver(), testNoAppLinkUrl);

            getTestBlocker().waitForSignals(1);

            assertNotNull(resolveTask);

            Task<AppLink> singleUrlResolveTask = (Task<AppLink>)resolveTask;

            assertTrue(singleUrlResolveTask.isCompleted() &&
                    !singleUrlResolveTask.isCancelled() &&
                    !singleUrlResolveTask.isFaulted());

            AppLink appLink = singleUrlResolveTask.getResult();
            assertNull(appLink);
        } catch (Exception e) {
            // Forcing the test to fail with details
            assertNull(e);
        }
    }

    public void testCachedAppLinkData() {
        String testUrlString = "https://fb.me/732873156764191";
        Uri testUrl = Uri.parse(testUrlString);
        Uri testWebUri = Uri.parse("http://www.facebook.com/");
        ArrayList<AppLink.Target> testTargets = new ArrayList<AppLink.Target>();
        testTargets.add(new AppLink.Target(
                "com.myapp",
                null,
                Uri.parse("myapp://3"),
                "my app"));
        testTargets.add(new AppLink.Target(
                "com.myapp-test",
                null,
                Uri.parse("myapp-test://4"),
                "my test app"));

        try {
            FacebookAppLinkResolver resolver = new FacebookAppLinkResolver();

            // This will prefetch the app link
            executeResolverOnBlockerThread(resolver, testUrl);
            getTestBlocker().waitForSignals(1);
            assertNotNull(resolveTask);

            // Now let's fetch it again. This should complete the task synchronously.
            Task<AppLink> cachedUrlResolveTask = resolver.getAppLinkFromUrlInBackground(testUrl);

            assertTrue(cachedUrlResolveTask.isCompleted() &&
                    !cachedUrlResolveTask.isCancelled() &&
                    !cachedUrlResolveTask.isFaulted());

            AppLink appLink = cachedUrlResolveTask.getResult();

            assertEquals(appLink.getSourceUrl(), testUrl);
            assertEquals(appLink.getWebUrl(), testWebUri);
            assertTrue(targetListsAreEqual(appLink.getTargets(), testTargets));
        } catch (Exception e) {
            // Forcing the test to fail with details
            assertNull(e);
        }
    }

    public void executeResolverOnBlockerThread(final FacebookAppLinkResolver resolver, final Uri testUrl) {
        final TestBlocker blocker = getTestBlocker();
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    resolveTask = resolver.getAppLinkFromUrlInBackground(testUrl);
                    resolveTask.continueWith(new Continuation() {
                        @Override
                        public Object then(Task task) throws Exception {
                            // Once the task is complete, unblock the test thread, so it can inspect for errors/results.
                            blocker.signal();
                            return null;
                        }
                    });
                } catch (Exception e) {
                    // Get back to the test case if there was an uncaught exception
                    blocker.signal();
                }
            }
        };

        Handler handler = new Handler(blocker.getLooper());
        handler.post(runnable);
    }

    private static boolean targetListsAreEqual(List<AppLink.Target> list1, List<AppLink.Target> list2) {
        if (list1 == null) {
            return list2 == null;
        } else if (list2 == null || list1.size() != list2.size()) {
            return false;
        }

        ArrayList<AppLink.Target> list2Copy = new ArrayList<AppLink.Target>(list2);

        for(int i = 0; i < list1.size(); i++) {
            int j;
            for (j = 0; j < list2Copy.size(); j++) {
                if (targetsAreEqual(list1.get(i), list2Copy.get(j))) {
                    break;
                }
            }

            if (j < list2Copy.size()) {
                // Found a match. Remove from the copy to make sure the same target isn't matched twice.
                list2Copy.remove(j);
            } else {
                // Match not found
                return false;
            }
        }
        return true;
    }

    private static boolean targetsAreEqual(AppLink.Target target1, AppLink.Target target2) {
        boolean isEqual =
                objectsAreEqual(target1.getPackageName(), target2.getPackageName()) &&
                objectsAreEqual(target1.getClassName(), target2.getClassName()) &&
                objectsAreEqual(target1.getAppName(), target2.getAppName()) &&
                objectsAreEqual(target1.getUrl(), target2.getUrl()) ;

        return isEqual;
    }

    private static boolean objectsAreEqual(Object s1, Object s2) {
        return s1 == null
                ? s2 == null
                : s1.equals(s2);
    }
}
