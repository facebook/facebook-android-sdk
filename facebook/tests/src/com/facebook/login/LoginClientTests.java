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

package com.facebook.login;

import android.support.v4.app.Fragment;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;

import com.facebook.AccessToken;
import com.facebook.FacebookTestCase;
import com.facebook.GraphRequest;
import com.facebook.GraphRequestBatch;
import com.facebook.GraphRequestBatchBridge;
import com.facebook.GraphResponse;
import com.facebook.GraphResponseBridge;
import com.facebook.TestBlocker;
import com.facebook.TestUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class LoginClientTests extends FacebookTestCase {
    private static final Set<String> PERMISSIONS = new HashSet<String>(
            Arrays.asList("go outside", "come back in"));

    class MockLoginClient extends LoginClient {
        Result result;
        boolean triedNextHandler = false;

        MockLoginClient(Fragment fragment) {
            super(fragment);
        }

        Request getRequest() {
            return pendingRequest;
        }

        void setRequest(Request request) {
            pendingRequest = request;
        }

        @Override
        void complete(Result result) {
            this.result = result;
        }

        @Override
        void tryNextHandler() {
            triedNextHandler = true;
        }
    }

    LoginClient.Request createRequest() {
        return new LoginClient.Request(
                LoginBehavior.SSO_WITH_FALLBACK,
                PERMISSIONS,
                DefaultAudience.FRIENDS,
                "1234",
                null
        );
    }

    class MockValidatingLoginClient extends MockLoginClient {
        private final HashMap<String, String> mapAccessTokenToFbid = new HashMap<String, String>();
        private Set<String> permissionsToReport = new HashSet<String>();
        private TestBlocker blocker;

        public MockValidatingLoginClient(Fragment fragment, TestBlocker blocker) {
            super(fragment);
            this.blocker = blocker;
        }

        public void addAccessTokenToFbidMapping(String accessToken, String fbid) {
            mapAccessTokenToFbid.put(accessToken, fbid);
        }

        public void setPermissionsToReport(Set<String> permissionsToReport) {
            this.permissionsToReport = permissionsToReport;
        }

        @Override
        void complete(Result result) {
            super.complete(result);
            blocker.signal();
        }

        @Override
        GraphRequest createGetProfileIdRequest(final String accessToken) {
            return new MockGraphRequest() {
                @Override
                public GraphResponse createResponse() {
                    String fbid = mapAccessTokenToFbid.get(accessToken);
                    JSONObject user = new JSONObject();
                    try {
                        user.put("id", fbid);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    return GraphResponseBridge.createGraphResponse(this, null, null, user);
                }
            };
        }

        @Override
        GraphRequest createGetPermissionsRequest(String accessToken) {
            final Set<String> permissions = permissionsToReport;
            return new MockGraphRequest() {
                @Override
                public GraphResponse createResponse() {
                    JSONObject result = new JSONObject();
                    JSONArray data = new JSONArray();
                    try {
                        if (permissions != null) {
                            for (String permission : permissions) {
                                JSONObject permissionsObject = new JSONObject();
                                permissionsObject.put("permission", permission);
                                permissionsObject.put("status", "granted");
                                data.put(permissionsObject);
                            }
                        }

                        result.put("data", data);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    return GraphResponseBridge.createGraphResponse(this, null, null, result);
                }
            };
        }
    }

    static final String USER_1_FBID = "user1";
    static final String USER_1_ACCESS_TOKEN = "An access token for user 1";
    static final String USER_2_FBID = "user2";
    static final String USER_2_ACCESS_TOKEN = "An access token for user 2";
    static final String APP_ID = "1234";

    LoginClient.Request createNewPermissionRequest() {
        return new LoginClient.Request(
                LoginBehavior.SSO_WITH_FALLBACK,
                PERMISSIONS,
                DefaultAudience.FRIENDS,
                "1234",
                null
        );
    }

    @LargeTest
    public void testReauthorizationWithSameFbidSucceeds() throws Exception {
        TestBlocker blocker = getTestBlocker();

        MockValidatingLoginClient client = new MockValidatingLoginClient(null, blocker);
        client.addAccessTokenToFbidMapping(USER_1_ACCESS_TOKEN, USER_1_FBID);
        client.addAccessTokenToFbidMapping(USER_2_ACCESS_TOKEN, USER_2_FBID);
        client.setPermissionsToReport(PERMISSIONS);

        LoginClient.Request request = createNewPermissionRequest();
        client.setRequest(request);

        AccessToken token = new AccessToken(
                USER_1_ACCESS_TOKEN,
                APP_ID,
                USER_1_FBID,
                PERMISSIONS,
                null,
                null,
                null,
                null);
        AccessToken.setCurrentAccessToken(token);
        LoginClient.Result result = LoginClient.Result.createTokenResult(request, token);

        client.completeAndValidate(result);

        blocker.waitForSignals(1);

        assertNotNull(client.result);
        assertEquals(LoginClient.Result.Code.SUCCESS, client.result.code);

        AccessToken resultToken = client.result.token;
        assertNotNull(resultToken);
        assertEquals(USER_1_ACCESS_TOKEN, resultToken.getToken());

        // We don't care about ordering.
        assertEquals(new HashSet<String>(PERMISSIONS), new HashSet<String>(resultToken.getPermissions()));
    }

    @LargeTest
    public void testReauthorizationWithDifferentFbidsFails() throws Exception {
        TestBlocker blocker = getTestBlocker();

        MockValidatingLoginClient client = new MockValidatingLoginClient(null, blocker);
        client.addAccessTokenToFbidMapping(USER_1_ACCESS_TOKEN, USER_1_FBID);
        client.addAccessTokenToFbidMapping(USER_2_ACCESS_TOKEN, USER_2_FBID);
        client.setPermissionsToReport(PERMISSIONS);

        LoginClient.Request request = createNewPermissionRequest();
        client.setRequest(request);

        AccessToken userOneToken = new AccessToken(
                USER_1_ACCESS_TOKEN,
                APP_ID,
                USER_1_FBID,
                PERMISSIONS,
                null,
                null,
                null,
                null);
        AccessToken.setCurrentAccessToken(userOneToken);

        AccessToken userTwoToken = new AccessToken(
                USER_2_ACCESS_TOKEN,
                APP_ID,
                USER_2_FBID,
                PERMISSIONS,
                null,
                null,
                null,
                null);
        LoginClient.Result result = LoginClient.Result.createTokenResult(request, userTwoToken);

        client.completeAndValidate(result);

        blocker.waitForSignals(1);

        assertNotNull(client.result);
        assertEquals(LoginClient.Result.Code.ERROR, client.result.code);

        assertNull(client.result.token);
        assertNotNull(client.result.errorMessage);
    }
}
