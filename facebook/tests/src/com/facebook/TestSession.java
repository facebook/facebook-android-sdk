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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.facebook.Session.AuthRequest;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

public class TestSession extends Session {
    private enum Mode {
        PRIVATE, SHARED
    }

    private static final String LOG_TAG = "FacebookTestSession";

    private static Map<String, TestAccount> appTestAccounts;
    private static String testApplicationSecret;
    private static String testApplicationId;
    private static String appAccessToken;

    private final String machineUniqueUserTag;
    private final String sessionUniqueUserTag;
    private final List<String> requestedPermissions;
    private final Mode mode;
    private String testAccountId;

    protected TestSession(Activity activity, List<String> permissions, TokenCache tokenCache,
            String machineUniqueUserTag, String sessionUniqueUserTag, Mode mode, Handler handler) {
        super(activity, TestSession.testApplicationId, permissions, tokenCache, handler);

        Validate.notNull(permissions, "permissions");

        // Validate these as if they were arguments even though they are statics.
        Validate.notNullOrEmpty(testApplicationId, "testApplicationId");
        Validate.notNullOrEmpty(testApplicationSecret, "testApplicationSecret");

        appAccessToken = testApplicationId + "|" + testApplicationSecret;

        this.machineUniqueUserTag = machineUniqueUserTag;
        this.sessionUniqueUserTag = sessionUniqueUserTag;
        this.mode = mode;
        this.requestedPermissions = permissions;
    }

    public static TestSession createSessionWithPrivateUser(Activity activity, List<String> permissions, Looper looper) {
        return createTestSession(activity, permissions, Mode.PRIVATE, null, looper);
    }

    public static TestSession createSessionWithSharedUser(Activity activity, List<String> permissions, Looper looper) {
        return createSessionWithSharedUser(activity, permissions, null, looper);
    }

    public static TestSession createSessionWithSharedUser(Activity activity, List<String> permissions,
            String sessionUniqueUserTag, Looper looper) {
        return createTestSession(activity, permissions, Mode.SHARED, sessionUniqueUserTag, looper);
    }

    public static final String getAppAccessToken() {
        return appAccessToken;
    }

    private static synchronized TestSession createTestSession(Activity activity, List<String> permissions, Mode mode,
            String sessionUniqueUserTag, Looper looper) {
        if (Utility.isNullOrEmpty(testApplicationId) || Utility.isNullOrEmpty(testApplicationSecret)) {
            throw new FacebookException("Must provide app ID and secret");
        }

        // TODO port: get this
        String machineUniqueUserTag = null;

        if (Utility.isNullOrEmpty(permissions)) {
            permissions = Arrays.asList("email", "publish_actions");
        }

        Handler handler = (looper != null) ? new Handler(looper) : new Handler();
        return new TestSession(activity, permissions, new TestTokenCache(), machineUniqueUserTag, sessionUniqueUserTag,
                mode, handler);
    }

    private static synchronized void retrieveTestAccountsForAppIfNeeded() {
        if (appTestAccounts != null) {
            return;
        }

        appTestAccounts = new HashMap<String, TestAccount>();

        // The data we need is split across two different FQL tables. We construct two queries, submit them
        // together (the second one refers to the first one), then cross-reference the results.

        // Get the test accounts for this app.
        String testAccountQuery = String.format("SELECT id,access_token FROM test_account WHERE app_id = %s",
                testApplicationId);
        // Get the user names for those accounts.
        String userQuery = "SELECT uid,name FROM user WHERE uid IN (SELECT id FROM #test_accounts)";

        Bundle parameters = new Bundle();

        // Build a JSON string that contains our queries and pass it as the 'q' parameter of the query.
        JSONObject multiquery;
        try {
            multiquery = new JSONObject();
            multiquery.put("test_accounts", testAccountQuery);
            multiquery.put("users", userQuery);
        } catch (JSONException exception) {
            throw new FacebookException(exception);
        }
        parameters.putString("q", multiquery.toString());

        // We need to authenticate as this app.
        parameters.putString("access_token", getAppAccessToken());

        Request request = new Request(null, "fql", parameters, null);
        Response response = request.execute();

        if (response.getError() != null) {
            throw response.getError();
        }

        FqlResponse fqlResponse = response.getGraphObjectAs(FqlResponse.class);

        GraphObjectList<FqlResult> fqlResults = fqlResponse.getData();
        if (fqlResults == null || fqlResults.size() != 2) {
            throw new FacebookException("Unexpected number of results from FQL query");
        }

        // We get back two sets of results. The first is from the test_accounts query, the second from the users query.
        Collection<TestAccount> testAccounts = fqlResults.get(0).getFqlResultSet().castToListOf(TestAccount.class);
        Collection<UserAccount> userAccounts = fqlResults.get(1).getFqlResultSet().castToListOf(UserAccount.class);

        // Use both sets of results to populate our static array of accounts.
        populateTestAccounts(testAccounts, userAccounts);

        return;
    }

