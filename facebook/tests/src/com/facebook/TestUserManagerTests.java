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

import android.os.Bundle;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONArray;

// Because TestUserManager is the component under test here, be careful in calling methods on
// FacebookTestCase that assume TestUserManager works correctly.
public class TestUserManagerTests extends FacebookTestCase {

    private TestUserManager createTestUserManager() {
        return new TestUserManager(getApplicationSecret(), getApplicationId());
    }

    @LargeTest
    public void testCanGetAccessTokenForPrivateUser() {
        TestUserManager testUserManager = createTestUserManager();
        AccessToken accessToken = testUserManager.getAccessTokenForPrivateUser(null);
        assertNotNull(accessToken);
    }

    @LargeTest
    public void testCanGetAccessTokenForSharedUser() {
        TestUserManager testUserManager = createTestUserManager();
        AccessToken accessToken = testUserManager.getAccessTokenForSharedUser(null);
        assertNotNull(accessToken);
    }

    @LargeTest
    public void testSharedUserDoesntCreateUnnecessaryUsers() throws Throwable {

        TestUserManager testUserManager = createTestUserManager();
        AccessToken accessToken = testUserManager.getAccessTokenForSharedUser(null);
        assertNotNull(accessToken);

        // Note that this test is somewhat brittle in that the count of test users could change for
        // external reasons while the test is running. For that reason it may not be appropriate for
        // an automated test suite, and could be run only when testing changes to TestSession.
        int startingUserCount = countTestUsers();

        accessToken = testUserManager.getAccessTokenForSharedUser(null);
        assertNotNull(accessToken);

        int endingUserCount = countTestUsers();

        assertEquals(startingUserCount, endingUserCount);
    }

    private int countTestUsers() {
        TestUserManager testUserManager = createTestUserManager();

        String appAccessToken = testUserManager.getAppAccessToken();
        assertNotNull(appAccessToken);

        Bundle parameters = new Bundle();

        parameters.putString("access_token", appAccessToken);
        parameters.putString("fields", "id");

        GraphRequest requestTestUsers =
                new GraphRequest(null, "app/accounts/test-users", parameters, null);

        GraphResponse response = requestTestUsers.executeAndWait();

        JSONArray data = response.getJSONObject().optJSONArray("data");
        return data.length();
    }
}