    private static synchronized void populateTestAccounts(Collection<TestAccount> testAccounts,
            Collection<UserAccount> userAccounts) {
        // We get different sets of data from each of these queries. We want to combine them into a single data
        // structure. We have added a Name property to the TestAccount interface, even though we don't really get
        // a name back from the service from that query. We stick the Name from the corresponding UserAccount in it.
        for (TestAccount testAccount : testAccounts) {
            storeTestAccount(testAccount);
        }

        for (UserAccount userAccount : userAccounts) {
            TestAccount testAccount = appTestAccounts.get(userAccount.getUid());
            if (testAccount != null) {
                testAccount.setName(userAccount.getName());
            }
        }
    }

    private static synchronized void storeTestAccount(TestAccount testAccount) {
        appTestAccounts.put(testAccount.getId(), testAccount);
    }

    private static synchronized TestAccount findTestAccountMatchingIdentifier(String identifier) {
        retrieveTestAccountsForAppIfNeeded();

        for (TestAccount testAccount : appTestAccounts.values()) {
            if (testAccount.getName().contains(identifier)) {
                return testAccount;
            }
        }
        return null;
    }

    public final String getTestUserId() {
        return testAccountId;
    }

    public static synchronized String getTestApplicationId() {
        return testApplicationId;
    }

    public static synchronized void setTestApplicationId(String value) {
        if (testApplicationId != null && !testApplicationId.equals(value)) {
            throw new FacebookException("Can't have more than one test application ID");
        }
        testApplicationId = value;
    }

    public static synchronized String getTestApplicationSecret() {
        return testApplicationSecret;
    }

    public static synchronized void setTestApplicationSecret(String value) {
        if (testApplicationSecret != null && !testApplicationSecret.equals(value)) {
            throw new FacebookException("Can't have more than one test application secret");
        }
        testApplicationSecret = value;
    }

    @Override
    void authorize(AuthRequest request) {
        if (mode == Mode.PRIVATE) {
            createTestAccountAndFinishAuth();
        } else {
            findOrCreateSharedTestAccount();
        }
    }

    @Override
    void postStateChange(final SessionState newState, final Exception error) {
        // Make sure these don't get overwritten.
        String id = testAccountId;
        String token = appAccessToken;

        super.postStateChange(newState, error);

        if (newState.getIsClosed() && id != null) {
            deleteTestAccount(id, token);
        }
    }

    private void findOrCreateSharedTestAccount() {
        TestAccount testAccount = findTestAccountMatchingIdentifier(getSharedTestAccountIdentifier());
        if (testAccount != null) {
            finishAuthWithTestAccount(testAccount);
        } else {
            createTestAccountAndFinishAuth();
        }
    }

    private void finishAuthWithTestAccount(TestAccount testAccount) {
        testAccountId = testAccount.getId();

        AccessToken accessToken = AccessToken.createFromString(testAccount.getAccessToken(), requestedPermissions);
        finishAuth(accessToken, null);
    }

    private TestAccount createTestAccountAndFinishAuth() {
        Bundle parameters = new Bundle();
        parameters.putString("installed", "true");
        // TODO parameters.putString("method", "post");
        parameters.putString("permissions", getPermissionsString());
        parameters.putString("access_token", getAppAccessToken());

        // If we're in shared mode, we want to rename this user to encode its permissions, so we can find it later
        // in another shared session. If we're in private mode, don't bother renaming it since we're just going to
        // delete it at the end of the session.
        if (mode == Mode.SHARED) {
            parameters.putString("name", String.format("Shared %s Testuser", getSharedTestAccountIdentifier()));
        }

        String graphPath = String.format("%s/accounts/test-users", testApplicationId);
        Request createUserRequest = new Request(null, graphPath, parameters, Request.POST_METHOD);
        Response response = createUserRequest.execute();

        FacebookException error = response.getError();
        TestAccount testAccount = response.getGraphObjectAs(TestAccount.class);
        if (error != null) {
            finishAuth(null, error);
            return null;
        } else {
            assert testAccount != null;

            // If we are in shared mode, store this new account in the dictionary so we can re-use it later.
            if (mode == Mode.SHARED) {
                // Remember the new name we gave it, since we didn't get it back in the results of the create request.
                testAccount.setName(parameters.getString("name"));
                storeTestAccount(testAccount);
            }

            finishAuthWithTestAccount(testAccount);

            return testAccount;
        }
    }

    private void deleteTestAccount(String testAccountId, String appAccessToken) {
        Bundle parameters = new Bundle();
        parameters.putString("access_token", appAccessToken);

        Request request = new Request(null, testAccountId, parameters, Request.DELETE_METHOD);
        Response response = request.execute();

        Exception error = response.getError();
        GraphObject graphObject = response.getGraphObject();
        if (error != null) {
            Log.w(LOG_TAG, String.format("Could not delete test acccount %s: %s", testAccountId, error.toString()));
        } else if (graphObject.get(Response.NON_JSON_RESPONSE_PROPERTY) == (Boolean) false) {
            Log.w(LOG_TAG, String.format("Could not delete test account %s: unknown reason", testAccountId));
        }
    }

    private String getPermissionsString() {
        return TextUtils.join(",", requestedPermissions);
    }

    private String getSharedTestAccountIdentifier() {
        // TODO port: use common hash algorithm across iOS and Android to avoid conflicts
        int permissionsHash = getPermissionsString().hashCode();
        int machineTagHash = (machineUniqueUserTag != null) ? machineUniqueUserTag.hashCode() : 0;
        int sessionTagHash = (sessionUniqueUserTag != null) ? sessionUniqueUserTag.hashCode() : 0;

        int combinedHash = permissionsHash ^ machineTagHash ^ sessionTagHash;
        return validNameStringFromInteger(combinedHash);
    }

    private String validNameStringFromInteger(int i) {
        String s = Integer.toString(i);
        StringBuilder result = new StringBuilder("Perm");

        // We know each character is a digit. Convert it into a letter starting with 'a'.
        for (char c : s.toCharArray()) {
            result.append((char) (c + 'a' - '0'));
        }

        return result.toString();
    }

    private interface TestAccount extends GraphObject {
        String getId();

        String getAccessToken();

        // Note: We don't actually get Name from our FQL query. We fill it in by correlating with UserAccounts.
        String getName();

        void setName(String name);
    }

    private interface UserAccount extends GraphObject {
        String getUid();

        String getName();

        void setName(String name);
    }

    private interface FqlResult extends GraphObject {
        GraphObjectList<GraphObject> getFqlResultSet();

    }

    private interface FqlResponse extends GraphObject {
        GraphObjectList<FqlResult> getData();
    }

    private static final class TestTokenCache extends TokenCache {
        private Bundle bundle;

        @Override
        public Bundle load() {
            return bundle;
        }

        @Override
        public void save(Bundle value) {
            bundle = value;
        }

        @Override
        public void clear() {
            bundle = null;
        }
    }
}
